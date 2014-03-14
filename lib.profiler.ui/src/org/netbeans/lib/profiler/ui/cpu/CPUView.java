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

package org.netbeans.lib.profiler.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.cpu.CPUResultsSnapshot;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileProvider;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.JExtendedSplitPane;

/**
 *
 * @author Jiri Sedlacek
 */
public class CPUView extends JPanel {
    
    private static final ClientUtils.SourceCodeSelection[] EMPTY_SELECTION =
                     new ClientUtils.SourceCodeSelection[0];
    
    private final ProfilerClient client;
    
    private CPUTableView tableView;
    private CPUTreeTableView treeTableView;
    
    
    public CPUView(ProfilerClient client) {
        this.client = client;
        initUI();
    }
    
    
    public void setView(boolean callTree, boolean hotSpots) {
        treeTableView.setVisible(callTree);
        tableView.setVisible(hotSpots);
    }
    
    
    public void refreshData() throws ClientUtils.TargetAppOrVMTerminated {
        client.forceObtainedResultsDump(true);
        
        if (treeTableView.isVisible()) {
            try {
                CPUResultsSnapshot newData = client.getCPUProfilingResultsSnapshot(false);
                if (newData != null) treeTableView.setData(newData);
            } catch (CPUResultsSnapshot.NoDataAvailableException e) {
            } catch (Throwable t) {
                if (t instanceof ClientUtils.TargetAppOrVMTerminated) {
                    throw ((ClientUtils.TargetAppOrVMTerminated)t);
                } else {
                    System.err.println(">>> " + t.getMessage());
                    t.printStackTrace(System.err);
                }
            }
        }
        
        if (tableView.isVisible()) {
            FlatProfileProvider dataProvider = client.getFlatProfileProvider();
            final FlatProfileContainer newData = dataProvider == null ? null :
                                                 dataProvider.createFlatProfile();
            if (newData != null) tableView.setData(newData);
        }
    }
    
    public void resetData() {
        treeTableView.resetData();
        tableView.resetData();
    }
    
    public boolean hasSelection() {
        if (tableView.isVisible()) {
            if (treeTableView.isVisible()) {
                return tableView.hasSelection() || treeTableView.hasSelection();
            } else {
                return tableView.hasSelection();
            }
        } else { 
            return treeTableView.hasSelection();
        }
    }
    
    public ClientUtils.SourceCodeSelection[] getSelections() {
        if (!hasSelection()) {
            return EMPTY_SELECTION;
        } else if (tableView.isVisible()) {
            if (treeTableView.isVisible()) {
                Set<ClientUtils.SourceCodeSelection[]> selections = tableView.getSelections();
                selections.addAll(treeTableView.getSelections());
                return selections.toArray(EMPTY_SELECTION);
            } else {
                return tableView.getSelections().toArray(EMPTY_SELECTION);
            }
        } else {
            return treeTableView.getSelections().toArray(EMPTY_SELECTION);
        }
    }
    
    
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        
        treeTableView = new CPUTreeTableView(client);
        tableView = new CPUTableView(client);
        
        JSplitPane split = new JExtendedSplitPane(JSplitPane.VERTICAL_SPLIT) {
            {
                setBorder(null);
                setDividerSize(5);

                if (getUI() instanceof BasicSplitPaneUI) {
                    BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
                    if (divider != null) {
                        Color c = UIUtils.isNimbus() ? UIUtils.getDisabledLineColor() :
                                new JSeparator().getForeground();
                        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, c));
                    }
                }
            }
        };
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setTopComponent(treeTableView);
        split.setBottomComponent(tableView);
        
        add(split, BorderLayout.CENTER);
        
//        // TODO: read last state?
//        setView(true, false);
    }
    
}
