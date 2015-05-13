/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.impl;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.Timer;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.SimpleProfilingStateAdapter;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.lib.profiler.ui.swing.GrayLabel;
import org.netbeans.modules.profiler.v2.ProfilerSession;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProfilerStatus {
    
    private final JLabel status;
    private final FixedWidthLabel label;
    private final ProfilerToolbar toolbar;
    private final Timer refreshTimer;
    
    public ProfilerStatus(final ProfilerSession session) {
        toolbar = ProfilerToolbar.create(false);
        
        toolbar.addSpace(2);
        toolbar.addSeparator();
        toolbar.addSpace(5);
        
        status = new GrayLabel("Status:");
        toolbar.add(status);
        
        toolbar.addSpace(5);
        
        label = new FixedWidthLabel("Inactive", "Paused", "Starting",
                                    "Stopped", "Changing", "Running");
        toolbar.add(label);
        
        toolbar.addSpace(3);
        
        refreshTimer = new Timer(1500, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatus(session);
            }
        });
        refreshTimer.setRepeats(false);
        // TODO: listener leaks, unregister!
        session.addListener(new SimpleProfilingStateAdapter() {
            public void update() { updateStatus(session); }
        });
    }
    
    public ProfilerToolbar getToolbar() {
        return toolbar;
    }
    
    private void updateStatus(ProfilerSession session) {
        int state = session.getState();
        
        switch (state) {
            case Profiler.PROFILING_INACTIVE:
                label.setText("Inactive");
                break;
            case Profiler.PROFILING_PAUSED:
                label.setText("Paused");
                break;
            case Profiler.PROFILING_STARTED:
                label.setText("Starting");
                break;
            case Profiler.PROFILING_STOPPED:
                label.setText("Stopped");
                break;
            case Profiler.PROFILING_IN_TRANSITION:
                label.setText("Changing");
                break;
            case Profiler.PROFILING_RUNNING:
                updateRunningStatus(session);
                break;
        }
        
        status.setEnabled(state != Profiler.PROFILING_INACTIVE);
    }
    
    private void updateRunningStatus(ProfilerSession session) {
        ProfilingSessionStatus pss = session.getProfiler().getTargetAppRunner().getProfilingSessionStatus();
        switch (pss.currentInstrType) {
            case CommonConstants.INSTR_RECURSIVE_FULL:
            case CommonConstants.INSTR_RECURSIVE_SAMPLED:
                int m = pss.getNInstrMethods();
                label.setDetailedText("Running, " + m + " methods instrumented");
                refreshTimer.start();
                break;
            case CommonConstants.INSTR_OBJECT_ALLOCATIONS:
            case CommonConstants.INSTR_OBJECT_LIVENESS:
                int c = pss.getNInstrClasses();
                label.setDetailedText("Running, " + c + " classes instrumented");
                refreshTimer.start();
                break;
            default:
                label.setText("Running");
        }
    }
    
    
    private static class FixedWidthLabel extends JLabel {
        
        private final Dimension size;
        private boolean detailsText;
        
        FixedWidthLabel(String... values) {
            size = new Dimension();
            for (String value : values) {
                setText(value);
                Dimension pref = super.getPreferredSize();
                size.width = Math.max(size.width, pref.width);
                size.height = Math.max(size.width, pref.height);
            }
        }
        
        public void setText(String text) {
            detailsText = false;
            super.setText(text);
        }
        
        public void setDetailedText(String details) {
            detailsText = true;
            super.setText(details);
        }
        
        public Dimension getPreferredSize() {
            return detailsText ? super.getPreferredSize() : size;
        }
        
    }
    
}
