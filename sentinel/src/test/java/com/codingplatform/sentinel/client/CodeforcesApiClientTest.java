package com.codingplatform.sentinel.client;

import com.codingplatform.sentinel.dto.CodeforcesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodeforcesApiClientTest {

    private CodeforcesApiClient apiClient;
    private MockRestServiceServer mockServer;



    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(builder).build();

        apiClient = new CodeforcesApiClient(builder);
    }


    @Test
    @DisplayName("Should successfully fetch and parse valid submissions")
    void testGetRecentSubmissions_Success() {
        String jsonResponse = """
            {
                "status": "OK",
                "result": [
                    {
                        "id": 100500,
                        "creationTimeSeconds": 1700000000,
                        "verdict": "OK",
                        "programmingLanguage": "C++ 20",
                        "timeConsumedMillis": 30,
                        "memoryConsumedBytes": 1024,
                        "passedTestCount": 10,
                        "problem": { 
                            "contestId": "1234", 
                            "index": "A", 
                            "name": "Watermelon", 
                            "type": "PROGRAMMING", 
                            "rating": 800 
                        }
                    }
                ]
            }
        """;

        String expectedUrl = "https://codeforces.com/api/user.status?handle=tourist&from=1&count=10";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<CodeforcesResponse.CfSubmission> submissions = apiClient.getRecentSubmissions("tourist");

        assertThat(submissions).hasSize(1);

        CodeforcesResponse.CfSubmission sub = submissions.get(0);
        assertThat(sub.id()).isEqualTo(100500L);
        assertThat(sub.verdict()).isEqualTo("OK");
        assertThat(sub.programmingLanguage()).isEqualTo("C++ 20");
        assertThat(sub.problem().name()).isEqualTo("Watermelon");

        mockServer.verify();
    }


    @Test
    @DisplayName("🛡️ Should return empty list if API status is not OK")
    void testGetRecentSubmissions_ApiFailed() {
        String jsonResponse = """
            {
                "status": "FAILED",
                "result": null
            }
        """;

        String expectedUrl = "https://codeforces.com/api/user.status?handle=unknown&from=1&count=10";

        mockServer.expect(requestTo(expectedUrl))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<CodeforcesResponse.CfSubmission> result = apiClient.getRecentSubmissions("unknown");

        assertThat(result).isEmpty();
        mockServer.verify();
    }


    @Test
    @DisplayName("🛡️ Fallback method should return empty list on exception")
    void testFallbackGetSubmissions() {
        String handle = "tourist";
        Throwable error = new RuntimeException("Codeforces is down");

        List<CodeforcesResponse.CfSubmission> result = apiClient.fallbackGetSubmissions(handle, error);

        assertThat(result).isEmpty();
    }
}