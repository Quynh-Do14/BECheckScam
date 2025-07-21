package com.example.checkscamv2.component;

import com.example.checkscamv2.util.FileCacheUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheCleanupScheduler {

    @Scheduled(cron = "0 0 */6 * * *")
    public void cleanOldCache() {
        long startTime = System.currentTimeMillis();
        System.out.println("[CacheCleanup] Bắt đầu cleanup cache...");
        FileCacheUtil.clearCache();
        long endTime = System.currentTimeMillis();
        System.out.println("Cleanup cache hoàn thành trong " + (endTime - startTime) + "ms");
        System.out.println("Cache size hiện tại: " + FileCacheUtil.getCacheSize());
        System.out.println("Memory usage: " + FileCacheUtil.getCacheMemoryUsage() + " bytes");
    }
}
