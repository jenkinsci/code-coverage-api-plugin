/*
 * Copyright (c) 2007-2018 Stephen Connolly, Michael Barrientos, Michael Barrientos, Shenyu Zheng and Jenkins contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.coverage.source;

import java.io.IOException;
import java.util.Map;

import org.kohsuke.stapler.DataBoundConstructor;
import org.jenkinsci.Symbol;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

import io.jenkins.plugins.coverage.targets.CoveragePaint;

// TODO: This class is deprecated and shoud be deleted if we have a new reporter step
public class DefaultSourceFileResolver extends SourceFileResolver {
    public static final String DEFAULT_SOURCE_CODE_STORE_DIRECTORY = "coverage-sources/";

    @DataBoundConstructor
    public DefaultSourceFileResolver(SourceFileResolverLevel level) {
        super(level);
    }

    public void resolveSourceFiles(Run<?, ?> run, FilePath workspace, TaskListener listener,
            Map<String, CoveragePaint> paints) throws IOException {
        // Code moved to SourceCodeFacade
    }

    @Symbol("sourceFiles")
    @Extension
    public static final class DefaultSourceFileResolverDescriptor<T extends SourceFileResolver>
            extends Descriptor<SourceFileResolver> {
        private static final ListBoxModel LEVELS = new ListBoxModel(
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.NEVER_STORE.getName(),
                        SourceFileResolver.SourceFileResolverLevel.NEVER_STORE.toString()),
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.STORE_LAST_BUILD.getName(),
                        SourceFileResolver.SourceFileResolverLevel.STORE_LAST_BUILD.toString()),
                new ListBoxModel.Option(SourceFileResolver.SourceFileResolverLevel.STORE_ALL_BUILD.getName(),
                        SourceFileResolver.SourceFileResolverLevel.STORE_ALL_BUILD.toString()));

        public DefaultSourceFileResolverDescriptor() {
            super(DefaultSourceFileResolver.class);
        }

        public ListBoxModel doFillLevelItems() {
            return LEVELS;
        }
    }
}
