package com.senacor.elasticsearch.evolution.core.internal.utils;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class AssertionUtilsTest {

    @Nested
    class requireNotEmptyString {
        @Test
        void nonEmptyString() {
            String obj = "a";
            String msg = "msg";

            assertThat(requireNotEmpty(obj, msg))
                    .isEqualTo(obj);
        }

        @Test
        void emptyString() {
            String obj = "";
            String msg = "msg";

            assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void nullString() {
            String obj = null;
            String msg = "msg";

            assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class requireNotBlank {
        @Test
        void nonEmptyString() {
            String obj = "a";
            String msg = "msg";

            assertThat(requireNotBlank(obj, msg))
                    .isEqualTo(obj);
        }

        @Test
        void emptyString() {
            String obj = "";
            String msg = "msg";

            assertThatThrownBy(() -> requireNotBlank(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void blankString() {
            String obj = " ";
            String msg = "msg";

            assertThatThrownBy(() -> requireNotBlank(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void nullString() {
            String obj = null;
            String msg = "msg";

            assertThatThrownBy(() -> requireNotBlank(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class requireNotEmptyCollection {
        @Test
        void nonEmptyCollection() {
            List obj = Collections.singletonList("a");
            String msg = "msg";

            assertThat(requireNotEmpty(obj, msg))
                    .isEqualTo(obj);
        }

        @Test
        void emptyCollection() {
            List obj = Collections.emptyList();
            String msg = "msg";

            assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void nullCollection() {
            List obj = null;
            String msg = "msg";

            assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                    .hasMessage(msg)
                    .isInstanceOf(NullPointerException.class);
        }
    }


    @Nested
    class requireCondition {
        @Test
        void isValid() {
            boolean value = true;

            Boolean res = requireCondition(value, Boolean.TRUE::equals, "my value (%s) must be true", value);

            assertThat(res).isEqualTo(value);
        }

        @Test
        void isInvalidValue() {
            boolean value = false;

            assertThatThrownBy(() -> requireCondition(value, Boolean.TRUE::equals, "my value (%s) must be true", value))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("my value (false) must be true");
        }

        @Test
        void isInvalidValueNoParameters() {
            boolean value = false;

            assertThatThrownBy(() -> requireCondition(value, Boolean.TRUE::equals, "my value must be true"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("my value must be true");
        }

        @Test
        void isInvalidNull() {
            Boolean value = null;

            assertThatThrownBy(() -> requireCondition(value, Boolean.TRUE::equals, "my value (%s) must be true", value))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("my value (null) must be true");
        }
    }
}