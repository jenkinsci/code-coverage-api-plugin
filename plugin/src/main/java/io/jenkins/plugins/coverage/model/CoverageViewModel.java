package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.Fraction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.TreeMapNode;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.TextFile;

import io.jenkins.plugins.coverage.model.CoverageTableModel.InlineRowRenderer;
import io.jenkins.plugins.coverage.model.CoverageTableModel.LinkedRowRenderer;
import io.jenkins.plugins.coverage.model.visualization.code.SourceCodeFacade;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageColorJenkinsId;
import io.jenkins.plugins.coverage.model.visualization.tree.TreeMapNodeConverter;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.util.BuildResultNavigator;

/**
 * Server side model that provides the data for the details view of the coverage results. The layout of the associated
 * view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 * @author Florian Orendi
 */
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "checkstyle:ClassDataAbstractionCoupling", "checkstyle:ClassFanOutComplexity"})
public class CoverageViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    private static final TreeMapNodeConverter TREE_MAP_NODE_CONVERTER = new TreeMapNodeConverter();
    private static final BuildResultNavigator NAVIGATOR = new BuildResultNavigator();
    private static final SourceCodeFacade SOURCE_CODE_FACADE = new SourceCodeFacade();

    static final String ABSOLUTE_COVERAGE_TABLE_ID = "absolute-coverage-table";
    static final String CHANGE_COVERAGE_TABLE_ID = "change-coverage-table";
    static final String INDIRECT_COVERAGE_TABLE_ID = "indirect-coverage-table";
    private static final String INLINE_SUFFIX = "-inline";

    private final Run<?, ?> owner;
    private final CoverageNode node;
    private final String id;

    private final CoverageNode changeCoverageTreeRoot;
    private final CoverageNode indirectCoverageChangesTreeRoot;

    private ColorProvider colorProvider = ColorProviderFactory.createDefaultColorProvider();

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
        this.id = "coverage"; // TODO: this needs to be a parameter

        // initialize filtered coverage trees so that they will not be calculated multiple times
        this.changeCoverageTreeRoot = node.getChangeCoverageTree();
        this.indirectCoverageChangesTreeRoot = node.getIndirectCoverageChangesTree();
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

    @JavaScriptMethod
    public CoverageOverview getOverview() {
        return new CoverageOverview(getNode().filterPackageStructure());
    }

    // FIXME: currently not in use anymore
    @JavaScriptMethod
    public CoverageOverview getChangeCoverageOverview() {
        return new CoverageOverview(changeCoverageTreeRoot.filterPackageStructure());
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

    private LinesChartModel createTrendChart(final String configuration) {
        Optional<CoverageBuildAction> latestAction = getLatestAction();
        if (latestAction.isPresent()) {
            return latestAction.get().createProjectAction().createChartModel(configuration);
        }
        return new LinesChartModel();
    }

    private Optional<CoverageBuildAction> getLatestAction() {
        return Optional.ofNullable(getOwner().getAction(CoverageBuildAction.class));
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
    public TreeMapNode getCoverageTree(final String coverageMetric) {
        CoverageMetric metric = getCoverageMetricFromText(coverageMetric);
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(getNode(), metric, colorProvider);
    }

    /**
     * Returns the root of the filtered tree of change coverage nodes for the ECharts treemap. This tree is used as
     * model for the chart on the client side. The tree is available for line and branch coverage.
     *
     * @param coverageMetric
     *         The used coverage metric - the default is the line coverage
     *
     * @return the tree of change coverage nodes for the ECharts treemap
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeMapNode getChangeCoverageTree(final String coverageMetric) {
        CoverageMetric metric = getCoverageMetricFromText(coverageMetric);
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(changeCoverageTreeRoot, metric, colorProvider);
    }

    /**
     * Returns the root of the filtered tree of indirect coverage changes for the ECharts treemap. This tree is used as
     * model for the chart on the client side. The tree is available for line and branch coverage.
     *
     * @param coverageMetric
     *         The used coverage metric - the default is the line coverage
     *
     * @return the tree of indirect coverage changes nodes for the ECharts treemap
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public TreeMapNode getCoverageChangesTree(final String coverageMetric) {
        CoverageMetric metric = getCoverageMetricFromText(coverageMetric);
        return TREE_MAP_NODE_CONVERTER.toTeeChartModel(indirectCoverageChangesTreeRoot, metric, colorProvider);
    }

    /**
     * Gets the {@link CoverageMetric} from a String representation used in the frontend. Only 'Line' and 'Branch' is
     * possible. 'Line' is used as a default.
     *
     * @param text
     *         The coverage metric as String
     *
     * @return the coverage metric
     */
    private CoverageMetric getCoverageMetricFromText(final String text) {
        if ("Branch".equals(text)) {
            return CoverageMetric.BRANCH;
        }
        return CoverageMetric.LINE;
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
        CoverageTableModel.RowRenderer renderer;
        String actualId;
        if (tableId.endsWith(INLINE_SUFFIX) && hasSourceCode()) {
            renderer = new InlineRowRenderer();
        }
        else {
            renderer = new LinkedRowRenderer(getOwner().getRootDir(), getId());
        }
        actualId = tableId.replace(INLINE_SUFFIX, StringUtils.EMPTY);

        switch (actualId) {
            case ABSOLUTE_COVERAGE_TABLE_ID:
                return new CoverageTableModel(tableId, getNode(), renderer, colorProvider);
            case CHANGE_COVERAGE_TABLE_ID:
                return new ChangeCoverageTableModel(tableId, getNode(), changeCoverageTreeRoot, renderer,
                        colorProvider);
            case INDIRECT_COVERAGE_TABLE_ID:
                return new IndirectCoverageChangesTable(tableId, getNode(), indirectCoverageChangesTreeRoot, renderer,
                        colorProvider);
            default:
                throw new NoSuchElementException("No such table with id " + actualId);
        }
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
        return NAVIGATOR.getSameUrlForOtherBuild(owner, currentUrl, CoverageBuildAction.DETAILS_URL,
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
        Optional<CoverageNode> targetResult
                = getNode().findByHashCode(CoverageMetric.FILE, Integer.parseInt(fileHash));
        if (targetResult.isPresent()) {
            try {
                CoverageNode fileNode = targetResult.get();
                return readSourceCode(fileNode, tableId);
            }
            catch (IOException | InterruptedException exception) {
                return ExceptionUtils.getStackTrace(exception);
            }
        }
        return Messages.Coverage_Not_Available();
    }

    /**
     * Reads the sourcecode corresponding to the passed {@link CoverageNode node} and filters the code dependent on the
     * table ID.
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
    private String readSourceCode(final CoverageNode sourceNode, final String tableId)
            throws IOException, InterruptedException {
        String content = "";
        File rootDir = getOwner().getRootDir();
        if (isSourceFileInNewFormatAvailable(sourceNode)) {
            content = SOURCE_CODE_FACADE.read(rootDir, getId(), sourceNode.getPath());
        }
        if (isSourceFileInOldFormatAvailable(sourceNode)) {
            content = new TextFile(getFileForBuildsWithOldVersion(rootDir,
                    sourceNode.getName())).read(); // fallback with sources persisted using the < 2.1.0 serialization
        }
        if (!content.isEmpty() && sourceNode instanceof FileCoverageNode) {
            String cleanTableId = StringUtils.removeEnd(tableId, INLINE_SUFFIX);
            FileCoverageNode fileNode = (FileCoverageNode) sourceNode;
            if (CHANGE_COVERAGE_TABLE_ID.equals(cleanTableId)) {
                return SOURCE_CODE_FACADE.calculateChangeCoverageSourceCode(content, fileNode);
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
     * @since 3.0.0
     */
    @JavaScriptMethod
    public boolean hasSourceCode() {
        return SOURCE_CODE_FACADE.hasStoredSourceCode(getOwner().getRootDir(), id);
    }

    /**
     * Checks whether change coverage exists.
     *
     * @return {@code true} whether change coverage exists, else {@code false}
     */
    public boolean hasChangeCoverage() {
        return getNode().hasChangeCoverage();
    }

    /**
     * Checks whether indirect coverage changes exist.
     *
     * @return {@code true} whether indirect coverage changes exist, else {@code false}
     */
    public boolean hasIndirectCoverageChanges() {
        return getNode().hasIndirectCoverageChanges();
    }

    /**
     * Returns whether the source file is available in Jenkins build folder.
     *
     * @param coverageNode
     *         The {@link CoverageNode} which is checked if there is a source file available
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileAvailable(final CoverageNode coverageNode) {
        return isSourceFileInNewFormatAvailable(coverageNode) || isSourceFileInOldFormatAvailable(coverageNode);
    }

    /**
     * Returns whether the source file is available in Jenkins build folder in the old format of the plugin versions
     * less than 2.1.0.
     *
     * @param coverageNode
     *         The {@link CoverageNode} which is checked if there is a source file available
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileInOldFormatAvailable(final CoverageNode coverageNode) {
        return isSourceFileInOldFormatAvailable(getOwner().getRootDir(), coverageNode.getName());
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
     * @param coverageNode
     *         The {@link CoverageNode} which is checked if there is a source file available
     *
     * @return {@code true} if the source file is available, {@code false} otherwise
     */
    public boolean isSourceFileInNewFormatAvailable(final CoverageNode coverageNode) {
        return isSourceFileInNewFormatAvailable(getOwner().getRootDir(), id, coverageNode.getPath());
    }

    static boolean isSourceFileInNewFormatAvailable(final File rootDir, final String id, final String nodePath) {
        return SOURCE_CODE_FACADE.createFileInBuildFolder(rootDir, id, nodePath).canRead();
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
            return streamCoverages().map(Coverage::getCoveredFraction)
                    .map(Fraction::doubleValue)
                    .collect(Collectors.toList());
        }

        public List<Integer> getMissed() {
            return streamCoverages().map(Coverage::getMissed).collect(Collectors.toList());
        }

        public List<Double> getMissedPercentages() {
            return streamCoverages().map(Coverage::getMissedFraction)
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
     * Used for parsing a Jenkins color mapping JSON string to a color map.
     */
    private static final class ColorMappingType extends TypeReference<HashMap<String, String>> {
    }
}
