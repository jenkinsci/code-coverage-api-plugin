package io.jenkins.plugins.coverage.metrics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.LinesOfCode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.Node;

import hudson.Functions;

import io.jenkins.plugins.coverage.metrics.visualization.code.SourceCodeFacade;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.metrics.visualization.colorization.CoverageLevel;
import io.jenkins.plugins.coverage.model.Messages;
import io.jenkins.plugins.datatables.DetailedCell;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableColumn.ColumnBuilder;
import io.jenkins.plugins.datatables.TableColumn.ColumnCss;
import io.jenkins.plugins.datatables.TableColumn.ColumnType;
import io.jenkins.plugins.datatables.TableConfiguration;
import io.jenkins.plugins.datatables.TableConfiguration.SelectStyle;
import io.jenkins.plugins.datatables.TableModel;

import static j2html.TagCreator.*;

/**
 * UI table model for the coverage details table.
 */
class CoverageTableModel extends TableModel {
    private static final int NO_COVERAGE_SORT = -1_000;
    private static final SourceCodeFacade SOURCE_CODE_FACADE = new SourceCodeFacade();

    /**
     * The alpha value for colors to be used to highlight the coverage within the table view.
     */
    private static final int TABLE_COVERAGE_COLOR_ALPHA = 80;

    static final DetailedCell<Integer> NO_COVERAGE
            = new DetailedCell<>(Messages.Coverage_Not_Available(), NO_COVERAGE_SORT);

    private final ColorProvider colorProvider;
    private final Node root;
    private final RowRenderer renderer;
    private final String id;

    /**
     * Creates an indirect coverage changes table model.
     *
     * @param id
     *         The ID of the table
     * @param root
     *         The root of the coverage tree
     * @param renderer
     *         the renderer to use for the file names
     * @param colors
     *         The {@link ColorProvider} which provides the used colors
     */
    CoverageTableModel(final String id, final Node root, final RowRenderer renderer, final ColorProvider colors) {
        super();

        this.id = id;
        this.root = root;
        this.renderer = renderer;
        colorProvider = colors;
    }

    RowRenderer getRenderer() {
        return renderer;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TableConfiguration getTableConfiguration() {
        TableConfiguration tableConfiguration = new TableConfiguration();
        tableConfiguration.responsive();
        renderer.configureTable(tableConfiguration);
        return tableConfiguration;
    }

    @Override
    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();

        TableColumn fileHash = new ColumnBuilder().withHeaderLabel("Hash")
                .withDataPropertyKey("fileHash")
                .withHeaderClass(ColumnCss.HIDDEN)
                .build();
        columns.add(fileHash);
        TableColumn fileName = new ColumnBuilder().withHeaderLabel(Messages.Column_File())
                .withDataPropertyKey("fileName")
                .withResponsivePriority(1)
                .build();
        columns.add(fileName);
        TableColumn packageName = new ColumnBuilder().withHeaderLabel(Messages.Column_Package())
                .withDataPropertyKey("packageName")
                .withResponsivePriority(50_000)
                .build();
        columns.add(packageName);
        TableColumn lineCoverage = new ColumnBuilder().withHeaderLabel(Messages.Column_LineCoverage())
                .withDataPropertyKey("lineCoverage")
                .withDetailedCell()
                .withType(ColumnType.NUMBER)
                .withResponsivePriority(1)
                .build();
        columns.add(lineCoverage);
        TableColumn lineCoverageDelta = new ColumnBuilder().withHeaderLabel(Messages.Column_DeltaLineCoverage("Δ"))
                .withDataPropertyKey("lineCoverageDelta")
                .withDetailedCell()
                .withType(ColumnType.NUMBER)
                .withResponsivePriority(2)
                .build();
        columns.add(lineCoverageDelta);
        TableColumn branchCoverage = new ColumnBuilder().withHeaderLabel(Messages.Column_BranchCoverage())
                .withDataPropertyKey("branchCoverage")
                .withDetailedCell()
                .withType(ColumnType.NUMBER)
                .withResponsivePriority(1)
                .build();
        columns.add(branchCoverage);
        TableColumn branchCoverageDelta = new ColumnBuilder().withHeaderLabel(
                        Messages.Column_DeltaBranchCoverage("Δ"))
                .withDataPropertyKey("branchCoverageDelta")
                .withDetailedCell()
                .withType(ColumnType.NUMBER)
                .withResponsivePriority(2)
                .build();
        columns.add(branchCoverageDelta);
        TableColumn loc = new ColumnBuilder().withHeaderLabel(Messages.Column_LinesOfCode())
                .withDataPropertyKey("loc")
                .withResponsivePriority(200)
                .withType(ColumnType.NUMBER)
                .build();
        columns.add(loc);

        return columns;
    }

    @Override
    public List<Object> getRows() {
        Locale browserLocale = Functions.getCurrentLocale();
        return root.getAllFileNodes().stream()
                .map(file -> new CoverageRow(file, browserLocale, renderer, colorProvider))
                .collect(Collectors.toList());
    }

    protected Node getRoot() {
        return root;
    }

    protected ColorProvider getColorProvider() {
        return colorProvider;
    }

    /**
     * UI row model for the coverage details table.
     */
    static class CoverageRow {
        private static final String COVERAGE_COLUMN_OUTER = "coverage-column-outer float-end";
        private static final String COVERAGE_COLUMN_INNER = "coverage-column-inner";
        private static final ElementFormatter FORMATTER = new ElementFormatter();

