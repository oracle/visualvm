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

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.sources.impl.SourceRoots;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;

/**
 * Implementation of the --source-roots argument
 *
 * @author Jiri Sedlacek
 */
public final class SourceRootsArgument extends OptionProcessor {
    
    private static final String ARGUMENT_LONG_NAME = "source-roots";            // NOI18N
    
    
    private final Option argument = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, ARGUMENT_LONG_NAME), "org.graalvm.visualvm.sources.arguments.Bundle", "Argument_SourceRoots_ShortDescr"); // NOI18N
    

    @Override
    protected Set<Option> getOptions() {
        return Collections.singleton(argument);
    }

    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        String[] values = maps.get(argument);
        if (values.length == 1) setValue(values[0]);
        else throw new CommandException(0, "--" + ARGUMENT_LONG_NAME + " requires exactly one value"); // NOI18N
    }
    
    
    private static final void setValue(String value) {
        if (value != null) value = value.trim();
        if (value == null || value.isEmpty()) SourceRoots.forceRoots(null);
        else SourceRoots.forceRoots(value.split(File.pathSeparator));
    }
    
}
