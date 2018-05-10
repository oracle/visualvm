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

package org.graalvm.visualvm.profiling.snapshot.diff;

import org.graalvm.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import org.graalvm.visualvm.core.datasupport.Positionable;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.uisupport.HTMLLabel;
import org.graalvm.visualvm.uisupport.UISupport;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.JComponent;
import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import org.graalvm.visualvm.lib.common.CommonUtils;
import org.graalvm.visualvm.lib.profiler.SnapshotResultsWindow;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
final class SnapshotDiffView extends DataSourceView {
    private static final Logger LOGGER = Logger.getLogger(SnapshotDiffView.class.getName());
    
    private SnapshotResultsWindow sdw = null;

    public SnapshotDiffView(SnapshotDiffContainer snapshotDiff) {
        super(snapshotDiff, NbBundle.getMessage(SnapshotDiffView.class, "LBL_Snapshots_Comparison"), // NOI18N
              ImageUtilities.loadImage("org/graalvm/visualvm/profiler/resources/diff.png", true), // NOI18N
              Positionable.POSITION_AT_THE_END, true);
    }
    
        
    protected void removed() {
        if (sdw != null) {
            CommonUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    try {
                        Method method = sdw.getClass().getDeclaredMethod("componentClosed");   // NOI18N
                        if (method != null) {
                            method.setAccessible(true);
                            method.invoke(sdw);
                        }
                    } catch (NoSuchMethodException noSuchMethodException) {
                        LOGGER.throwing(SnapshotDiffView.class.getName(), "removed", noSuchMethodException);   // NOI18N
                    } catch (SecurityException securityException) {
                        LOGGER.throwing(SnapshotDiffView.class.getName(), "removed", securityException);   // NOI18N
                    } catch (IllegalAccessException illegalAccessException) {
                        LOGGER.throwing(SnapshotDiffView.class.getName(), "removed", illegalAccessException);   // NOI18N
                    } catch (IllegalArgumentException illegalArgumentException) {
                        LOGGER.throwing(SnapshotDiffView.class.getName(), "removed", illegalArgumentException);   // NOI18N
                    } catch (InvocationTargetException invocationTargetException) {
                        LOGGER.throwing(SnapshotDiffView.class.getName(), "removed", invocationTargetException);   // NOI18N
                    }
                    sdw = null;
                }
            });
        }
    }
    
    protected DataViewComponent createComponent() {
        SnapshotDiffContainer snapshotDiff = (SnapshotDiffContainer)getDataSource();
//        sdw = SnapshotsDiffWindow.get(snapshotDiff.getDiff(),
//                                      snapshotDiff.getSnapshot1().getLoadedSnapshot(),
//                                      snapshotDiff.getSnapshot2().getLoadedSnapshot());
        
        // Workaround to hide the links to original snapshots which cannot be
        // displayed in VisualVM (opening the snapshots doesn't fit VisualVM
        // workflow, APIs are not ready yet).
//        try {
//            MemoryDiffPanel mdp = (MemoryDiffPanel)sdw.getComponent(0);
//            JToolBar tb = (JToolBar)mdp.getComponent(1);
//            HTMLLabel htl = (HTMLLabel)tb.getComponent(6);
//            htl.setVisible(false);
//        } catch (Exception e) {} // Original UI probably changed
        
        return new DataViewComponent( new MasterViewSupport().getMasterView(),
                                      new DataViewComponent.MasterViewConfiguration(true));
    }
    
    
    // --- General data --------------------------------------------------------
    
    private class MasterViewSupport extends JPanel  {
        
        public DataViewComponent.MasterView getMasterView() {
            try {
                JComponent memoryDiffPanel = (JComponent)sdw.getComponent(0);
                memoryDiffPanel.setOpaque(false);
                final JToolBar toolBar = (JToolBar)memoryDiffPanel.getComponent(1);
                toolBar.setOpaque(false);
                ((JComponent)toolBar.getComponent(0)).setOpaque(false);
                ((JComponent)toolBar.getComponent(1)).setOpaque(false);
                ((JComponent)toolBar.getComponent(3)).setOpaque(false);
                ((JComponent)toolBar.getComponent(4)).setOpaque(false);
                ((JComponent)toolBar.getComponent(5)).setOpaque(false);

                JPanel toolbarSpacer = new JPanel(null) {
                    public Dimension getPreferredSize() {
                        if (UISupport.isGTKLookAndFeel() || UISupport.isNimbusLookAndFeel()) {
                            int currentWidth = toolBar.getSize().width;
                            int minimumWidth = toolBar.getMinimumSize().width;
                            int extraWidth = currentWidth - minimumWidth;
                            return new Dimension(Math.max(extraWidth, 0), 0);
                        } else {
                            return super.getPreferredSize();
                        }
                    }
                };
                toolbarSpacer.setOpaque(false);
                Component descriptionLabel = toolBar.getComponent(7);
                toolBar.remove(descriptionLabel);
                toolBar.remove(6);
                toolBar.add(toolbarSpacer);
                toolBar.add(descriptionLabel);
            } catch (Exception e) {}

            sdw.setPreferredSize(new Dimension(1, 1));
            SnapshotDiffContainer snapshotDiff = (SnapshotDiffContainer)getDataSource();
            String caption = NbBundle.getMessage(SnapshotDiffView.class, "DESCR_Snapshots_Comparison", // NOI18N
                    new Object[] { DataSourceDescriptorFactory.getDescriptor(snapshotDiff.getSnapshot1()).getName(),
                                   DataSourceDescriptorFactory.getDescriptor(snapshotDiff.getSnapshot2()).getName()});
            return new DataViewComponent.MasterView(caption, null, sdw);   // NOI18N
        }
        
    }
    
}
