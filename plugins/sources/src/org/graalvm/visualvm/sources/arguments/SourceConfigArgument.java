/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources.arguments;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Option;

/**
 * Implementation of the --source-config argument
 *
 * @author Jiri Sedlacek
 */
class SourceConfigArgument {
    
    static final String LONG_NAME = "source-config";                            // NOI18N
    
    static final Option ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, LONG_NAME), "org.graalvm.visualvm.sources.arguments.Bundle", "Argument_SourceConfig_ShortDescr"); // NOI18N
    
    
    static void process(String[] values, String[] rootsValues, String[] viewerValues) throws CommandException {
        if (rootsValues != null || viewerValues != null)
            throw new CommandException(0, "--" + LONG_NAME +                    // NOI18N
                    " not allowed with --" + SourceRootsArgument.LONG_NAME +    // NOI18N
                    " or --" + SourceViewerArgument.LONG_NAME);                 // NOI18N
        
        if (values.length == 1) setValue(values[0]);
        else throw new CommandException(0, "--" + LONG_NAME + " requires exactly one value"); // NOI18N
    }
    
    
    private static final void setValue(String value) throws CommandException {
        if (value != null) value = value.trim();
        if (value == null || value.isEmpty()) {
            SourceRootsArgument.setValue(null);
            SourceViewerArgument.setValue(null);
        } else {
            try (InputStreamReader isr = new InputStreamReader(new FileInputStream(value), "UTF-8")) { // NOI18N
                Properties props = new Properties();
                props.load(isr);

                String sourceRoots = props.getProperty(SourceRootsArgument.LONG_NAME);
                if (sourceRoots != null) SourceRootsArgument.setValue(sourceRoots);

                String sourceViewer = props.getProperty(SourceViewerArgument.LONG_NAME);
                if (sourceViewer != null) SourceViewerArgument.setValue(sourceViewer);
            } catch (IOException e) {
                throw new CommandException(0, "--" + LONG_NAME + " failed to read config " + value + ": " + e.getMessage()); // NOI18N
            }
        }
    }
    
}
