package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import j2html.tags.ContainerTag;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.ModelObject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.CoverageResult.JSCoverageResult;
import io.jenkins.plugins.coverage.targets.Ratio;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableColumn.ColumnCss;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.datatables.TableModel.DetailedColumnDefinition;

import static j2html.TagCreator.*;

/**
 * Server side model that provides the data for the details view of the coverage results. The layout of the associated
 * view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 */
public class CoverageViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private final Run<?, ?> owner;
    private final CoverageResult result;
    private final String displayName;

    /**
     * Creates a new view model instance.
     *
     * @param owner
     *         the owner of this view
     * @param result
     *         the results to be shown
     * @param displayName
     *         human-readable name of this view (used in bread-crumb)
     */
    public CoverageViewModel(final Run<?, ?> owner, final CoverageResult result, final String displayName) {
        this.owner = owner;
        this.result = result;
        this.displayName = displayName;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    public CoverageResult getResult() {
        return result;
    }

    @Override
    public String getDisplayName() {
        return Messages.Coverage_Title(displayName);
    }

    /**
     * Interface for javascript code to get code coverage result.
     *
     * @return aggregated coverage results
     */
    @JavaScriptMethod
    public List<JSCoverageResult> getOverallStatistics() {
        List<JSCoverageResult> results = new ArrayList<>();

        List<Entry<CoverageElement, Ratio>> elements = new ArrayList<>(getResult().getResults().entrySet());
        elements.sort(Collections.reverseOrder(Entry.comparingByKey()));

        for (Map.Entry<CoverageElement, Ratio> c : elements) {
            results.add(new JSCoverageResult(c.getKey().getName(), c.getValue()));
        }

        return results;
    }

    /**
     * Interface for javascript code to get child coverage result.
     *
     * @return aggregated child coverage results
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public Map<String, List<JSCoverageResult>> getDetailsOfFirstChild() {
        return getResult().getChildrenReal()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().jsGetResults()));
    }

    /**
     * Interface for javascript code to get child coverage result.
     *
     * @return aggregated child coverage results
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeChartNode getCoverageTree() {
        CoverageNode tree = CoverageNode.fromResult(getResult());
        tree.splitPackages();
        return tree.toChartTree();
    }

    @Override
    public TableModel getTableModel(final String id) {
        return new CoverageTableModel(getResult());
    }

    /**
     * Returns a new sub-page for the selected link.
     *
     * @param link
     *         the link to identify the sub-page to show
     * @param request
     *         Stapler request
     * @param response
     *         Stapler response
     *
     * @return the new sub page
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        String[] split = link.split("\\.", 2);
        if (split.length == 2) {
            Optional<CoverageResult> targetResult = getResult().find(split[0], split[1]);
            if (targetResult.isPresent()) {
                CoverageResult coverageResult = targetResult.get();
                if (coverageResult.getElement().equals(CoverageElement.FILE)) {
                    return new SourceViewModel(getOwner(), coverageResult, coverageResult.getDisplayName());
                }
                return new CoverageViewModel(getOwner(), coverageResult, coverageResult.getDisplayName());
            }
        }
        return this; // fallback on broken URLs
    }

    private static class CoverageTableModel extends TableModel {
        private final CoverageResult result;

        CoverageTableModel(final CoverageResult result) {
            this.result = result;
        }

        @Override
        public String getId() {
            return "coverage-details";
        }

        @Override
        public List<TableColumn> getColumns() {
            List<TableColumn> columns = new ArrayList<>();

            columns.add(new TableColumn("Package", "packageName"));
            columns.add(new TableColumn("File", "fileName"));
            columns.add(new TableColumn("Line Coverage", "lineCoverageValue").setHeaderClass(ColumnCss.NUMBER));
            columns.add(new TableColumn("Line Coverage", "lineCoverageChart", "number"));
            columns.add(new TableColumn("Branch Coverage", "branchCoverageValue").setHeaderClass(ColumnCss.NUMBER));
            columns.add(new TableColumn("Branch Coverage", "branchCoverageChart", "number"));

            return columns;
        }

        @Override
        public List<Object> getRows() {
            return result.getAll(CoverageElement.FILE).stream()
                    .map(CoverageRow::new).collect(Collectors.toList());
        }
    }

    /**
     * A table row that shows the coverage statistics of files.
     */
    @SuppressWarnings("PMD.DataClass") // Used to automatically convert to JSON object
    public static class CoverageRow {
        private final CoverageResult result;

        CoverageRow(final CoverageResult result) {
            this.result = result;
        }

        public String getFileName() {
            String fileName = result.getName();
            return a().withHref("file." + fileName.hashCode()).withText(fileName).render();
        }

        public String getPackageName() {
            return result.getParentName();
        }

        public String getLineCoverageValue() {
            return result.printCoverageFor(CoverageElement.LINE);
        }

        public DetailedColumnDefinition getLineCoverageChart() {
            return createDetailedColumnFor(CoverageElement.LINE);
        }

        public String getBranchCoverageValue() {
            return result.printCoverageFor(CoverageElement.CONDITIONAL);
        }

        public DetailedColumnDefinition getBranchCoverageChart() {
            return createDetailedColumnFor(CoverageElement.CONDITIONAL);
        }

        private DetailedColumnDefinition createDetailedColumnFor(final CoverageElement conditional) {
            return new DetailedColumnDefinition(getBarChartFor(result.getCoverage(conditional)),
                    result.getCoverage(conditional).getPercentageString());
        }

        private String getBarChartFor(final Ratio lineCoverage) {
            return join(getBarChart("covered", lineCoverage.getPercentage()),
                    getBarChart("missed", 100 - lineCoverage.getPercentage())).render();
        }

        private ContainerTag getBarChart(final String className, final int percentage) {
            return span().withClasses("bar-graph", className, className + "--hover")
                    .withStyle("width:" + percentage + "%").withText(".");
        }
    }
}
