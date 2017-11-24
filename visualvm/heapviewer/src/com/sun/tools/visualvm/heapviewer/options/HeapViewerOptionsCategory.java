/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapviewer.options;

import com.sun.tools.visualvm.core.options.UISupport;
import com.sun.tools.visualvm.heapviewer.oql.CustomOQLQueries;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.heapwalk.OQLSupport;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
@OptionsPanelController.TopLevelRegistration(
        id = "HeapViewerOptions",
        categoryName = "#HeapViewerOptionsCategory_Name", 
        iconBase = "com/sun/tools/visualvm/heapviewer/options/heapViewer32.png",
        position = 1525)
@NbBundle.Messages({
        "HeapViewerOptionsCategory_Name=Heap Viewer"
}) 
public final class HeapViewerOptionsCategory extends OptionsPanelController {
    
    private static final HelpCtx HELP_CTX = null;
//    private static final HelpCtx HELP_CTX = new HelpCtx("HeapViewerOptions.Help"); // NOI18N
    
    public static final String OPTIONS_HANDLE = "HeapViewerOptions"; // NOI18N
    
    private HeapViewerOptionsPanel settingsPanel;
    private JComponent settingsComponent;

    
    private HeapViewerOptionsPanel getPanel() {
        if (settingsPanel == null) settingsPanel = new HeapViewerOptionsPanel();
        return settingsPanel;
    }
    
    public JComponent getComponent(Lookup lookup) {
        if (settingsComponent == null) settingsComponent = UISupport.createScrollableContainer(getPanel());
        return settingsComponent;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }
    
    public boolean isChanged() {
        if (settingsPanel == null) return false;
        
        List<OQLSupport.Query> master = CustomOQLQueries.instance().list();
        List<OQLSupport.Query> edited = getPanel().getQueries();
        
        if (master.size() != edited.size()) return true;
        
        for (int i = 0; i < master.size(); i++)
            if (!sameQuery(master.get(i), edited.get(i))) return true;
        
        return false;
    }

    public boolean isValid() {
        return true;
    }

    public void applyChanges() {
        if (settingsPanel == null) return;
        CustomOQLQueries.instance().set(getPanel().getQueries());
    }

    public void cancel() {
    }

    public void update() {
        new RequestProcessor("OQL Scripts Loader").post(new Runnable() { // NOI18N
            public void run() {
                final List<OQLSupport.Query> queries = CustomOQLQueries.instance().list();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        getPanel().setQueries(queries);
                    }
                });
            }
        });
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
    
    
    private static boolean sameQuery(OQLSupport.Query query1, OQLSupport.Query query2) {
        if (!Objects.equals(query1.getName(), query2.getName())) return false;
        if (!Objects.equals(query1.getDescription(), query2.getDescription())) return false;
        return true;
    }

}
