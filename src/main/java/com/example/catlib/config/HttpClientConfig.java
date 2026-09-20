package com.example.catlib.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/*
 * Central configuration for the HTTP client used by external API services.
 *
 * Previously CatService created its own HttpClient instance:
 *
 *     HttpClient.newHttpClient()
 *
 * Instead, we let Spring create and manage a reusable HttpClient bean.
 * This also gives us one place to configure settings such as connection timeout.
 */

@Configuration
public class HttpClientConfig {

    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                // Prevents the application from waiting indefinitely
                // while trying to establish a connection to an external API.
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}