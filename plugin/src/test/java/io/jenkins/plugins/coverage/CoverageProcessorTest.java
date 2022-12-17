package io.jenkins.plugins.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Acceptance tests for CoverageProcessor. Verifies if set options in CoverageProcessor are used and lead to accepted
 * results.
 */
public class CoverageProcessorTest {
    public JenkinsRule j = new JenkinsRule();
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
