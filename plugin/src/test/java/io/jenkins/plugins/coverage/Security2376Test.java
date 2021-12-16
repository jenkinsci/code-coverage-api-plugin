package io.jenkins.plugins.coverage;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import java.io.IOException;

public class Security2376Test {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @LocalData
    @Test(expected = SecurityException.class)
    public void testDeserialization() throws IOException, ClassNotFoundException {
        // coverage-report is just a serialized empty IdentityHashMap, not on the serialization allowlist as of Jenkins 2.303
        final FreeStyleProject fs = (FreeStyleProject) j.jenkins.getItemByFullName("fs");
        CoverageProcessor.recoverCoverageResult(fs.getBuild("1"));
        // Without the fix, this fails with:
        // Caused by: java.lang.ClassCastException: java.util.IdentityHashMap cannot be cast to io.jenkins.plugins.coverage.targets.CoverageResult
    }
}
