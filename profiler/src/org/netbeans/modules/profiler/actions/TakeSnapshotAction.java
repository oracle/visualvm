/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.actions;

import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.modules.profiler.ResultsListener;
import org.netbeans.modules.profiler.ResultsManager;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.utilities.Delegate;
import org.netbeans.modules.profiler.utilities.ProfilerUtils;
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
    "HINT_TakeSnapshotAction=Take Snapshot of Collected Results"
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
    
    @ActionID(id = "org.netbeans.modules.profiler.actions.TakeSnapshotAction", category = "Profile")
    @ActionRegistration(displayName = "#LBL_TakeSnapshotAction")
    @ActionReferences(value = {
        @ActionReference(path = "Shortcuts", name = "C-F2"),
        @ActionReference(path = "Menu/Profile", position = 1200)})
    public static TakeSnapshotAction getInstance() {
        return Singleton.INSTANCE;
    }
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public TakeSnapshotAction() {
        listener = Lookup.getDefault().lookup(Listener.class);
        listener.setDelegate(this);
        setIcon(Icons.getIcon(ProfilerIcons.SNAPSHOT_TAKE));
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
