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

package com.sun.tools.visualvm.profiler;

import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.modules.profiler.LoadedSnapshot;
import org.netbeans.modules.profiler.SnapshotResultsWindow;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
class ProfilerSnapshotView extends DataSourceView {
    private static final Logger LOGGER = Logger.getLogger(ProfilerSnapshotView.class.getName());
    
    private LoadedSnapshot loadedSnapshot = null;
    private SnapshotResultsWindow srw = null;

    public ProfilerSnapshotView(ProfilerSnapshot snapshot) {
        this(snapshot, DataSourceDescriptorFactory.getDescriptor(snapshot));
    }
    
    private ProfilerSnapshotView(ProfilerSnapshot snapshot, DataSourceDescriptor descriptor) {
        super(snapshot, descriptor.getName(), descriptor.getIcon(), Positionable.POSITION_AT_THE_END, true);
        loadedSnapshot = snapshot.getLoadedSnapshot();
    }
    
        
    protected void removed() {
        if (srw != null) {
            IDEUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    try {
                        Method method = srw.getClass().getDeclaredMethod("componentClosed");
                        if (method != null) {
                            method.setAccessible(true);
                            method.invoke(srw);
                        }
                    } catch (NoSuchMethodException noSuchMethodException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(), "removed", noSuchMethodException);
                    } catch (SecurityException securityException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(), "removed", securityException);
                    } catch (IllegalAccessException illegalAccessException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(), "removed", illegalAccessException);
                    } catch (IllegalArgumentException illegalArgumentException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(), "removed", illegalArgumentException);
                    } catch (InvocationTargetException invocationTargetException) {
                        LOGGER.throwing(ProfilerSnapshotView.class.getName(), "removed", invocationTargetException);
                    }
                    srw = null;
                }
            });
        }
        loadedSnapshot = null;
    }
    
    protected DataViewComponent createComponent() {
        srw = SnapshotResultsWindow.get(loadedSnapshot, CommonConstants.SORTING_COLUMN_DEFAULT, false);
        DataViewComponent dvc = new DataViewComponent(
                new MasterViewSupport().getMasterView(),
                new DataViewComponent.MasterViewConfiguration(true));
        
        return dvc;
    }
    
    
    // --- General data --------------------------------------------------------
    
    private class MasterViewSupport extends JPanel  {
        
        public DataViewComponent.MasterView getMasterView() {
            srw.setPreferredSize(new Dimension(1, 1));
            return new DataViewComponent.MasterView(NbBundle.getMessage(ProfilerSnapshotView.class, "MSG_Profiler_Snapshot"), null, srw);   // NOI18N
        }
        
    }
    
}
