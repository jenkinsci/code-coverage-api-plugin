package io.jenkins.plugins.coverage.model;

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;

import edu.hm.hafner.util.ResourceTest;

import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import io.jenkins.plugins.coverage.targets.CoverageResult;

/**
 * Base class for coverage tests that work on real coverage reports.
 *
 * @author Ullrich Hafner
 */
public abstract class AbstractCoverageTest extends ResourceTest {
    static final double PRECISION = 0.01;
    static final CoverageMetric MODULE = CoverageMetric.MODULE;
    static final CoverageMetric PACKAGE = CoverageMetric.PACKAGE;
    static final CoverageMetric FILE = CoverageMetric.FILE;
    static final CoverageMetric CLASS = CoverageMetric.CLASS;
    static final CoverageMetric METHOD = CoverageMetric.METHOD;
    static final CoverageMetric LINE = CoverageMetric.LINE;
    static final CoverageMetric INSTRUCTION = CoverageMetric.INSTRUCTION;
    static final CoverageMetric BRANCH = CoverageMetric.BRANCH;

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    public CoverageResult readResult(final String fileName) {
        try {
            JacocoReportAdapter parser = new JacocoReportAdapter("unused");
            CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
            CoverageResult result = parser.getResult(getResourceAsFile(fileName).toFile());
            result.stripGroup();
            return result;
        }
        catch (CoverageException exception) {
            throw new AssertionError(exception);
        }
    }

    public CoverageNode readNode(final String fileName) {
        return new CoverageNodeConverter().convert(readResult(fileName));
    }

    protected CoverageResult readReport(final String fileName) throws CoverageException {
        JacocoReportAdapter parser = new JacocoReportAdapter("Hello");
        CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
        CoverageResult result = parser.getResult(getResourceAsFile(fileName).toFile());
        result.stripGroup();
        return result;
    }
}
