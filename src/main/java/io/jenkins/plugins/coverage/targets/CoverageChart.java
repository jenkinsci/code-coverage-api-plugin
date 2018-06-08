/*
 * Copyright (c) 2007-2018 Seiji Sogabe, Michael Barrientos, Balázs Póka, Jeff Pearce and Jenkins contributors
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

import hudson.util.ChartUtil;
import hudson.util.ColorPalette;
import hudson.util.DataSetBuilder;
import hudson.util.ShiftedCategoryAxis;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import java.awt.*;
import java.util.Map;

// Code adopted from Cobertura Plugin https://github.com/jenkinsci/cobertura-plugin/
public class CoverageChart {
    private CategoryDataset dataset;
    private int lowerBound;
    private int upperBound;

    /**
     * Constructor
     *
     * @param chartable Chartable object to chart
     */
    public CoverageChart(Chartable chartable) {
        this(chartable, isZoomCoverageChart(chartable), getMaximumBuilds(chartable));
    }

    /**
     * Constructor
     *
     * @param chartable         Chartable object to chart
     * @param zoomCoverageChart true to zoom coverage chart
     * @param maximumBuilds     maximum builds to include
     */
    protected CoverageChart(Chartable chartable, boolean zoomCoverageChart, int maximumBuilds) {
        if (chartable == null) throw new NullPointerException("Cannot draw null-chart");
        if (chartable.getPreviousResult() == null)
            throw new NullPointerException("Need at least two result to draw a chart");
        DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();
        int min = 100;
        int max = 0;
        int n = 0;
        for (Chartable a = chartable; a != null; a = a.getPreviousResult()) {
            ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(a.getOwner());
            for (Map.Entry<CoverageMetric, Ratio> value : a.getResults().entrySet()) {
                dsb.add(value.getValue().getPercentageFloat(), value.getKey().getName(), label);
                min = Math.min(min, value.getValue().getPercentage());
                max = Math.max(max, value.getValue().getPercentage());
            }
            n++;
            if (maximumBuilds != 0 && n >= maximumBuilds) break;
        }
        int range = max - min;
        this.dataset = dsb.build();
        if (zoomCoverageChart) {
            this.lowerBound = min - 1;
            this.upperBound = max + (range < 5 ? 0 : 1);
        } else {
            this.lowerBound = -1;
            this.upperBound = 101;
        }
    }

    protected static boolean isZoomCoverageChart(Chartable chartable) {
//		if( chartable == null ) return false;
//		CoberturaBuildAction action = chartable.getOwner().getAction(CoberturaBuildAction.class);
//		boolean zoomCoverageChart = false;
//		if( action != null )
//		{
//			return action.getZoomCoverageChart();
//		}
//		else
//		{
//			Log.warn( "Couldn't find CoberturaPublisher to decide if the graph should be zoomed" );
//			return false;
//		}
        //TODO replace the CoberturaBuildAction to CoverageAction
        return false;
    }

    protected static int getMaximumBuilds(Chartable chartable) {
//		if( chartable == null ) return 0;
//		CoberturaBuildAction action = chartable.getOwner().getAction(CoberturaBuildAction.class);
//		if( action != null )
//		{
//			return action.getMaxNumberOfBuilds();
//		}
//		else
//		{
//			Log.warn( "Couldn't find CoberturaPublisher to decide the maximum number of builds to be graphed" );
//			return 0;
//		}
        //TODO replace the CoberturaBuildAction to CoverageAction
        return 0;
    }

    public JFreeChart createChart() {

        final JFreeChart chart = ChartFactory.createLineChart(null, // chart title
                null, // unused
                "%", // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                false // urls
        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        final LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);

        chart.setBackgroundPaint(Color.white);

        final CategoryPlot plot = chart.getCategoryPlot();

        // plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperBound(upperBound);
        rangeAxis.setLowerBound(lowerBound);

        final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setBaseStroke(new BasicStroke(1.5f));
        ColorPalette.apply(renderer);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

        return chart;
    }

    protected CategoryDataset getDataset() {
        return dataset;
    }

    protected int getLowerBound() {
        return lowerBound;
    }

    protected int getUpperBound() {
        return upperBound;
    }
}
