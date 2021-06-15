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

package org.graalvm.visualvm.lib.ui.monitor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import javax.swing.JPanel;
import org.graalvm.visualvm.lib.jfluid.results.monitor.VMTelemetryDataManager;
import org.graalvm.visualvm.lib.ui.graphs.CPUGraphPanel;
import org.graalvm.visualvm.lib.ui.graphs.GraphPanel;
import org.graalvm.visualvm.lib.ui.graphs.MemoryGraphPanel;
import org.graalvm.visualvm.lib.ui.graphs.SurvivingGenerationsGraphPanel;
import org.graalvm.visualvm.lib.ui.graphs.ThreadsGraphPanel;

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
