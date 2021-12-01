package io.jenkins.plugins.coverage.model;

import org.jenkinsci.test.acceptance.docker.DockerFixture;
import org.jenkinsci.test.acceptance.docker.fixtures.SshdContainer;

/**
 * Represents a server with SSHD, Java tooling, Maven and Git.
 *
 * @author Ullrich Hafner
 */
@DockerFixture(id = "java-git", ports = {22, 8080})
public class JavaGitContainer extends SshdContainer {
    /**
     * Creates a new instance of {@link JavaGitContainer}.
     */
    public JavaGitContainer() {
        super();
        // required for dependency injection
    }
}