package com.example.checkscamv2.util;

import java.io.File;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

public class FileCacheUtil {

    private static final String CACHE_DIR = "cache/";

    public static String getCacheFilename(String url) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(url.getBytes());
        String hex = HexFormat.of().formatHex(hash);
        return CACHE_DIR + hex + ".png";
    }

    public static boolean isFileValid(String filename) {
        File file = new File(filename);
        if (!file.exists()) return false;
        long lastModified = file.lastModified();
        long now = Instant.now().toEpochMilli();
        return now - lastModified < 24 * 60 * 60 * 1000;
    }

    public static void ensureCacheDirExists() {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    // Xóa cache cũ hơn 24h và trả về số file đã xóa
    public static int clearCache() {
        File dir = new File(CACHE_DIR);
        long now = System.currentTimeMillis();
        long expiry = 24 * 60 * 60 * 1000;
        int deleted = 0;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".png")) {
                        if (now - file.lastModified() > expiry) {
                            if (file.delete()) deleted++;
                        }
                    }
                }
            }
        }
        return deleted;
    }

    // Đếm số file cache hiện tại
    public static int getCacheSize() {
        File dir = new File(CACHE_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
            return files != null ? files.length : 0;
        }
        return 0;
    }

    // Tổng dung lượng file cache hiện tại
    public static long getCacheMemoryUsage() {
        File dir = new File(CACHE_DIR);
        long total = 0;
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".png"));
            if (files != null) {
                for (File file : files) {
                    total += file.length();
                }
            }
        }
        return total;
    }
}
