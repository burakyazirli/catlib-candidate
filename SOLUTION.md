# SOLUTION

## 1. What did the existing code do?

The starter project was a small Spring Boot REST API built around CATAAS.

It exposed one endpoint:

```http
GET /api/cat/{tag}
```

The request was received by `CatController` and passed to `CatService`.

`CatService` then:

1. Built a CATAAS request using the supplied tag.
2. Sent the request using Java's `HttpClient`.
3. Parsed the returned JSON using Jackson's `ObjectMapper`.
4. Extracted the cat ID from the response.
5. Built the final image URL.
6. Returned a `CatResponse` containing the original tag and the image URL.

So, at a high level, the starter application was a small wrapper around the CATAAS API.

The basic happy path worked, but the service contained some implementation details that were tightly coupled and several failure cases were not handled explicitly.

---

## 2. What did you find wrong with it, and what did you do about it?

I first ran the existing application and tested the original endpoint before changing anything.

After reviewing the starter code, I made several changes.

### Hardcoded external API URL

The CATAAS base URL was hardcoded inside the Java service.

That means changing the external API address would require modifying Java code and rebuilding the application.

I moved the URL into `application.properties`:

```properties
cataas.base-url=https://cataas.com
```

I followed the same approach for Open Library:

```properties
openlibrary.base-url=https://openlibrary.org
```

The values are injected into the services using Spring configuration.

---

### Services created their own dependencies

The original `CatService` created its own `HttpClient` and `ObjectMapper`.

I changed this to constructor dependency injection.

A shared `HttpClient` is configured as a Spring bean, while Spring's `ObjectMapper` bean is injected into the services.

This keeps dependency creation outside the business logic and also makes the services easier to test.

---

### URL construction was based on string concatenation

The original CATAAS URL was constructed manually.

That can become a problem when a tag contains spaces or other characters that need URL encoding.

I changed the implementation to use `UriComponentsBuilder`.

For example, a topic such as:

```text
funny cat
```

is correctly encoded when the external request is created.

---

### External HTTP status codes were not checked

The original implementation expected a successful response from CATAAS.

If the external API returned an error, the code could still continue and try to parse the body as if it were a valid response.

I added explicit checks for non-2xx HTTP responses.

Those failures are converted into an `ExternalApiException`.

---

### Missing CATAAS response data was not validated

The application expected every successful CATAAS response to contain a cat ID.

I added a check before constructing the final image URL.

If the returned ID is missing or blank, the application now treats the external response as invalid instead of returning an incorrect URL.

---

### External network errors were not handled consistently

During development I encountered a real SSL/network error while calling Open Library:

```text
javax.net.ssl.SSLException
```

At that point the exception escaped from the HTTP client and resulted in a generic `500 Internal Server Error`.

I added handling for network-level failures around the external HTTP calls.

`IOException` failures are translated into controlled external-service errors, and interrupted HTTP requests restore the thread interrupt flag before returning an error.

The application generally maps external dependency failures to:

```text
502 Bad Gateway
```

---

### Error responses were inconsistent

I added:

- `ExternalApiException`
- `ApiError`
- `GlobalExceptionHandler`

This gives the application one place to convert external API failures into structured HTTP responses.

An example response is:

```json
{
  "timestamp": "2026-09-19T04:19:43.0310476",
  "status": 404,
  "message": "CATAAS request failed"
}
```

For this small project I intentionally kept the exception structure simple instead of introducing many specialized exception classes.

---

### Logging

I added lightweight logging around calls to external APIs.

For example:

- outgoing CATAAS requests
- outgoing Open Library requests
- external HTTP failures
- important connection failures

I avoided adding logs to every method because I did not think that would add useful information for an application of this size.

---

## 3. Walk us through the decisions you made that weren't specified

Several implementation details were intentionally left open in the task.

### Package and service structure

I kept responsibilities separated into a small number of Spring components:

```text
config/
controller/
exception/
model/
service/
```

The main services have different responsibilities:

