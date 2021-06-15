/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;


/**
 * Run Garbage Collection in the target application VM
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_RunGCAction=Run &GC",
    "HINT_RunGCAction=Request garbage collection in the profiled process"
})
public final class RunGCAction extends ProfilingAwareAction {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int[] ENABLED_STATES = new int[] { Profiler.PROFILING_RUNNING };

    final private static class Singleton {
        final private static RunGCAction INSTANCE = new RunGCAction();
    }

    @ActionID(id = "org.graalvm.visualvm.lib.profiler.actions.RunGCAction", category = "Profile")
    @ActionRegistration(displayName = "#LBL_RunGCAction", lazy=false)
    @ActionReference(path = "Menu/Profile", position = 700, separatorAfter=800)
    public static RunGCAction getInstance() {
        return Singleton.INSTANCE;
    }


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected RunGCAction() {
        super();
        setIcon(Icons.getIcon(ProfilerIcons.RUN_GC));
        putValue("iconBase", Icons.getResource(ProfilerIcons.RUN_GC)); // NOI18N
        putProperty(Action.SHORT_DESCRIPTION, Bundle.HINT_RunGCAction());
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return Bundle.LBL_RunGCAction();
    }

    public void performAction() {
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
            public void run() {
                try {
                    Profiler.getDefault().getTargetAppRunner().runGC();
                } catch (final ClientUtils.TargetAppOrVMTerminated e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { ProfilerDialogs.displayWarning(e.getMessage()); }
                    });
                    ProfilerLogger.log(e.getMessage());
                }
            }
        });
    }

    protected int[] enabledStates() {
        return ENABLED_STATES;
    }
}
