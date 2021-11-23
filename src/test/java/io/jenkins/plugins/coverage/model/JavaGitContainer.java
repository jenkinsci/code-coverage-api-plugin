package io.jenkins.plugins.coverage.model;

import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;

@DockerFixture(id = "java-git", ports = {22, 8080}
)
public class JavaGitContainer extends SshdContainer {
    public JavaGitContainer() {
    }
}
