package io.jenkins.plugins.coverage.targets;

import hudson.model.Api;

public class RestResultWrapper {
    private Object o;

    public RestResultWrapper(Object o) {
        this.o = o;
    }

    public Api getApi() {
        return new Api(o);
    }
}
