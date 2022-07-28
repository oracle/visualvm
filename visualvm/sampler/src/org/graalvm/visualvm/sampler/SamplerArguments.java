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
package org.graalvm.visualvm.sampler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.ApplicationFinder;
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
public final class SamplerArguments extends OptionProcessor {
    
    private static final String START_CPU_LONG_NAME = "start-cpu-sampler";      // NOI18N
    private static final Option START_CPU_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, START_CPU_LONG_NAME), "org.graalvm.visualvm.sampler.Bundle", "Argument_StartCpu_ShortDescr"); // NOI18N
    private static final String START_MEMORY_LONG_NAME = "start-memory-sampler";// NOI18N
    private static final Option START_MEMORY_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, START_MEMORY_LONG_NAME), "org.graalvm.visualvm.sampler.Bundle", "Argument_StartMemory_ShortDescr"); // NOI18N
    private static final String SNAPSHOT_LONG_NAME = "snapshot-sampler";        // NOI18N
    private static final Option SNAPSHOT_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, SNAPSHOT_LONG_NAME), "org.graalvm.visualvm.sampler.Bundle", "Argument_Snapshot_ShortDescr"); // NOI18N
    private static final String STOP_LONG_NAME = "stop-sampler";                // NOI18N
    private static final Option STOP_ARGUMENT = Option.shortDescription(Option.requiredArgument(Option.NO_SHORT_NAME, STOP_LONG_NAME), "org.graalvm.visualvm.sampler.Bundle", "Argument_Stop_ShortDescr"); // NOI18N
    
    static enum Request { NONE, CPU, MEMORY };
    
    
    @Override
    protected Set<Option> getOptions() {
        Set<Option> options = new HashSet<>();
        options.add(START_CPU_ARGUMENT);
        options.add(START_MEMORY_ARGUMENT);
        options.add(SNAPSHOT_ARGUMENT);
        options.add(STOP_ARGUMENT);
        return options;
    }
    
    @Override
    protected void process(Env env, Map<Option, String[]> maps) throws CommandException {
        String[] startCPU = maps.get(START_CPU_ARGUMENT);
        if (startCPU != null) {
            final String[] _startCPU = startCPU.length == 1 ? startCPU[0].split("@") : null; // NOI18N
            if (_startCPU != null && _startCPU.length == 2) startCPU[0] = _startCPU[0];
            new Finder(startCPU, START_CPU_LONG_NAME) {
                @Override
                public void found(Application application) {
                    String settings = _startCPU != null && _startCPU.length == 2 ? _startCPU[1] : null;
                    SamplerSupport.getInstance().startCPU(application, settings);
                }
            }.find();
            return;
        }
        
        String[] startMemory = maps.get(START_MEMORY_ARGUMENT);
        if (startMemory != null) {
            final String[] _startMemory = startMemory.length == 1 ? startMemory[0].split("@") : null; // NOI18N
            if (_startMemory != null && _startMemory.length == 2) startMemory[0] = _startMemory[0];
            new Finder(startMemory, START_MEMORY_LONG_NAME) {
                @Override
                public void found(Application application) {
                    String settings = _startMemory != null && _startMemory.length == 2 ? _startMemory[1] : null;
                    SamplerSupport.getInstance().startMemory(application, settings);
                }
            }.find();
            return;
        }
        
        final String[] snapshot = maps.get(SNAPSHOT_ARGUMENT);
        final String[] stop = maps.get(STOP_ARGUMENT);
        if (snapshot != null) {
            new Finder(snapshot, SNAPSHOT_LONG_NAME) {
                @Override
                public void found(Application application) {
                    SamplerSupport.getInstance().takeSnapshot(application, true);
                    if (stop != null && stop.length == 1 && stop[0].equals(snapshot[0]))
                        SamplerSupport.getInstance().stop(application);
                }
            }.find();
            return;
        }
        
        if (stop != null) {
            new Finder(stop, STOP_LONG_NAME) {
                @Override
                public void found(Application application) {
                    SamplerSupport.getInstance().stop(application);
                }
            }.find();
        }
    }
    
    
    private static abstract class Finder extends ApplicationFinder {
        
        Finder(String[] pids, String longName) throws CommandException {
            super(resolvePid(pids, longName));
        }
        
        
        public final void notFound(int pid, String id) {
            NotifyDescriptor desc = new NotifyDescriptor.Message(NbBundle.getMessage(SamplerArguments.class, "MSG_NO_APP_PID", new Object[] { Integer.toString(pid) }), NotifyDescriptor.WARNING_MESSAGE);
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
