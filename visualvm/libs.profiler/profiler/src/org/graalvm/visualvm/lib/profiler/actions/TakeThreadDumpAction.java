/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.actions;

import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import javax.swing.SwingWorker;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.jfluid.results.threads.ThreadDump;
import org.graalvm.visualvm.lib.profiler.ThreadDumpWindow;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
@NbBundle.Messages({
    "LBL_TakeThreadDumpAction=&Take Thread Dump",
    "HINT_TakeThreadDumpAction=Take thread dump from the profiled process"
})
public class TakeThreadDumpAction extends ProfilingAwareAction {

    private static final int[] ENABLED_STATES = new int[]{Profiler.PROFILING_RUNNING};

    private static final class Singleton {

        final private static TakeThreadDumpAction INSTANCE = new TakeThreadDumpAction();
    }

    @ActionID(id = "org.graalvm.visualvm.lib.profiler.actions.TakeThreadDumpAction", category = "Profile")
    @ActionRegistration(displayName = "#LBL_TakeThreadDumpAction")
    @ActionReferences(value = {
        //        @ActionReference(path = "Shortcuts", name = "C-F3"),
        @ActionReference(path = "Menu/Profile", position = 500)})
    public static TakeThreadDumpAction getInstance() {
        return Singleton.INSTANCE;
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public TakeThreadDumpAction() {
        setIcon(Icons.getIcon(ProfilerIcons.SNAPSHOT_THREADS));
        putValue("iconBase", Icons.getResource(ProfilerIcons.SNAPSHOT_THREADS)); // NOI18N
        putProperty(Action.SHORT_DESCRIPTION, Bundle.HINT_TakeThreadDumpAction());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return Bundle.LBL_TakeThreadDumpAction();
    }

    @Override
    protected int[] enabledStates() {
        return ENABLED_STATES;
    }

    @Override
    public void performAction() {
        new SwingWorker<ThreadDump, Object>() {

            @Override
            protected ThreadDump doInBackground()throws Exception {
                try {
                    ProfilerClient client = Profiler.getDefault().getTargetAppRunner().getProfilerClient();
                    return client.takeThreadDump();

                } catch (ClientUtils.TargetAppOrVMTerminated ex) {
                    ProfilerLogger.log(ex.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    ThreadDump threadDump = get();
                    if (threadDump != null) {
                        ThreadDumpWindow win = new ThreadDumpWindow(threadDump);
                        win.open();
                        win.requestActive();
                    }
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (ExecutionException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            
        }.execute();
    }
}
