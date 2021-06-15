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

import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.event.SimpleProfilingStateAdapter;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
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
     *  By default this method use {@link #shouldBeEnabled(org.graalvm.visualvm.lib.common.Profiler)} to update the
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
