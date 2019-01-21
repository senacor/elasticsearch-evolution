package com.senacor.elasticsearch.evolution.core.internal.utils;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Andreas Keefer
 */
public class AssertionUtils {

    public static String requireNotEmpty(String obj, String message) {
        Objects.requireNonNull(obj, message);
        if (obj.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return obj;
    }

    public static String requireNotBlank(String obj, String message) {
        Objects.requireNonNull(obj, message);
        if (obj.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
        return obj;
    }

    public static Collection<?> requireNotEmpty(Collection<?> obj, String message) {
        Objects.requireNonNull(obj, message);
        if (obj.isEmpty()) {
            throw new IllegalStateException(message);
        }
        return obj;
    }
}