- `CatService` handles CATAAS communication.
- `OpenLibraryService` handles Open Library communication.
- `TopicContentService` coordinates the two external services.
- `StorageService` handles local storage and stored-content summaries.

I preferred this over putting API communication, storage and orchestration into one large service.

---

### Open Library integration

I used Open Library's search endpoint:

```text
/search.json?q={topic}&limit=1
```

A broad topic can return a very large result set, but the task only requires metadata to be stored alongside the image.

For that reason I request only the first matching result.

I also chose not to model the entire Open Library response.

The application currently uses:

```text
key
title
author_name
first_publish_year
```

These were enough for the task and kept the model small.

---

### Main endpoint method

The main endpoint is:

```http
POST /api/content/{topic}
```

I chose `POST` because this operation changes application state by writing files to local storage.

During development the endpoint was initially tested as a `GET`, but I changed it after considering the side effect of creating stored data.

---

### Main workflow

The request flow is:

```text
topic
  |
  +----> CatService
  |        |
  |        +----> CATAAS
  |                  |
  |                  +----> cat image URL
  |
  +----> OpenLibraryService
           |
           +----> Open Library
                      |
                      +----> book metadata

cat + book
    |
    v
TopicContentService
    |
    v
StorageService
    |
    +----> cat.jpg
    +----> metadata.json
```

`TopicContentService` is deliberately small and acts as the orchestration layer rather than performing HTTP or filesystem work itself.

---

### Local storage layout

I chose the following directory structure:

```text
downloads/
└── <topic>-<timestamp>/
    ├── cat.jpg
    └── metadata.json
```

For example:

```text
downloads/
└── space-1789917644312/
    ├── cat.jpg
    └── metadata.json
```

I used a timestamp so multiple requests for the same topic do not overwrite previous results.

Each request therefore becomes an independent stored record.

---

### Topic sanitization

The topic comes from the request, so I did not use the raw value directly as a filesystem path.

Before it is included in the directory name, characters outside a small safe set are replaced.

This avoids problems with characters such as path separators and makes the storage directory safer to create.

---

### Metadata format

I chose JSON for the metadata file because:

- the task explicitly suggests JSON
- Open Library already returns JSON
- Jackson is already part of the application
- it is easy to inspect manually
- it can easily be parsed again by the summary endpoint

The metadata is written using Jackson with pretty printing.

---

### Summary endpoint

I implemented:

```http
GET /api/content/summary
```

`StorageService` scans the storage directory and creates a summary of each stored item.

For every directory the response includes:

- folder name
- whether the image exists
- whether the metadata exists
- book metadata when available

I deliberately made the summary tolerant of incomplete directories.

During development, an earlier version of the application had already created a directory containing an image but no metadata file.

Instead of allowing one incomplete record to break the complete summary request, the application reports the state of that directory.

---

### Configuration

I kept environment-dependent values outside the services:

```properties
cataas.base-url=https://cataas.com
openlibrary.base-url=https://openlibrary.org
app.storage-dir=downloads
```

The `HttpClient` is also configured centrally as a Spring bean.

This makes the application easier to configure without changing Java code.

---

### Error handling

I chose one small application exception for external API problems:

```text
ExternalApiException
```

and one global handler:

```text
GlobalExceptionHandler
```

A `404` returned by the external service can be represented as `404 Not Found`.

Other external dependency failures are generally exposed as:

```text
502 Bad Gateway
```

I considered introducing more exception types, but I felt it would add more structure than this small project currently needs.

---

### Tests

I kept the existing Spring context test and added a unit test for `TopicContentService`.

The unit test uses Mockito rather than real external APIs.

It verifies that the orchestration layer:

1. requests cat data
2. requests book data
3. stores the image
4. stores the metadata
5. returns the expected combined result

I also used a `test.http` file to manually exercise the real endpoints while developing the application.

The final project was verified with:

```bash
mvn clean test
```

and the tests completed successfully.

---

## 4. Which parts did you use AI tools for? What did you prompt, what did it give you, and did you change anything?

I used ChatGPT throughout the task as a development assistant.

I did not ask it to generate the entire solution in one step.

