package io.jenkins.plugins.coverage;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage of a specific file.
 */
public class SourceCodeView extends PageObject {

    /**
     * Creates a new {@link PageObject} representing the file coverage.
     *
     * @param parent
     *         a finished build configured with a static analysis tool
     * @param rel
     *         the id of the file
     */
    public SourceCodeView(Build parent, String rel) {
        super(parent, parent.url(rel));
    }

    /**
     * Returns if file table is displayed.
     *
     * @return if file table is displayed
     */
    public boolean isFileTableDisplayed() {
        try {
            WebElement fileTable = getElement(By.id("file-table"));
            return fileTable != null && fileTable.isDisplayed();
        }
        catch (NoSuchElementException exception) {
            return false;
        }
    }
}
