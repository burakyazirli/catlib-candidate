# CatLib

This is the completed version of the CatLib take-home engineering task.

## What this app does

CatLib is a small Spring Boot REST API that integrates with two external services:

- CATAAS for cat images
- Open Library for book metadata

The application keeps the original CATAAS endpoint and adds a new workflow where a topic is used to retrieve content from both APIs, store the results locally, and return a summary of previously stored content.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/cat/{tag}` | Fetches a cat image from CATAAS using the given tag |
| POST | `/api/content/{topic}` | Fetches cat and book data for the topic and stores the result locally |
| GET | `/api/content/summary` | Returns a summary of all locally stored content |

---

## Get a Cat by Tag

The original endpoint is still available.

### Example

```http
GET http://localhost:8080/api/cat/space
```

### Response

```json
{
  "tag": "space",
  "imageUrl": "https://cataas.com/cat/7oTjTbSAhqeQCl7O"
}
```

The application sends the supplied tag to CATAAS and returns the resulting image URL.

---

## Fetch and Store Topic Content

This endpoint uses the supplied topic to retrieve information from both CATAAS and Open Library.

### Example

```http
POST http://localhost:8080/api/content/space
```

For the supplied topic, the application:

1. Requests a matching cat from CATAAS.
2. Requests the first matching book from Open Library.
3. Downloads the cat image.
4. Creates a local storage directory for the request.
5. Stores the image as `cat.jpg`.
6. Stores the Open Library metadata as `metadata.json`.
7. Returns the combined result.

### Response

```json
{
  "topic": "space",
  "cat": {
    "tag": "space",
    "imageUrl": "https://cataas.com/cat/7oTjTbSAhqeQCl7O"
  },
  "book": {
    "key": "/works/OL5724837W",
    "title": "Revelation Space",
    "author_name": [
      "Alastair Reynolds",
      "John Lee"
    ],
    "first_publish_year": 2000
  }
}
```

The Open Library request uses `limit=1` because only one metadata record is required for each stored topic request.

---

## Get Stored Content Summary

The application can inspect the local storage directory and return a summary of everything stored so far.

### Example

```http
GET http://localhost:8080/api/content/summary
```

### Response

```json
{
  "total": 1,
  "items": [
    {
      "folderName": "space-1789917644312",
      "imageStored": true,
      "metadataStored": true,
      "book": {
        "key": "/works/OL5724837W",
        "title": "Revelation Space",
        "author_name": [
          "Alastair Reynolds",
          "John Lee"
        ],
        "first_publish_year": 2000
      }
    }
  ]
}
```

The summary endpoint also tolerates incomplete storage directories.

For example, if an image exists but the metadata file is missing, the stored item can still be included in the summary without causing the entire request to fail.

---

## Local Storage

Stored content is written to the configured local storage directory.

The default directory is:

```text
downloads/
```

Each request creates a separate timestamped directory.

Example:

```text
downloads/
└── space-1789917644312/
    ├── cat.jpg
    └── metadata.json
