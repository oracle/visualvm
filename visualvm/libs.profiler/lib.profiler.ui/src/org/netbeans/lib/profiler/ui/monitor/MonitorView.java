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

package org.netbeans.lib.profiler.ui.monitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.results.monitor.VMTelemetryDataManager;
import org.netbeans.lib.profiler.ui.graphs.CPUGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.GraphPanel;
import org.netbeans.lib.profiler.ui.graphs.MemoryGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.SurvivingGenerationsGraphPanel;
import org.netbeans.lib.profiler.ui.graphs.ThreadsGraphPanel;

/**
 *
 * @author Jiri Sedlacek
 */
public class MonitorView extends JPanel {
    
    private VMTelemetryModels models;
    
    private GraphPanel cpuPanel;
    private GraphPanel memoryPanel;
    private GraphPanel gcPanel;
    private GraphPanel threadsPanel;
    
    
    public MonitorView(VMTelemetryDataManager dataManager) {
        initUI(dataManager);
    }
    
    
    public void setupCPUView(boolean visible) {
        cpuPanel.setVisible(visible);
    }
    
    public void setupMemoryView(boolean visible) {
        memoryPanel.setVisible(visible);
    }
    
    public void setupGCView(boolean visible) {
        gcPanel.setVisible(visible);
    }
    
    public void setupThreadsView(boolean visible) {
        threadsPanel.setVisible(visible);
    }
    
    
    public void cleanup() {
        cpuPanel.cleanup();
        memoryPanel.cleanup();
        gcPanel.cleanup();
        threadsPanel.cleanup();
        
        models.cleanup();
    }
    
    
    private void initUI(VMTelemetryDataManager dataManager) {
        setLayout(new GraphsLayout());
        
        models = new VMTelemetryModels(dataManager);
        
        cpuPanel = CPUGraphPanel.createBigPanel(models);
        add(cpuPanel);
        
        memoryPanel = MemoryGraphPanel.createBigPanel(models);
        add(memoryPanel);
        
        gcPanel = SurvivingGenerationsGraphPanel.createBigPanel(models);
        add(gcPanel);
        
        threadsPanel = ThreadsGraphPanel.createBigPanel(models);
        add(threadsPanel);
    }
    
    
    private class GraphsLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {}

        public void removeLayoutComponent(Component comp) {}

        public Dimension preferredLayoutSize(Container parent) { return new Dimension(); }

        public Dimension minimumLayoutSize(Container parent) { return new Dimension(); }

        public void layoutContainer(Container parent) {
            int w = getWidth();
            int h = getHeight();
            
            boolean c = cpuPanel.isVisible();
            boolean m = memoryPanel.isVisible();
            boolean g = gcPanel.isVisible();
            boolean t = threadsPanel.isVisible();
            
            int h1 = c || m ? (g || t ? h / 2 : h) : 0;
            int h2 = h - h1;
            
            if (h1 > 0) {
                if (c && m) {
                    int w1 = w / 2;
                    int w2 = w - w1;
                    cpuPanel.setBounds(0, 0, w1, h1);
                    memoryPanel.setBounds(w1, 0, w2, h1);
                } else if (c) {
                    cpuPanel.setBounds(0, 0, w, h1);
                } else {
                    memoryPanel.setBounds(0, 0, w, h1);
                }
            }
            
            if (h2 > 0) {
                if (g && t) {
                    int w1 = w / 2;
                    int w2 = w - w1;
                    gcPanel.setBounds(0, h1, w1, h2);
                    threadsPanel.setBounds(w1, h1, w2, h2);
                } else if (g) {
                    gcPanel.setBounds(0, h1, w, h2);
                } else {
                    threadsPanel.setBounds(0, h1, w, h2);
                }
            }
        }
        
    }
    
}
