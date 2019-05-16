package io.jenkins.plugins.coverage.source;

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.coverage.source.code.JGitUtil;
import io.jenkins.plugins.coverage.targets.CoverageElement;
import io.jenkins.plugins.coverage.targets.CoverageSourceFileAnalysis;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Source code relative coverage resolver. with Git
 */
public class SourceFileCoverageAnalysisResolver implements ExtensionPoint, Describable<SourceFileCoverageAnalysisResolver> {

    /**
     * Source file's root path.
     */
    private String rootPath = "";
    /**
     * the branch name matching regex. default is ".*"
     */
    private String branchNameMatchRegex = ".*";
    /**
     * relative coverage summary's level.
     */
    private CoverageElement level;

    @DataBoundConstructor
    public SourceFileCoverageAnalysisResolver(String rootPath, String branchNameMatchRegex, String level) {
        this.rootPath = rootPath;
        this.branchNameMatchRegex = branchNameMatchRegex;
        this.level = CoverageElement.get(level);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<SourceFileCoverageAnalysisResolver> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getBranchNameMatchRegex() {
        return branchNameMatchRegex;
    }

    public String getLevel() {
        return level.getName();
    }

    public CoverageElement getLevelElement() {
        return this.level;
    }

    public CoverageSourceFileAnalysis resolveCoverageSourceFileVCS(Run<?, ?> run, FilePath workspace, TaskListener listener,
                                                                   String realRootPath,
                                                                   SourceFileCoverageAnalysisResolver resolver) {
        String lastCommitName = JGitUtil.getLastCommit(realRootPath);
        String branchName = JGitUtil.getCurrentBranchName(realRootPath);
        if (null == lastCommitName) {
            listener.error("not such git commit in path:%s", realRootPath);
            return null;
        }
        return new CoverageSourceFileAnalysis(realRootPath, resolver.getBranchNameMatchRegex(), resolver.getLevelElement(), branchName, lastCommitName);
    }

    @Symbol("sourceFileCoverageAnalysis")
    @Extension
    public static final class SourceFileCoverageAnalysisResolverDescriptor extends Descriptor<SourceFileCoverageAnalysisResolver> {

        private static final ListBoxModel LEVELS = new ListBoxModel(
                new ListBoxModel.Option(CoverageElement.LINE.getName(), Messages.SourceFileResolver_analysisByLine()),
                new ListBoxModel.Option(CoverageElement.CONDITIONAL.getName(), Messages.SourceFileResolver_analysisByCodeBranch())
        );

        public SourceFileCoverageAnalysisResolverDescriptor() {
            super(SourceFileCoverageAnalysisResolver.class);
        }

        public ListBoxModel doItems() {
            return LEVELS;
        }
    }
}