```

The timestamp prevents repeated requests for the same topic from overwriting previous results.

The topic is also sanitized before it is used as part of the directory name.

---

## Project Structure

The main source code is organized as follows:

```text
src/main/java/com/example/catlib/
├── config/
├── controller/
├── exception/
├── model/
└── service/
```

### Main Components

#### CatController

Handles the original CATAAS endpoint:

```text
GET /api/cat/{tag}
```

#### TopicContentController

Handles the new combined content and storage endpoints:

```text
POST /api/content/{topic}
GET  /api/content/summary
```

#### CatService

Responsible for communicating with CATAAS.

It:

- Builds and encodes the request URL
- Sends the HTTP request
- Validates the external HTTP status
- Parses the returned cat ID
- Creates the final image URL
- Handles connection and external API failures

#### OpenLibraryService

Responsible for communicating with Open Library.

It:

- Searches Open Library using the supplied topic
- Limits the search to one result
- Parses the response into application models
- Handles external HTTP and connection failures

#### TopicContentService

Coordinates the main workflow.

It:

- Fetches cat information
- Fetches book metadata
- Calls the storage service
- Returns the combined response

#### StorageService

Responsible for local persistence.

It:

- Downloads the cat image
- Creates timestamped storage directories
- Writes the image as `cat.jpg`
- Writes Open Library metadata as `metadata.json`
- Reads stored directories for the summary endpoint

#### GlobalExceptionHandler

Handles external API exceptions and converts them into structured HTTP responses.

---

## Error Handling

External API failures are handled using `ExternalApiException` and `GlobalExceptionHandler`.

Handled cases include:

- CATAAS returning a non-successful HTTP status
- Open Library returning a non-successful HTTP status
- Network connection failures
- SSL/network failures
- Interrupted HTTP requests
- CATAAS returning a response without a cat ID
- Cat image download failures

External dependency failures are generally returned as:

```text
502 Bad Gateway
```

A `404` response from an external API can be returned as:

```text
404 Not Found
```

### Example Error Response

```json
{
  "timestamp": "2026-09-19T04:19:43.0310476",
  "status": 404,
  "message": "CATAAS request failed"
}
```

---

## Requirements

- Java 17+
- Maven 3.8+

The application was developed and tested using Java 17.

---

## Running the App

From the project root, using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

On Windows, if Maven is installed:

```powershell
mvn spring-boot:run
```

The application starts on port `8080` by default.

```text
http://localhost:8080
```

---

## Build the Application

Using the Maven wrapper:

```bash
./mvnw clean package
```

Or using an installed Maven version:

```powershell
mvn clean package
```

Then run the generated JAR:

```bash
java -jar target/catlib-0.0.1-SNAPSHOT.jar
```

---

## Running Tests

Using the Maven wrapper:

```bash
./mvnw test
```

Or:

```powershell
mvn test
```

The current test suite includes:

- Spring Boot application context test
- Unit test for `TopicContentService`

The `TopicContentService` test verifies that:

- Cat data is requested
- Book data is requested
- Image storage is called
- Metadata storage is called
- The expected combined response is returned

The project can also be clean-built and tested with:

```bash
mvn clean test
```

---

## Configuration

Application configuration is located in:

```text
src/main/resources/application.properties
```

Current configuration:

```properties
spring.application.name=catlib

server.port=8080

cataas.base-url=https://cataas.com
openlibrary.base-url=https://openlibrary.org

app.storage-dir=downloads
```

External API URLs and the storage directory are kept outside the Java classes so they can be changed without modifying application source code.

---

## External APIs

### CATAAS

CATAAS is used to retrieve cat information and download cat images.

No authentication is required.

https://cataas.com

### Open Library

Open Library is used to retrieve book metadata related to the supplied topic.

No authentication is required.

https://openlibrary.org/developers/api

---

## Generated Files

The following directories contain generated files:

```text
target/
downloads/
```

They are excluded from version control through `.gitignore`.

`target/` contains Maven build output.

`downloads/` contains the locally stored images and metadata created while the application is running.

---

## Manual API Testing

A `test.http` file is included in the project for manual endpoint testing.

Example requests:

```http
### Original CATAAS endpoint
GET http://localhost:8080/api/cat/space
Accept: application/json


### Fetch and store topic content
POST http://localhost:8080/api/content/space
Accept: application/json


### Stored content summary
GET http://localhost:8080/api/content/summary
Accept: application/json


### Error handling example
GET http://localhost:8080/api/cat/funny cat
Accept: application/json
```

These requests can be executed directly from VS Code when using the REST Client extension.

---

## Notes

- CATAAS and Open Library are external services, so requests depend on network availability.
- External API URLs are configurable through `application.properties`.
- Each content request creates a separate local storage directory.
- The local filesystem is intentionally used as the storage mechanism for this take-home task.
- More production-oriented storage, resilience, consistency and testing considerations are discussed in `SOLUTION.md`.