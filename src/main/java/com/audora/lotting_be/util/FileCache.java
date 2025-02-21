package com.audora.lotting_be.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileCache {
    private static final Map<String, File> cache = new ConcurrentHashMap<>();

    public static void put(String fileId, File file) {
        cache.put(fileId, file);
    }

    public static File get(String fileId) {
        return cache.get(fileId);
    }

    public static void remove(String fileId) {
        cache.remove(fileId);
    }
}
