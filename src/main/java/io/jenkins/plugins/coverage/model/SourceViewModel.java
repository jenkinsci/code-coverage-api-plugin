package io.jenkins.plugins.coverage.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.ModelObject;
import hudson.model.Run;

import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageResult;
import io.jenkins.plugins.coverage.targets.CoverageResult.CoverageStatistics;
import io.jenkins.plugins.coverage.targets.Ratio;

/**
 * Server side model that provides the data for the source code view of the coverage results. The layout of the associated
 * view is defined corresponding jelly view 'index.jelly'.
 *
 * @author Ullrich Hafner
 */
public class SourceViewModel implements ModelObject {
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
     * @param displayName
     *         human-readable name of this view (used in bread-crumb)
     */
    public SourceViewModel(final Run<?, ?> owner, final CoverageResult result, final String displayName) {
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

    /**
     * Interface for javascript code to get code coverage result.
     *
     * @return aggregated coverage results
     */
    @JavaScriptMethod
    public List<CoverageStatistics> getOverallStatistics() {
        List<CoverageStatistics> results = new ArrayList<>();

        List<Entry<CoverageElement, Ratio>> elements = new ArrayList<>(getResult().getResults().entrySet());
        elements.sort(Collections.reverseOrder(Entry.comparingByKey()));

        for (Map.Entry<CoverageElement, Ratio> c : elements) {
            results.add(new CoverageStatistics(c.getKey().getName(), c.getValue()));
        }

        return results;
    }

    @Override
    public String getDisplayName() {
        return Messages.Coverage_Title(displayName);
    }

    public String getContent() {
        return result.getSourceFileContent();
    }
}
