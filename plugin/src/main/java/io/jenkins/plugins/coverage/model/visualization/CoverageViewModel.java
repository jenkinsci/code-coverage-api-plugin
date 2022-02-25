package io.jenkins.plugins.coverage.model.visualization;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.TreeMapNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import j2html.tags.ContainerTag;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.Functions;
import hudson.model.ModelObject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.model.Coverage;
import io.jenkins.plugins.coverage.model.CoverageBuildAction;
import io.jenkins.plugins.coverage.model.CoverageMetric;
import io.jenkins.plugins.coverage.model.CoverageNode;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.coverage.model.coverage.CoverageTreeCreator;
import io.jenkins.plugins.coverage.model.visualization.code.SourceCodeFacade;
import io.jenkins.plugins.coverage.model.visualization.code.SourceViewModel;
import io.jenkins.plugins.coverage.model.visualization.tree.TreeMapNodeConverter;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableColumn.ColumnCss;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.datatables.TableModel.DetailedColumnDefinition;
import io.jenkins.plugins.util.BuildResultNavigator;

import static j2html.TagCreator.*;

/**
 * Server side model that provides the data for the details view of the coverage results. The layout of the associated
 * view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 */
public class CoverageViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private static final CoverageMetric LINE_COVERAGE = CoverageMetric.LINE;
    private static final CoverageMetric BRANCH_COVERAGE = CoverageMetric.BRANCH;
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    private static final TreeMapNodeConverter TREE_MAP_NODE_CONVERTER = new TreeMapNodeConverter();
    private static final BuildResultNavigator NAVIGATOR = new BuildResultNavigator();

    private static final CoverageTreeCreator COVERAGE_CALCULATOR = new CoverageTreeCreator();

    private final Run<?, ?> owner;
    private final CoverageNode node;
    private final String id;

    /**
     * Creates a new view model instance.
     *
     * @param owner
     *         the owner of this view
     * @param node
     *         the coverage node to be shown
     */
    public CoverageViewModel(final Run<?, ?> owner, final CoverageNode node) {
        super();

        this.owner = owner;
        this.node = node;
        id = "coverage"; // TODO: this needs to be a parameter
    }

    public String getId() {
        return id;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    public CoverageNode getNode() {
        return node;
    }

    @Override
    public String getDisplayName() {
        return Messages.Coverage_Title(node.getName());
    }

    @JavaScriptMethod
    public CoverageOverview getOverview() {
        return new CoverageOverview(getNode());
    }

    /**
     * Returns the root of the tree of nodes for the ECharts treemap. This tree is used as model for the chart on the
     * client side.
     *
     * @return the tree of nodes for the ECharts treemap
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeMapNode getCoverageTree() {
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(getNode());
    }

    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeMapNode getChangeCoverageTree() {
        CoverageNode changeCoverageTree = COVERAGE_CALCULATOR.createChangeCoverageTree(getNode());
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(changeCoverageTree);
    }

    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeMapNode getCoverageChangesTree() {
        CoverageNode coverageChangesTree = COVERAGE_CALCULATOR.createUnexpectedCoverageChangesTree(getNode());
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(coverageChangesTree);
    }

    /**
     * Returns the table model that shows the files along with the branch and line coverage. Currently, only one table
     * is shown in the view, so the ID is not used.
     *
     * @param tableId
     *         ID of the table model
     *
     * @return the table model with the specified ID
     */
    @Override
    public TableModel getTableModel(final String tableId) {
        CoverageNode root = getNode();
        if ("change-coverage-details".equals(tableId)) {
            root = COVERAGE_CALCULATOR.createChangeCoverageTree(root);
        }
        else if ("coverage-changes-details".equals(tableId)) {
            root = COVERAGE_CALCULATOR.createUnexpectedCoverageChangesTree(root);
        }
        return new CoverageTableModel(root, getOwner().getRootDir(), tableId);
    }

    private LinesChartModel createTrendChart(final String configuration) {
        Optional<CoverageBuildAction> latestAction = getLatestAction();
        if (latestAction.isPresent()) {
            return latestAction.get().createProjectAction().createChartModel(configuration);
        }
        return new LinesChartModel();
    }

    /**
     * Returns the trend chart configuration.
     *
     * @param configuration
     *         JSON object to configure optional properties for the trend chart
     *
     * @return the trend chart model (converted to a JSON string)
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public String getTrendChart(final String configuration) {
        return JACKSON_FACADE.toJson(createTrendChart(configuration));
    }

    private Optional<CoverageBuildAction> getLatestAction() {
        return Optional.ofNullable(getOwner().getAction(CoverageBuildAction.class));
    }

    /**
     * Returns the URL for coverage results of the selected build. Based on the current URL, the new URL will be
     * composed by replacing the current build number with the selected build number.
     *
     * @param selectedBuildDisplayName
     *         the selected build to open the new results for
     * @param currentUrl
     *         the absolute URL to this details view results
     *
     * @return the URL to the results or an empty string if the results are not available
     */
    @JavaScriptMethod
    public String getUrlForBuild(final String selectedBuildDisplayName, final String currentUrl) {
        return NAVIGATOR.getSameUrlForOtherBuild(owner,
                currentUrl, CoverageBuildAction.DETAILS_URL, selectedBuildDisplayName)
                .orElse(StringUtils.EMPTY);
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
     * @return the new sub-page
     */
    @SuppressWarnings("unused") // Called by jelly view
    @CheckForNull
    public SourceViewModel getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if (StringUtils.isNotEmpty(link)) {
            try {
                Optional<CoverageNode> targetResult
                        = getNode().findByHashCode(CoverageMetric.FILE, Integer.parseInt(link));
                if (targetResult.isPresent()) {
                    return new SourceViewModel(getOwner(), targetResult.get());
                }
            }
            catch (NumberFormatException exception) {
                // ignore
            }
        }
        return null; // fallback on broken URLs
    }

    /**
     * Returns whether the source file is available in Jenkins build folder.
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileAvailable() {
        return isSourceFileInNewFormatAvailable() || isSourceFileInOldFormatAvailable();
    }

    /**
     * Returns whether the source file is available in Jenkins build folder in the old format of the plugin versions
     * less than 2.1.0.
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileInOldFormatAvailable() {
        return isSourceFileInOldFormatAvailable(getOwner().getRootDir(), getNode().getName());
    }

    static boolean isSourceFileInOldFormatAvailable(final File rootDir, final String nodeName) {
        return getFileForBuildsWithOldVersion(rootDir, nodeName).canRead();
    }

    /**
     * Returns a file to the sources in release in the old format of the plugin versions less than 2.1.0.
     *
     * @param buildFolder
     *         top-level folder of the build results
     * @param fileName
     *         base filename of the coverage node
     *
     * @return the file
     */
    public static File getFileForBuildsWithOldVersion(final File buildFolder, final String fileName) {
        return new File(new File(buildFolder, "coverage-sources"), sanitizeFilename(fileName));
    }

    private static String sanitizeFilename(final String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    /**
     * Returns whether the source file is available in Jenkins build folder in the new format of the plugin versions
     * greater or equal than 2.1.0.
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileInNewFormatAvailable() {
        return isSourceFileInNewFormatAvailable(getOwner().getRootDir(), id, getNode().getPath());
    }

    static boolean isSourceFileInNewFormatAvailable(final File rootDir, final String id, final String nodePath) {
        return new SourceCodeFacade().createFileInBuildFolder(rootDir, id, nodePath).canRead();
    }

    /**
     * UI model for the coverage overview bar chart. Shows the coverage results for the different coverage metrics.
     */
    public static class CoverageOverview {
        private final CoverageNode coverage;

        CoverageOverview(final CoverageNode coverage) {
            this.coverage = coverage;
        }

        public List<String> getMetrics() {
            return getMetricsDistribution().keySet().stream()
                    .skip(1) // ignore the root of the tree as the coverage is always 1 of 1
                    .map(CoverageMetric::getName)
                    .collect(Collectors.toList());
        }

        public List<Integer> getCovered() {
            return streamCoverages().map(Coverage::getCovered).collect(Collectors.toList());
        }

        public List<Double> getCoveredPercentages() {
            return streamCoverages().map(Coverage::getCoveredPercentage)
                    .map(Fraction::doubleValue)
                    .collect(Collectors.toList());
        }

        public List<Integer> getMissed() {
            return streamCoverages().map(Coverage::getMissed).collect(Collectors.toList());
        }

        public List<Double> getMissedPercentages() {
            return streamCoverages().map(Coverage::getMissedPercentage)
                    .map(Fraction::doubleValue)
                    .collect(Collectors.toList());
        }

        private Stream<Coverage> streamCoverages() {
            return getMetricsDistribution().values().stream().skip(1);
        }

        private SortedMap<CoverageMetric, Coverage> getMetricsDistribution() {
            return coverage.getMetricsDistribution();
        }
    }

    /**
     * UI table model for the coverage details table.
     */
    private static class CoverageTableModel extends TableModel {
        private final CoverageNode root;
        private final File buildFolder;
        private final String id;

        CoverageTableModel(final CoverageNode root, final File buildFolder, final String id) {
            super();

            this.root = root;
            this.buildFolder = buildFolder;
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<TableColumn> getColumns() {
            List<TableColumn> columns = new ArrayList<>();

            TableColumn fileHashColumn = new TableColumn("Hash", "fileHash");
            fileHashColumn.setHeaderClass(ColumnCss.HIDDEN);

            columns.add(fileHashColumn);
            columns.add(new TableColumn("Package", "packageName"));
            columns.add(new TableColumn("File", "fileName"));
            columns.add(new TableColumn("Line Coverage", "lineCoverageValue").setHeaderClass(ColumnCss.PERCENTAGE));
            columns.add(new TableColumn("Line Coverage", "lineCoverageChart", "number"));
            columns.add(new TableColumn("Branch Coverage", "branchCoverageValue").setHeaderClass(ColumnCss.PERCENTAGE));
            columns.add(new TableColumn("Branch Coverage", "branchCoverageChart", "number"));

            return columns;
        }

        @Override
        public List<Object> getRows() {
            Locale browserLocale = Functions.getCurrentLocale();
            return root.getAll(CoverageMetric.FILE).stream()
                    .map((CoverageNode file) -> new CoverageRow(file, buildFolder, browserLocale))
                    .collect(Collectors.toList());
        }
    }

    /**
     * UI row model for the coverage details table.
     */
    private static class CoverageRow {
        private final CoverageNode root;
        private final File buildFolder;
        private final Locale browserLocale;

        CoverageRow(final CoverageNode root, final File buildFolder, final Locale browserLocale) {
            this.root = root;
            this.buildFolder = buildFolder;
            this.browserLocale = browserLocale;
        }

        public String getFileHash() {
            return String.valueOf(root.getName().hashCode());
        }

        public String getFileName() {
            String fileName = root.getName();
            if (isSourceFileInNewFormatAvailable(buildFolder, "coverage", root.getPath())
                    || isSourceFileInOldFormatAvailable(buildFolder, fileName)) {
                return a().withHref(String.valueOf(fileName.hashCode())).withText(fileName).render();
            }
            return fileName;
        }

        public String getPackageName() {
            return root.getParentName();
        }

        public String getLineCoverageValue() {
            return printCoverage(getLineCoverage());
        }

        private Coverage getLineCoverage() {
            return root.getCoverage(LINE_COVERAGE);
        }

        public DetailedColumnDefinition getLineCoverageChart() {
            return createDetailedColumnFor(LINE_COVERAGE);
        }

        public String getBranchCoverageValue() {
            return printCoverage(getBranchCoverage());
        }

        private String printCoverage(final Coverage coverage) {
            if (coverage.isSet()) {
                return coverage.formatCoveredPercentage(browserLocale);
            }
            return Messages.Coverage_Not_Available();
        }

        private Coverage getBranchCoverage() {
            return root.getCoverage(BRANCH_COVERAGE);
        }

        public DetailedColumnDefinition getBranchCoverageChart() {
            return createDetailedColumnFor(BRANCH_COVERAGE);
        }

        private DetailedColumnDefinition createDetailedColumnFor(final CoverageMetric element) {
            Coverage coverage = root.getCoverage(element);

            return new DetailedColumnDefinition(getBarChartFor(coverage),
                    String.valueOf(coverage.getCoveredPercentage()));
        }

        private String getBarChartFor(final Coverage coverage) {
            return join(getBarChart("covered", coverage.getCoveredPercentage()),
                    getBarChart("missed", coverage.getMissedPercentage())).render();
        }

        private ContainerTag getBarChart(final String className, final Fraction percentage) {
            return span().withClasses("bar-graph", className, className + "--hover")
                    .withStyle("width:" + (percentage.doubleValue() * 100) + "%").withText(".");
        }
    }
}
