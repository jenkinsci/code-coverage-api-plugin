package io.jenkins.plugins.coverage.metrics.source;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Mutation;

import j2html.tags.ContainerTag;
import j2html.tags.UnescapedText;

import static j2html.TagCreator.*;

/**
 * Provides all required information for a {@link FileNode} so that its source code can be rendered together with the
 * line and mutation coverage in HTML.
 */
class MutationSourcePrinter extends CoverageSourcePrinter {
    private static final long serialVersionUID = -2215657894423024907L;

    private final int[] survivedPerLine;
    private final int[] killedPerLine;
    private final String[] tooltipPerLine;

    MutationSourcePrinter(final FileNode file) {
        super(file);

        survivedPerLine = new int[size()];
        killedPerLine = new int[size()];
        tooltipPerLine = new String[size()];
        Arrays.fill(tooltipPerLine, StringUtils.EMPTY);

        extractMutationDetails(file.getMutationsPerLine());

        for (Mutation mutation : file.getMutations()) {
            if (mutation.hasSurvived()) {
                survivedPerLine[findIndexOfLine(mutation.getLine())]++;
            }
            else if (mutation.isKilled()) {
                killedPerLine[findIndexOfLine(mutation.getLine())]++;
            }
        }
    }

    private void extractMutationDetails(final NavigableMap<Integer, List<Mutation>> mutationsPerLine) {
        for (Entry<Integer, List<Mutation>> entry : mutationsPerLine.entrySet()) {
            var indexOfLine = findIndexOfLine(entry.getKey());

            tooltipPerLine[indexOfLine] = createInfo(entry.getValue());
        }
    }

    private String createInfo(final List<Mutation> allMutations) {
        ContainerTag killedContainer = listMutations(allMutations,
                Mutation::isKilled, "Killed Mutations:");
        ContainerTag survivedContainer = listMutations(allMutations,
                Mutation::hasSurvived, "Survived Mutations:");
        if (killedContainer.getNumChildren() == 0 && survivedContainer.getNumChildren() == 0) {
            return "Not covered";
        }
        return div().with(killedContainer, survivedContainer).render();
    }

    private ContainerTag listMutations(final List<Mutation> allMutations,
            final Predicate<Mutation> predicate, final String title) {
        var filtered = div();
        var killed = asBulletPoints(allMutations, predicate);
        if (!killed.isEmpty()) {
            filtered.with(div().with(new UnescapedText(title), ul().with(killed)));
        }
        return filtered;
    }

    private List<ContainerTag> asBulletPoints(final List<Mutation> mutations, final Predicate<Mutation> predicate) {
        return mutations.stream().filter(predicate).map(mutation ->
                li().withText(String.format("%s (%s)", mutation.getDescription(), mutation.getMutator())))
                .collect(Collectors.toList());
    }

    public int getSurvived(final int line) {
        return getCounter(line, survivedPerLine);
    }

    public int getKilled(final int line) {
        return getCounter(line, killedPerLine);
    }

    @Override
    public String getColorClass(final int line) {
        if (getCovered(line) == 0) {
            return NO_COVERAGE;
        }
        if (getKilled(line) == 0) {
            return NO_COVERAGE;
        }
        else if (getSurvived(line) == 0) {
            return FULL_COVERAGE;
        }
        else {
            return PARTIAL_COVERAGE;
        }
    }

    @Override
    public String getTooltip(final int line) {
        return StringUtils.defaultIfBlank(tooltipPerLine[findIndexOfLine(line)], super.getTooltip(line));
    }

    @Override
    public String getSummaryColumn(final int line) {
        var killed = getKilled(line);
        var survived = getSurvived(line);
        if (survived + killed > 0) {
            return String.format("%d/%d", killed, survived + killed);
        }
        return String.valueOf(killed);
    }
}
