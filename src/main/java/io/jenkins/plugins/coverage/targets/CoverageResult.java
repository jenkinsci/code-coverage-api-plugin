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
import hudson.model.Api;
import hudson.model.Item;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.Graph;
import hudson.util.TextFile;
import io.jenkins.plugins.coverage.BuildUtils;
import io.jenkins.plugins.coverage.CoverageAction;
import io.jenkins.plugins.coverage.source.DefaultSourceFileResolver;
import org.jfree.chart.JFreeChart;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
public class CoverageResult implements Serializable, Chartable {

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
    private final String name;

    // these two pointers form a tree structure where edges are names.
    private CoverageResult parent;

    private final Map<String, CoverageResult> children = new TreeMap<>();

    private final Map<CoverageMetric, Ratio> aggregateResults = new EnumMap<>(CoverageMetric.class);

    private final Map<CoverageMetric, Ratio> localResults = new EnumMap<>(CoverageMetric.class);

    /**
     * Line-by-line coverage information. Computed lazily, since it's memory intensive.
     */
    private final CoveragePaint paint;

    private String relativeSourcePath;

    public transient Run<?, ?> owner = null;

    public CoverageResult(CoverageElement elementType, CoverageResult parent, String name) {
        this.element = elementType;
        this.paint = CoveragePaintRule.makePaint(element);
        this.parent = parent;
        this.name = name;
        this.relativeSourcePath = null;
        if (this.parent != null) {
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
    }

    /**
     * Getter for property 'name'.
     *
     * @return Value for property 'name'.
     */
    public String getName() {
        return name == null || name.trim().length() == 0 ? "Project" : name;
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
            return new File(owner.getRootDir(), DefaultSourceFileResolver.DEFAULT_SOURCE_CODE_STORE_DIRECTORY + relativeSourcePath);
        }
        return null;
    }

