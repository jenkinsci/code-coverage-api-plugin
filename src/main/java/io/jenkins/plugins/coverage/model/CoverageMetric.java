package io.jenkins.plugins.coverage.model;

import java.io.Serializable;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A coverage metric to identify the coverage result type. Note: this class has a natural ordering that is inconsistent
 * with equals.
 *
 * @author Ullrich Hafner
 */
public class CoverageMetric implements Comparable<CoverageMetric>, Serializable {
    private static final long serialVersionUID = 3664773349525399092L;
    private static final int DEFAULT_ORDER = 100;

    private enum MetricType {
        LEAF,
        COMPOSITE
    }

    /** Module coverage. */
    public static final CoverageMetric MODULE = new CoverageMetric("Module", -10);
    private static final CoverageMetric REPORT = new CoverageMetric("Report", -10);
    /** Package or namespace coverage. */
    public static final CoverageMetric PACKAGE = new CoverageMetric("Package", 1);
    /** File coverage. */
    public static final CoverageMetric FILE = new CoverageMetric("File", 2);
    /** Class coverage. */
    public static final CoverageMetric CLASS = new CoverageMetric("Class", 3);
    /** Method coverage. */
    public static final CoverageMetric METHOD = new CoverageMetric("Method", 4);
    /** Instruction coverage. */
    public static final CoverageMetric INSTRUCTION = new CoverageMetric("Instruction", 11, MetricType.LEAF);
    /** Line coverage. */
    public static final CoverageMetric LINE = new CoverageMetric("Line", 10, MetricType.LEAF);
    /** Branch coverage. */
    public static final CoverageMetric BRANCH = new CoverageMetric("Branch", 12, MetricType.LEAF);
    private static final CoverageMetric CONDITIONAL = new CoverageMetric("Conditional", 12, MetricType.LEAF);

    /**
     * Creates a new {@link CoverageMetric} with the specified name. If the name is the same as the name of one of the
     * predefined metrics, then the existing metric is returned.
     *
     * @param name
     *         the name of the metric
     *
     * @return the metric
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    public static CoverageMetric valueOf(final String name) {
        if (MODULE.equalsIgnoreCase(name) || REPORT.equalsIgnoreCase(name)) {
            return MODULE;
        }
        if (PACKAGE.equalsIgnoreCase(name)) {
            return PACKAGE;
        }
        if (FILE.equalsIgnoreCase(name)) {
            return FILE;
        }
        if (CLASS.equalsIgnoreCase(name)) {
            return CLASS;
        }
        if (METHOD.equalsIgnoreCase(name)) {
            return METHOD;
        }
        if (INSTRUCTION.equalsIgnoreCase(name)) {
            return INSTRUCTION;
        }
        if (LINE.equalsIgnoreCase(name)) {
            return LINE;
        }
        if (BRANCH.equalsIgnoreCase(name) || CONDITIONAL.equalsIgnoreCase(name)) {
            return BRANCH;
        }
        return new CoverageMetric(name, DEFAULT_ORDER);
    }

    /**
     * Checks if this instance has a name that is equal to the specified name (ignoring case).
     *
     * @param searchName
     *         the coverage metric name to check
     *
     * @return {@code true} if this instance has the same name, {@code false} otherwise
     */
    public boolean equalsIgnoreCase(final String searchName) {
        return equalsIgnoreCase(getName(), searchName);
    }

    /**
     * <p>Compares two CharSequences, returning {@code true} if they represent
     * equal sequences of characters, ignoring case.</p>
     *
     * <p>{@code null}s are handled without exceptions. Two {@code null}
     * references are considered equal. The comparison is <strong>case insensitive</strong>.</p>
     *
     * <pre>
     * StringUtils.equalsIgnoreCase(null, null)   = true
     * StringUtils.equalsIgnoreCase(null, "abc")  = false
     * StringUtils.equalsIgnoreCase("abc", null)  = false
     * StringUtils.equalsIgnoreCase("abc", "abc") = true
     * StringUtils.equalsIgnoreCase("abc", "ABC") = true
     * </pre>
     *
     * @param a
     *         the first CharSequence, may be {@code null}
     * @param b
     *         the second CharSequence, may be {@code null}
     *
     * @return {@code true} if the CharSequences are equal (case-insensitive), or both {@code null}
     */
    public static boolean equalsIgnoreCase(@CheckForNull final String a, @CheckForNull final String b) {
        return StringUtils.equals(normalize(a), normalize(b));
    }

    private static String normalize(@CheckForNull final String input) {
        return StringUtils.defaultString(input).toUpperCase(Locale.ENGLISH);
    }

    private final String name;
    private final int order;
    private final boolean leaf;

    private CoverageMetric(final String name, final int order) {
        this(name, order, MetricType.COMPOSITE);
    }

    private CoverageMetric(final String name, final int order, final MetricType type) {
        this.name = name;
        this.order = order;
        this.leaf = type == MetricType.LEAF;
    }

    public String getName() {
        return name;
    }

    public int getOrder() {
        return order;
    }

    public boolean isLeaf() {
        return leaf;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int compareTo(final CoverageMetric other) {
        return order - other.order;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CoverageMetric that = (CoverageMetric) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
