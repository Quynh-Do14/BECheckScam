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
        return now - lastModified < 24 * 60 * 60 * 1000; // < 24 giờ
    }

    public static void ensureCacheDirExists() {
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public static void clearCache() {
        File dir = new File(CACHE_DIR);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }
}
