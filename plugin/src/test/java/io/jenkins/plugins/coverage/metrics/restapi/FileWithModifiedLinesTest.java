package io.jenkins.plugins.coverage.metrics.restapi;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Tests {@link FileWithModifiedLines}.
 */
class FileWithModifiedLinesTest {
    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(FileWithModifiedLines.class).usingGetClass().verify();
    }
}
