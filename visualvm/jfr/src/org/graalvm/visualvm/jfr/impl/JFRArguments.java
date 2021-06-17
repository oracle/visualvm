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
package org.graalvm.visualvm.jfr.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.ApplicationFinder;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.jfr.JFRSnapshotSupport;
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
public class JFRArguments extends OptionProcessor {
    
    private static final String START_LONG_NAME = "start-jfr";                  // NOI18N
    private static final Option START_JFR_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, START_LONG_NAME), "org.graalvm.visualvm.jfr.impl.Bundle", "Argument_Start_ShortDescr"); // NOI18N
    private static final String DUMP_LONG_NAME = "dump-jfr";                    // NOI18N
    private static final Option DUMP_JFR_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, DUMP_LONG_NAME), "org.graalvm.visualvm.jfr.impl.Bundle", "Argument_Dump_ShortDescr"); // NOI18N
    private static final String STOP_LONG_NAME = "stop-jfr";                    // NOI18N
    private static final Option STOP_JFR_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, STOP_LONG_NAME), "org.graalvm.visualvm.jfr.impl.Bundle", "Argument_Stop_ShortDescr"); // NOI18N
    
    
    @Override
    protected Set<Option> getOptions() {
        Set<Option> options = new HashSet();
        options.add(START_JFR_ARGUMENT);
        options.add(DUMP_JFR_ARGUMENT);
        options.add(STOP_JFR_ARGUMENT);
        return options;
    }
    
    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        String[] startJFR = maps.get(START_JFR_ARGUMENT);
        if (startJFR != null) {
            final String[] _startJFR = startJFR.length == 1 ? startJFR[0].split("@") : null; // NOI18N
            if (_startJFR != null && _startJFR.length == 2) startJFR[0] = _startJFR[0];
            new Finder(startJFR, START_LONG_NAME) {
                @Override
                public void found(final Application application) {
                    VisualVM.getInstance().runTask(new Runnable() {
                        public void run() {
                            if (JFRSnapshotSupport.supportsJfrStart(application)) {
                                String params = _startJFR != null && _startJFR.length == 2 ? _startJFR[1] : null;
                                JFRSnapshotSupport.jfrStartRecording(application, params);
                            }
                        }
                    });
                }
            }.find();
            return;
        }
        
        final String[] dumpJFR = maps.get(DUMP_JFR_ARGUMENT);
        final String[] stopJFR = maps.get(STOP_JFR_ARGUMENT);
        if (dumpJFR != null) {
            new Finder(dumpJFR, DUMP_LONG_NAME) {
                @Override
                public void found(final Application application) {
                    VisualVM.getInstance().runTask(new Runnable() {
                        public void run() {
                            if (JFRSnapshotSupport.supportsJfrDump(application)) {
                                boolean stop = stopJFR != null && stopJFR.length == 1 && stopJFR[0].equals(dumpJFR[0]);
                                if (stop && !JFRSnapshotSupport.supportsJfrStop(application)) stop = false;
                                JFRSnapshotSupport.takeJfrDump(application, stop, true);
                            }
                        }
                    });
                }
            }.find();
            return;
        }
        
        if (stopJFR != null) {
            new Finder(stopJFR, STOP_LONG_NAME) {
                @Override
                public void found(final Application application) {
                    VisualVM.getInstance().runTask(new Runnable() {
                        public void run() {
                            if (JFRSnapshotSupport.supportsJfrStop(application)) {
                                JFRSnapshotSupport.jfrStopRecording(application);
                            }
                        }
                    });
                }
            }.find();
        }
    }
    
    
    private static abstract class Finder extends ApplicationFinder {
        
        Finder(String[] pids, String longName) throws CommandException {
            super(resolvePid(pids, longName));
        }
        
        
        public final void notFound(int pid, String id) {
            NotifyDescriptor desc = new NotifyDescriptor.Message(NbBundle.getMessage(JFRArguments.class, "MSG_NO_APP_PID", new Object[] { Integer.toString(pid) }), NotifyDescriptor.WARNING_MESSAGE); // NOI18N
            DialogDisplayer.getDefault().notifyLater(desc);
        }
        
        
        private static int resolvePid(String[] pids, String longName) throws CommandException {
            if (pids.length == 1) {
                try {
                    return Integer.valueOf(pids[0]);
                } catch (NumberFormatException e) {
                    throw new CommandException(0, "Incorrect pid format for --" + longName + ": " + e.getMessage()); // NOI18N
                }
            } else {
                throw new CommandException(0, "--" + longName + " requires exactly one value"); // NOI18N
            }
        }
        
    }
    
}
