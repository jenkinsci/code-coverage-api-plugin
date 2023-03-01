package io.jenkins.plugins.coverage.metrics.steps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.hm.hafner.echarts.LabeledTreeMapNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Percentage;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.Api;
import hudson.model.ModelObject;
import hudson.model.Run;

import io.jenkins.plugins.bootstrap5.MessagesViewModel;
import io.jenkins.plugins.coverage.metrics.charts.TreeMapNodeConverter;
import io.jenkins.plugins.coverage.metrics.color.ColorProvider;
import io.jenkins.plugins.coverage.metrics.color.ColorProviderFactory;
import io.jenkins.plugins.coverage.metrics.color.CoverageColorJenkinsId;
import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.coverage.metrics.model.ElementFormatter;
import io.jenkins.plugins.coverage.metrics.source.SourceCodeFacade;
import io.jenkins.plugins.coverage.metrics.source.SourceViewModel;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTableModel.InlineRowRenderer;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTableModel.LinkedRowRenderer;
import io.jenkins.plugins.coverage.metrics.steps.CoverageTableModel.RowRenderer;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.util.BuildResultNavigator;
import io.jenkins.plugins.util.QualityGateResult;

/**
 * Server side model that provides the data for the details view of the coverage results. The layout of the associated
 * view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public class CoverageViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private static final TreeMapNodeConverter TREE_MAP_NODE_CONVERTER = new TreeMapNodeConverter();
    private static final BuildResultNavigator NAVIGATOR = new BuildResultNavigator();
    private static final SourceCodeFacade SOURCE_CODE_FACADE = new SourceCodeFacade();

    static final String ABSOLUTE_COVERAGE_TABLE_ID = "absolute-coverage-table";
    static final String MODIFIED_LINES_COVERAGE_TABLE_ID = "modified-lines-coverage-table";
    static final String MODIFIED_FILES_COVERAGE_TABLE_ID = "modified-files-coverage-table";
    static final String INDIRECT_COVERAGE_TABLE_ID = "indirect-coverage-table";
    private static final String INLINE_SUFFIX = "-inline";
    private static final String INFO_MESSAGES_VIEW_URL = "info";

    private static final ElementFormatter FORMATTER = new ElementFormatter();
    private static final Set<Metric> TREE_METRICS = Set.of(Metric.LINE, Metric.INSTRUCTION, Metric.BRANCH, Metric.MUTATION);
    private final Run<?, ?> owner;
    private final String optionalName;
    private final CoverageStatistics statistics;
    private final QualityGateResult qualityGateResult;
    private final String referenceBuild;
    private final FilteredLog log;
    private final Node node;
    private final String id;

    private final Node modifiedLinesCoverageTreeRoot;
    private final Node modifiedFilesCoverageTreeRoot;
    private final Node indirectCoverageChangesTreeRoot;
    private final Function<String, String> trendChartFunction;

    private ColorProvider colorProvider = ColorProviderFactory.createDefaultColorProvider();

    @SuppressWarnings("checkstyle:ParameterNumber")
    CoverageViewModel(final Run<?, ?> owner, final String id, final String optionalName, final Node node,
            final CoverageStatistics statistics, final QualityGateResult qualityGateResult,
            final String referenceBuild, final FilteredLog log, final Function<String, String> trendChartFunction) {
        super();

        this.owner = owner;

        this.id = id;
        this.optionalName = optionalName;

        this.node = node;
        this.statistics = statistics;
        this.qualityGateResult = qualityGateResult;
        this.referenceBuild = referenceBuild;

        this.log = log;

        // initialize filtered coverage trees so that they will not be calculated multiple times
        modifiedLinesCoverageTreeRoot = node.filterByModifiedLines();
        modifiedFilesCoverageTreeRoot = node.filterByModifiedFiles();
        indirectCoverageChangesTreeRoot = node.filterByIndirectChanges();
        this.trendChartFunction = trendChartFunction;
    }

    public String getId() {
        return id;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    public Node getNode() {
        return node;
    }

    public ElementFormatter getFormatter() {
        return FORMATTER;
    }

    /**
     * Returns the value metrics that should be visualized in a tree map.
     *
     * @return the value metrics
     */
    public NavigableSet<Metric> getTreeMetrics() {
        var valueMetrics = node.getValueMetrics();
        valueMetrics.retainAll(TREE_METRICS);
        return valueMetrics;
    }

    @Override
    public String getDisplayName() {
        if (StringUtils.isBlank(node.getName()) || "-".equals(node.getName())) {
            if (StringUtils.isBlank(optionalName)) {
                return Messages.Coverage_Link_Name();
            }
            return optionalName;
        }
        if (StringUtils.isBlank(optionalName)) {
            return Messages.Coverage_Title(node.getName());
        }
        return String.format("%s: %s", optionalName, node.getName());
    }

    /**
     * Gets the remote API for this action. Depending on the path, a different result is selected.
     *
     * @return the remote API
     */
    public Api getApi() {
        return new Api(new CoverageApi(statistics, qualityGateResult, referenceBuild));
    }

    /**
     * Gets a set of color IDs which can be used to dynamically load the defined Jenkins colors.
     *
     * @return the available color IDs
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public Set<String> getJenkinsColorIDs() {
        return CoverageColorJenkinsId.getAll();
    }

    /**
     * Creates a new {@link ColorProvider} based on the passed color json string which contains the set Jenkins colors.
     *
     * @param colors
     *         The dynamically loaded Jenkins colors to be used for highlighting the coverage tree as json string
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public void setJenkinsColors(final String colors) {
        colorProvider = createColorProvider(colors);
    }

    /**
     * Parses the passed color json string to a {@link ColorProvider}.
     *
     * @param json
     *         The color json
     *
     * @return the created color provider
     */
    private ColorProvider createColorProvider(final String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> colorMapping = mapper.readValue(json, new ColorMappingType());
            return ColorProviderFactory.createColorProvider(colorMapping);
        }
        catch (JsonProcessingException e) {
            return ColorProviderFactory.createDefaultColorProvider();
        }
    }

    // FIXME: check why this model works on a filtered view
    @JavaScriptMethod
    public CoverageOverview getOverview() {
        return new CoverageOverview(node);
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
        return trendChartFunction.apply(configuration);
    }

    /**
     * Returns the root of the tree of nodes for the ECharts treemap. This tree is used as model for the chart on the
     * client side. The tree is available for line and branch coverage.
     *
     * @param coverageMetric
     *         The used coverage metric - the default is the line coverage
     *
     * @return the tree of nodes for the ECharts treemap
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public LabeledTreeMapNode getCoverageTree(final String coverageMetric) {
        Metric metric = getCoverageMetricFromText(coverageMetric);
        return TREE_MAP_NODE_CONVERTER.toTreeChartModel(getNode(), metric, colorProvider);
    }

    /**
     * Gets the {@link Metric} from a String representation used in the frontend. Only 'Line' and 'Branch' is possible.
     * 'Line' is used as a default.
     *
     * @param text
     *         The coverage metric as String
     *
     * @return the coverage metric
     */
    private Metric getCoverageMetricFromText(final String text) {
        // FIXME: Move to metric
        if (text.contains("line")) {
            return Metric.LINE;
        }
        if (text.contains("branch")) {
            return Metric.BRANCH;
        }
        if (text.contains("instruction")) {
            return Metric.INSTRUCTION;
        }
        if (text.contains("mutation")) {
            return Metric.MUTATION;
        }
        if (text.contains("loc")) {
            return Metric.LOC;
        }
        if (text.contains("density")) {
            return Metric.COMPLEXITY_DENSITY;
        }
        return Metric.COMPLEXITY;
    }

    /**
     * Returns the table model that matches with the passed table ID and shows the files along with the branch and line
     * coverage.
     *
     * @param tableId
     *         ID of the table model
     *
     * @return the table model with the specified ID
     */
    @Override
    public TableModel getTableModel(final String tableId) {
        RowRenderer renderer = createRenderer(tableId);

        String actualId = tableId.replace(INLINE_SUFFIX, StringUtils.EMPTY);
        switch (actualId) {
            case ABSOLUTE_COVERAGE_TABLE_ID:
                return new CoverageTableModel(tableId, getNode(), renderer, colorProvider);
            case MODIFIED_LINES_COVERAGE_TABLE_ID:
                return new ModifiedLinesCoverageTableModel(tableId, getNode(), modifiedLinesCoverageTreeRoot, renderer,
                        colorProvider);
            case MODIFIED_FILES_COVERAGE_TABLE_ID:
                return new ModifiedFilesCoverageTableModel(tableId, getNode(), modifiedFilesCoverageTreeRoot, renderer,
                        colorProvider);
            case INDIRECT_COVERAGE_TABLE_ID:
                return new IndirectCoverageChangesTable(tableId, getNode(), indirectCoverageChangesTreeRoot, renderer,
                        colorProvider);
            default:
                throw new NoSuchElementException("No such table with id " + actualId);
        }
    }

    private RowRenderer createRenderer(final String tableId) {
        RowRenderer renderer;
        if (tableId.endsWith(INLINE_SUFFIX) && hasSourceCode()) {
            renderer = new InlineRowRenderer();
        }
        else {
            renderer = new LinkedRowRenderer(getOwner().getRootDir(), getId());
        }
        return renderer;
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
        return NAVIGATOR.getSameUrlForOtherBuild(owner, currentUrl, id,
                selectedBuildDisplayName).orElse(StringUtils.EMPTY);
    }

    /**
     * Gets the source code of the file which is represented by the passed hash code. The coverage of the source code is
     * highlighted by using HTML. Depending on the passed table ID, the source code is returned filtered with only the
     * relevant lines of code.
     *
     * @param fileHash
     *         The hash code of the requested file
     * @param tableId
     *         The ID of the source file table
     *
     * @return the highlighted source code
     */
    @JavaScriptMethod
    public String getSourceCode(final String fileHash, final String tableId) {
        Optional<Node> targetResult
                = getNode().findByHashCode(Metric.FILE, Integer.parseInt(fileHash));
        if (targetResult.isPresent()) {
            try {
                Node fileNode = targetResult.get();
                return readSourceCode(fileNode, tableId);
            }
            catch (IOException | InterruptedException exception) {
                return ExceptionUtils.getStackTrace(exception);
            }
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Reads the sourcecode corresponding to the passed {@link Node node} and filters the code dependent on the table
     * ID.
     *
     * @param sourceNode
     *         The node
     * @param tableId
     *         The table ID
     *
     * @return the sourcecode with highlighted coverage
     * @throws IOException
     *         if reading failed
     * @throws InterruptedException
     *         if reading failed
     */
    private String readSourceCode(final Node sourceNode, final String tableId)
            throws IOException, InterruptedException {
        String content = "";
        File rootDir = getOwner().getRootDir();
        if (isSourceFileAvailable(sourceNode)) {
            content = SOURCE_CODE_FACADE.read(rootDir, getId(), sourceNode.getPath());
        }
        if (!content.isEmpty() && sourceNode instanceof FileNode) {
            FileNode fileNode = (FileNode) sourceNode;
            String cleanTableId = StringUtils.removeEnd(tableId, INLINE_SUFFIX);
            if (MODIFIED_LINES_COVERAGE_TABLE_ID.equals(cleanTableId)) {
                return SOURCE_CODE_FACADE.calculateModifiedLinesCoverageSourceCode(content, fileNode);
            }
            else if (INDIRECT_COVERAGE_TABLE_ID.equals(cleanTableId)) {
                return SOURCE_CODE_FACADE.calculateIndirectCoverageChangesSourceCode(content, fileNode);
            }
            else {
                return content;
            }
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Checks whether source files are stored.
     *
     * @return {@code true} when source files are stored, {@code false} otherwise
     */
    @JavaScriptMethod
    public boolean hasSourceCode() {
        return SOURCE_CODE_FACADE.hasStoredSourceCode(getOwner().getRootDir(), id);
    }

    /**
     * Checks whether modified lines coverage exists.
     *
     * @return {@code true} whether modified lines coverage exists, else {@code false}
     */
    public boolean hasModifiedLinesCoverage() {
        return getNode().getAllFileNodes().stream().anyMatch(FileNode::hasModifiedLines);
    }

    /**
     * Checks whether indirect coverage changes exist.
     *
     * @return {@code true} whether indirect coverage changes exist, else {@code false}
     */
    public boolean hasIndirectCoverageChanges() {
        return getNode().getAllFileNodes().stream().anyMatch(FileNode::hasIndirectCoverageChanges);
    }

    /**
     * Returns whether the source file is available in Jenkins build folder.
     *
     * @param coverageNode
     *         The {@link Node} which is checked if there is a source file available
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileAvailable(final Node coverageNode) {
        return SOURCE_CODE_FACADE.canRead(getOwner().getRootDir(), id, coverageNode.getPath());
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
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        if (INFO_MESSAGES_VIEW_URL.equals(link)) {
            return new MessagesViewModel(getOwner(), Messages.MessagesViewModel_Title(),
                    log.getInfoMessages(), log.getErrorMessages());
        }
        if (StringUtils.isNotEmpty(link)) {
            try {
                Optional<Node> targetResult
                        = getNode().findByHashCode(Metric.FILE, Integer.parseInt(link));
                if (targetResult.isPresent() && targetResult.get() instanceof FileNode) {
                    return new SourceViewModel(getOwner(), getId(), (FileNode) targetResult.get());
                }
            }
            catch (NumberFormatException exception) {
                // ignore
            }
        }
        return null; // fallback on broken URLs
    }

    /**
     * UI model for the coverage overview bar chart. Shows the coverage results for the different coverage metrics.
     */
    public static class CoverageOverview {
        private final Node coverage;
        private static final ElementFormatter ELEMENT_FORMATTER = new ElementFormatter();

        CoverageOverview(final Node coverage) {
            this.coverage = coverage;
        }

        public List<String> getMetrics() {
            return sortCoverages()
                    .map(Coverage::getMetric)
                    .map(ELEMENT_FORMATTER::getLabel)
                    .collect(Collectors.toList());
        }

        private Stream<Coverage> sortCoverages() {
            return ELEMENT_FORMATTER.getSortedCoverageValues(coverage)
                    .filter(c -> c.getTotal() > 1); // ignore elements that have a total of 1
        }

        public List<Integer> getCovered() {
            return getCoverageCounter(Coverage::getCovered);
        }

        public List<Integer> getMissed() {
            return getCoverageCounter(Coverage::getMissed);
        }

        private List<Integer> getCoverageCounter(final Function<Coverage, Integer> property) {
            return sortCoverages().map(property).collect(Collectors.toList());
        }

        public List<Double> getCoveredPercentages() {
            return getPercentages(Coverage::getCoveredPercentage);
        }

        public List<Double> getMissedPercentages() {
            return getPercentages(c -> Percentage.valueOf(c.getMissed(), c.getTotal()));
        }

        private List<Double> getPercentages(final Function<Coverage, Percentage> displayType) {
            return sortCoverages().map(displayType)
                    .map(Percentage::toDouble)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Used for parsing a Jenkins color mapping JSON string to a color map.
     */
    private static final class ColorMappingType extends TypeReference<HashMap<String, String>> {
    }
}
