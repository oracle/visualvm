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
    "ThreadsFeature_name=Threads",
    "ThreadsFeature_show=Show:",
    "ThreadsFeature_filterAll=All threads",
    "ThreadsFeature_filterLive=Live threads",
    "ThreadsFeature_filterFinished=Finished threads",
    "ThreadsFeature_timeline=Timeline:",
    "ThreadsFeature_application=Application:",
    "ThreadsFeature_threadDump=Thread Dump",
    "ThreadsFeature_heapDump=Heap Dump",
    "ThreadsFeature_gc=GC"
})
final class ThreadsFeature extends ProfilerFeature.Basic {
    
    private static enum Filter { ALL, LIVE, FINISHED }
    
    private JLabel shLabel;
    private PopupButton shFilter;
    
    private JLabel tlLabel;
    private JButton tlZoomInButton;
    private JButton tlZoomOutButton;
    private JToggleButton tlFitWidthButton;
    
    private JLabel apLabel;
    private JButton apThreadDumpButton;
    
    private ProfilerToolbar toolbar;
    
    private Filter filter;
    
    
    ThreadsFeature() {
        super(Bundle.ThreadsFeature_name(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
    }

    
    public JPanel getResultsUI() {
        return new JPanel();
    }
    
    public ProfilerToolbar getToolbar() {
        if (toolbar == null) {
            shLabel = new JLabel(Bundle.ThreadsFeature_show());
            shLabel.setForeground(UIUtils.getDisabledLineColor());
            
            shFilter = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) { populateFilters(popup); }
            };
            shFilter.setEnabled(false);
            
            tlLabel = new JLabel(Bundle.ThreadsFeature_timeline());
            tlLabel.setForeground(UIUtils.getDisabledLineColor());
            
            tlZoomInButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_IN));
            tlZoomInButton.setEnabled(false);
            
            tlZoomOutButton = new JButton(Icons.getIcon(GeneralIcons.ZOOM_OUT));
            tlZoomOutButton.setEnabled(false);
            
            tlFitWidthButton = new JToggleButton(Icons.getIcon(GeneralIcons.SCALE_TO_FIT));
            tlFitWidthButton.setEnabled(false);
            
            apLabel = new JLabel(Bundle.ThreadsFeature_application());
            apLabel.setForeground(UIUtils.getDisabledLineColor());
            
            apThreadDumpButton = new JButton(Bundle.ThreadsFeature_threadDump(), Icons.getIcon(ProfilerIcons.WINDOW_THREADS));
            apThreadDumpButton.setEnabled(false);
            
            toolbar = ProfilerToolbar.create(true);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(shLabel);
            toolbar.addSpace(2);
            toolbar.add(shFilter);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(tlLabel);
            toolbar.addSpace(2);
            toolbar.add(tlZoomInButton);
            toolbar.add(tlZoomOutButton);
            toolbar.add(tlFitWidthButton);
            
            toolbar.addSpace(2);
            toolbar.addSeparator();
            toolbar.addSpace(5);
            
            toolbar.add(apLabel);
            toolbar.addSpace(2);
            toolbar.add(apThreadDumpButton);
            
            setFilter(Filter.ALL);
        }
        
        return toolbar;
    }
    
    private void populateFilters(JPopupMenu popup) {
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterAll(), getFilter() == Filter.ALL) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(Filter.ALL); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterLive(), getFilter() == Filter.LIVE) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(Filter.LIVE); }
        });
        
        popup.add(new JRadioButtonMenuItem(Bundle.ThreadsFeature_filterFinished(), getFilter() == Filter.FINISHED) {
            protected void fireActionPerformed(ActionEvent e) { setFilter(Filter.FINISHED); }
        });
    }

    private void setFilter(Filter filter) {
        if (filter == this.filter) return;
        
        this.filter = filter;
        
        switch (filter) {
            case ALL:
                shFilter.setText(Bundle.ThreadsFeature_filterAll());
                break;
            case LIVE:
                shFilter.setText(Bundle.ThreadsFeature_filterLive());
                break;
            case FINISHED:
                shFilter.setText(Bundle.ThreadsFeature_filterFinished());
                break;
        }
    }
    
    private Filter getFilter() {
        return filter;
    }
    
}
