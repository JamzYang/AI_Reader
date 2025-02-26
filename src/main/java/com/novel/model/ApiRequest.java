package com.novel.model;

public class ApiRequest {
    private final String prompt;
    private final int retryCount;
    private final long retryDelayMs;

    public ApiRequest(String prompt, int retryCount, long retryDelayMs) {
        this.prompt = prompt;
        this.retryCount = retryCount;
        this.retryDelayMs = retryDelayMs;
    }

    public String getPrompt() {
        return prompt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }
}
