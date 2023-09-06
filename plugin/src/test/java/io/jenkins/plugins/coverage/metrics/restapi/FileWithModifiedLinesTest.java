package io.jenkins.plugins.coverage.metrics.restapi;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import io.jenkins.plugins.coverage.metrics.AbstractModifiedFilesCoverageTest;

/**
 * Tests the {@link FileWithModifiedLines} class.
 */
class FileWithModifiedLinesTest extends AbstractModifiedFilesCoverageTest {
    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(FileWithModifiedLines.class).usingGetClass().verify();
    }
}
