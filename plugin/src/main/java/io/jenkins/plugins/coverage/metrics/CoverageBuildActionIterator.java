package io.jenkins.plugins.coverage.metrics;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;

import hudson.model.Run;

import io.jenkins.plugins.coverage.metrics.model.CoverageStatistics;
import io.jenkins.plugins.util.BuildAction;

/**
 * Iterates over a collection of builds that contain results of a given generic type. These results are available via a
 * given subtype of {@link BuildAction} that has to be attached to each build of the selected job. A new iterator
 * starts from a baseline build where it selects the attached action of the given type. Then it moves back in the build
 * history until no more builds are available.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CoverageBuildActionIterator implements Iterator<BuildResult<CoverageStatistics>> {
    private final ActionSelector<CoverageBuildAction> actionSelector;
    private Optional<CoverageBuildAction> latestAction;

    CoverageBuildActionIterator(final Optional<CoverageBuildAction> latestAction,
            final Predicate<CoverageBuildAction> filter) {
        this.latestAction = latestAction;
        actionSelector = new ActionSelector<>(CoverageBuildAction.class, filter);
    }

    @Override
    public boolean hasNext() {
        return latestAction.isPresent();
    }

    @Override
    public BuildResult<CoverageStatistics> next() {
        if (latestAction.isEmpty()) {
            throw new NoSuchElementException(
                    "There is no action available anymore. Use hasNext() before calling next().");
        }

        CoverageBuildAction buildAction = latestAction.get();
        Run<?, ?> run = buildAction.getOwner();
        latestAction = actionSelector.apply(run.getPreviousBuild());

        int buildTimeInSeconds = (int) (run.getTimeInMillis() / 1000);
        Build build = new Build(run.getNumber(), run.getDisplayName(), buildTimeInSeconds);

        return new BuildResult<>(build, buildAction.getStatistics());
    }

    private static class ActionSelector<T extends BuildAction<?>> implements Function<Run<?, ?>, Optional<T>> {
        private final Class<T> actionType;
        private final Predicate<? super T> predicate;

        ActionSelector(final Class<T> actionType, final Predicate<? super T> predicate) {
            this.actionType = actionType;
            this.predicate = predicate;
        }

        @Override
        public Optional<T> apply(final Run<?, ?> baseline) {
            for (Run<?, ?> run = baseline; run != null; run = run.getPreviousBuild()) {
                Optional<T> action = run.getActions(actionType)
                        .stream()
                        .filter(predicate)
                        .findAny();
                if (action.isPresent()) {
                    return action;
                }
            }

            return Optional.empty();
        }
    }

    public static class CIterable implements Iterable<BuildResult<CoverageStatistics>> {
        private final Optional<CoverageBuildAction> latestAction;
        private final Predicate<CoverageBuildAction> filter;

        CIterable(final Optional<CoverageBuildAction> latestAction, final Predicate<CoverageBuildAction> filter) {
            this.latestAction = latestAction;
            this.filter = filter;
        }

        @Override
        public Iterator<BuildResult<CoverageStatistics>> iterator() {
            return new CoverageBuildActionIterator(latestAction, filter);
        }
    }
}
