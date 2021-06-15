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
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.profiler.ResultsListener;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.utilities.Delegate;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;


/**
 * Action to take snapshot of results.
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_TakeSnapshotAction=&Take Snapshot of Collected Results",
    "HINT_TakeSnapshotAction=Take snapshot of collected results"
})
public final class TakeSnapshotAction extends ProfilingAwareAction {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int[] ENABLED_STATES = new int[] {
                                                    Profiler.PROFILING_RUNNING, Profiler.PROFILING_PAUSED,
                                                    Profiler.PROFILING_STOPPED
                                                };

    private Listener listener;

    /*
     * The following code is an externalization of various listeners registered
     * in the global lookup and needing access to an enclosing instance of
     * TakeSnapshotAction.
     * The enclosing instance will use the FQN registration to obtain the shared instance
     * of the listener implementation and inject itself as a delegate into the listener.
     */
    @ServiceProvider(service=ResultsListener.class)
    public static final class Listener extends Delegate<TakeSnapshotAction> implements ResultsListener {

        @Override
        public void resultsAvailable() {
            if (getDelegate() != null) getDelegate().updateAction();
        }

        @Override
        public void resultsReset() {
            if (getDelegate() != null) getDelegate().updateAction();
        }
        
    }
    
    private static final class Singleton {
        final private static TakeSnapshotAction INSTANCE = new TakeSnapshotAction();
    }
    
    @ActionID(id = "org.graalvm.visualvm.lib.profiler.actions.TakeSnapshotAction", category = "Profile")
    @ActionRegistration(displayName = "#LBL_TakeSnapshotAction")
    @ActionReferences(value = {
        @ActionReference(path = "Shortcuts", name = "A-F2"),
        @ActionReference(path = "Menu/Profile", position = 900)})
    public static TakeSnapshotAction getInstance() {
        return Singleton.INSTANCE;
    }
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public TakeSnapshotAction() {
        listener = Lookup.getDefault().lookup(Listener.class);
        listener.setDelegate(this);
        setIcon(Icons.getIcon(ProfilerIcons.SNAPSHOT_TAKE));
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_TakeSnapshotAction());
        putValue("iconBase", Icons.getResource(ProfilerIcons.SNAPSHOT_TAKE)); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    protected boolean shouldBeEnabled(Profiler profiler) {
        return super.shouldBeEnabled(profiler) && ResultsManager.getDefault().resultsAvailable();
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;

        // If you will provide context help then use:
        // return new HelpCtx(MyAction.class);
    }

    public String getName() {
        return Bundle.LBL_TakeSnapshotAction();
    }

    @Override
    public void performAction() {
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    ResultsManager.getDefault().takeSnapshot();
                }
            });
    }

    @Override
    protected int[] enabledStates() {
        return ENABLED_STATES;
    }

    protected boolean requiresInstrumentation() {
        return true;
    }
}
