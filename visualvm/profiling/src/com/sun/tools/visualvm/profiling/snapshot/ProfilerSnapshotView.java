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

package com.sun.tools.visualvm.profiling.snapshot;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.SnapshotResultsWindow;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
final class ProfilerSnapshotView extends DataSourceView {
    private static final Logger LOGGER =
            Logger.getLogger(ProfilerSnapshotView.class.getName());
    
    private LoadedSnapshot loadedSnapshot = null;
    private SnapshotResultsWindow srw = null;

    public ProfilerSnapshotView(ProfilerSnapshot snapshot) {
        this(snapshot, DataSourceDescriptorFactory.getDescriptor(snapshot));
    }
    
    private ProfilerSnapshotView(ProfilerSnapshot snapshot, DataSourceDescriptor descriptor) {
        super(snapshot, descriptor.getName(), descriptor.getIcon(),
              Positionable.POSITION_AT_THE_END, true);
        loadedSnapshot = snapshot.getLoadedSnapshot();
    }
    
        
    protected void removed() {
        if (srw != null) {
            IDEUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    try {
                        Method method = srw.getClass().getDeclaredMethod("componentClosed");   // NOI18N
                        if (method != null) {
                            method.setAccessible(true);
                            method.invoke(srw);
                        }
                    } catch (NoSuchMethodException noSuchMethodException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                        "removed", noSuchMethodException);   // NOI18N
                    } catch (SecurityException securityException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                        "removed", securityException);   // NOI18N
                    } catch (IllegalAccessException illegalAccessException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                        "removed", illegalAccessException);   // NOI18N
                    } catch (IllegalArgumentException illegalArgumentException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                        "removed", illegalArgumentException);   // NOI18N
                    } catch (InvocationTargetException invocationTargetException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(),
                                        "removed", invocationTargetException);   // NOI18N
                    }
                    srw = null;
                }
            });
        }
        loadedSnapshot = null;
    }
    
    protected DataViewComponent createComponent() {
        srw = SnapshotResultsWindow.get(loadedSnapshot, CommonConstants.
                                        SORTING_COLUMN_DEFAULT, false);
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport().getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private class MasterViewSupport extends JPanel  {
        
        public DataViewComponent.MasterView getMasterView() {
            try {
                JComponent cpuResPanel = (JComponent)srw.getComponent(0);
                cpuResPanel.setOpaque(false);
                JToolBar toolBar = (JToolBar)cpuResPanel.getComponent(1);
                toolBar.setOpaque(false);
                int componentsCount = toolBar.getComponentCount();
                ((JComponent)toolBar.getComponent(0)).setOpaque(false);
                ((JComponent)toolBar.getComponent(1)).setOpaque(false);
                ((JComponent)toolBar.getComponent(componentsCount - 3)).setOpaque(false);
                ((JComponent)toolBar.getComponent(componentsCount - 2)).setOpaque(false);
                ((JComponent)toolBar.getComponent(componentsCount - 1)).setOpaque(false);
                JTabbedPane tabbedPane = (JTabbedPane)cpuResPanel.getComponent(0);
                JComponent infoPanel = (JComponent)tabbedPane.getComponentAt(tabbedPane.getTabCount() - 1);
                infoPanel.setBorder(BorderFactory.createEmptyBorder());
            } catch (Exception e) {}
            srw.setPreferredSize(new Dimension(1, 1));
            return new DataViewComponent.MasterView(NbBundle.getMessage(
                    ProfilerSnapshotView.class, "DESCR_Profiler_Snapshot"), null, srw);   // NOI18N
        }
        
    }
    
}
