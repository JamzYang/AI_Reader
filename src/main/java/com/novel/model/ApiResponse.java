package com.novel.model;

public class ApiResponse {
    private final int statusCode;
    private final String content;
    private final String error;

    private ApiResponse(int statusCode, String content, String error) {
        this.statusCode = statusCode;
        this.content = content;
        this.error = error;
    }

    public static ApiResponse success(int statusCode, String content) {
        return new ApiResponse(statusCode, content, null);
    }

    public static ApiResponse error(int statusCode, String error) {
        return new ApiResponse(statusCode, null, error);
    }

    public boolean isSuccess() {
        return statusCode == 200 && error == null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getContent() {
        return content;
    }

    public String getError() {
        return error;
    }
}
