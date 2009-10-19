/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.heapdump.impl;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.heapdump.HeapDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.BorderLayout;
import java.awt.Dimension;
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
import org.netbeans.modules.profiler.heapwalk.HeapWalker;
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
        super(heapDump, descriptor.getName(), descriptor.getIcon(), 0, true);
    }
    
        
    protected DataViewComponent createComponent() {
        HeapDump heapDump = (HeapDump)getDataSource();
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport(heapDump).getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private static class MasterViewSupport extends JPanel  {
        
        private JLabel progressLabel;
        private JPanel contentsPanel;
        
        public MasterViewSupport(HeapDump heapDump) {
            initComponents();
            loadHeap(heapDump.getFile());
        }
        
        
        public DataViewComponent.MasterView getMasterView() {
            return new DataViewComponent.MasterView(NbBundle.getMessage(HeapDumpView.class, "LBL_Heap_Dump"), null, new ScrollableContainer(this)); // NOI18N
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            progressLabel = new JLabel(NbBundle.getMessage(HeapDumpView.class, "LBL_Loading_Heap_Dump"), SwingConstants.CENTER);    // NOI18N
        
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
                    JComponent hwView = hw.getTopComponent();
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
