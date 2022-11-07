package io.jenkins.plugins.coverage.metrics;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Metric;

import hudson.model.View;
import hudson.views.ListViewColumn;

import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.coverage.metrics.visualization.dashboard.CoverageMetricColumn;
import io.jenkins.plugins.util.IntegrationTestWithJenkinsPerTest;

import static io.jenkins.plugins.coverage.model.Assertions.*;

/**
 * Tests the Job DSL Plugin.
 *
 * @author Ullrich Hafner
 */
class JobDslITest extends IntegrationTestWithJenkinsPerTest {
    /**
     * Creates a freestyle job from a YAML file and verifies that issue recorder finds warnings.
     */
    @Test
    void shouldCreateFreestyleJobUsingJobDslAndVerifyIssueRecorderWithDefaultConfiguration() {
        configureJenkins("column-metric-dsl.yaml");

        View view = getJenkins().getInstance().getView("dsl-view");

        assertThat(view).isNotNull();

        assertThat(view.getColumns())
                .extracting(ListViewColumn::getColumnCaption)
                .contains(new CoverageMetricColumn().getColumnCaption());

        assertThat(view.getColumns()).first()
                .isInstanceOfSatisfying(CoverageMetricColumn.class,
                        c -> {
                            assertThat(c)
                                    .hasColumnCaption(Messages.Coverage_Column())
                                    .hasCoverageMetric(Metric.LINE.name());
                        });
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
