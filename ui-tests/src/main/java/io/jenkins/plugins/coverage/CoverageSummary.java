package io.jenkins.plugins.coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class CoverageSummary extends PageObject {
    private final WebElement coverageReportLink;
    private final WebElement summary;
    private final WebElement referenceBuild;
    private final List<WebElement> coverage;
    private final WebElement failMsg;
    private final List<WebElement> coverageChanges;

    /**
     * Creates a new page object representing the coverage summary on the build page of a job.
     *
     * @param parent
     *         a finished build configured with a static analysis tool
     * @param id
     *         the type of the result page (e.g. simian, checkstyle, cpd, etc.)
     */
    public CoverageSummary(Build parent, String id) {
        super(parent, parent.url(id));

        this.summary = getElement(By.id(id + "-summary"));
        this.coverageReportLink = getElement(By.id("coverage-hrefCoverageReport"));
        this.referenceBuild = getElement(By.id("coverage-reference"));
        this.coverage = this.summary.findElements(by.id("project-coverage"));
        this.failMsg = getElement(By.id("coverage-fail-msg"));
        this.coverageChanges = this.summary.findElements(by.id("project-coverage-delta"));

    }

    public WebElement getCoverageReportLink() {
        return coverageReportLink;
    }

    /**
     * Opens attached {@link CoverageReport}.
     *
     * @return attached CoverageReport.
     */
    public CoverageReport openCoverageReport() {
        return openPage(getCoverageReportLink(), CoverageReport.class);
    }

    /**
     * Get coverage of Summary.
     *
     * @return Hashmap with coverage and value
     */
    public HashMap<String, Double> getCoverage() {
        HashMap<String, Double> coverage = new HashMap<>();
        for (WebElement result : this.coverage) {
            String message = result.getText();
            String type = message.substring(0, message.indexOf(":")).trim();
            double value = Double.parseDouble(message.substring(message.indexOf(":") + 1, message.indexOf("%")).trim());
            coverage.put(type, value);
        }
        return coverage;
    }

    /**
     * Get relative changes of Coverage from previous build to this build.
     *
     * @return List of relative changes
     */
    public List<Double> getCoverageChanges() {
        List<Double> changes = new ArrayList<>();
        for (WebElement result : this.coverageChanges) {
            String message = result.getText();
            double value = Double.parseDouble(message.substring(1, message.indexOf("%")).trim());
            changes.add(value);
        }
        return changes;
    }

    /**
     * Get fail message in coverage summary.
     *
     * @return fail message
     */
    public String getFailMsg() {
        return this.failMsg.getText();
    }

    /**
     * Get url of reference build.
     *
     * @return url of reference build
     */
    public String getReferenceBuild() {
        return this.referenceBuild.findElement(By.tagName("a")).getAttribute("href");
    }

    /**
     * Open linked reference build in summary.
     *
     * @return {@link CoverageReport} of reference build
     */
    public CoverageReport openReferenceBuild() {
        WebElement a = this.referenceBuild.findElement(By.tagName("a"));
        return openPage(a, CoverageReport.class);
    }

    private <T extends PageObject> T openPage(WebElement link, Class<T> type) {
        String href = link.getAttribute("href");
        T result = newInstance(type, url(href));
        link.click();
        return result;
    }

    public static boolean isSummaryDisplayed(WebDriver pageObject, String elementId) {
        try {
            WebElement summary = pageObject.findElement(By.id(elementId));
            return summary != null && summary.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }
    }

}
