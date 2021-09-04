package io.jenkins.plugins.coverage.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.echarts.JacksonFacade;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Test for the class {@link TreeNode}.
 *
 * @author Andreas Pabst
 */
public class TreeNodeTest {
    /**
     * Test if packages with two identical package levels are inserted correctly.
     */
    @Test
    public void shouldInsertTwoLevelPackage() {
        TreeNode root = new TreeNode("");

        TreeNode myClass = new TreeNode("com.example.MyClass");
        TreeNode otherClass = new TreeNode("com.example.other.OtherClass");

        root.insertNode(myClass);
        root.insertNode(otherClass);

        assertThat(root.getChildren()).hasSize(1);
        TreeNode child = root.getChildren().get(0);
        assertThat(child).hasName("com");

        assertThat(child.getChildren()).hasSize(1);
        child = child.getChildren().get(0);
        assertThat(child).hasName("example");

        assertThat(child.getChildren()).hasSize(2);
        assertThat(child.getChildren().get(0)).hasName("MyClass");

        child = child.getChildren().get(1);
        assertThat(child).hasName("other");
        assertThat(child.getChildren()).hasSize(1);
        assertThat(child.getChildren().get(0)).hasName("OtherClass");
    }

    /**
     * Test if the value of the metric is kept correctly.
     */
    @Test
    public void shouldGetSpecificMetricValue() {
        final double metricValue = 42;

        TreeNode node = new TreeNode("node", metricValue);
        assertThat(node.getValue()).isEqualTo(42);
    }

    /**
     * Test if all children values are summed up correctly.
     */
    @Test
    public void shouldSumUpChildrenValues() {
        final double metricValue1 = 42;
        final double metricValue2 = 47;
        final double metricValue3 = 11;

        TreeNode root = new TreeNode("");
        root.insertNode(new TreeNode("test.node1", metricValue1));
        root.insertNode(new TreeNode("test.node2", metricValue2));
        root.insertNode(new TreeNode("node3", metricValue3));

        assertThat(root.getValue()).isEqualTo(metricValue1 + metricValue2 + metricValue3);
    }

    /**
     * Test if the package is collapsed correctly.
     */
    @Test
    public void shouldCollapsePackage() {
        TreeNode rootNode = threeLevelTree();
        rootNode.collapsePackage();

        assertThat(rootNode.getName()).isEqualTo("levelOneNode.levelTwoNode");
        assertThat(rootNode.getChildren()).hasSize(2);
    }

    private TreeNode threeLevelTree() {
        TreeNode leafNode2 = new TreeNode("leafNode1");
        TreeNode leafNode1 = new TreeNode("leafNode2");
        TreeNode levelTwoNode = new TreeNode("levelTwoNode");
        levelTwoNode.insertNode(leafNode1);
        levelTwoNode.insertNode(leafNode2);

        TreeNode levelOneNode = new TreeNode("levelOneNode");
        levelOneNode.insertNode(levelTwoNode);

        TreeNode rootNode = new TreeNode("");
        rootNode.insertNode(levelOneNode);

        return rootNode;
    }

    /**
     * Test the equals and hash functions.
     */
    @Test
    public void shouldBeEqualAndHash() {
        final String name = "name";
        final double one = 1.0;
        TreeNode node = new TreeNode(name);

        assertThat(node).isNotEqualTo(new TreeNode(name, one));
        assertThat(node).isEqualTo(node);
        assertThat(node).isEqualTo(new TreeNode(name));

        node.insertNode(new TreeNode("name1"));
        assertThat(node).isNotEqualTo(new TreeNode(name));

        assertThat(node).isNotEqualTo("test");

        assertThat(node.hashCode()).isEqualTo(node.hashCode());

        assertThat(node.hashCode()).isNotEqualTo(new TreeNode(name).hashCode());
    }

    /**
     * Test if the JSON serialisation is correct.
     */
    @Test
    public void shouldContainRelevantInformationInJson() {
        JacksonFacade facade = new JacksonFacade();
        TreeNode root = new TreeNode("");
        root.insertNode(new TreeNode("com.example.Bar", 5.0));
        root.insertNode(new TreeNode("com.example.package.Foo", 2.0));
        root.collapsePackage();

        assertThat(facade.toJson(root)).isEqualTo("{\"name\":\"com.example\",\"value\":7.0,\"children\":["
                + "{\"name\":\"Bar\",\"value\":5.0,\"children\":[]},"
                + "{\"name\":\"package\",\"value\":2.0,\"children\":["
                + "{\"name\":\"Foo\",\"value\":2.0,\"children\":[]}"
                + "]}]}");
    }
}
