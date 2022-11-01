package io.jenkins.plugins.coverage.model;

import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link CoverageTreeCreator}.
 *
 * @author Florian Orendi
 */
class CoverageTreeCreatorTest extends AbstractCoverageTest {
    @Test
    void shouldCreateEmptyChangeCoverageTreeWithoutChanges() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        Node tree = readCoverageTree();
        Node changeCoverageTree = coverageTreeCreator.createChangeCoverageTree(tree);
        assertThat(changeCoverageTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getChildren()).isEmpty();
                    assertThat(root.getValues()).isEmpty();
                });
    }

    @Test
    void shouldCreateEmptyIndirectCoverageChangesTreeWithoutChanges() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        Node tree = readCoverageTree();
        Node indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(tree);
        assertThat(indirectCoverageChangesTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getChildren()).isEmpty();
                    assertThat(root.getValues()).isEmpty();
                });
    }

    @Test
    void shouldCreateChangeCoverageTree() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        Node tree = createTreeWithChangeCoverage();
        Node changeCoverageTree = coverageTreeCreator.createChangeCoverageTree(tree);
        assertThat(changeCoverageTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getAll(FILE)).hasSize(1);
                    var builder = new CoverageBuilder();
                    assertThat(root.getValue(LINE)).isNotEmpty().contains(
                            builder.setMetric(Metric.LINE).setCovered(2).setMissed(2).build());
                    assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                            builder.setMetric(Metric.BRANCH).setCovered(4).setMissed(4).build());
                });
    }

    @Test
    void shouldCreateIndirectCoverageChangesTree() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        Node tree = createTreeWithIndirectCoverageChanges();
        Node indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(tree);
        assertThat(indirectCoverageChangesTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getAll(FILE)).hasSize(1);
                    var builder = new CoverageBuilder();
                    assertThat(root.getValue(LINE)).isNotEmpty().contains(
                            builder.setMetric(Metric.LINE).setCovered(2).setMissed(2).build());
                    assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                            builder.setMetric(Metric.BRANCH).setCovered(4).setMissed(4).build());
                });
    }

    /**
     * Creates an instance of {@link CoverageTreeCreator}.
     *
     * @return the created instance
     */
    private CoverageTreeCreator createCoverageTreeCreator() {
        return new CoverageTreeCreator();
    }

    /**
     * Reads the coverage tree from the report 'jacoco-codingstyle.xml'.
     *
     * @return the {@link Node} root of the tree
     */
    private Node readCoverageTree() {
        return readJacocoResult("jacoco-codingstyle.xml");
    }

    /**
     * Creates a coverage tree which contains a {@link FileNode} with changed code lines and change coverage.
     *
     * @return the root of the tree
     */
    private Node createTreeWithChangeCoverage() {
        Node root = readCoverageTree();
        FileNode file = attachFileCoverageNodeToTree(root);
        attachCodeChanges(file);
        return root;
    }

    /**
     * Creates a coverage tree which contains a {@link FileNode} with indirect coverage changes.
     *
     * @return the root of the tree
     */
    private Node createTreeWithIndirectCoverageChanges() {
        Node root = readCoverageTree();
        FileNode file = attachFileCoverageNodeToTree(root);
        attachIndirectCoverageChanges(file);
        return root;
    }

    /**
     * Attaches a custom {@link FileNode file node} to an existing coverage tree and returns the inserted
     * instance.
     *
     * @param root
     *         The root of the tree
     *
     * @return the inserted instance
     */
    private FileNode attachFileCoverageNodeToTree(final Node root) {
        FileNode file = new FileNode("CoverageTreeTest.java");
        Optional<Node> packageNode = root.getAll(PACKAGE).stream().findFirst();
        assertThat(packageNode).isNotEmpty();
        packageNode.get().addChild(file);
        attachCoveragePerLine(file);
        return file;
    }

    /**
     * Attaches the coverage-per-line to the passed {@link FileNode node}.
     *
     * @param file
     *         The node to which coverage information should be added
     */
    private void attachCoveragePerLine(final FileNode file) {
        var method = new MethodNode("aMethod", "{}");
        var builder = new CoverageBuilder().setMetric(Metric.LINE);
        file.addCounters(10, 1, 0);
        file.addCounters(11, 0, 1);
        file.addCounters(12, 1, 0);
        file.addCounters(13, 0, 1);
        method.addValue(builder.setCovered(2).setMissed(2).build());

        builder.setMetric(Metric.BRANCH);
        file.addCounters(11, 0, 4);
        file.addCounters(12, 4, 0);
        method.addValue(builder.setCovered(4).setMissed(4).build());

        file.addChild(method);
    }

    /**
     * Attaches indirect coverage changes to the passed {@link FileNode node}.
     *
     * @param file
     *         The node to which information about indirect coverage changes should be added
     */
    private void attachIndirectCoverageChanges(final FileNode file) {
        SortedMap<Integer, Integer> indirectChanges = new TreeMap<>();
        indirectChanges.put(10, 1);
        indirectChanges.put(11, -4);
        indirectChanges.put(12, 4);
        indirectChanges.put(13, -1);
        file.getIndirectCoverageChanges().putAll(indirectChanges);
    }

    /**
     * Attaches changed code lines to the passed {@link FileNode node}.
     *
     * @param file
     *         The node to which information about changed code lines should be added
     */
    private void attachCodeChanges(final FileNode file) {
        SortedSet<Integer> changes = new TreeSet<>();
        changes.add(10);
        changes.add(11);
        changes.add(12);
        changes.add(13);
        file.getChangedLines().addAll(changes);

    }
}
