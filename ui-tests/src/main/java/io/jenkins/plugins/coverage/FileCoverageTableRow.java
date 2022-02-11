package io.jenkins.plugins.coverage;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;

/**
 * Representation of a table row in {@link FileCoverageTable}.
 */
public class FileCoverageTableRow {
    private static final int PACKAGE = 0;
    private static final int FILE = 1;
    private static final int LINE_COVERAGE = 2;
    private static final int BRANCH_COVERAGE = 4;
    private final WebElement row;
    private final FileCoverageTable table;

    FileCoverageTableRow(WebElement row, FileCoverageTable table) {
        this.row = row;
        this.table = table;
    }

    final WebElement getRow() {
        return row;
    }

    final FileCoverageTable getTable() {
        return table;
    }

    /**
     * Get package name of table row.
     *
     * @return package name
     */
    public String getPackage() {
        return getRow().findElements(By.tagName("td")).get(PACKAGE).getText();
    }

    /**
     * Get package name of table row.
     *
     * @return package name
     */
    public String getFile() {
        return getRow().findElements(By.tagName("td")).get(FILE).getText();
    }

    /**
     * Get line coverage of table row.
     *
     * @return branch coverage
     */
    public String getLineCoverage() {
        return getRow().findElements(By.tagName("td")).get(LINE_COVERAGE).getText();
    }

    /**
     * Get branch coverage of table row.
     *
     * @return branch coverage
     */
    public String getBranchCoverage() {
        return getRow().findElements(By.tagName("td")).get(BRANCH_COVERAGE).getText();
    }

    /**
     * Returns all possible headers representing the columns of the table.
     *
     * @return the headers of the table
     */
    List<String> getHeaders() {
        return getTable().getHeaders();
    }

    /**
     * Returns all table data fields in the table row.
     *
     * @return the table data fields
     */
    final List<WebElement> getCells() {
        return getRow().findElements(By.tagName("td"));
    }

    /**
     * Opens new page of file coverage in {@link SourceCodeView}.
     *
     * @param build
     *         current build
     *
     * @return Page with coverage of file
     */
    public SourceCodeView openFileLink(Build build) {
        WebElement fileLink = getFileLink();
        String link = fileLink.getAttribute("href");
        SourceCodeView sourceCodeView = new SourceCodeView(build,
                link.substring(link.lastIndexOf("/") + 1));
        fileLink.click();
        return sourceCodeView;
    }

    /**
     * Checks if file has a clickable link.
     *
     * @return if file is clickable.
     */
    public boolean hasFileLink() {
        try {
            getFileLink();
            return true;
        }
        catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Get link of file in row.
     *
     * @return link of file item
     */
    private WebElement getFileLink() {
        return this.row.findElements(By.tagName("td"))
                .get(1)
                .findElement(By.tagName("a"));
    }
}
