package io.jenkins.plugins.coverage;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;

/**
 * Representation of a table row in {@link FileCoverageTable}.
 */
public class FileCoverageTableRow {
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
        return this.row.findElements(By.tagName("td")).get(0).getText();
    }

    /**
     * Get package name of table row.
     *
     * @return package name
     */
    public String getFile() {
        return this.row.findElements(By.tagName("td")).get(1).getText();
    }

    /**
     * Get line coverage of table row.
     *
     * @return branch coverage
     */
    public String getLineCoverage() {
        return this.row.findElements(By.tagName("td")).get(2).getText();
    }

    /**
     * Get branch coverage of table row.
     *
     * @return branch coverage
     */
    public String getBranchCoverage() {
        return this.row.findElements(By.tagName("td")).get(4).getText();
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

    public SourceCodeView openFileLink(Build build) {
        WebElement link = this.row.findElements(By.tagName("td"))
                .get(1)
                .findElement(By.tagName("a"));
        SourceCodeView sourceCodeView = new SourceCodeView(build,
                link.getAttribute("href").substring(link.getAttribute("href").lastIndexOf("/") + 1));
        link.click();
        return sourceCodeView;
    }
    /**
     * Returns a specific table data field specified by the header of the column.
     *
     * @param header
     *         the header text specifying the column
     *
     * @return the WebElement of the table data field
     */
    final WebElement getCell(String header) {
        return getCells().get(getHeaders().indexOf(header));
    }

    /**
     * Returns the String representation of the table cell.
     *
     * @param header
     *         the header specifying the column
     *
     * @return the String representation of the cell
     */
    final String getCellContent(String header) {
        if (!getHeaders().contains(header)) {
            return "-";
        }
        return getCell(header).getText();
    }

    final boolean isDetailsRow() {
        return getCells().size() == 1;
    }
}
