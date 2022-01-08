package io.jenkins.plugins.coverage;

import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;


import com.gargoylesoftware.htmlunit.ScriptResult;
import com.google.inject.Injector;


import org.jenkinsci.test.acceptance.po.PageObject;


import static net.javacrumbs.jsonunit.assertj.JsonAssertions.*;


/**
 * {@link PageObject} representing the Coverage Report.
 */
//@WithPlugins("warnings-ng")
public class CoverageReport extends PageObject {
    private static final String COVERAGE_OVERVIEW_CHART = "coverage-overview";


    @SuppressWarnings("unused") // Required to dynamically create page object using reflection
    public CoverageReport(final Injector injector, final URL url, final String id) {
        super(injector, url);

    }

    public WebElement getCoverageTrend() {
        return getElement(By.id("coverage-trend"));
    }

    public WebElement getCoverageOverview() {
        return getElement(By.id("coverage-overview"));

    }

    void getPackageOverview(){
        ensurePackageOverviewIsActive();
    }

    private void ensurePackageOverviewIsActive() {
    }

    void getFileOverview(){
        ensureFileOverviewIsActive();

    }

    private void ensureFileOverviewIsActive() {
    }

    public String getChartById(final String elementId) {
        Object result = executeScript(String.format(
                "delete(window.Array.prototype.toJSON) %n"
                        + "return JSON.stringify(echarts.getInstanceByDom(document.getElementById(\"%s\")).getOption())",
                elementId));
        ScriptResult scriptResult = new ScriptResult(result);

        return scriptResult.getJavaScriptResult().toString();
    }

    //TOOD: auslagern
    public void verfiesOverview() {
        String overview = getChartById("coverage-overview");

    }
}
