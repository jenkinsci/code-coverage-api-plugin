package io.jenkins.plugins.coverage.metrics;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.TopLevelItem;
import hudson.model.View;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.views.ListViewColumn;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.coverage.metrics.CoverageTool.CoverageParser;
import io.jenkins.plugins.coverage.metrics.QualityGate.QualityGateCriticality;
import io.jenkins.plugins.coverage.metrics.visualization.dashboard.CoverageMetricColumn;
import io.jenkins.plugins.prism.SourceCodeDirectory;
import io.jenkins.plugins.prism.SourceCodeRetention;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerTest;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests support for column and job configurations via the Job DSL Plugin.
 *
 * @author Ullrich Hafner
 */
class JobDslITest extends IntegrationTestWithJenkinsPerTest {
    /**
     * Creates a freestyle job from a YAML file and verifies that issue recorder finds warnings.
     */
    @Test
    void shouldCreateColumnFromYamlConfiguration() {
        configureJenkins("column-metric-dsl.yaml");

        View view = getJenkins().getInstance().getView("dsl-view");

        assertThat(view).isNotNull();

        assertThat(view.getColumns())
                .extracting(ListViewColumn::getColumnCaption)
                .contains(new CoverageMetricColumn().getColumnCaption());

        assertThat(view.getColumns()).first()
                .isInstanceOfSatisfying(CoverageMetricColumn.class,
                        c -> assertThat(c)
                                .hasColumnCaption(Messages.Coverage_Column())
                                .hasCoverageMetric(Metric.LINE.name()));
    }

    /**
     * Creates a freestyle job from a YAML file and verifies that issue recorder finds warnings.
     */
    @Test
    void shouldCreateFreestyleJobFromYamlConfiguration() {
        configureJenkins("job-dsl.yaml");

        TopLevelItem project = getJenkins().jenkins.getItem("dsl-freestyle-job");

        assertThat(project).isNotNull();
        assertThat(project).isInstanceOf(FreeStyleProject.class);

        DescribableList<Publisher, Descriptor<Publisher>> publishers = ((FreeStyleProject) project).getPublishersList();
        assertThat(publishers).hasSize(1);

        Publisher publisher = publishers.get(0);
        assertThat(publisher).isInstanceOfSatisfying(CoverageRecorder.class, this::assertRecorderProperties);
    }

    private void assertRecorderProperties(final CoverageRecorder recorder) {
        assertThat(recorder.getTools()).hasSize(2).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        new CoverageTool(CoverageParser.JACOCO, "jacoco-pattern.*"),
                        new CoverageTool(CoverageParser.COBERTURA, "cobertura-pattern.*"));
        assertThat(recorder.getQualityGates()).hasSize(2).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(
                        new QualityGate(70.0, Metric.LINE, Baseline.PROJECT, QualityGateCriticality.UNSTABLE),
                        new QualityGate(80.0, Metric.BRANCH, Baseline.CHANGE, QualityGateCriticality.FAILURE));
        assertThat(recorder.getSourceDirectories()).hasSize(2).extracting(SourceCodeDirectory::getPath)
                .containsExactlyInAnyOrder("directory-1", "directory-2");
        assertThat(recorder)
                .hasId("my-coverage")
                .hasName("My Coverage")
                .hasScm("my-git")
                .hasSourceCodeEncoding("UTF-8")
                .hasSourceCodeRetention(SourceCodeRetention.EVERY_BUILD)
                .isEnabledForFailure()
                .isFailOnError()
                .isSkipPublishingChecks()
                .isSkipSymbolicLinks();
    }

    /**
     * Helper method to get jenkins configuration file.
     *
     * @param fileName
     *         file with configuration.
     */
    private void configureJenkins(final String fileName) {
        try {
            ConfigurationAsCode.get().configure(getResourceAsFile(fileName).toUri().toString());
        }
        catch (ConfiguratorException e) {
            throw new AssertionError(e);
        }
    }
}
