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
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.openide.util.actions.CallableSystemAction;


/**
 * @author Ian Formanek
 */
public abstract class ProfilingAwareAction extends CallableSystemAction {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    boolean enabledSet = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected ProfilingAwareAction() {
        Profiler.getDefault().addProfilingStateListener(new SimpleProfilingStateAdapter() {

            @Override
            protected void update() {
                updateAction();
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public final boolean isEnabled() {
        if(enabledSet) {
            return super.isEnabled();
        } else {
            return shouldBeEnabled(Profiler.getDefault());
        }
    }

    @Override
    public final void setEnabled(boolean value) {
        enabledSet = true;
        super.setEnabled(value);
    }

    /** Called whenever state of the Profiler has changed.
     *  By default this method use {@link #shouldBeEnabled(org.netbeans.lib.profiler.common.Profiler)} to update the
     *  enabled property of the action.
     */
    protected void updateAction()
    {
        setEnabled(shouldBeEnabled(Profiler.getDefault()));
    }

    /** Compute if the action is enabled based on the state of the Profiler.
     *  Default implementation uses array returned by the {@link #enabledStates() } to determine the state.
     */
    protected boolean shouldBeEnabled(Profiler profiler) {
        boolean shouldBeEnabled = false;
        int lastProfilingState = profiler.getProfilingState();
        int lastInstrumentation = lastProfilingState != Profiler.PROFILING_INACTIVE ?
                                profiler.getTargetAppRunner().getProfilerClient().getCurrentInstrType() :
                                 CommonConstants.INSTR_NONE;

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

    /** Used by the default implementation of the {@link #shouldBeEnabled(Profiler) } to determine the enabled
     *  state of the action. */
    protected abstract int[] enabledStates();

    @Override
    protected final boolean asynchronous() {
        return false;
    }

    protected boolean requiresInstrumentation() {
        return false;
    }
    
}
