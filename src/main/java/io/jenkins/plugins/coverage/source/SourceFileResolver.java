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

import hudson.ExtensionPoint;
import hudson.FilePath;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.coverage.targets.CoveragePaint;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Map;

public abstract class SourceFileResolver implements ExtensionPoint, Describable<SourceFileResolver> {

    private DefaultSourceFileResolver.SourceFileResolverLevel level;


    public SourceFileResolver(DefaultSourceFileResolver.SourceFileResolverLevel level) {
        this.level = level;
    }

    public abstract void resolveSourceFiles(Run<?, ?> run, FilePath workspace, TaskListener listener, Map<String, CoveragePaint> paints) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public Descriptor<SourceFileResolver> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }


    public DefaultSourceFileResolver.SourceFileResolverLevel getLevel() {
        return level;
    }

    public void setLevel(DefaultSourceFileResolver.SourceFileResolverLevel level) {
        this.level = level;
    }

    public enum SourceFileResolverLevel {
        NEVER_STORE(Messages.SourceFileResolver_neverSave()),
        STORE_LAST_BUILD(Messages.SourceFileResolver_saveLast()),
        STORE_ALL_BUILD(Messages.SourceFileResolver_saveAll());

        private String name;

        SourceFileResolverLevel(String name) {
            this.name = name;
        }


        public String getName() {
            return name;
        }
    }


}
