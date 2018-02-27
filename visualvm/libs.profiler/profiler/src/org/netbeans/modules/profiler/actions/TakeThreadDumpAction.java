/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.actions;

import java.util.concurrent.ExecutionException;
import javax.swing.Action;
import javax.swing.SwingWorker;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.threads.ThreadDump;
import org.netbeans.modules.profiler.ThreadDumpWindow;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
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

    @ActionID(id = "org.netbeans.modules.profiler.actions.TakeThreadDumpAction", category = "Profile")
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
