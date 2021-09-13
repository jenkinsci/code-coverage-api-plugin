/*
 * Copyright (c) 2007-2018 Stephen Connolly, Michael Barrientos, Jeff Pearce, Shenyu Zheng and Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.coverage.targets;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import hudson.model.AbstractBuild;
import hudson.model.Item;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.TextFile;

import io.jenkins.plugins.coverage.BuildUtils;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.exception.CoverageException;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/

/**
 * <p>Coverage result for a specific programming element.</p>
 * Instances of {@link CoverageResult} form a tree structure to progressively represent smaller elements.
 *
 * @author Stephen Connolly
 * @since 22-Aug-2007 18:47:10
 */
@ExportedBean(defaultVisibility = 2)
public class CoverageResult implements Serializable, Chartable, ModelObject {
    private static final long serialVersionUID = -3524882671364156445L;

    private static final int DEFAULT_MAX_BUILDS_SHOW_IN_TREND = 6;

    /**
     * The type of the programming element.
     */
    private final CoverageElement element;

    /**
     * Name of the programming element that this result object represent, such as package name, class name, method name,
     * etc.
     */
    private String name;
    private String tag;

    private String referenceBuildUrl = null;
    private float changeRequestCoverageDiffWithTargetBranch = 0;

    // these two pointers form a tree structure where edges are names.
    @CheckForNull
    private CoverageResult parent;

    private final Map<String, CoverageResult> children = new TreeMap<>();

    // FIXME: storing different maps does not make much sense?
    private final Map<CoverageElement, Ratio> aggregateResults = new TreeMap<>();

    private final Map<CoverageElement, Ratio> localResults = new TreeMap<>();

    private final Map<CoverageElement, Float> deltaResults = new TreeMap<>();

    /**
     * Line-by-line coverage information. Computed lazily, since it's memory intensive.
     */
    private CoveragePaint paint;

    private String relativeSourcePath;

    private final Map<String, Set<String>> additionalProperties = new HashMap<>();

    public transient Run<?, ?> owner = null;

    public CoverageResult(final CoverageElement elementType, final CoverageResult parent, final String name) {
        this.element = elementType;
        this.parent = parent;
        this.name = name;
        this.relativeSourcePath = null;
        if (this.parent != null) {
            if (parent.getPaint() != null) {
                this.paint = new CoveragePaint(element);
            }
            this.parent.children.put(name, this);
        }
    }

    // ---------- REFACTORING START ------------------

    // FIXME: currently this class handles UI requests and stores the coverage model
    //        it would make more sense to split this information

    public void stripGroup() {
        if (getElement().equals(CoverageElement.REPORT) && hasSingletonChild()) {
            CoverageResult group = getSingletonChild();
            if (group.getElement().getName().equals("Group")) {
                children.clear();
                children.putAll(group.children);
            }

        }
    }

    @Override
    public String toString() {
        return String.format("CoverageResult of %s (line: %s, branch: %s)", name,
                formatCoverage(CoverageElement.LINE), formatCoverage(CoverageElement.CONDITIONAL));
    }

    /**
     * Returns the size of this result, i.e. the number of children that are part of this report.
     *
     * @return the number of children
     */
    public int size() {
        return getChildren().size();
    }

    /**
     * Returns the most important coverage elements.
     *
     * @return most important coverage elements
     */
    public Collection<CoverageElement> getImportantElements() {
        List<CoverageElement> importantElements = new ArrayList<>();
        importantElements.add(CoverageElement.LINE);
        importantElements.add(CoverageElement.CONDITIONAL);
        importantElements.retainAll(aggregateResults.keySet());
        return importantElements;
    }

    /**
     * Returns whether a delta result with respect to the reference build is available.
     *
     * @param coverageElement
     *         the element to check
     *
     * @return {@code true} if a delta result is available, {@code false} otherwise
     */
    public boolean hasDelta(final CoverageElement coverageElement) {
        return deltaResults.containsKey(coverageElement);
    }

