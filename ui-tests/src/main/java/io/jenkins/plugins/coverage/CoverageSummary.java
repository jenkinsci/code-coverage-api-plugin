package io.jenkins.plugins.coverage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        WebElement coverage = getElement(By.id(id + "-summary"));

        this.coverageReportLink = getElement(By.id("coverage-hrefCoverageReport"));
        getElement(by.href(id));
        //zuweisen der Werte
    }

    public WebElement getCoverageReportLink() {
        return coverageReportLink;
    }

    public CoverageReport openCoverageReport() {
        return openPage(getCoverageReportLink(), CoverageReport.class);
    }


    private <T extends PageObject> T openPage(final WebElement link, final Class<T> type) {
        String href = link.getAttribute("href");
        //FIXME: missing dependency?
        /**
         * java.lang.NoSuchMethodError: 'com.google.common.base.Objects$ToStringHelper com.google.common.base.Objects.toStringHelper(java.lang.Class)'
         *
         * 	at com.google.inject.internal.InjectorImpl.toString(InjectorImpl.java:1116)
         * 	at java.base/java.lang.String.valueOf(String.java:2951)
         * 	at java.base/java.lang.StringBuilder.append(StringBuilder.java:168)
         * 	at java.base/java.util.AbstractCollection.toString(AbstractCollection.java:473)
         * 	at java.base/java.lang.String.valueOf(String.java:2951)
         * 	at java.base/java.lang.StringBuilder.append(StringBuilder.java:168)
         * 	at org.jenkinsci.test.acceptance.po.CapybaraPortingLayerImpl.newInstance(CapybaraPortingLayerImpl.java:451)
         * 	at io.jenkins.plugins.coverage.CoverageSummary.openPage(CoverageSummary.java:53)
         * 	at io.jenkins.plugins.coverage.CoverageSummary.openCoverageReport(CoverageSummary.java:44)
         * 	at UITest.createJob(UITest.java:53)
         * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
         * 	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
         * 	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
         * 	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
         * 	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
         * 	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
         * 	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
         * 	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
         * 	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
         * 	at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:61)
         * 	at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:61)
         * 	at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:61)
         * 	at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:61)
         * 	at org.jenkinsci.test.acceptance.junit.JenkinsAcceptanceTestRule$1$2$1.evaluate(JenkinsAcceptanceTestRule.java:168)
         * 	at org.jenkinsci.test.acceptance.junit.FilterRule$1.evaluate(FilterRule.java:63)
         * 	at org.junit.rules.TestWatcher$1.evaluate(TestWatcher.java:61)
         * 	at org.jenkinsci.test.acceptance.junit.JenkinsAcceptanceTestRule$1.evaluate(JenkinsAcceptanceTestRule.java:58)
         * 	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
         * 	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
         * 	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
         * 	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
         * 	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
         * 	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
         * 	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
         * 	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
         * 	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
         * 	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
         * 	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
         * 	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
         * 	at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
         * 	at com.intellij.junit4.JUnit4IdeaTestRunner.startRunnerWithArgs(JUnit4IdeaTestRunner.java:69)
         * 	at com.intellij.rt.junit.IdeaTestRunner$Repeater.startRunnerWithArgs(IdeaTestRunner.java:33)
         * 	at com.intellij.rt.junit.JUnitStarter.prepareStreamsAndStart(JUnitStarter.java:235)
         * 	at com.intellij.rt.junit.JUnitStarter.main(JUnitStarter.java:54)
         */
        T result = newInstance(type, injector, url(href), id);
        link.click();
        return result;
    }

}
