package com.senacor.elasticsearch.evolution.core.internal.utils;

import java.util.Random;

/**
 * @author Andreas Keefer
 */
public class RandomUtils {

    private static final Random RANDOM = new Random();

    public static int getRandomInt(int min, int max) {
        if (min == max) {
            return min;
        }
        return RANDOM.nextInt(max - min) + min;
    }
}
