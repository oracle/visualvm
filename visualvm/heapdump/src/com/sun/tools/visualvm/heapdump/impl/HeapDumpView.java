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

import com.sun.tools.visualvm.heapdump.HeapDump;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import com.sun.tools.visualvm.heapdump.HeapDumpSupport;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.heapwalk.HeapWalker;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class HeapDumpView extends DataSourceView {
    
    private static final String IMAGE_PATH = "com/sun/tools/visualvm/heapdump/resources/heapdump.png";

    private DataViewComponent view;
    

    public HeapDumpView(HeapDump heapDump) {
        super(heapDump.getFile().getName(), new ImageIcon(Utilities.loadImage(IMAGE_PATH, true)).getImage(), 0);
        view = createViewComponent(heapDump);
        HeapDumpSupport.getInstance().getHeapDumpPluggableView().makeCustomizations(view, heapDump);
    }
        
    public DataViewComponent getView() {
        return view;
    }
    
    public boolean isClosable() {
        return true;
    }
    
    
    private DataViewComponent createViewComponent(HeapDump heapDump) {
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
            return new DataViewComponent.MasterView("Heap dump", null, new ScrollableContainer(this));
        }
        
        
        private void initComponents() {
            setLayout(new BorderLayout());
            
            progressLabel = new JLabel("Loading Heap Dump...", SwingConstants.CENTER);
        
            contentsPanel = new JPanel(new BorderLayout());
            contentsPanel.add(progressLabel, BorderLayout.CENTER);
            contentsPanel.setBackground(Color.WHITE);
            
            add(contentsPanel, BorderLayout.CENTER);
        }
        
        private void loadHeap(final File file) {
          RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
              try {
                final HeapWalker hw = new HeapWalker(file);
                SwingUtilities.invokeLater(new Runnable() { public void run() {
                    contentsPanel.remove(progressLabel);
                    JComponent hwView = hw.getTopComponent();
                    hwView.setPreferredSize(new Dimension(1, 1));
                    contentsPanel.add(hwView, BorderLayout.CENTER);
                    contentsPanel.revalidate();
                    contentsPanel.repaint();
//                    contentsPanel.doLayout();
                } });
              } catch (FileNotFoundException ex) {
                ex.printStackTrace();
              } catch (IOException ex) {
                ex.printStackTrace();
              }
            }
          });
        }
        
    }
    
}
