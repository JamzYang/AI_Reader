package com.novel.service;

public interface RateLimiterService {
    /**
     * 获取令牌，如果超过限制则等待
     */
    void acquire() throws InterruptedException;

    /**
     * 关闭限流器
     */
    void shutdown();
}
