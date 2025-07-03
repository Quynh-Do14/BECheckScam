package com.example.checkscamv2.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;

@Component
public class CacheCleanupScheduler {

    @Scheduled(cron = "0 0 0 * * *")//giay phut gio ngaytrongthang(1-31) thang nam ngaytrongtuan(0-7)
    public void cleanOldCache() {
        File cacheDir = new File("cache/");
        long now = Instant.now().toEpochMilli();
        long expiry = 24 * 60 * 60 * 1000; // 24h

        if (cacheDir.exists() && cacheDir.isDirectory()) {
            for (File file : cacheDir.listFiles()) {
                if (now - file.lastModified() > expiry) {
                    file.delete();
                }
            }
        }
    }
}
