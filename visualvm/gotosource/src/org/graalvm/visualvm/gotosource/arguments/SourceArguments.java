/*
 * Copyright (c) 2020, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.gotosource.arguments;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionGroups;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=OptionProcessor.class)
public final class SourceArguments extends OptionProcessor {
    
    @Override
    protected Set<Option> getOptions() {
        return Collections.singleton(OptionGroups.anyOf(SourceRootsArgument.ARGUMENT, SourceViewerArgument.ARGUMENT, SourceConfigArgument.ARGUMENT));
    }
    
    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        String[] sourceRoots = maps.get(SourceRootsArgument.ARGUMENT);
        String[] sourceViewer = maps.get(SourceViewerArgument.ARGUMENT);
        String[] sourceConfig = maps.get(SourceConfigArgument.ARGUMENT);
        
        if (sourceConfig != null) SourceConfigArgument.process(sourceConfig, sourceRoots, sourceViewer);
        if (sourceRoots != null) SourceRootsArgument.process(sourceRoots);
        if (sourceViewer != null) SourceViewerArgument.process(sourceViewer);
    }
    
    
    static String decode(String value) {
        value = value.replace("%27", "'");                                     // NOI18N
        value = value.replace("%22", "\"");                                     // NOI18N
        value = value.replace("%20", " ");                                      // NOI18N
        return value;
    }
    
}
