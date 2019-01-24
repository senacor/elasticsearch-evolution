package com.senacor.elasticsearch.evolution.core.test;

import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.mockito.Mock;

import java.lang.reflect.Parameter;

import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Copied and slightly modified from the <a href="https://github.com/junit-team/junit5-samples/blob/026a9d9abe06b6173398c1a2518793259cd190f2/junit5-mockito-extension/src/main/java/com/example/mockito/MockitoExtension.java">junit5-samples project</a>.
 * Usage:
 * <pre>{@code
 * @ExtendWith(MockitoExtension.class)
 * public class MyClassTest {
 *
 *     @Mock
 *     private MyFirstService myFirstServiceMock;
 *
 *     @Spy
 *     private MySecondService mySecondServiceSpy;
 *
 *     // ...
 * }
 * }</pre>
 */
public class MockitoExtension implements TestInstancePostProcessor, ParameterResolver {

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        initMocks(testInstance);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext
                .getParameter()
                .isAnnotationPresent(Mock.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getMock(parameterContext.getParameter(), extensionContext);
    }

    private Object getMock(Parameter parameter, ExtensionContext extensionContext) {
        Class<?> mockType = parameter.getType();
        Store mocks = extensionContext.getStore(Namespace.create(MockitoExtension.class, mockType));
        String mockName = getMockName(parameter);

        if (mockName != null) {
            return mocks.getOrComputeIfAbsent(mockName, key -> mock(mockType, mockName));
        } else {
            return mocks.getOrComputeIfAbsent(mockType.getCanonicalName(), key -> mock(mockType));
        }
    }

    private String getMockName(Parameter parameter) {
        String explicitMockName = parameter
                .getAnnotation(Mock.class)
                .name()
                .trim();
        if (!explicitMockName.isEmpty()) {
            return explicitMockName;
        } else if (parameter.isNamePresent()) {
            return parameter.getName();
        }
        return null;
    }

}
