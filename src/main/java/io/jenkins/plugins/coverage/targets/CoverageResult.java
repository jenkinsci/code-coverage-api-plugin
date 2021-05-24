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
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


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

    /**
     * Generated
     */
    private static final long serialVersionUID = -3524882671364156445L;

    private static final int DEFAULT_MAX_BUILDS_SHOW_IN_TREND = 6;

    /**
     * The type of the programming element.
     */
    private final CoverageElement element;

    /**
     * Name of the programming element that this result object represent, such as package name, class name, method name, etc.
     */
    private String name;
    private String tag;

    private String referenceBuildUrl = null;
    private float changeRequestCoverageDiffWithTargetBranch = 0;

    // these two pointers form a tree structure where edges are names.
    private CoverageResult parent;

    private final Map<String, CoverageResult> children = new TreeMap<>();

    private final Map<CoverageElement, Ratio> aggregateResults = new TreeMap<>();

    private final Map<CoverageElement, Ratio> localResults = new TreeMap<>();

    private final Map<CoverageElement, Float> deltaResults = new TreeMap<>();

    /**
     * Line-by-line coverage information. Computed lazily, since it's memory intensive.
     */
    private CoveragePaint paint;

    private String relativeSourcePath;

    private Map<String, Set<String>> additionalProperties = new HashMap<>();

    public transient Run<?, ?> owner = null;

    public CoverageResult(CoverageElement elementType, CoverageResult parent, String name) {
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

    /**
     * Getter for property 'relativeSourcePath'.
     *
     * @return Value for property 'relativeSourcePath'.
     */
    public String getRelativeSourcePath() {
        return relativeSourcePath;
    }

    /**
     * Setter for property 'relativeSourcePath'.
     *
     * @param relativeSourcePath Value to set for property 'relativeSourcePath'.
     */
    public void setRelativeSourcePath(String relativeSourcePath) {
        this.relativeSourcePath = relativeSourcePath;

        if (!StringUtils.isEmpty(relativeSourcePath)) {
            paint = new CoveragePaint(element);
        }
    }

    /**
     * Getter for property 'name'.
     *
     * @return Value for property 'name'.
     */
    public String getName() {
        return name == null || name.trim().length() == 0 ? "Project" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for property 'parent'.
     *
     * @return Value for property 'parent'.
     */
    public CoverageResult getParent() {
        return parent;
    }

    /**
     * Getter for property 'element'.
     *
     * @return Value for property 'element'.
     */
    public CoverageElement getElement() {
        return element;
    }

    /**
     * Getter for property 'sourceCodeLevel'.
     *
     * @return Value for property 'sourceCodeLevel'.
     */
    public boolean isSourceCodeLevel() {
        return relativeSourcePath != null;
    }

    public boolean isAggregatedLevel() {
        return element.equals(CoverageElement.AGGREGATED_REPORT);
    }

    /**
     * Getter for property 'paint'.
     *
     * @return Value for property 'paint'.
     */
    public CoveragePaint getPaint() {
        return paint;
    }

    public void paint(int line, int hits) {
        if (paint != null) {
            paint.paint(line, hits);
        }
    }

    public void paint(int line, int hits, int branchHits, int branchTotal) {
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
            File sourceFile = new File(owner.getRootDir(), DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + sanitizeFilename(relativeSourcePath));
            if (sourceFile.exists()) {
                return sourceFile;
            }
            // keep compatibility
            sourceFile = new File(owner.getRootDir(), DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + relativeSourcePath);
            if (sourceFile.exists()) {
                return sourceFile;
            }

            // try to normalize file path
            return Paths.get(sourceFile.getPath()).normalize().toFile();
        }
        return null;
    }

    private String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    /**
     * Get delta coverage from {@link #deltaResults} for a specific {@link CoverageElement}.
     *
     * @param element
     *          the element to get the diff coverage for.
     * @return
     *          the diff coverage or 0, if diff coverage for element is not available.
     */
    public float getCoverageDelta(CoverageElement element) {
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
     * @param changeRequestCoverageDiffWithTargetBranch Value to set for property 'changeRequestCoverageDiffWithTargetBranch'.
     * @deprecated diff coverage is stored in {@link #deltaResults}.
     */
    @Deprecated
    public void setChangeRequestCoverageDiffWithTargetBranch(float changeRequestCoverageDiffWithTargetBranch) {
        this.changeRequestCoverageDiffWithTargetBranch = changeRequestCoverageDiffWithTargetBranch;
    }

    /**
     * Getter for property 'referenceBuildUrl'.
     *
     * @return Value for property 'referenceBuildUrl'.
     */
    public String getReferenceBuildUrl() {
        return referenceBuildUrl;
    }

    /**
     * Setter for property 'referenceBuildUrl'.
     *
     * @param referenceBuildUrl Value to set for property 'referenceBuildUrl'.
     */
    public void setReferenceBuildUrl(String referenceBuildUrl) {
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
        } catch (IOException e) {
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


    public Set<String> getChildren(CoverageElement element) {
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

    /**
     * Getter for property 'children'.
     *
     * @return Value for property 'children'.
     */
    public Map<String, CoverageResult> getChildrenReal() {
        return children;
    }

    /**
     * Getter for property 'results'.
     *
     * @return Value for property 'results'.
     */
    public Map<CoverageElement, Ratio> getResults() {
        return Collections.unmodifiableMap(aggregateResults);
    }

    /**
     * Getter for property 'deltaResults'.
     *
     * @return Value for property 'deltaResults'.
     */
    public Map<CoverageElement, Float> getDeltaResults() {
        return Collections.unmodifiableMap(deltaResults);
    }

    /**
     * Setter for property 'deltaResults'.
     *
     * @param deltaResults Value to set for property 'deltaResults'.
     */
    public void setDeltaResults(Map<CoverageElement, Float> deltaResults) {
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


    public String urlTransform(String name) {
        StringBuilder buf = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (('0' <= c && '9' >= c)
                    || ('A' <= c && 'Z' >= c)
                    || ('a' <= c && 'z' >= c)) {
                buf.append(c);
            } else {
                buf.append('_');
            }
        }
        return buf.toString();
    }

    public String xmlTransform(String name) {
        return name.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    public String relativeUrl(CoverageResult parent) {
        StringBuilder url = new StringBuilder("..");
        CoverageResult p = getParent();
        while (p != null && p != parent) {
            url.append("/..");
            p = p.getParent();
        }
        return url.toString();
    }

    public CoverageResult getChild(String name) {
        return children.get(name);
    }

    public Ratio getCoverage(CoverageElement element) {
        return aggregateResults.get(element);
    }


    public Set<CoverageElement> getElements() {
        return Collections.unmodifiableSet(
                aggregateResults.isEmpty() ? Collections.emptySet() : aggregateResults.keySet());
    }


    public void updateCoverage(CoverageElement element, Ratio additionalResult) {
        if (localResults.containsKey(element)) {
            Ratio existingResult = localResults.get(element);
            localResults.put(element, CoverageAggregationRule.combine(element, existingResult, additionalResult));
        } else {
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
     * @param owner Value to set for property 'owner'.
     */
    public void setOwner(Run<?, ?> owner) {
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

            boolean isChildCovered = false;

            if (child.aggregateResults.entrySet().stream().anyMatch(coverageElementRatioEntry ->
                    coverageElementRatioEntry.getValue().numerator > 0)) {
                isChildCovered = true;
            }

            aggregateResults.put(child.getElement(), Ratio.create(prevTotal.numerator + (isChildCovered ? 1 : 0), prevTotal.denominator + 1));
        }

        // override any local results
        aggregateResults.putAll(localResults);
    }

    public void setOwner(AbstractBuild<?, ?> owner) {
        setOwner((Run<?, ?>) owner);
    }


    public void merge(CoverageResult another) throws CoverageException {
        if (!element.equals(another.element)) {
            throw new CoverageException(String.format("Unable to merge reports: Unmatched element %s and %s", element.getName(), another.getElement().getName()));
        }

        for (Map.Entry<String, CoverageResult> childBeMerged : another.getChildrenReal().entrySet()) {
            if (getChild(childBeMerged.getKey()) == null) {
                childBeMerged.getValue().resetParent(this);
            } else {
                getChild(childBeMerged.getKey()).merge(childBeMerged.getValue());
            }
        }


    }


    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
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
        } else {
            CoverageResult prevParent = parent.getPreviousResult();
            return prevParent == null ? null : prevParent.getChild(name);
        }
    }

    public Object getDynamic(String token, StaplerRequest req, StaplerResponse rsp) throws IOException {
        token = token.toLowerCase();
        String restPath = req.getRestOfPath();

        // prevent name conflict
        if (restPath.startsWith("/api/")) {
            if (token.equals("trend")) {
                return new RestResultWrapper(new CoverageTrendTree(getName(), getCoverageTrends(), getChildrenReal()));
            } else if (token.equals("result")) {
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
     * @param p parent
     */
    public void addParent(CoverageResult p) {
        if (parent == null) {
            parent = p;
            if (this.parent != null) {
                this.parent.children.put(name, this);
            }
        }
    }

    public void resetParent(CoverageResult p) {
        parent = null;
        addParent(p);
    }

    public void addAdditionalProperty(String propertyName, String value) {
        additionalProperties.putIfAbsent(propertyName, new HashSet<>());
        additionalProperties.get(propertyName).add(value);
    }

    public Set<String> getAdditionalProperty(String propertyName) {
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
                        } else {
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

        public JSCoverageResult(String name, Ratio ratio) {
            this.name = name;
            this.ratio = ratio;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Ratio getRatio() {
            return ratio;
        }

        public void setRatio(Ratio ratio) {
            this.ratio = ratio;
        }
    }
}
