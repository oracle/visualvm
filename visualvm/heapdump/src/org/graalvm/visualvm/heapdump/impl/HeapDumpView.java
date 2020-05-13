/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.heapdump.impl;

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.heapdump.HeapDump;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.heapviewer.HeapViewer;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.core.VisualVM;
import org.graalvm.visualvm.core.snapshot.SnapshotView;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class HeapDumpView extends SnapshotView {
    
    private final static Logger LOGGER = Logger.getLogger(HeapDumpView.class.getName());
    
    
    private MasterViewSupport mvs;
    
    
    public HeapDumpView(HeapDump heapDump) {
        this(heapDump, DataSourceDescriptorFactory.getDescriptor(heapDump));
    }
    
    private HeapDumpView(HeapDump heapDump, DataSourceDescriptor descriptor) {
        super(heapDump, descriptor.getName(), descriptor.getIcon(), 0);
    }
    
        
    protected DataViewComponent createComponent() {
        HeapDump heapDump = (HeapDump)getDataSource();
        mvs = new MasterViewSupport(heapDump);
        DataViewComponent dvc = new DataViewComponent(mvs.getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    protected void removed() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { if (mvs != null) mvs.closed(); }
        });
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        private JLabel progressLabel;
        private JPanel contentsPanel;
        
        private HeapViewer heapViewer;
        
        public MasterViewSupport(HeapDump heapDump) {
            File file = heapDump.getFile();
            initComponents(file != null);
            if (file != null) loadHeap(file);
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(HeapDumpView.class, "LBL_Heap_Dump"), null, new ScrollableContainer(this)); // NOI18N
        }
        
        
        void closed() {
            if (heapViewer != null) heapViewer.closed();
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
          VisualVM.getInstance().runTask(new Runnable() {
            public void run() {
              try {
                final HeapViewer _heapViewer = new HeapViewer(file);
                SwingUtilities.invokeLater(new Runnable() { public void run() {
                    heapViewer = _heapViewer;
                    contentsPanel.remove(progressLabel);
                    contentsPanel.add(heapViewer.getComponent(), BorderLayout.CENTER);
                    contentsPanel.revalidate();
                    contentsPanel.repaint();
                } });
              } catch (final IOException ex) {
                SwingUtilities.invokeLater(new Runnable() { public void run() {
                  progressLabel.setText(NbBundle.getMessage(HeapDumpView.class, "LBL_Loading_Heap_Dump_failed2", ex.getMessage()));
                  contentsPanel.revalidate();
                  contentsPanel.repaint();
                } });
                LOGGER.throwing(HeapDumpView.class.getName(), "loadHeap", ex);  // NOI18N
                LOGGER.log(Level.INFO, "Failed to load heap dump", ex);  // NOI18N
              }
            }
          });
        }
        
    }
    
}
