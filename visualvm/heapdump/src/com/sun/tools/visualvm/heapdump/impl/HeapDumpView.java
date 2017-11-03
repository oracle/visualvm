/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.application.snapshot.ApplicationSnapshot;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.heapdump.HeapDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.heapwalker.v2.HeapWalker;
import org.netbeans.modules.profiler.heapwalker.v2.ui.HeapWalkerWindow;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class HeapDumpView extends DataSourceView {
    private final static Logger LOGGER = Logger.getLogger(HeapDumpView.class.getName());
    
    public HeapDumpView(HeapDump heapDump) {
        this(heapDump, DataSourceDescriptorFactory.getDescriptor(heapDump));
    }
    
    private HeapDumpView(HeapDump heapDump, DataSourceDescriptor descriptor) {
        super(heapDump, descriptor.getName(), descriptor.getIcon(), 0, isClosableView(heapDump));
    }
    
        
    protected DataViewComponent createComponent() {
        HeapDump heapDump = (HeapDump)getDataSource();
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(heapDump).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    private static boolean isClosableView(HeapDump heapDump) {
        // HeapDump invisible
        if (!heapDump.isVisible()) return false;
        
        // HeapDump not in DataSources tree
        DataSource owner = heapDump.getOwner();
        if (owner == null) return false;
        
        while (owner != null && owner != DataSource.ROOT) {
            // Application snapshot provides link to open the HeapDump
            if (owner instanceof ApplicationSnapshot) return true;
            // Subtree containing HeapDump invisible
            if (!owner.isVisible()) return false;
            owner = owner.getOwner();
        }
        
        // HeapDump visible in DataSources tree
        if (owner == DataSource.ROOT) return true;
        
        // HeapDump not in DataSources tree
        return false;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        private JLabel progressLabel;
        private JPanel contentsPanel;
        
        public MasterViewSupport(HeapDump heapDump) {
            File file = heapDump.getFile();
            initComponents(file != null);
            if (file != null) loadHeap(file);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(HeapDumpView.class, "LBL_Heap_Dump"), null, new ScrollableContainer(this)); // NOI18N
        }
        
        
        private void initComponents(boolean hasDump) {
            setLayout(new BorderLayout());
            
            String label = hasDump ? NbBundle.getMessage(HeapDumpView.class, "LBL_Loading_Heap_Dump") :    // NOI18N
                                     NbBundle.getMessage(HeapDumpView.class, "LBL_Loading_Heap_Dump_failed");    // NOI18N
            progressLabel = new JLabel(label, SwingConstants.CENTER);
        
            contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.add(progressLabel, BorderLayout.CENTER);
            contentsPanel.setOpaque(false);
            
            add(contentsPanel, BorderLayout.CENTER);
            setOpaque(false);
        }
        
        private void loadHeap(final File file) {
          RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
              try {
                final HeapWalker hw = new HeapWalker(file);
                SwingUtilities.invokeLater(new Runnable() { public void run() {
                    contentsPanel.remove(progressLabel);
                    JComponent hwView = new HeapWalkerWindow(hw);
                    try {
                        JComponent fragmentWalker = (JComponent)hwView.getComponent(0);
                        fragmentWalker.setOpaque(false);
                        JToolBar toolBar = (JToolBar)fragmentWalker.getComponent(0);
                        JComponent controllerPanel = (JComponent)fragmentWalker.getComponent(1);
                        toolBar.setOpaque(false);
                        ((JComponent)toolBar.getComponent(0)).setOpaque(false);
                        ((JComponent)toolBar.getComponent(1)).setOpaque(false);
                        controllerPanel.setOpaque(false);
                    } catch (Exception e) {}
                    contentsPanel.add(hwView, BorderLayout.CENTER);
                    contentsPanel.revalidate();
                    contentsPanel.repaint();
                } });
              } catch (FileNotFoundException ex) {
                LOGGER.throwing(HeapDumpView.class.getName(), "loadHeap", ex);  // NOI18N
              } catch (IOException ex) {
                LOGGER.throwing(HeapDumpView.class.getName(), "loadHeap", ex);  // NOI18N
              }
            }
          });
        }
        
    }
    
}