    /**
     * Returns the delta result with respect to the reference build.
     *
     * @param coverageElement
     *         the element to get the delta for
     *
     * @return the delta result (if available)
     */
    public String getDelta(final CoverageElement coverageElement) {
        Float delta = deltaResults.get(coverageElement);
        if (delta == null) {
            return "n/a";
        }
        return String.format("%+.3f", delta);
    }

    private String formatCoverage(final CoverageElement coverageElement) {
        Ratio ratio = getCoverage(coverageElement);

        return Ratio.NULL.equals(ratio) ? "n/a" : ratio.getPercentageString();
    }

    /**
     * Returns whether a delta computation with a reference build is available.
     *
     * @return {@code true} if there is a reference build, {@code false} if not
     */
    public boolean hasReferenceBuild() {
        return StringUtils.isNotBlank(referenceBuildUrl);
    }

    public Map<CoverageElement, Ratio> getLocalResults() {
        return localResults;
    }

    /**
     * Returns the singleton child of this result.
     *
     * @return the singleton child of this result
     * @throws NoSuchElementException
     *         if this result does not contain children
     * @see #hasSingletonChild()
     */
    public CoverageResult getSingletonChild() {
        return children.values()
                .stream()
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("No child found"));
    }

    /**
     * Returns whether this result has a singleton child.
     *
     * @return {@code true} if this result has a singleton child, {@code false} otherwise
     */
    public boolean hasSingletonChild() {
        return children.size() == 1;
    }

    /**
     * Returns whether this result has a parent.
     *
     * @return {@code true} if this result has a parent, {@code false} if it is the root of the hierarchy
     */
    public boolean hasParent() {
        return parent != null;
    }

    /**
     * Returns the name of the parent element or "-" if there is no such element.
     *
     * @return the name of the parent element
     */
    public String getParentName() {
        return parent != null ? parent.getName() : "-";
    }

    /**
     * Returns recursively all elements of the given type.
     *
     * @param element
     *         the element type to look for
     *
     * @return all elements of the given type
     */
    public List<CoverageResult> getAll(final CoverageElement element) {
        List<CoverageResult> fileNodes = children.values()
                .stream()
                .filter(result -> element.equals(result.getElement()))
                .collect(Collectors.toList());
        children.values()
                .stream()
                .filter(result -> isContainerNode(result, element))
                .map(child -> child.getAll(element))
                .flatMap(List::stream).forEach(fileNodes::add);
        return fileNodes;
    }

    private boolean isContainerNode(final CoverageResult result, final CoverageElement otherElement) {
        CoverageElement coverageElement = result.getElement();
        return !otherElement.equals(coverageElement) && !coverageElement.isBasicBlock();
    }

    /**
     * Finds the coverage element with the given name.
     *
     * @param element
     *         the coverage element name
     * @param name
     *         the name of the coverage instance
     *
     * @return the result if found
     */
    public Optional<CoverageResult> find(final String element, final String name) {
        int hashCode = Integer.parseInt(name);
        for (String key : children.keySet()) {
            if (key.hashCode() == hashCode) {
                CoverageResult childResult = children.get(key);
                if (childResult.getElement().getName().equalsIgnoreCase(element)) {
                    return Optional.of(childResult);
                }
            }
        }
        return children.values()
                .stream()
                .map(child -> child.find(element, name))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny();
    }

    /**
     * Returns the coverage ratio for the specified element.
     *
     * @param coverageElement
     *         the element to get the coverage ratio for
     *
     * @return coverage ratio if available
     */
    public Optional<Ratio> getCoverageFor(final CoverageElement coverageElement) {
        if (aggregateResults.containsKey(coverageElement)) {
            return Optional.ofNullable(aggregateResults.get(coverageElement));
        }
        if (localResults.containsKey(coverageElement)) {
            return Optional.ofNullable(localResults.get(coverageElement));
        }
        return Optional.empty();
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param coverageElement
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     */
    public String printCoverageFor(final CoverageElement coverageElement) {
        if (aggregateResults.containsKey(coverageElement)) {
            return String.format("%.2f%%", aggregateResults.get(coverageElement).getPercentageFloat());
        }
        if (localResults.containsKey(coverageElement)) {
            return String.format("%.2f%%", localResults.get(coverageElement).getPercentageFloat());
        }
        return "n/a";
    }

    // ---------- REFACTORING END ------------------

    public String getRelativeSourcePath() {
        return relativeSourcePath;
    }

    public void setRelativeSourcePath(final String relativeSourcePath) {
        this.relativeSourcePath = relativeSourcePath;

        if (!StringUtils.isEmpty(relativeSourcePath)) {
            paint = new CoveragePaint(element);
        }
    }

    public String getName() {
        return name == null || name.trim().length() == 0 ? "Project" : name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public CoverageResult getParent() {
        return parent;
    }

    public CoverageElement getElement() {
        return element;
    }

    public boolean isSourceCodeLevel() {
        return relativeSourcePath != null;
    }

    public boolean isAggregatedLevel() {
        return element.equals(CoverageElement.AGGREGATED_REPORT);
    }

    public CoveragePaint getPaint() {
        return paint;
    }

    public void paint(final int line, final int hits) {
        if (paint != null) {
            paint.paint(line, hits);
        }
    }

    public void paint(final int line, final int hits, final int branchHits, final int branchTotal) {
        if (paint != null) {
            paint.paint(line, hits, branchHits, branchTotal);
        }
    }

    /**
     * gets the file corresponding to the source file.
     *
     * @return The file where the source file should be (if it exists)
     */
    private File getSourceFile() {
        if (hasPermission()) {
            File sourceFile = new File(owner.getRootDir(),
                    DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + sanitizeFilename(
                            relativeSourcePath));
            if (sourceFile.exists()) {
                return sourceFile;
            }
            // keep compatibility
            sourceFile = new File(owner.getRootDir(),
                    DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + relativeSourcePath);
            if (sourceFile.exists()) {
                return sourceFile;
            }

            // try to normalize file path
            return Paths.get(sourceFile.getPath()).normalize().toFile();
        }
        return null;
    }

    private String sanitizeFilename(final String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    /**
     * Get delta coverage from {@link #deltaResults} for a specific {@link CoverageElement}.
     *
     * @param element
     *         the element to get the diff coverage for.
     *
     * @return the diff coverage or 0, if diff coverage for element is not available.
     */
    public float getCoverageDelta(final CoverageElement element) {
        return deltaResults.getOrDefault(element, 0.0F);
    }

    /**
     * Getter for property 'changeRequestCoverageDiffWithTargetBranch'.
     *
     * @return Value for property 'changeRequestCoverageDiffWithTargetBranch'.
     * @deprecated use {@link #getCoverageDelta(CoverageElement)} instead.
     */
    @Deprecated
    public float getChangeRequestCoverageDiffWithTargetBranch() {
        return changeRequestCoverageDiffWithTargetBranch;
    }

    /**
     * Setter for property 'changeRequestCoverageDiffWithTargetBranch'.
     *
     * @param changeRequestCoverageDiffWithTargetBranch
     *         Value to set for property 'changeRequestCoverageDiffWithTargetBranch'.
     *
     * @deprecated diff coverage is stored in {@link #deltaResults}.
     */
    @Deprecated
    public void setChangeRequestCoverageDiffWithTargetBranch(final float changeRequestCoverageDiffWithTargetBranch) {
        this.changeRequestCoverageDiffWithTargetBranch = changeRequestCoverageDiffWithTargetBranch;
    }

    public String getReferenceBuildUrl() {
        return referenceBuildUrl;
    }

    public void setReferenceBuildUrl(final String referenceBuildUrl) {
        this.referenceBuildUrl = referenceBuildUrl;
    }

    /**
     * Getter for property 'sourceFileAvailable'.
     *
     * @return Value for property 'sourceFileAvailable'.
     */
    public boolean isSourceFileAvailable() {
        if (hasPermission()) {
            File sourceFile = getSourceFile();
            return sourceFile != null && sourceFile.exists();
        }
        return false;
    }

    public boolean hasPermission() {
        return owner.hasPermission(Item.WORKSPACE);
    }

    /**
     * Getter for property 'sourceFileContent'.
     *
     * @return Value for property 'sourceFileContent'.
     */
    public String getSourceFileContent() {
        if (!hasPermission()) {
            return null;
        }
        File sourceFile = getSourceFile();
        if (sourceFile == null) {
            return null;
        }
        try {
            return new TextFile(sourceFile).read();
        }
        catch (IOException e) {
            return null;
        }
    }

    /**
     * Getter for property 'parents'.
     *
     * @return Value for property 'parents'.
     */
    public List<CoverageResult> getParents() {
        List<CoverageResult> result = new ArrayList<>();
        CoverageResult p = getParent();
        while (p != null) {
            result.add(p);
            p = p.getParent();
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Getter for property 'childElements'.
     *
     * @return Value for property 'childElements'.
     */
    public Set<CoverageElement> getChildElements() {
        Set<CoverageElement> result = new TreeSet<>();
        for (CoverageResult child : children.values()) {
            result.add(child.element);
        }
        return result;
    }

    public CoverageElement getChildElement() {
        return getChildElements().stream().findAny().orElse(null);
    }

    public Set<String> getChildren(final CoverageElement element) {
        Set<String> result = new TreeSet<>();
        for (CoverageResult child : children.values()) {
            if (child.element.equals(element)) {
                result.add(child.name);
            }
        }
        return result;
    }

    /**
     * Getter for keys of property 'children'.
     *
     * @return Value for keys of property 'children'.
     */
    public Set<String> getChildren() {
        return children.keySet();
    }

    public Map<String, CoverageResult> getChildrenReal() {
        return children;
    }

    public Map<CoverageElement, Ratio> getResults() {
        return Collections.unmodifiableMap(aggregateResults);
    }

    public Map<CoverageElement, Float> getDeltaResults() {
        return Collections.unmodifiableMap(deltaResults);
    }

    public void setDeltaResults(final Map<CoverageElement, Float> deltaResults) {
        this.deltaResults.clear();
        this.deltaResults.putAll(deltaResults);
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    @Exported(name = "results")
    public CoverageTree getResultsAPI() {
        return new CoverageTree(name, aggregateResults, children);
    }

    public List<CoverageTrend> getCoverageTrends() {

        if (getPreviousResult() == null) {
            return null;
        }

        List<CoverageTrend> coverageTrends = new LinkedList<>();
        int i = 0;
        for (Chartable c = this; c != null && i < DEFAULT_MAX_BUILDS_SHOW_IN_TREND; c = c.getPreviousResult(), i++) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(c.getOwner());

            List<CoverageTreeElement> elements = c.getResults().entrySet().stream()
                    .map(e -> new CoverageTreeElement(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            CoverageTrend trend = new CoverageTrend(label.toString(), elements);
            coverageTrends.add(trend);
        }
        return coverageTrends;
    }

    public String urlTransform(final String name) {
        StringBuilder buf = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (('0' <= c && '9' >= c)
                    || ('A' <= c && 'Z' >= c)
                    || ('a' <= c && 'z' >= c)) {
                buf.append(c);
            }
            else {
                buf.append('_');
            }
        }
        return buf.toString();
    }

    public String xmlTransform(final String name) {
        return name.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    public String relativeUrl(final CoverageResult parent) {
        StringBuilder url = new StringBuilder("..");
        CoverageResult p = getParent();
        while (p != null && p != parent) {
            url.append("/..");
            p = p.getParent();
        }
        return url.toString();
    }

    public CoverageResult getChild(final String name) {
        return children.get(name);
    }

    public Ratio getCoverage(final CoverageElement element) {
        return aggregateResults.getOrDefault(element, Ratio.NULL);
    }

    public Set<CoverageElement> getElements() {
        return Collections.unmodifiableSet(
                aggregateResults.isEmpty() ? Collections.emptySet() : aggregateResults.keySet());
    }

    public void updateCoverage(final CoverageElement element, final Ratio additionalResult) {
        if (localResults.containsKey(element)) {
            Ratio existingResult = localResults.get(element);
            localResults.put(element, CoverageAggregationRule.combine(element, existingResult, additionalResult));
        }
        else {
            localResults.put(element, additionalResult);
        }
    }

    /**
     * Getter for property 'owner'.
     *
     * @return Value for property 'owner'.
     */
    public Run<?, ?> getOwner() {
        return owner;
    }

    /**
     * Setter for property 'owner'.
     *
     * @param owner
     *         Value to set for property 'owner'.
     */
    public void setOwner(final Run<?, ?> owner) {
        this.owner = owner;
        aggregateResults.clear();
        for (CoverageResult child : children.values()) {
            child.setOwner(owner);
            if (paint != null && child.paint != null) {
                paint.add(child.paint);
            }
            for (Map.Entry<CoverageElement, Ratio> childResult : child.aggregateResults.entrySet()) {
                aggregateResults.putAll(CoverageAggregationRule.aggregate(child.getElement(),
                        childResult.getKey(), childResult.getValue(), aggregateResults));
            }

            Ratio prevTotal = aggregateResults.get(child.getElement());
            if (prevTotal == null) {
                prevTotal = Ratio.create(0, 0);
            }

            boolean isChildCovered = child.aggregateResults.entrySet().stream().anyMatch(coverageElementRatioEntry ->
                    coverageElementRatioEntry.getValue().numerator > 0);

            aggregateResults.put(child.getElement(),
                    Ratio.create(prevTotal.numerator + (isChildCovered ? 1 : 0), prevTotal.denominator + 1));
        }

        // override any local results
        aggregateResults.putAll(localResults);
    }

    public void setOwner(final AbstractBuild<?, ?> owner) {
        setOwner((Run<?, ?>) owner);
    }

    public void merge(final CoverageResult another) throws CoverageException {
        if (!element.equals(another.element)) {
            throw new CoverageException(
                    String.format("Unable to merge reports: Unmatched element %s and %s", element.getName(),
                            another.getElement().getName()));
        }

        for (Map.Entry<String, CoverageResult> childBeMerged : another.getChildrenReal().entrySet()) {
            if (getChild(childBeMerged.getKey()) == null) {
                childBeMerged.getValue().resetParent(this);
            }
            else {
                getChild(childBeMerged.getKey()).merge(childBeMerged.getValue());
            }
        }

    }

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    /**
     * Getter for property 'previousResult'.
     *
     * @return Value for property 'previousResult'.
     */
    public CoverageResult getPreviousResult() {
        if (parent == null) {
            if (owner == null) {
                return null;
            }
            Run<?, ?> prevBuild = BuildUtils.getPreviousNotFailedCompletedBuild(owner);
            CoverageAction action = null;
            while ((prevBuild != null) && (null == (action = prevBuild.getAction(CoverageAction.class)))) {
                prevBuild = BuildUtils.getPreviousNotFailedCompletedBuild(prevBuild);
            }
            return action == null ? null : action.getResult();
        }
        else {
            CoverageResult prevParent = parent.getPreviousResult();
            return prevParent == null ? null : prevParent.getChild(name);
        }
    }

    public Object getDynamic(String token, final StaplerRequest req, final StaplerResponse rsp) throws IOException {
        token = token.toLowerCase();
        String restPath = req.getRestOfPath();

        // prevent name conflict
        if (restPath.startsWith("/api/")) {
            if (token.equals("trend")) {
                return new RestResultWrapper(new CoverageTrendTree(getName(), getCoverageTrends(), getChildrenReal()));
            }
            else if (token.equals("result")) {
                return new RestResultWrapper(this);
            }
        }

        for (String name : children.keySet()) {
            if (urlTransform(name).toLowerCase().equals(token)) {
                return getChild(name);
            }
        }
        return null;
    }

    /**
     * Getter for property 'paintedSources'.
     *
     * @return Value for property 'paintedSources'.
     */
    public Map<String, CoveragePaint> getPaintedSources() {
        Map<String, CoveragePaint> result = new HashMap<>();
        // check the children
        for (CoverageResult child : children.values()) {
            result.putAll(child.getPaintedSources());
        }
        if (relativeSourcePath != null && paint != null) {
            result.put(relativeSourcePath, paint);
        }
        return result;
    }

    public Object getLast() {
        return getPreviousResult();
    }

    /**
     * add parent for CoverageResult(Only effect when parent is null)
     *
     * @param p
     *         parent
     */
    public void addParent(final CoverageResult p) {
        if (parent == null) {
            parent = p;
            if (this.parent != null) {
                this.parent.children.put(name, this);
            }
        }
    }

    public void resetParent(final CoverageResult p) {
        parent = null;
        addParent(p);
    }

    public void addAdditionalProperty(final String propertyName, final String value) {
        additionalProperties.putIfAbsent(propertyName, new HashSet<>());
        additionalProperties.get(propertyName).add(value);
    }

    public Set<String> getAdditionalProperty(final String propertyName) {
        return additionalProperties.get(propertyName);
    }

    /**
     * Interface for javascript code to get code coverage result.
     *
     * @return aggregated coverage results
     */
    @JavaScriptMethod
    public List<JSCoverageResult> jsGetResults() {
        List<JSCoverageResult> results = new LinkedList<>();

        for (Map.Entry<CoverageElement, Ratio> c : aggregateResults.entrySet()) {
            results.add(new JSCoverageResult(c.getKey().getName(), c.getValue()));
        }

        return results;
    }

    /**
     * Interface for javascript code to get child coverage result.
     *
     * @return aggregated child coverage results
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public Map<String, List<JSCoverageResult>> jsGetChildResults() {
        return getChildrenReal()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().jsGetResults()));
    }

    /**
     * Interface for javascript code to get code coverage trend.
     *
     * @return coverage trend
     */
    @JavaScriptMethod
    @SuppressWarnings("unused")
    public Map<String, List<JSCoverageResult>> jsGetTrendResults() {
        Map<String, List<JSCoverageResult>> results = new LinkedHashMap<>();

        if (getPreviousResult() == null) {
            return results;
        }

        int i = 0;
        for (Chartable c = this; c != null && i < DEFAULT_MAX_BUILDS_SHOW_IN_TREND; c = c.getPreviousResult(), i++) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(c.getOwner());

            List<JSCoverageResult> r = c.getResults().entrySet().stream()
                    .filter(e -> {
                        if (isAggregatedLevel()) {
                            return e.getKey().equals(CoverageElement.LINE) || e.getKey().equals(CoverageElement.REPORT);
                        }
                        else {
                            return true;
                        }
                    })
                    .map(e -> new JSCoverageResult(e.getKey().getName(), e.getValue()))
                    .collect(Collectors.toList());

            if (r.size() != 0) {
                results.put(label.toString(), r);
            }
        }
        return results;
    }

    // see https://issues.jenkins-ci.org/browse/JENKINS-60359
    @Override
    public String getDisplayName() {
        return getName();
    }

    public static class JSCoverageResult {
        private String name;
        private Ratio ratio;

        public JSCoverageResult(final String name, final Ratio ratio) {
            this.name = name;
            this.ratio = ratio;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Ratio getRatio() {
            return ratio;
        }

        public void setRatio(final Ratio ratio) {
            this.ratio = ratio;
        }
    }
}
