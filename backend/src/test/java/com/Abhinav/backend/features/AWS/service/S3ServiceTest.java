package com.Abhinav.backend.features.AWS.service;

import com.Abhinav.backend.features.judge0.service.Judge0Service;
import com.Abhinav.backend.features.problem.model.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ObjectMapper objectMapper;

    private S3Service s3Service;

    private final UUID problemId = UUID.randomUUID();
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        s3Service = new S3Service(s3Presigner, s3Client, objectMapper, redisTemplate);

        ReflectionTestUtils.setField(s3Service, "bucketName", "testcase-bucket");
        ReflectionTestUtils.setField(s3Service, "testCaseCacheTtlMinutes", 30L);
        ReflectionTestUtils.setField(s3Service, "maxUploadSizeKb", 300L);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        testProblem = Problem.builder()
                .id(problemId)
                .title("Sample Problem")
                .sampleTestCases("[{\"stdin\":\"1 2\",\"expected_output\":\"3\"}]")
                .hiddenTestCasesS3Key("problems/" + problemId + "/testcases.zip")
                .build();
    }

    private byte[] createSampleZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("test1.in"));
            zos.write("10 20".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("test1.out"));
            zos.write("30".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should return cached test cases on initial CACHE HIT without acquiring lock or calling S3")
    void getOrFetchAllTestCases_CacheHit() {
        String cacheKey = "testcases:problem:" + problemId;
        List<Judge0Service.TestCase> cachedCases = List.of(
                new Judge0Service.TestCase("1 2", "3"),
                new Judge0Service.TestCase("10 20", "30")
        );

        when(valueOperations.get(cacheKey)).thenReturn(cachedCases);

        List<Judge0Service.TestCase> result = s3Service.getOrFetchAllTestCases(testProblem);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).input()).isEqualTo("1 2");
        assertThat(result.get(1).input()).isEqualTo("10 20");

        verify(valueOperations, times(1)).get(cacheKey);
        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Should fetch from sample and S3 on CACHE MISS, store in Redis, and return test cases")
    void getOrFetchAllTestCases_CacheMiss_FetchesAndCaches() throws IOException {
        String cacheKey = "testcases:problem:" + problemId;
        byte[] zipBytes = createSampleZip();

        when(valueOperations.get(cacheKey)).thenReturn(null);

        GetObjectResponse response = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> responseInputStream =
                new ResponseInputStream<>(response, new ByteArrayInputStream(zipBytes));

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        List<Judge0Service.TestCase> result = s3Service.getOrFetchAllTestCases(testProblem);

        assertThat(result).hasSize(2); // 1 sample + 1 hidden
        assertThat(result.get(0).input()).isEqualTo("1 2");
        assertThat(result.get(0).expectedOutput()).isEqualTo("3");
        assertThat(result.get(1).input()).isEqualTo("10 20");
        assertThat(result.get(1).expectedOutput()).isEqualTo("30");

        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
        verify(valueOperations, times(1)).set(eq(cacheKey), eq(result), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("Should return test cases on double-check if another thread populated cache while waiting for lock")
    void getOrFetchAllTestCases_DoubleCheckHit() {
        String cacheKey = "testcases:problem:" + problemId;
        List<Judge0Service.TestCase> cachedCases = List.of(
                new Judge0Service.TestCase("1 2", "3")
        );

        // First call (initial check before lock) returns null; second call (double-check after lock) returns cachedCases
        when(valueOperations.get(cacheKey))
                .thenReturn(null)
                .thenReturn(cachedCases);

        List<Judge0Service.TestCase> result = s3Service.getOrFetchAllTestCases(testProblem);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).input()).isEqualTo("1 2");

        verify(valueOperations, times(2)).get(cacheKey);
        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("Concurrent requests for same problem: only ONE thread fetches from S3 (preventing cache stampede)")
    void getOrFetchAllTestCases_ConcurrentRequests_OnlyOneThreadCallsS3() throws Exception {
        String cacheKey = "testcases:problem:" + problemId;
        byte[] zipBytes = createSampleZip();

        AtomicInteger s3CallCount = new AtomicInteger(0);
        AtomicReference<List<Judge0Service.TestCase>> inMemoryCache = new AtomicReference<>(null);

        // Simulate Redis get: returns whatever is in inMemoryCache
        when(valueOperations.get(cacheKey)).thenAnswer(inv -> inMemoryCache.get());

        // Simulate Redis set: saves into inMemoryCache
        doAnswer(inv -> {
            List<Judge0Service.TestCase> cases = inv.getArgument(1);
            inMemoryCache.set(cases);
            return null;
        }).when(valueOperations).set(eq(cacheKey), any(), anyLong(), any());

        // Simulate S3 download with artificial latency to provoke concurrency race conditions
        when(s3Client.getObject(any(GetObjectRequest.class))).thenAnswer(inv -> {
            s3CallCount.incrementAndGet();
            Thread.sleep(50); // simulate slow S3 fetch
            GetObjectResponse response = GetObjectResponse.builder().build();
            return new ResponseInputStream<>(response, new ByteArrayInputStream(zipBytes));
        });

        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numThreads);
        List<Future<List<Judge0Service.TestCase>>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executorService.submit(() -> {
                startGate.await(); // wait until all threads are ready
                try {
                    return s3Service.getOrFetchAllTestCases(testProblem);
                } finally {
                    endGate.countDown();
                }
            }));
        }

        // Release all threads simultaneously
        startGate.countDown();
        boolean completedInTime = endGate.await(5, TimeUnit.SECONDS);
        assertThat(completedInTime).isTrue();

        // Verify all 10 threads obtained the identical valid test cases
        for (Future<List<Judge0Service.TestCase>> future : futures) {
            List<Judge0Service.TestCase> result = future.get();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).input()).isEqualTo("1 2");
            assertThat(result.get(1).input()).isEqualTo("10 20");
        }

        // CRUCIAL: S3 must have been called EXACTLY ONCE despite 10 concurrent requests!
        assertThat(s3CallCount.get()).isEqualTo(1);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
        verify(valueOperations, times(1)).set(eq(cacheKey), any(), eq(30L), eq(TimeUnit.MINUTES));

        executorService.shutdown();
    }
}
