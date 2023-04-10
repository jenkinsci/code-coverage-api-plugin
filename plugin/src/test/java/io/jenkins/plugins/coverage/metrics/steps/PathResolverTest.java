package io.jenkins.plugins.coverage.metrics.steps;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import io.jenkins.plugins.coverage.metrics.steps.PathResolver.RemoteResultWrapper;

import static org.assertj.core.api.Assertions.*;

class PathResolverTest {
    @Nested
    class RemoteResultWrapperTest {
        @Test
        void shouldCreateWrapper() {
            var result = "result";

            var wrapper = new RemoteResultWrapper<>(result, "title");

            assertThat(wrapper.getResult()).isEqualTo(result);

            wrapper.logInfo("Hello %s", "World");
            assertThat(wrapper.getInfoMessages()).containsExactly("Hello World");
        }

        @Test
        void shouldAdhereToEquals() {
            EqualsVerifier.simple().forClass(RemoteResultWrapper.class)
                    .suppress(Warning.NULL_FIELDS)
                    .verify();
        }
    }
}
