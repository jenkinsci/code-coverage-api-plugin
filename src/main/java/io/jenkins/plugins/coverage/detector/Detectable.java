package io.jenkins.plugins.coverage.detector;

import io.jenkins.plugins.coverage.adapter.CoverageReportAdapter;

import java.io.File;

/**
 * Coverage report will be able to be automatically found and match.
 * <p>
 * It should be implemented by Descriptor of {@link CoverageReportAdapter}
 * </p>
 */
public interface Detectable {
    /**
     * @param file file be detected
     * @return <code>true</code> if file match the rule
     */
    boolean detect(File file);
}
