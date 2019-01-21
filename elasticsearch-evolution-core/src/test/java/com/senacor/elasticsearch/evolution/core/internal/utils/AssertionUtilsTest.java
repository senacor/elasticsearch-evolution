package com.senacor.elasticsearch.evolution.core.internal.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotBlank;
import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Andreas Keefer
 */
class AssertionUtilsTest {

    @Test
    void requireNotEmpty_nonEmptyString() {
        String obj = "a";
        String msg = "msg";

        assertThat(requireNotEmpty(obj, msg))
                .isEqualTo(obj);
    }

    @Test
    void requireNotEmpty_emptyString() {
        String obj = "";
        String msg = "msg";

        assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireNotEmpty_nullString() {
        String obj = null;
        String msg = "msg";

        assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requireNotBlank_nonEmptyString() {
        String obj = "a";
        String msg = "msg";

        assertThat(requireNotBlank(obj, msg))
                .isEqualTo(obj);
    }

    @Test
    void requireNotBlank_emptyString() {
        String obj = "";
        String msg = "msg";

        assertThatThrownBy(() -> requireNotBlank(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireNotBlank_blankString() {
        String obj = " ";
        String msg = "msg";

        assertThatThrownBy(() -> requireNotBlank(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireNotBlank_nullString() {
        String obj = null;
        String msg = "msg";

        assertThatThrownBy(() -> requireNotBlank(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requireNotEmpty_nonEmptyCollection() {
        List obj = Collections.singletonList("a");
        String msg = "msg";

        assertThat(requireNotEmpty(obj, msg))
                .isEqualTo(obj);
    }

    @Test
    void requireNotEmpty_emptyCollection() {
        List obj = Collections.emptyList();
        String msg = "msg";

        assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void requireNotEmpty_nullCollection() {
        List obj = null;
        String msg = "msg";

        assertThatThrownBy(() -> requireNotEmpty(obj, msg))
                .hasMessage(msg)
                .isInstanceOf(NullPointerException.class);
    }
}