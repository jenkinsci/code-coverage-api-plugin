package io.jenkins.plugins.coverage.model;

import edu.hm.hafner.util.ResourceTest;

import io.jenkins.plugins.coverage.CoverageNodeConverter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter;
import io.jenkins.plugins.coverage.adapter.JacocoReportAdapter.JacocoReportAdapterDescriptor;
import io.jenkins.plugins.coverage.adapter.JavaCoverageReportAdapterDescriptor;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageElementRegister;
import io.jenkins.plugins.coverage.targets.CoverageResult;

/**
 * Base class for coverage tests that work on real coverage reports.
 *
 * @author Ullrich Hafner
 */
public class AbstractCoverageTest extends ResourceTest {
    static final double PRECISION = 0.01;
    static final CoverageElement REPORT = CoverageElement.REPORT;
    static final CoverageElement PACKAGE = JavaCoverageReportAdapterDescriptor.PACKAGE;
    static final CoverageElement SOURCE_FILE = CoverageElement.FILE;
    static final CoverageElement CLASS_NAME = JavaCoverageReportAdapterDescriptor.CLASS;
    static final CoverageElement METHOD = JavaCoverageReportAdapterDescriptor.METHOD;
    static final CoverageElement LINE = CoverageElement.LINE;
    static final CoverageElement INSTRUCTION = JacocoReportAdapterDescriptor.INSTRUCTION;
    static final CoverageElement BRANCH = CoverageElement.CONDITIONAL;

    CoverageResult readResult(final String fileName) {
        try {
            JacocoReportAdapter parser = new JacocoReportAdapter("Hello");
            CoverageElementRegister.addCoverageElements(new JacocoReportAdapterDescriptor().getCoverageElements());
            CoverageResult result = parser.getResult(getResourceAsFile(fileName).toFile());
            result.stripGroup();
            return result;
        }
        catch (CoverageException exception) {
            throw new AssertionError(exception);
        }
    }

    CoverageNode readNode(final String fileName) {
        return CoverageNodeConverter.convert(readResult(fileName));
    }
}
