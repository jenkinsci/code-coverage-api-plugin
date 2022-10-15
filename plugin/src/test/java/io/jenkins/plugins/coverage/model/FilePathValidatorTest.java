package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.ContainerNode;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
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
        Node root = readExampleReport();
        Node rootCopy = root.copyTree();
        FilteredLog log = createLog();

        verifyPathUniqueness(root, log);

        assertThat(root).isEqualTo(rootCopy);
        assertThat(log.getErrorMessages()).isEmpty();
    }

    @Test
    void shouldCorrectInvalidFileStructure() {
        Node root = createMultiModuleReportWithDuplicates();
        FilteredLog log = createLog();

        verifyPathUniqueness(root, log);

        assertThat(root.getAll(Metric.PACKAGE))
                .hasSize(2);
        assertThat(root.getAll(Metric.FILE))
                .hasSize(3);
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
     * @return the {@link Node root} of the tree
     */
    private Node createMultiModuleReportWithDuplicates() {
        Node root = new ContainerNode("root");

        Node module1 = new ModuleNode("module1");
        root.addChild(module1);
        Node packageName = new PackageNode("package");
        module1.addChild(packageName);
        Node file = new FileNode("file");
        packageName.addChild(file);
        Node file2 = new FileNode("file2");
        packageName.addChild(file2);

        Node module2 = new ModuleNode("module2");
        root.addChild(module2);
        Node packageCopy = packageName.copy();
        module2.addChild(packageCopy);
        Node fileCopy = file.copy();
        packageCopy.addChild(fileCopy);

        return root;
    }

    /**
     * Reads the coverage tree from the report 'jacoco-codingstyle.xml'.
     *
     * @return the {@link Node} root of the tree
     */
    private Node readExampleReport() {
        return readJacocoResult("jacoco-codingstyle.xml");
    }
}
