package ru.hh.school.homework;

import java.util.Random;

public class MockSearchEngine {
    private static final Random rnd = new Random();

    public static Long mockSearch(String query) {
        try {
            Thread.sleep(1000);
            return rnd.nextLong(500_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
