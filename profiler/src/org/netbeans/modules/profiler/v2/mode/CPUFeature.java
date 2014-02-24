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

package org.netbeans.modules.profiler.v2.mode;

import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ui.components.PopupButton;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "CPUFeature_name=CPU",
    "CPUFeature_lrLabel=Live results:",
    "CPUFeature_viewHotSpots=Hot Spots",
    "CPUFeature_viewCallTree=Call Tree",
    "CPUFeature_viewCombined=Combined",
    "CPUFeature_pdLabel=Profiling data:", 
    "CPUFeature_snapshot=Snapshot", 
    "CPUFeature_apLabel=Application:", 
    "CPUFeature_threadDump=Thread Dump"
})
final class CPUFeature extends ProfilerFeature.Basic {
    
    private static enum View { HOT_SPOTS, CALL_TREE, COMBINED }
    
    private JLabel lrLabel;
    private JButton lrPauseButton;
    private JToggleButton lrRefreshButton;
    private PopupButton lrView;
    
    private JLabel pdLabel;
    private JButton pdSnapshotButton;
    private JButton pdResetResultsButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    
    private ProfilerToolbar toolbar;
    
    private View view;
    
    
    CPUFeature() {
        super(Bundle.CPUFeature_name(), Icons.getIcon(ProfilerIcons.CPU));
    }

    
    public JPanel getResultsUI() {
        return new JPanel();
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            lrLabel = new JLabel(Bundle.CPUFeature_lrLabel());
            lrLabel.setForeground(UIUtils.getDisabledLineColor());
            
            lrPauseButton = new JButton(Icons.getIcon(GeneralIcons.PAUSE));
            lrPauseButton.setEnabled(false);
            
            lrRefreshButton = new JToggleButton(Icons.getIcon(GeneralIcons.UPDATE_NOW));
            lrRefreshButton.setEnabled(false);
            
            lrView = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) { populateViews(popup); }
            };
            lrView.setEnabled(false);
            
            pdLabel = new JLabel(Bundle.CPUFeature_pdLabel());
            pdLabel.setForeground(UIUtils.getDisabledLineColor());
            
            pdSnapshotButton = new JButton(Bundle.CPUFeature_snapshot(), Icons.getIcon(ProfilerIcons.SNAPSHOT_TAKE));
            pdSnapshotButton.setEnabled(false);
            
            pdResetResultsButton = new JButton(Icons.getIcon(ProfilerIcons.RESET_RESULTS));
            pdResetResultsButton.setEnabled(false);
            
            apLabel = new JLabel(Bundle.CPUFeature_apLabel());
            apLabel.setForeground(UIUtils.getDisabledLineColor());
            
            apThreadDumpButton = new JButton(Bundle.CPUFeature_threadDump(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
            apThreadDumpButton.setEnabled(false);
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(lrLabel);
            toolbar.addSpace(2);
            toolbar.add(lrPauseButton);
            toolbar.add(lrRefreshButton);
            toolbar.add(lrView);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(pdLabel);
            toolbar.addSpace(2);
            toolbar.add(pdSnapshotButton);
            toolbar.add(pdResetResultsButton);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            
            setView(View.HOT_SPOTS);
        }
        
        return toolbar;
    }
    
    private void populateViews(JPopupMenu popup) {
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewHotSpots(), getView() == View.HOT_SPOTS) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.HOT_SPOTS); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewCallTree(), getView() == View.CALL_TREE) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.CALL_TREE); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.CPUFeature_viewCombined(), getView() == View.COMBINED) {
            protected void fireActionPerformed(ActionEvent e) { setView(View.COMBINED); }
        });
    }

    private void setView(View view) {
        if (view == this.view) return;
        
        this.view = view;
        
        switch (view) {
            case HOT_SPOTS:
                lrView.setText(Bundle.CPUFeature_viewHotSpots());
                break;
            case CALL_TREE:
                lrView.setText(Bundle.CPUFeature_viewCallTree());
                break;
            case COMBINED:
                lrView.setText(Bundle.CPUFeature_viewCombined());
                break;
        }
    }
    
    private View getView() {
        return view;
    }
    
}
