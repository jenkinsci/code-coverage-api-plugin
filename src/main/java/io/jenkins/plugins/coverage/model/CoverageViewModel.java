package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public TableModel getTableModel(final String id) {
        return new CoverageTableModel(getResult());
    }

    /**
     * Returns a new sub page for the selected link.
     *
     * @param link
     *         the link to identify the sub page to show
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
            columns.add(new TableColumn("Line Coverage", "lineCoverage").setHeaderClass(ColumnCss.NUMBER));
            columns.add(new TableColumn("Branch Coverage", "branchCoverage").setHeaderClass(ColumnCss.NUMBER));

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

        public String getLineCoverage() {
            return result.printCoverageFor(CoverageElement.LINE);
        }

        public String getBranchCoverage() {
            return result.printCoverageFor(CoverageElement.CONDITIONAL);
        }
    }
}