    /**
     * Getter for property 'sourceFileAvailable'.
     *
     * @return Value for property 'sourceFileAvailable'.
     */
    public boolean isSourceFileAvailable() {
        if (hasPermission()) {
            return getSourceFile().exists();
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
        if (hasPermission()) {
            try {
                return new TextFile(getSourceFile()).read();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Getter for property 'parents'.
     *
     * @return Value for property 'parents'.
     */
    public List<CoverageResult> getParents() {
        List<CoverageResult> result = new ArrayList<CoverageResult>();
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
        Set<CoverageElement> result = EnumSet.noneOf(CoverageElement.class);
        for (CoverageResult child : children.values()) {
            result.add(child.element);
        }
        return result;
    }

    public CoverageElement getChildElement() {
        return getChildElements().stream().findAny().orElse(null);
    }


    public Set<String> getChildren(CoverageElement element) {
        Set<String> result = new TreeSet<String>();
        for (CoverageResult child : children.values()) {
            if (child.element.equals(element)) {
                result.add(child.name);
            }
        }
        return result;
    }

    public Set<CoverageMetric> getChildMetrics(CoverageElement element) {
        Set<CoverageMetric> result = new TreeSet<CoverageMetric>();
        for (CoverageResult child : children.values()) {
            if (child.element.equals(element)) {
                result.addAll(child.getMetrics());
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
    public Map<CoverageMetric, Ratio> getResults() {
        return Collections.unmodifiableMap(aggregateResults);
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
        return name.replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;");
    }

    public String relativeUrl(CoverageResult parent) {
        StringBuffer url = new StringBuffer("..");
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

    public Ratio getCoverage(CoverageMetric metric) {

        return aggregateResults.get(metric);
    }

    public Ratio getCoverageWithEmpty(CoverageMetric metric) {
        if (aggregateResults.containsKey(metric))
            return aggregateResults.get(metric);
        Map<CoverageMetric, Ratio> currMetricSet = new EnumMap<CoverageMetric, Ratio>(CoverageMetric.class);
        currMetricSet.putAll(aggregateResults);
        if (!currMetricSet.containsKey(metric)) {
            return null;
        }
        return currMetricSet.get(metric);
    }

    /**
     * Getter for property 'metrics'.
     *
     * @return Value for property 'metrics'.
     */
    public Set<CoverageMetric> getMetrics() {
        return Collections.unmodifiableSet(
                aggregateResults.isEmpty() ? EnumSet.noneOf(CoverageMetric.class) : EnumSet.copyOf(aggregateResults.keySet()));
    }

    public Set<CoverageMetric> getMetricsWithEmpty() {
        Map<CoverageMetric, Ratio> currMetricSet = new EnumMap<CoverageMetric, Ratio>(CoverageMetric.class);
        currMetricSet.putAll(aggregateResults);
        fixEmptyMetrics(findEmptyMetrics(currMetricSet), currMetricSet);
        return Collections.unmodifiableSet(
                currMetricSet.isEmpty() ? EnumSet.noneOf(CoverageMetric.class) : EnumSet.copyOf(currMetricSet.keySet()));
    }

    private List<CoverageMetric> findEmptyMetrics(Map<CoverageMetric, Ratio> currMetricSet) {
        List<CoverageMetric> allMetrics = new LinkedList<CoverageMetric>(Arrays.asList(CoverageMetric.JAVA_PACKAGES, CoverageMetric.JAVA_FILES, CoverageMetric.JAVA_CLASSES, CoverageMetric.JAVA_METHODS, CoverageMetric.LINE, CoverageMetric.CONDITIONAL));
        List<CoverageMetric> missingMetrics = new LinkedList<CoverageMetric>();
        for (CoverageMetric currMetric : allMetrics) {
            if (!currMetricSet.containsKey(currMetric)) {
                missingMetrics.add(currMetric);
            }
        }
        return missingMetrics;
    }

    private void fixEmptyMetrics(List<CoverageMetric> missingMetrics, Map<CoverageMetric, Ratio> currMetricSet) {
        for (CoverageMetric missing : missingMetrics) {
            currMetricSet.put(missing, Ratio.create(1, 1));
        }
    }

    public void updateMetric(CoverageMetric metric, Ratio additionalResult) {
        if (localResults.containsKey(metric)) {
            Ratio existingResult = localResults.get(metric);
            localResults.put(metric, CoverageAggregationRule.combine(metric, existingResult, additionalResult));
        } else {
            localResults.put(metric, additionalResult);
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
            if (paint != null && child.paint != null && CoveragePaintRule.propagatePaintToParent(child.element)) {
                paint.add(child.paint);
            }
            for (Map.Entry<CoverageMetric, Ratio> childResult : child.aggregateResults.entrySet()) {
                aggregateResults.putAll(CoverageAggregationRule.aggregate(child.getElement(),
                        childResult.getKey(), childResult.getValue(), aggregateResults));
            }
        }
        // override any local results (as they should be more accurate than the aggregated ones)
        aggregateResults.putAll(localResults);
        // now inject any results from CoveragePaint as they should be most accurate.
        if (paint != null) {
            aggregateResults.putAll(paint.getResults());
        }
    }

    public void setOwner(AbstractBuild<?, ?> owner) {
        setOwner((Run<?, ?>) owner);
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

    public void doCoverageHighlightedSource(StaplerRequest req, StaplerResponse rsp) throws IOException {
        // TODO
    }

    /**
     * Generates the graph that shows the coverage trend up to this report.
     *
     * @param req the stapler request
     * @param rsp the stapler response
     * @throws IOException from StaplerResponse.sendRedirect2
     */
    public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException {
        new Graph(owner.getTimestamp(), 500, 200) {
            @Override
            protected JFreeChart createGraph() {
                return new CoverageChart(CoverageResult.this).createChart();
            }
        }.doPng(req, rsp);
    }

    /**
     * Getter for property 'paintedSources'.
     *
     * @return Value for property 'paintedSources'.
     */
    public Map<String, CoveragePaint> getPaintedSources() {
        Map<String, CoveragePaint> result = new HashMap<String, CoveragePaint>();
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

    /**
     * Interface for javascript code to get code coverage result.
     *
     * @return aggregated coverage results
     */
    @JavaScriptMethod
    public List<JSCoverageResult> jsGetResults() {
        List<JSCoverageResult> results = new LinkedList<>();

        for (Map.Entry<CoverageMetric, Ratio> c : aggregateResults.entrySet()) {
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
                            return e.getKey().equals(CoverageMetric.LINE) || e.getKey().equals(CoverageMetric.REPORTS);
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
