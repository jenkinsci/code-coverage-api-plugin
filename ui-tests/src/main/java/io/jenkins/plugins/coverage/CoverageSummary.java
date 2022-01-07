package io.jenkins.plugins.coverage;

import org.jenkinsci.test.acceptance.po.Build;
        import org.jenkinsci.test.acceptance.po.PageObject;

/**
 * {@link PageObject} representing the coverage summary on the build page of a job.
 */
public class CoverageSummary extends PageObject {
    //TODO: Attribute wie z. B. Liste mit den Metrics inkl. getter und setter. Initialisierung der Werte im Konstruktor
    //TODO: wahrscheinlich weitere Attribute in summary.jelly notwendig
    private final String id;

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
        //zuweisen der Werte
    }


}
