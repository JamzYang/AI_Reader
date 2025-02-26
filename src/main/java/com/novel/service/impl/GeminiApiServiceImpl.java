package com.novel.service.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.novel.model.ApiRequest;
import com.novel.model.ApiResponse;
import com.novel.service.GeminiApiService;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiApiServiceImpl implements GeminiApiService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiApiServiceImpl.class);
    private final String apiKey;
    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiApiServiceImpl(String apiKey) {
        this(apiKey, HttpClient.newHttpClient(), new GsonBuilder().setPrettyPrinting().create());
    }

    // 用于测试的构造函数，允许注入mock对象
    public GeminiApiServiceImpl(String apiKey, HttpClient httpClient, Gson gson) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    @Override
    public ApiResponse callApi(ApiRequest request) throws Exception {
        Exception lastException = null;
        
        for (int retry = 0; retry <= request.getRetryCount(); retry++) {
            if (retry > 0) {
                logger.warn("API调用失败，尝试重试 {}/{}", retry, request.getRetryCount());
                Thread.sleep(request.getRetryDelayMs());
            }

            try {
                HttpRequest httpRequest = buildRequest(request.getPrompt());
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return ApiResponse.success(response.statusCode(), response.body());
                } else {
                    lastException = new RuntimeException("API调用失败，状态码: " + response.statusCode() + 
                        ", 响应体: " + response.body());
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("API调用出错", e);
            }
        }

        return ApiResponse.error(500, lastException.getMessage());
    }

    private HttpRequest buildRequest(String prompt) {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                Constants.GEMINI_MODEL, apiKey);

        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
    }
}
