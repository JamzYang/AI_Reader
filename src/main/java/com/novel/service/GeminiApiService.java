package com.novel.service;

import com.novel.model.ApiRequest;
import com.novel.model.ApiResponse;

public interface GeminiApiService {
    /**
     * 调用Gemini API
     */
    ApiResponse callApi(ApiRequest request) throws Exception;
}
