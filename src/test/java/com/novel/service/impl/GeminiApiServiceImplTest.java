package com.novel.service.impl;

import com.google.gson.Gson;
import com.novel.model.ApiRequest;
import com.novel.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class GeminiApiServiceImplTest {
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse<String> httpResponse;
    @Mock
    private Gson gson;

    private GeminiApiServiceImpl apiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        apiService = new GeminiApiServiceImpl("test-api-key", httpClient, gson);
    }

    @Test
    void testCallApiSuccess() throws Exception {
        // 准备测试数据
        String responseBody = "{\"success\":true,\"content\":\"test content\"}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(responseBody);
//        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        // 执行测试
        ApiRequest request = new ApiRequest("test prompt", 3, 1000);
        ApiResponse response = apiService.callApi(request);

        // 验证结果
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertEquals(responseBody, response.getContent());
    }

    @Test
    void testCallApiFailure() throws Exception {
        // 准备测试数据
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("error message");
//        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        // 执行测试
        ApiRequest request = new ApiRequest("test prompt", 0, 1000);
        ApiResponse response = apiService.callApi(request);

        // 验证结果
        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getError());
    }

    @Test
    void testCallApiWithRetry() throws Exception {
        // 准备测试数据
        when(httpResponse.statusCode())
            .thenReturn(500)  // 第一次失败
            .thenReturn(200); // 第二次成功
        when(httpResponse.body())
            .thenReturn("error")
            .thenReturn("success");
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        // 执行测试
        ApiRequest request = new ApiRequest("test prompt", 3, 100);
        ApiResponse response = apiService.callApi(request);

        // 验证结果
        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertEquals("success", response.getContent());
    }
}
