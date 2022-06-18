package io.jenkins.plugins.coverage.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import hudson.Functions;

import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProvider.DisplayColors;
import io.jenkins.plugins.coverage.model.visualization.colorization.ColorProviderFactory;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageChangeTendency;
import io.jenkins.plugins.coverage.model.visualization.colorization.CoverageLevel;
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
    private static final ColorProvider COLOR_PROVIDER = ColorProviderFactory.createColorProvider();
    private static final int NO_COVERAGE_SORT = -1000;
    static final DetailedCell<Integer> NO_COVERAGE = new DetailedCell<>(Messages.Coverage_Not_Available(),
            NO_COVERAGE_SORT);
    private final CoverageNode root;
    private final File buildFolder;
    private final String id;
    private final String resultsId;
    private final boolean isInline;

    /**
     * Creates an indirect coverage changes table model.
     *
     * @param id
     *         The ID of the table
     * @param root
     *         The root of the origin coverage tree
     * @param buildFolder
     *         the build folder to store the source code files
     * @param resultsId
     *         the ID of the results as prefix for the source code files in the build folder
     */
    public CoverageTableModel(final String id, final CoverageNode root, final File buildFolder, final String resultsId, final boolean isInline) {
        super();

        this.root = root;
        this.buildFolder = buildFolder;
        this.id = id;
        this.resultsId = resultsId;
        this.isInline = isInline;
    }

    String getResultsId() {
        return resultsId;
    }

    File getBuildFolder() {
        return buildFolder;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public TableConfiguration getTableConfiguration() {
        TableConfiguration tableConfiguration = new TableConfiguration();
        tableConfiguration.responsive();
        if (isInline) {
            tableConfiguration.select(SelectStyle.SINGLE);
        }
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
                .withResponsivePriority(50000)
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
        TableColumn branchCoverageDelta = new ColumnBuilder().withHeaderLabel(Messages.Column_DeltaLineCoverage("Δ"))
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
        return root.getAll(CoverageMetric.FILE).stream()
                .map(file -> new CoverageRow(file, buildFolder, getResultsId(), browserLocale, isInline))
                .collect(Collectors.toList());
    }

    protected CoverageNode getRoot() {
        return root;
    }

    /**
     * UI row model for the coverage details table.
     */
    static class CoverageRow {
        private static final String COVERAGE_COLUMN_OUTER = "coverage-column-outer float-end";
        private static final String COVERAGE_COLUMN_INNER = "coverage-column-inner";
        private final CoverageNode root;
        private final File buildFolder;
        private final String resultsId;
        private final Locale browserLocale;
        private boolean isInline;

        CoverageRow(final CoverageNode root, final File buildFolder, final String resultsId, final Locale browserLocale) {
            this(root, buildFolder, resultsId, browserLocale, false);
        }

        CoverageRow(final CoverageNode root, final File buildFolder, final String resultsId, final Locale browserLocale, final boolean isInline) {
            this.root = root;
            this.buildFolder = buildFolder;
            this.resultsId = resultsId;
            this.browserLocale = browserLocale;
            this.isInline = isInline;
        }

        public String getFileHash() {
            return String.valueOf(root.getName().hashCode());
        }

        public String getFileName() {
            String fileName = root.getName();
            if (!isInline && CoverageViewModel.isSourceFileInNewFormatAvailable(buildFolder, resultsId, root.getPath())
                    || CoverageViewModel.isSourceFileInOldFormatAvailable(buildFolder, fileName)) {
                return a().withHref(String.valueOf(fileName.hashCode())).withText(fileName).render();
            }
            return fileName;
        }

        public String getPackageName() {
            return root.getParentName();
        }

        public DetailedCell<?> getLineCoverage() {
            Coverage coverage = root.getCoverage(CoverageMetric.LINE);
            return createColoredCoverageColumn(coverage, "The total line coverage of the file");
        }

        public DetailedCell<?> getBranchCoverage() {
            Coverage coverage = root.getCoverage(CoverageMetric.BRANCH);
            return createColoredCoverageColumn(coverage, "The total branch coverage of the file");
        }

        public DetailedCell<?> getLineCoverageDelta() {
            return createColoredFileCoverageDeltaColumn(CoverageMetric.LINE);
        }

        public DetailedCell<?> getBranchCoverageDelta() {
            return createColoredFileCoverageDeltaColumn(CoverageMetric.BRANCH);
        }

        public int getLoc() {
            if (root instanceof FileCoverageNode) { // FIXME: Move LOC up in the hierarchy
                return ((FileCoverageNode) root).getCoveragePerLine().size();
            }
            return 0;
        }

        /**
         * Creates a table cell which colorizes the shown coverage dependent on the coverage percentage.
         *
         * @param coverage
         *         the coverage of the element
         * @param tooltip
         *         the tooltip which describes the value
         *
         * @return the new {@link DetailedCell}
         */
        protected DetailedCell<?> createColoredCoverageColumn(final Coverage coverage, final String tooltip) {
            if (coverage.isSet()) {
                double percentage = coverage.getCoveredPercentage().getDoubleValue();
                DisplayColors colors = CoverageLevel.getDisplayColorsOfCoverageLevel(percentage, COLOR_PROVIDER);
                String cell = div().withClasses(COVERAGE_COLUMN_OUTER).with(
                                div().withClasses(COVERAGE_COLUMN_INNER)
                                        .withStyle(String.format(
                                                "background-image: linear-gradient(90deg, %s %f%%, transparent %f%%);",
                                                colors.getFillColorAsHex(),
                                                percentage, percentage))
                                        .withTitle(tooltip)
                                        .withText(coverage.formatCoveredPercentage(browserLocale)))
                        .render();
                return new DetailedCell<>(cell, percentage);
            }
            return NO_COVERAGE;
        }

        /**
         * Creates a table cell which colorizes the tendency of the shown coverage delta.
         *
         * @param coveragePercentage
         *         The coverage delta as percentage
         * @param tooltip
         *         The tooltip which describes the value
         *
         * @return the created {@link DetailedCell}
         */
        protected DetailedCell<?> createColoredCoverageDeltaColumn(
                final CoveragePercentage coveragePercentage, final String tooltip) {
            double coverageValue = coveragePercentage.getDoubleValue();
            DisplayColors colors = CoverageChangeTendency.getDisplayColorsForTendency(coverageValue, COLOR_PROVIDER);
            String cell = div().withClasses(COVERAGE_COLUMN_OUTER).with(
                            div().withClasses(COVERAGE_COLUMN_INNER)
                                    .withStyle(String.format("background-color:%s;", colors.getFillColorAsHex()))
                                    .withText(coveragePercentage.formatDeltaPercentage(browserLocale))
                                    .withTitle(tooltip))
                    .render();
            return new DetailedCell<>(cell, coverageValue);
        }

        protected CoverageNode getRoot() {
            return root;
        }

        /**
         * Creates a colored column for visualizing the file coverage delta against a reference for the passed
         * {@link CoverageMetric}.
         *
         * @param coverageMetric
         *         The coverage metric
         *
         * @return the created {@link DetailedCell}
         * @since 3.0.0
         */
        private DetailedCell<?> createColoredFileCoverageDeltaColumn(final CoverageMetric coverageMetric) {
            // this is only available for versions later than 3.0.0 which introduced FileCoverageNode
            if (root instanceof FileCoverageNode) {
                FileCoverageNode fileNode = (FileCoverageNode) root;
                if (fileNode.hasFileCoverageDelta(coverageMetric)) {
                    CoveragePercentage delta = fileNode.getFileCoverageDeltaForMetric(coverageMetric);
                    return createColoredCoverageDeltaColumn(delta,
                            "The total file coverage delta against the reference build");
                }
            }
            return NO_COVERAGE;
        }
    }
}
