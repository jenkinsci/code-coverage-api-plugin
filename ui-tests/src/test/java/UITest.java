import org.junit.Test;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.WorkflowJob;

public class UITest extends AbstractJUnitTest {

    @Test
    public void helloWorld() {
        System.out.println("Hello World");
        WorkflowJob job = jenkins.jobs.create(WorkflowJob.class);
        job.sandbox.check();
    }

}
