package com.senacor.elasticsearch.evolution.core.internal.utils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Andreas Keefer
 */
public class AssertionUtils {

    private AssertionUtils() {
    }

    public static String requireNotEmpty(String obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).isEmpty()) {
            throw new IllegalStateException(message.formatted(args));
        }
        return obj;
    }

    public static String requireNotBlank(String obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).trim().isEmpty()) {
            throw new IllegalStateException(message.formatted(args));
        }
        return obj;
    }

    public static Collection<?> requireNotEmpty(Collection<?> obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).isEmpty()) {
            throw new IllegalStateException(message.formatted(args));
        }
        return obj;
    }

    public static <T> T requireCondition(T value, Predicate<T> predicate, String message, Object... args) {
        if (!predicate.test(value)) {
            throw new IllegalStateException(message.formatted(args));
        }
        return value;
    }
}
