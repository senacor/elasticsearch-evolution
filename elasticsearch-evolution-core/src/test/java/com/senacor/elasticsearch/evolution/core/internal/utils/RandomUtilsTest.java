package com.senacor.elasticsearch.evolution.core.internal.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author Andreas Keefer
 */
class RandomUtilsTest {

    @Nested
    class getRandomInt {

        @Test
        void isAlwaysInSpecifiedRange() {
            IntStream.range(0, 100000)
                    .forEach(value -> {
                        int randomInt = RandomUtils.getRandomInt(10, 20);
                        assertThat(randomInt).isBetween(10, 19);
                    });
        }

        @Test
        void statisticalSpreadIsOK() {
            Map<Integer, Long> collect = IntStream.range(0, 100000)
                    .mapToObj(value -> RandomUtils.getRandomInt(10, 20))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            System.out.println("statisticalSpread: " + collect);
            assertSoftly(softly -> {
                softly.assertThat(collect).containsOnlyKeys(10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
                collect.values().forEach(count -> softly.assertThat(count).isBetween(9500L, 10500L));
            });
        }

        @Test
        void minAndMaxIsEquals() {
            int value = 0;
            assertThat(RandomUtils.getRandomInt(value, value)).isEqualTo(value);
        }
    }
}