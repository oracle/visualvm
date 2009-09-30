/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.lib.profiler.common.event.ProfilingStateListener;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.openide.util.actions.CallableSystemAction;


/**
 * @author Ian Formanek
 */
public abstract class ProfilingAwareAction extends CallableSystemAction implements ProfilingStateListener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int lastInstrumentation = CommonConstants.INSTR_NONE;
    private int lastProfilingState = Profiler.PROFILING_INACTIVE;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected ProfilingAwareAction() {
        Profiler.getDefault().addProfilingStateListener(this);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isEnabled() {
        if (!NetBeansProfiler.isInitialized()) {
            return false;
        }

        boolean shouldBeEnabled = false;
        lastProfilingState = Profiler.getDefault().getProfilingState();
        lastInstrumentation = Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType();

        final int[] enabledStates = enabledStates();

        for (int i = 0; i < enabledStates.length; i++) {
            if (lastProfilingState == enabledStates[i]) {
                shouldBeEnabled = true;

                break;
            }
        }

        if (shouldBeEnabled && requiresInstrumentation()) {
            shouldBeEnabled = (lastInstrumentation != CommonConstants.INSTR_NONE);
        }

        return shouldBeEnabled;
    }

    public final void instrumentationChanged(final int oldInstrType, final int currentInstrType) {
        updateAction();
    }

    public final void profilingStateChanged(final ProfilingStateEvent e) {
        updateAction();
    }

    public final void threadsMonitoringChanged() {
        updateAction();
    }

    protected abstract int[] enabledStates();

    protected final boolean asynchronous() {
        return false;
    }

    protected boolean requiresInstrumentation() {
        return false;
    }

    private void updateAction() {
        if (lastProfilingState != Profiler.getDefault().getProfilingState()) {
            boolean shouldBeEnabled = isEnabled();
            firePropertyChange(PROP_ENABLED, !shouldBeEnabled, shouldBeEnabled);
        } else if (lastInstrumentation != Profiler.getDefault().getTargetAppRunner().getProfilerClient().getCurrentInstrType()) { // for actions that require instrumentation we need to check if it has not changed

            boolean shouldBeEnabled = isEnabled();
            firePropertyChange(PROP_ENABLED, !shouldBeEnabled, shouldBeEnabled);
        }
    }
}
