package io.jenkins.plugins.coverage.model;

import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

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
        CoverageNode tree = readCoverageTree();
        CoverageNode changeCoverageTree = coverageTreeCreator.createChangeCoverageTree(tree);
        assertThat(changeCoverageTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getChildren()).isEmpty();
                    assertThat(root.getLeaves()).isEmpty();
                });
    }

    @Test
    void shouldCreateEmptyIndirectCoverageChangesTreeWithoutChanges() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        CoverageNode tree = readCoverageTree();
        CoverageNode indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(tree);
        assertThat(indirectCoverageChangesTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getChildren()).isEmpty();
                    assertThat(root.getLeaves()).isEmpty();
                });
    }

    @Test
    void shouldCreateChangeCoverageTree() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        CoverageNode tree = createTreeWithChangeCoverage();
        CoverageNode changeCoverageTree = coverageTreeCreator.createChangeCoverageTree(tree);
        assertThat(changeCoverageTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getAll(FILE)).hasSize(1);
                    assertThat(root.getCoverage(LINE)).isEqualTo(
                            new Coverage.CoverageBuilder().setCovered(2).setMissed(2).build());
                    assertThat(root.getCoverage(BRANCH)).isEqualTo(
                            new Coverage.CoverageBuilder().setCovered(4).setMissed(4).build());
                });
    }

    @Test
    void shouldCreateIndirectCoverageChangesTree() {
        CoverageTreeCreator coverageTreeCreator = createCoverageTreeCreator();
        CoverageNode tree = createTreeWithIndirectCoverageChanges();
        CoverageNode indirectCoverageChangesTree = coverageTreeCreator.createIndirectCoverageChangesTree(tree);
        assertThat(indirectCoverageChangesTree)
                .isNotNull()
                .isNotSameAs(tree)
                .satisfies(root -> {
                    assertThat(root.getName()).isEqualTo(tree.getName());
                    assertThat(root.getPath()).isEqualTo(tree.getPath());
                    assertThat(root.getMetric()).isEqualTo(tree.getMetric());
                    assertThat(root.getAll(FILE)).hasSize(1);
                    assertThat(root.getCoverage(LINE)).isEqualTo(
                            new Coverage.CoverageBuilder().setCovered(2).setMissed(2).build());
                    assertThat(root.getCoverage(BRANCH)).isEqualTo(
                            new Coverage.CoverageBuilder().setCovered(4).setMissed(4).build());
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
     * @return the {@link CoverageNode} root of the tree
     */
    private CoverageNode readCoverageTree() {
        CoverageNode root = readNode("jacoco-codingstyle.xml");
        root.splitPackages();
        return root;
    }

    /**
     * Creates a coverage tree which contains a {@link FileCoverageNode} with changed code lines and change coverage.
     *
     * @return the root of the tree
     */
    private CoverageNode createTreeWithChangeCoverage() {
        CoverageNode root = readCoverageTree();
        FileCoverageNode file = attachFileCoverageNodeToTree(root);
        attachCodeChanges(file);
        return root;
    }

    /**
     * Creates a coverage tree which contains a {@link FileCoverageNode} with indirect coverage changes.
     *
     * @return the root of the tree
     */
    private CoverageNode createTreeWithIndirectCoverageChanges() {
        CoverageNode root = readCoverageTree();
        FileCoverageNode file = attachFileCoverageNodeToTree(root);
        attachIndirectCoverageChanges(file);
        return root;
    }

    /**
     * Attaches a custom {@link FileCoverageNode file node} to an existing coverage tree and returns the inserted
     * instance.
     *
     * @param root
     *         The root of the tree
     *
     * @return the inserted instance
     */
    private FileCoverageNode attachFileCoverageNodeToTree(final CoverageNode root) {
        FileCoverageNode file = new FileCoverageNode("CoverageTreeTest.java", "");
        Optional<CoverageNode> packageNode = root.getAll(PACKAGE).stream().findFirst();
        assertThat(packageNode).isNotEmpty();
        packageNode.get().add(file);
        file.setParent(packageNode.get());
        attachCoveragePerLine(file);
        return file;
    }

    /**
     * Attaches the coverage-per-line to the passed {@link FileCoverageNode node}.
     *
     * @param file
     *         The node to which coverage information should be added
     */
    private void attachCoveragePerLine(final FileCoverageNode file) {
        SortedMap<Integer, Coverage> coveragePerLine = new TreeMap<>();
        coveragePerLine.put(10, new Coverage.CoverageBuilder().setCovered(1).setMissed(0).build());
        coveragePerLine.put(11, new Coverage.CoverageBuilder().setCovered(0).setMissed(4).build());
        coveragePerLine.put(12, new Coverage.CoverageBuilder().setCovered(4).setMissed(0).build());
        coveragePerLine.put(13, new Coverage.CoverageBuilder().setCovered(0).setMissed(1).build());
        file.setCoveragePerLine(coveragePerLine);
    }

    /**
     * Attaches indirect coverage changes to the passed {@link FileCoverageNode node}.
     *
     * @param file
     *         The node to which information about indirect coverage changes should be added
     */
    private void attachIndirectCoverageChanges(final FileCoverageNode file) {
        SortedMap<Integer, Integer> indirectChanges = new TreeMap<>();
        indirectChanges.put(10, 1);
        indirectChanges.put(11, -4);
        indirectChanges.put(12, 4);
        indirectChanges.put(13, -1);
        file.setIndirectCoverageChanges(indirectChanges);
    }

    /**
     * Attaches changed code lines to the passed {@link FileCoverageNode node}.
     *
     * @param file
     *         The node to which information about changed code lines should be added
     */
    private void attachCodeChanges(final FileCoverageNode file) {
        SortedSet<Integer> changes = new TreeSet<>();
        changes.add(10);
        changes.add(11);
        changes.add(12);
        changes.add(13);
        file.setChangedCodeLines(changes);
    }
}
