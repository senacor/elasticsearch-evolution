package com.senacor.elasticsearch.evolution.core.internal.utils;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Andreas Keefer
 */
public class AssertionUtils {

    public static String requireNotEmpty(String obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).isEmpty()) {
            throw new IllegalStateException(String.format(message, args));
        }
        return obj;
    }

    public static String requireNotBlank(String obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).trim().isEmpty()) {
            throw new IllegalStateException(String.format(message, args));
        }
        return obj;
    }

    public static Collection<?> requireNotEmpty(Collection<?> obj, String message, Object... args) {
        if (Objects.requireNonNull(obj, message).isEmpty()) {
            throw new IllegalStateException(String.format(message, args));
        }
        return obj;
    }

    public static <T> T requireCondition(T value, Predicate<T> predicate, String message, Object... args) {
        if (!predicate.test(value)) {
            throw new IllegalStateException(String.format(message, args));
        }
        return value;
    }
}
