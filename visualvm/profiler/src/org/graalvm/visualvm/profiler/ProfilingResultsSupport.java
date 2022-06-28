/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.profiler;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.lib.profiler.ResultsListener;
import org.graalvm.visualvm.lib.profiler.spi.ProfilerStorageProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
final class ProfilingResultsSupport extends JPanel {

    ProfilingResultsSupport() {
        super();
        initComponents();
    }

    public DataViewComponent.DetailsView getDetailsView() {
        return new DataViewComponent.DetailsView(NbBundle.getMessage(
                ProfilingResultsSupport.class, "MSG_Profiling_results"), null, 10, // NOI18N
                this, null);
    }

    public void setProfilingResultsDisplay(JComponent profilingResultsDisplay) {
        removeAll();
        if (profilingResultsDisplay != null) {
            add(profilingResultsDisplay);
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setOpaque(false);
    }
    
    
    static abstract class ResultsView extends JPanel {
        
        abstract void refreshResults();
        
        abstract void resetResults();
        
        abstract void sessionStateChanged(int sessionState);
        
        protected static final RequestProcessor RESULTS_PROCESSOR = new RequestProcessor("Results View Processor"); // NOI18N
        
    }
    
    
    @ServiceProvider(service=ResultsListener.class)
    public static class ResultsResetter implements ResultsListener {
        
        private final List<ResultsView> views = new ArrayList();
        
        
        public static ResultsResetter registerView(ResultsView view) {
            ResultsResetter handler = Lookup.getDefault().lookup(ResultsResetter.class);
            handler.views.add(view);
            return handler;
        }
        
        public void unregisterView(ResultsView view) {
            views.remove(view);
        }
        
        
        public void resultsAvailable() {}
        
        public void resultsReset() {
            for (ResultsView updater : views) updater.resetResults();
        }

    }
    
    
    @ServiceProvider(service=ProfilerStorageProvider.class)
    public static class VisualVMStorageProvider extends ProfilerStorageProvider.Abstract {

        private static final String PROFILER_FOLDER = "NBProfiler/Config";  // NOI18N
        private static final String SETTINGS_FOLDER = "Settings";   // NOI18N

        public FileObject getGlobalFolder(boolean create) throws IOException {
            FileObject folder = FileUtil.getConfigFile(PROFILER_FOLDER);
            FileObject settingsFolder = folder.getFileObject(SETTINGS_FOLDER, null);

            if ((settingsFolder == null) && create)
                settingsFolder = folder.createFolder(SETTINGS_FOLDER);

            return settingsFolder;
        }

        public FileObject getProjectFolder(Lookup.Provider project, boolean create) throws IOException {
            return null;
        }

        public Lookup.Provider getProjectFromFolder(FileObject settingsFolder) {
            return null;
        }

    }
    
}
