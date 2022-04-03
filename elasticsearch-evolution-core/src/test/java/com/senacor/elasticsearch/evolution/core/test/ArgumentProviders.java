package com.senacor.elasticsearch.evolution.core.test;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Andreas Keefer
 */
public class ArgumentProviders {

    public static class SuccessHttpCodesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            return IntStream.range(200, 300)
                    .mapToObj(Arguments::of);
        }
    }

    public static class FailingHttpCodesProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
            Stream<Integer> status1xx = IntStream.range(100, 200).boxed();
            return Stream.concat(status1xx, IntStream.range(300, 600).boxed())
                    .map(Arguments::of);
        }
    }
}
