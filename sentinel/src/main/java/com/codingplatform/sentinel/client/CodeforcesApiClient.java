package com.codingplatform.sentinel.client;

import com.codingplatform.sentinel.dto.CodeforcesResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

@Service
public class CodeforcesApiClient {

    private static final Logger log = LoggerFactory.getLogger(CodeforcesApiClient.class);
    private final RestClient restClient;

    public CodeforcesApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("https://codeforces.com/api").build();
    }

    @CircuitBreaker(name = "codeforces-api", fallbackMethod = "fallbackGetSubmissions")
    public List<CodeforcesResponse.CfSubmission> getRecentSubmissions(String handle) {
        log.info("Fetching submissions for handle: {}", handle);

        CodeforcesResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/user.status")
                        .queryParam("handle", handle)
                        .queryParam("from", 1)
                        .queryParam("count", 10)
                        .build())
                .retrieve()
                .body(CodeforcesResponse.class);

        if (response != null && "OK".equals(response.status()) && response.result() != null) {
            return response.result();
        }

        return Collections.emptyList();
    }

    public List<CodeforcesResponse.CfSubmission> fallbackGetSubmissions(String handle, Throwable t) {
        log.warn("⚠️ Codeforces API unreachable for {}. Reason: {}", handle, t.getMessage());
        return Collections.emptyList();
    }
}