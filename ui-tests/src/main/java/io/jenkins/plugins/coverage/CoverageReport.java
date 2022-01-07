package io.jenkins.plugins.coverage;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;


/**
 * {@link PageObject} representing the Coverage Report.
 */
public class CoverageReport extends PageObject {
    private final String id;

    /**
     * Creates an instance of the page displaying the coverage reprt.
     *
     * @param parent
     *         a finished build configured with a static analysis tool
     * @param id
     *         the type of the result page (e.g. simian, checkstyle, cpd, etc.)
     */
    public CoverageReport(final Build parent, final String id) {
        super(parent, parent.url(id));
        this.id = id;
    }


}
