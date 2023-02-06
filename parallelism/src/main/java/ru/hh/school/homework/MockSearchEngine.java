package ru.hh.school.homework;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MockSearchEngine {
    private static final Random rnd = new Random();
    private static final Map<String, Long> SEARCH_CACHE = new ConcurrentHashMap<>();
    public static Long mockSearch(String query) {
        try {
            Thread.sleep(1000);
            return rnd.nextLong(500_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