Instead, I used it interactively while implementing and testing each part of the project.

I used AI mainly for:

- reviewing the existing service design
- discussing problems in the starter code
- discussing how to configure a reusable `HttpClient`
- suggesting safer URL construction
- helping understand Spring startup errors
- discussing external API error handling
- creating initial model class structures
- discussing the local storage layout
- reviewing controller and service responsibilities
- helping create the Mockito unit test
- cleaning up code formatting
- reviewing the README
- helping structure this `SOLUTION.md`

Typical prompts were similar to:

```text
Explain what is wrong with this service and why.
```

```text
How should I build this URL safely when the topic contains spaces?
```

```text
Why is Spring failing to start with this configuration property?
```

```text
How should I structure the Open Library response without modelling every field?
```

```text
What would be a simple local storage structure for this task?
```

The suggestions were not treated as correct automatically.

I applied changes incrementally and repeatedly:

- compiled the project
- started the application
- sent real HTTP requests
- inspected filesystem output
- inspected logs
- reproduced error cases
- adjusted the implementation

I also changed or rejected some AI suggestions when they seemed unnecessary for the scope of the task.

---

## 5. What did the AI get wrong or miss, if anything?

There were several cases where the AI suggestions needed correction or simplification.

### The main endpoint initially used GET

During the first implementation, the combined endpoint was temporarily exposed as:

```http
GET /api/content/{topic}
```

That works technically, but it is not a good HTTP design because the request creates files and changes application state.

I later changed it to:

```http
POST /api/content/{topic}
```

This should have been identified earlier.

---

### Network-level errors were initially missed

The first external API error handling focused mostly on HTTP status codes.

During actual testing, Open Library produced an SSL/network exception.

The error was:

```text
javax.net.ssl.SSLException
```

and originally resulted in a generic HTTP 500 response.

That real failure showed that checking response status codes was not enough.

The external HTTP calls were then updated to also handle connection failures and interrupted requests.

---

### Some suggestions were more complicated than necessary

At one point the discussion moved toward more detailed exception handling and additional abstractions.

For this take-home application I decided not to introduce:

- a large custom exception hierarchy
- retry frameworks
- circuit breakers
- persistence repositories
- unnecessary interfaces around every service

Those could be useful in a larger production application, but I did not want to over-engineer this task.

---

### Temporary development code was not part of the final design

While developing the Open Library integration, I created a temporary endpoint to test Open Library separately.

That was useful while building the feature but no longer served a purpose after the combined endpoint was complete.

I removed the temporary controller before finalizing the project.

---

### Suggested comments were sometimes too verbose

While learning and changing the starter project I temporarily kept old versions of code commented out and added detailed comments.

That was useful during development, but it made the final code noisy.

Before finishing the project I removed the old commented code and kept only comments that added useful context.

---

### AI output still required manual verification

The main lesson was that AI suggestions were useful for speed and discussion, but the actual application behavior still needed to be tested.

Compilation, runtime tests, filesystem inspection and stack traces exposed issues that would not have been caught by simply accepting generated code.

---

## 6. The storage is local for now. How would you approach making it production-ready?

The current filesystem storage is intentionally simple because the assignment asks for local storage.

I would change several things for a production application.

### Separate binary and structured storage

I would not store images on the application's local filesystem.

Images would normally be stored in object storage such as:

- Amazon S3
- Azure Blob Storage
- Google Cloud Storage

The application would store the object key or URL instead of relying on a local file path.

---

### Store metadata in a database

Structured metadata would be stored in a database such as PostgreSQL.

For example, a record could contain:

```text
id
topic
book_key
book_title
authors
first_publish_year
image_object_key
created_at
status
```

The summary endpoint could then query the database rather than scanning the filesystem.

---

### Introduce stable identifiers

The current implementation identifies stored entries using a directory containing the topic and timestamp.

For production I would generate a proper record ID, such as a UUID.

The topic and creation timestamp would then be normal metadata fields rather than part of the record identity.

---

### Handle partial operations

The current storage operation is not transactional.

For example:

