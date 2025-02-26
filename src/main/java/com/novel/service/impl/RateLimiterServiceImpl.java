package com.novel.service.impl;

import com.novel.service.RateLimiterService;
import com.novel.util.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiterServiceImpl implements RateLimiterService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterServiceImpl.class);
    private final AtomicInteger requestCounter;
    private final ScheduledExecutorService scheduler;

    public RateLimiterServiceImpl(ScheduledExecutorService scheduler) {
        this.requestCounter = new AtomicInteger(0);
        this.scheduler = scheduler;
    }

    @Override
    public void acquire() throws InterruptedException {
        int currentCount = requestCounter.incrementAndGet();
        
        if (currentCount > Constants.MAX_REQUESTS_PER_MINUTE) {
            logger.info("已达到每分钟请求限制，等待下一个时间窗口...");
            
            CountDownLatch latch = new CountDownLatch(1);
            scheduler.schedule(() -> {
                requestCounter.set(1);
                latch.countDown();
            }, 60, TimeUnit.SECONDS);
            
            latch.await();
        }
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
    }
}
