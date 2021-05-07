/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.heapdump.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.ApplicationFinder;
import org.graalvm.visualvm.heapdump.HeapDumpSupport;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=OptionProcessor.class)
public final class HeapDumpArgument extends OptionProcessor {
    
    private static final String LONG_NAME = "heapdump";                         // NOI18N
    private static final Option ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, LONG_NAME), "org.graalvm.visualvm.heapdump.impl", "Argument_ShortDescr"); // NOI18N
    
    
    @Override
    protected Set<Option> getOptions() {
        return Collections.singleton(ARGUMENT);
    }
    
    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        String[] pids = maps.get(ARGUMENT);
        if (pids != null && pids.length == 1) {
            try {
                int pid = Integer.valueOf(pids[0]);
                new ApplicationFinder(pid) {
                    @Override
                    public void found(Application application) {
                        HeapDumpSupport.getInstance().takeHeapDump(application, true);
                    }
                    @Override
                    public void notFound(int pid, String id) {
                        NotifyDescriptor desc = new NotifyDescriptor.Message(NbBundle.getMessage(HeapDumpArgument.class, "MSG_NO_APP_PID", new Object[] { Integer.toString(pid) }), NotifyDescriptor.WARNING_MESSAGE);
                        DialogDisplayer.getDefault().notifyLater(desc);
                    }
                }.find();
            } catch (NumberFormatException e) {
                throw new CommandException(0, "Incorrect pid format for --" + LONG_NAME + ": " + e.getMessage()); // NOI18N
            }
        } else {
            throw new CommandException(0, "--" + LONG_NAME + " requires exactly one value"); // NOI18N
        }
    }
    
}