        private final FileNode file;
        private final Locale browserLocale;
        private final RowRenderer renderer;
        private final ColorProvider colorProvider;

        CoverageRow(final FileNode file, final Locale browserLocale, final RowRenderer renderer, final ColorProvider colors) {
            this.file = file;
            this.browserLocale = browserLocale;
            this.renderer = renderer;
            colorProvider = colors;
        }

        public String getFileHash() {
            return String.valueOf(file.getPath().hashCode());
        }

        public String getFileName() {
            return renderer.renderFileName(file.getName(), file.getPath());
        }

        public String getPackageName() {
            return file.getParentName();
        }

        public DetailedCell<?> getLineCoverage() {
            return createColoredCoverageColumn(getCoverageOfNode(Metric.LINE));
        }

        public DetailedCell<?> getBranchCoverage() {
            return createColoredCoverageColumn(getCoverageOfNode(Metric.BRANCH));
        }

        Coverage getCoverageOfNode(final Metric metric) {
            return file.getTypedValue(metric, Coverage.nullObject(metric));
        }

        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredFileCoverageDeltaColumn(Metric.LINE);
        }

        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredFileCoverageDeltaColumn(Metric.BRANCH);
        }

        public int getLoc() {
            return file.getTypedValue(Metric.LOC, new LinesOfCode(0)).getValue();
        }

        /**
         * Creates a table cell which colorizes the shown coverage dependent on the coverage percentage.
         *
         * @param coverage
         *         the coverage of the element
         * @return the new {@link DetailedCell}
         */
        protected DetailedCell<?> createColoredCoverageColumn(final Coverage coverage) {
            if (coverage.isSet()) {
                double percentage = coverage.getCoveredPercentage().doubleValue() * 100.0;
                DisplayColors colors = CoverageLevel.getDisplayColorsOfCoverageLevel(percentage, colorProvider);
                String cell = div().withClasses(COVERAGE_COLUMN_OUTER).with(
                                div().withClasses(COVERAGE_COLUMN_INNER)
                                        .withStyle(String.format(
                                                "background-image: linear-gradient(90deg, %s %f%%, transparent %f%%);",
                                                colors.getFillColorAsRGBAHex(TABLE_COVERAGE_COLOR_ALPHA),
                                                percentage, percentage))
                                        .withText(FORMATTER.formatPercentage(coverage, browserLocale)))
                        .render();
                return new DetailedCell<>(cell, percentage);
            }
            return NO_COVERAGE;
        }

        /**
         * Creates a table cell which colorizes the tendency of the shown coverage delta.
         *
         * @param delta
         *         The coverage delta as percentage
         *
         * @return the created {@link DetailedCell}
         */
        protected DetailedCell<?> createColoredCoverageDeltaColumn(final Metric metric, final Fraction delta) {
            double percentage = delta.doubleValue() * 100.0;
            DisplayColors colors = CoverageChangeTendency.getDisplayColorsForTendency(percentage, colorProvider);
            String cell = div().withClasses(COVERAGE_COLUMN_OUTER).with(
                            div().withClasses(COVERAGE_COLUMN_INNER)
                                    .withStyle(String.format("background-color:%s;", colors.getFillColorAsRGBAHex(
                                            TABLE_COVERAGE_COLOR_ALPHA)))
                                    .withText(FORMATTER.formatDelta(metric, delta, browserLocale)))
                    .render();
            return new DetailedCell<>(cell, percentage);
        }

        protected FileNode getFile() {
            return file;
        }

        /**
         * Creates a colored column for visualizing the file coverage delta against a reference for the passed
         * {@link Metric}.
         *
         * @param metric
         *         The coverage metric
         *
         * @return the created {@link DetailedCell}
         * @since 3.0.0
         */
        private DetailedCell<?> createColoredFileCoverageDeltaColumn(final Metric metric) {
            if (file.hasChangeCoverage(metric)) {
                return createColoredCoverageDeltaColumn(metric, file.getChangeCoverage(metric));
            }
            return NO_COVERAGE;
        }
    }

    /**
     * Renders filenames with links. Selection will be handled by opening a new page using the provided link.
     */
    static class LinkedRowRenderer implements RowRenderer {
        private final File buildFolder;
        private final String resultsId;

        LinkedRowRenderer(final File buildFolder, final String resultsId) {
            this.buildFolder = buildFolder;
            this.resultsId = resultsId;
        }

        @Override
        public void configureTable(final TableConfiguration tableConfiguration) {
            // nothing required
        }

        @Override
        public String renderFileName(final String fileName, final String path) {
            if (SOURCE_CODE_FACADE.createFileInBuildFolder(buildFolder, resultsId, path).canRead()) {
                return a().withHref(String.valueOf(path.hashCode())).withText(fileName).render();
            }
            return fileName;
        }
    }

    /**
     * Renders filenames without links. Selection will be handled using the table select events.
     */
    static class InlineRowRenderer implements RowRenderer {
        @Override
        public void configureTable(final TableConfiguration tableConfiguration) {
            tableConfiguration.select(SelectStyle.SINGLE);
        }

        @Override
        public String renderFileName(final String fileName, final String path) {
            return fileName;
        }
    }

    /**
     * Renders filenames in table cells.
     */
    interface RowRenderer {
        void configureTable(TableConfiguration tableConfiguration);

        String renderFileName(String fileName, String path);
    }
}
