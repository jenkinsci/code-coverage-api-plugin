package io.jenkins.plugins.coverage;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class  CoverageSummary extends PageObject {
    private final String id;
    private final WebElement coverageReportLink;

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
    public CoverageSummary(final Build parent, final String id) {
        super(parent, parent.url(id));

        this.id = id;
        WebElement summary = getElement(By.id(id + "-summary"));

        this.coverageReportLink = getElement(By.id("coverage-hrefCoverageReport"));
        getElement(by.href(id));
        this.referenceBuild = getElement(By.id("coverage-reference"));
        this.coverage = summary.findElements(by.id("coverage-value"));
        this.failMsg = getElement(By.id("coverage-fail-msg"));
        this.coverageChanges = summary.findElements(by.id("coverage-change"));

    }

    public WebElement getCoverageReportLink() {
        return coverageReportLink;
    }

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

    public List<Double> getCoverageChanges() {
        List<Double> changes = new LinkedList<>();
        for (WebElement result : this.coverageChanges) {
            String message = result.getText();
            double value = Double.parseDouble(message.substring(1, message.indexOf("%")).trim());
            changes.add(value);
        }
        return changes;
    }

    public String getFailMsg() {
        return this.failMsg.getText();
    }

    public String getReferenceBuild() {
        return this.referenceBuild.getText();
    }

    public CoverageReport openReferenceBuild() {
        WebElement a = this.referenceBuild.findElement(By.tagName("a"));
        return openPage(a, CoverageReport.class);
    }

    private <T extends PageObject> T openPage(final WebElement link, final Class<T> type) {
        String href = link.getAttribute("href");
        T result = newInstance(type, injector, url(href));
        link.click();
        return result;
    }

}
