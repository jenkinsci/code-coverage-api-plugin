package io.jenkins.plugins.coverage.metrics.model;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Metric;

import static org.assertj.core.api.Assertions.*;

class ElementFormatterTest {
    @Test
    void shouldHandleOverflowGracefully() {
        var formatter = new ElementFormatter();

        var fraction = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        var delta = formatter.formatDelta(fraction, Metric.LINE, Locale.ENGLISH);

        assertThat(delta).isEqualTo("+100.00%");
    }
}
