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
import org.graalvm.visualvm.lib.jfluid.TargetAppRunner;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.profiler.utilities.Delegate;
import org.graalvm.visualvm.lib.profiler.ResultsListener;
import org.graalvm.visualvm.lib.profiler.ResultsManager;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.graalvm.visualvm.lib.profiler.utilities.ProfilerUtils;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.lookup.ServiceProvider;


/**
 * Reset Collected Results for the profiled application (= Reset Collectors)
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "LBL_ResetResultsAction=R&eset Collected Results",
    "HINT_ResetResultsAction=Reset collected results"
})
public final class ResetResultsAction extends CallableSystemAction {

    Listener resultListener;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /*
     * The following code is an externalization of various listeners registered
     * in the global lookup and needing access to an enclosing instance of
     * ResetResultsAction.
     * The enclosing instance will use the FQN registration to obtain the shared instance
     * of the listener implementation and inject itself as a delegate into the listener.
     */
    @ServiceProvider(service=ResultsListener.class)
    public static final class Listener extends Delegate<ResetResultsAction> implements ResultsListener {
        @Override
        public void resultsAvailable() {
            if (getDelegate() != null) getDelegate().updateEnabledState();
        }

        @Override
        public void resultsReset() { 
            if (getDelegate() != null) getDelegate().updateEnabledState();
        }
    }
    
    final private static class Singleton {
        final private static ResetResultsAction INSTANCE = new ResetResultsAction();
    }
    
    private ResetResultsAction() {
        putValue(Action.NAME, Bundle.LBL_ResetResultsAction());
        putValue(Action.SHORT_DESCRIPTION, Bundle.HINT_ResetResultsAction());
        putValue(Action.SMALL_ICON, Icons.getIcon(ProfilerIcons.RESET_RESULTS));
        putValue("iconBase", Icons.getResource(ProfilerIcons.RESET_RESULTS)); // NOI18N
        
        resultListener = Lookup.getDefault().lookup(Listener.class);
        resultListener.setDelegate(this);
        updateEnabledState();
    }
    
    @ActionID(category="Profile", id="org.graalvm.visualvm.lib.profiler.actions.ResetResultsAction")
    @ActionRegistration(displayName="#LBL_ResetResultsAction", lazy=false)
    @ActionReferences({
        @ActionReference(path="Menu/Profile", position=1000, separatorAfter=1100),
        @ActionReference(path = "Shortcuts", name = "AS-F2")
    })
    public static ResetResultsAction getInstance() {
        return Singleton.INSTANCE;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    /**
     * Invoked when an action occurs.
     */
    @Override
    public void performAction() {
        
        ProfilerUtils.runInProfilerRequestProcessor(new Runnable() {
            @Override
            public void run() {
                ResultsManager.getDefault().reset();
        
                try {
                    TargetAppRunner runner = Profiler.getDefault().getTargetAppRunner();

                    if (runner.targetJVMIsAlive()) {
                        runner.resetTimers();
                    } else {
                        runner.getProfilerClient().resetClientData();

                        // TODO 
                        //        CPUCallGraphBuilder.resetCollectors();
                    }
                } catch (ClientUtils.TargetAppOrVMTerminated targetAppOrVMTerminated) {} // ignore
            }
        });
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
    
    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public String getName() {
        return Bundle.LBL_ResetResultsAction();
    }

    @Override
    protected String iconResource() {
        return Icons.getResource(ProfilerIcons.RESET_RESULTS);
    }
    
    private void updateEnabledState() {
        setEnabled(ResultsManager.getDefault().resultsAvailable());
    }
}
