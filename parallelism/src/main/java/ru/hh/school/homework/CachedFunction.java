package ru.hh.school.homework;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class CachedFunction<T, R> implements Function<T, R> {
    private final Map<T, R> cache = new ConcurrentHashMap<>();
    private final Function<T, R> operation;

    public CachedFunction(Function<T, R> operation) {
        this.operation = operation;
    }

    @Override
    public R apply(T arg) {
        if (cache.containsKey(arg)) {
            return cache.get(arg);
        }
        var res = operation.apply(arg);
        cache.put(arg, res);
        return res;
    }
}
