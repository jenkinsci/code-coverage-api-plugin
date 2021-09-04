package io.jenkins.plugins.coverage.targets;

import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;

public class CoverageElement implements Comparable<CoverageElement>, Serializable {
    private static final long serialVersionUID = 6722992955158201174L;

    public final static CoverageElement AGGREGATED_REPORT = new CoverageElement("Aggregated Report", Integer.MIN_VALUE);
    public final static CoverageElement REPORT = new CoverageElement("Report", Integer.MIN_VALUE + 1);

    public final static CoverageElement FILE = new CoverageElement("File", 2);

    public final static CoverageElement LINE = new CoverageElement("Line", Integer.MAX_VALUE - 1, true);
    public final static CoverageElement CONDITIONAL = new CoverageElement("Branch", Integer.MAX_VALUE, true);


    public final static String COVERAGE_ELEMENT_TYPE_NONE = "None";
    public final static String COVERAGE_ELEMENT_TYPE_JAVA = "Java";
    public final static String COVERAGE_ELEMENT_TYPE_JAVASCRIPT = "JavaScript";

    private final String name;
    private final int order;
    private final boolean isBasicBlock;

    public CoverageElement(final String name, final int order) {
        this(name, order, false);
    }

    public CoverageElement(final String name, final int order, final boolean isBasicBlock) {
        this.name = name;
        this.order = order;
        this.isBasicBlock = isBasicBlock;
    }

    public String getName() {
        return name;
    }

    public boolean isBasicBlock() {
        return isBasicBlock;
    }

    public boolean is(final String name) {
        return this.name.equals(name);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoverageElement that = (CoverageElement) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public int compareTo(@Nonnull final CoverageElement coverageElement) {
        if (this.order == coverageElement.order) {
            return 0;
        }

        return this.order < coverageElement.order ? -1 : 1;
    }

    public static CoverageElement get(final String name) {
        // Type element are equal when their names are equal,
        // so we can get CoverageElement just by name to keep the code simpler
        return CoverageElementRegister.getDespiteType(name);
    }

    public static CoverageElement get(final String type, final String name) {
        return CoverageElementRegister.get(type, name);
    }
}