1. the image could be downloaded successfully
2. the image could be written to disk
3. writing metadata could fail

That could leave an incomplete record.

For production I would introduce an explicit record state such as:

```text
PENDING
COMPLETED
FAILED
```

or write all local output to a temporary location and only expose the completed record when all required operations succeed.

---

### Validate downloaded content

The current implementation stores the downloaded image as:

```text
cat.jpg
```

For a production implementation I would inspect:

- `Content-Type`
- image format
- maximum response size
- file size
- allowed media types

I would either preserve the correct extension or convert the image into a known format.

---

### External service resilience

For production I would consider:

- connect timeouts
- request timeouts
- limited retries
- exponential backoff
- circuit breakers
- rate-limit handling

A library such as Resilience4j could be introduced if the application's scale justified it.

Retries should only be used for failures that are likely to be transient.

---

### Observability

I would also add:

- structured logs
- request/correlation IDs
- health checks
- monitoring and alerting

This would make external service problems much easier to diagnose.

---

### Deployment configuration

Environment-dependent configuration would be provided through environment variables or deployment configuration rather than relying on one local properties file.

Secrets, if any external services later required them, would be stored using an appropriate secret-management solution.

---

## 7. What's the weakest part of your solution? What would you do differently with more time?

The weakest part of the current solution is the local persistence workflow.

It satisfies the task, but filesystem operations are not transactional.

The current sequence is roughly:

```text
create directory
download image
write image
write metadata
```

If something fails in the middle, an incomplete directory can remain.

The summary endpoint tolerates this, but tolerating incomplete data is not the same as guaranteeing storage consistency.

With more time I would improve this first.

---

### More precise exception handling

Some service methods still use broad:

```java
throws Exception
```

signatures.

With more time I would replace those with more specific application exceptions and filesystem exceptions.

That would make each method's contract clearer and allow more precise HTTP responses.

---

### Open Library no-result behavior

The application currently selects the first Open Library result.

If Open Library returns no matching documents, this behavior should be defined more explicitly.

For example, the application could either:

- return a controlled `404`
- store the cat without book metadata
- reject the whole operation

The correct choice depends on the desired product behavior.

I would define that behavior explicitly and add tests for it.

---

### Input validation

I would add explicit validation for the topic.

For example:

- blank topics
- very long topics
- unsupported characters
- path-like values

The filesystem name is already sanitized, but validating the API input itself would provide clearer feedback to callers.

---

### More automated tests

The current automated tests cover:

- Spring application startup
- the central orchestration flow

With more time I would add tests for:

- CATAAS non-2xx responses
- Open Library non-2xx responses
- external connection failures
- interrupted requests
- missing CATAAS IDs
- Open Library returning zero results
- image download failures
- filesystem failures
- incomplete stored records
- corrupted metadata
- summary endpoint behavior
- controller-level error responses

I would also use a mock HTTP server for integration tests so external-client behavior could be tested deterministically without depending on the real public APIs.

---

### Parallel external requests

The CATAAS and Open Library calls do not depend on each other.

They are currently made sequentially because it keeps the implementation simple and easy to understand.

For a higher-throughput production service, I would consider performing these independent external requests concurrently and then combining their results.

I would only make that change after measuring whether the added complexity provided a meaningful latency improvement.

---

## Final Thoughts

My main goal was to keep the implementation proportional to the assignment.

I wanted to fix the obvious problems in the starter application, introduce the required second API and storage layer, and separate responsibilities without turning a small Spring Boot application into an unnecessarily complex architecture.

The final application:

- preserves the original CATAAS functionality
- integrates Open Library
- accepts a topic through a dedicated endpoint
- retrieves content from both APIs
- downloads the cat image
- stores the image locally
- stores Open Library metadata as JSON
- prevents repeated requests from overwriting previous records
- provides a stored-content summary endpoint
- handles common external API and network failures
- uses configurable external service URLs
- uses Spring dependency injection
- contains automated tests
- contains manual REST requests for verification

With additional time, I would focus first on storage consistency, more precise error handling, input validation and broader automated test coverage.