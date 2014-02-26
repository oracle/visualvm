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

package org.netbeans.modules.profiler.v2.features;

import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.netbeans.modules.profiler.v2.ui.components.PopupButton;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "LocksFeature_name=Locks",
    "LocksFeature_show=View by:",
    "LocksFeature_aggregationByThreads=Threads",
    "LocksFeature_aggregationByMonitors=Monitors",
    "LocksFeature_application=Application:",
    "LocksFeature_threadDump=Thread Dump"
})
final class LocksFeature extends ProfilerFeature.Basic {
    
    private static enum Aggregation { BY_THREADS, BY_MONITORS }
    
    private JLabel shLabel;
    private PopupButton shAggregation;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    
    private ProfilerToolbar toolbar;
    
    private Aggregation aggregation;
    
    
    LocksFeature() {
        super(Bundle.LocksFeature_name(), Icons.getIcon(ProfilerIcons.WINDOW_LOCKS));
    }
    

    public JPanel getResultsUI() {
        return new JPanel();
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            shLabel = new JLabel(Bundle.LocksFeature_show());
            shLabel.setForeground(UIUtils.getDisabledLineColor());
            
            shAggregation = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) { populateFilters(popup); }
            };
            shAggregation.setEnabled(false);
            
            apLabel = new JLabel(Bundle.LocksFeature_application());
            apLabel.setForeground(UIUtils.getDisabledLineColor());
            
            apThreadDumpButton = new JButton(Bundle.LocksFeature_threadDump(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
            apThreadDumpButton.setEnabled(false);
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(shLabel);
            toolbar.addSpace(2);
            toolbar.add(shAggregation);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            
            setAggregation(Aggregation.BY_THREADS);
        }
        
        return toolbar;
    }
    
    private void populateFilters(JPopupMenu popup) {
        popup.add(new JRadioButtonMenuItem(Bundle.LocksFeature_aggregationByThreads(), getAggregation() == Aggregation.BY_THREADS) {
            protected void fireActionPerformed(ActionEvent e) { setAggregation(Aggregation.BY_THREADS); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.LocksFeature_aggregationByMonitors(), getAggregation() == Aggregation.BY_MONITORS) {
            protected void fireActionPerformed(ActionEvent e) { setAggregation(Aggregation.BY_MONITORS); }
        });
    }

    private void setAggregation(Aggregation aggregation) {
        if (aggregation == this.aggregation) return;
        
        this.aggregation = aggregation;
        
        switch (aggregation) {
            case BY_THREADS:
                shAggregation.setText(Bundle.LocksFeature_aggregationByThreads());
                break;
            case BY_MONITORS:
                shAggregation.setText(Bundle.LocksFeature_aggregationByMonitors());
                break;
        }
    }
    
    private Aggregation getAggregation() {
        return aggregation;
    }
    
}
