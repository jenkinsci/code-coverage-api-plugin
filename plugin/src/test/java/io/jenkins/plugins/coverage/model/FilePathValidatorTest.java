package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;

import static io.jenkins.plugins.coverage.model.FilePathValidator.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link FilePathValidator}.
 *
 * @author Florian Orendi
 */
class FilePathValidatorTest extends AbstractCoverageTest {

    @Test
    void shouldAcceptValidFileStructure() {
        CoverageNode root = readExampleReport();
        CoverageNode rootCopy = root.copyTree();
        FilteredLog log = createLog();

        FilePathValidator.verifyPathUniqueness(root, log);
        assertThat(root).isEqualTo(rootCopy);
        assertThat(log.getErrorMessages()).isEmpty();
    }

    @Test
    void shouldCorrectInvalidFileStructure() {
        CoverageNode root = createMultiModuleReportWithDuplicates();
        FilteredLog log = createLog();

        FilePathValidator.verifyPathUniqueness(root, log);
        assertThat(root.getAll(CoverageMetric.PACKAGE))
                .hasSize(1)
                .satisfies(node -> assertThat(node.get(0).getName()).isEqualTo("package"));
        assertThat(root.getAll(CoverageMetric.FILE))
                .hasSize(1)
                .satisfies(node -> assertThat(node.get(0).getName()).isEqualTo("file2"));
        assertThat(log.getErrorMessages()).contains(
                AMBIGUOUS_FILES_MESSAGE + System.lineSeparator() + "package/file",
                REMOVED_MESSAGE,
                PACKAGE_INFO_MESSAGE
        );
    }

    /**
     * Creates a {@link FilteredLog log}.
     *
     * @return the created log
     */
    private FilteredLog createLog() {
        return new FilteredLog("Log file validations:");
    }

    /**
     * Creates a coverage tree for a multi-module project with files with duplicate fully qualified names.
     *
     * @return the {@link CoverageNode root} of the tree
     */
    private CoverageNode createMultiModuleReportWithDuplicates() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "root");
        CoverageNode module1 = new CoverageNode(CoverageMetric.MODULE, "module1");
        CoverageNode module2 = new CoverageNode(CoverageMetric.MODULE, "module2");
        CoverageNode packageName = new PackageCoverageNode("package");
        CoverageNode packageCopy = packageName.copyEmpty();
        CoverageNode file = new FileCoverageNode("file", "file");
        CoverageNode file2 = new FileCoverageNode("file2", "file2");
        CoverageNode fileCopy = file.copyEmpty();
        root.add(module1);
        root.add(module2);
        module1.add(packageName);
        module2.add(packageCopy);
        packageName.add(file);
        packageName.add(file2);
        packageCopy.add(fileCopy);

        return root;
    }

    /**
     * Reads the coverage tree from the report 'jacoco-codingstyle.xml'.
     *
     * @return the {@link CoverageNode} root of the tree
     */
    private CoverageNode readExampleReport() {
        return readNode("jacoco-codingstyle.xml");
    }
}
