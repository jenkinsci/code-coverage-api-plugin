package io.jenkins.plugins.coverage.source;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

import io.jenkins.plugins.coverage.targets.CoveragePaint;

public abstract class SourceFileResolver implements ExtensionPoint, Describable<SourceFileResolver> {
    private DefaultSourceFileResolver.SourceFileResolverLevel level;
    private Set<String> possiblePaths;

    public SourceFileResolver(final DefaultSourceFileResolver.SourceFileResolverLevel level) {
        this.level = level;
    }

    public abstract void resolveSourceFiles(Run<?, ?> run, FilePath workspace, TaskListener listener, Map<String, CoveragePaint> paints) throws IOException;

    public void setPossiblePaths(final Set<String> possiblePaths) {
        this.possiblePaths = possiblePaths;
    }

    public Set<String> getPossiblePaths() {
        return possiblePaths;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<SourceFileResolver> getDescriptor() {
        return Jenkins.get().getDescriptorOrDie(getClass());
    }

    public DefaultSourceFileResolver.SourceFileResolverLevel getLevel() {
        return level;
    }

    public void setLevel(final DefaultSourceFileResolver.SourceFileResolverLevel level) {
        this.level = level;
    }

    public enum SourceFileResolverLevel {
        NEVER_STORE(Messages.SourceFileResolver_neverSave()),
        STORE_LAST_BUILD(Messages.SourceFileResolver_saveLast()),
        STORE_ALL_BUILD(Messages.SourceFileResolver_saveAll());

        private final String name;

        SourceFileResolverLevel(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
