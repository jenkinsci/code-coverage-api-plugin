package io.jenkins.plugins.coverage;

import java.util.HashMap;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class CoverageSummary extends PageObject {
    //TODO: Attribute wie z. B. Liste mit den Metrics inkl. getter und setter. Initialisierung der Werte im Konstruktor
    //TODO: wahrscheinlich weitere Attribute in summary.jelly notwendig
    private final String id;
    private final WebElement coverageReportLink;

    private final List<WebElement> results;

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

        results = summary.findElements(by.id("coverage-value"));
    }

    public WebElement getCoverageReportLink() {
        return coverageReportLink;
    }

    public CoverageReport openCoverageReport() {
        return openPage(getCoverageReportLink(), CoverageReport.class);
    }

    /**
     * Get coverage of Summary.
     * @return Hashmap with coverage and value
     */
    public HashMap<String, Double> getCoverage() {
        HashMap<String, Double> coverage = new HashMap<>();
        for (WebElement result : results) {
            String message = result.getText();
            String type = message.substring(0, message.indexOf(":")).trim();
            Double value = Double.parseDouble(message.substring(message.indexOf(":") + 1, message.indexOf("%")).trim());
            coverage.put(type, value);
        }
        return coverage;
    }

    private <T extends PageObject> T openPage(final WebElement link, final Class<T> type) {
        String href = link.getAttribute("href");
        T result = newInstance(type, injector, url(href));
        link.click();
        return result;
    }

}
