/*
 * The MIT License
 *
 * Copyright 2021 CloudBees, Inc.
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

package io.jenkins.plugins.coverage;

import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.remoting.ChannelBuilder;
import java.io.File;
import jenkins.ReflectiveFilePathFilter;
import jenkins.SlaveToMasterFileCallable;
import jenkins.security.ChannelConfigurator;

/**
 * Prevents all {@link SlaveToMasterFileCallable}s from running during tests, to make sure we do not rely on them.
 */
public class BlockSlaveToMasterFileCallable extends ReflectiveFilePathFilter {

    @Override protected boolean op(String name, File path) throws SecurityException {
        throw new SecurityException("refusing to " + name + " on " + path);
    }

    @Extension public static class ChannelConfiguratorImpl extends ChannelConfigurator {

        @Override public void onChannelBuilding(ChannelBuilder builder, @Nullable Object context) {
            new BlockSlaveToMasterFileCallable().installTo(builder, 1000); // higher priority than, say, AdminFilePathFilter or even DefaultFilePathFilter
        }

    }

}
