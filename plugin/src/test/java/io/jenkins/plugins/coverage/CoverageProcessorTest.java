package io.jenkins.plugins.coverage;

import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher;
import io.jenkins.plugins.coverage.CoveragePublisher.CoveragePublisher.SourceFileResolver;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Acceptance tests for CoverageProcessor. Verifies if set options in CoverageProcessor are used and lead to accepted
 * results.
 */
public class CoverageProcessorTest {

    /**
     * Test roundFloat method in CoverageProcessor
     */
    @Test
    public void testFloatRounding() {
        float number=-0.001f;
        float scaledNumber= CoverageProcessor.roundFloat(number,2);
        Assert.assertEquals(scaledNumber,0.00);
    }

}
