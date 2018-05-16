package io.jenkins.plugins.coverage.adapter;

import java.io.File;

public interface Detectable {
    boolean detect(File file);
}
