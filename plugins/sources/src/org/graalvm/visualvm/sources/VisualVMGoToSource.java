/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.spi.java.GoToSourceProvider;
import org.graalvm.visualvm.sources.impl.SourceHandles;
import org.graalvm.visualvm.sources.impl.SourceRoots;
import org.graalvm.visualvm.sources.impl.SourceViewers;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "VisualVMGoToSource_NoSourceRootsCaption=Go To Source", // NOI18N
    "VisualVMGoToSource_NoSourceRoots=<html><br><b>Source roots have not been defined yet.</b><br><br>Use Options | Sources | Definitions to define the directories or archives containing the sources.</html>", // NOI18N
    "VisualVMGoToSource_SourceNotFound=No source found for {0}",                // NOI18N
    "VisualVMGoToSource_OpenSourceFailed=Failed to open source for {0}"         // NOI18N
})
final class VisualVMGoToSource {
    
    private static final Logger LOGGER = Logger.getLogger(VisualVMGoToSource.class.getName());
    

    private static boolean openSourceImpl(SourceHandle handle) {
        try {
            if (!SourceViewers.getSelectedViewer().open(handle))
                ProfilerDialogs.displayError(Bundle.VisualVMGoToSource_OpenSourceFailed(SourceHandle.simpleUri(handle.getSourceUri())));
            return true;
        } catch (Throwable t) {
            ProfilerDialogs.displayError(Bundle.VisualVMGoToSource_OpenSourceFailed(SourceHandle.simpleUri(handle.getSourceUri())));
            LOGGER.log(Level.INFO, "Failed to open source " + handle.toString(), t); // NOI18N
            return true;
        } finally {
            try { handle.close(); }
            catch (Throwable t) { LOGGER.log(Level.INFO, "Failed to close source " + handle.toString(), t); } // NOI18N
        }
    }
    
    
    @ServiceProvider(service=GoToSourceProvider.class)
    public static final class Provider extends GoToSourceProvider {
        
        @Override
        public boolean openSource(Lookup.Provider project, String className, String methodName, String signature, int line) {
            if (SourceRoots.getRoots().length == 0) {
                ProfilerDialogs.displayWarning(Bundle.VisualVMGoToSource_NoSourceRoots(), Bundle.VisualVMGoToSource_NoSourceRootsCaption(), null);
                OptionsDisplayer.getDefault().open("SourcesOptions");           // NOI18N
            } else {
                for (SourceHandleProvider provider : SourceHandles.registeredProviders()) {
                    SourceHandle handle = provider.createHandle(className, methodName, signature, line);
                    if (handle != null) return openSourceImpl(handle);
                }

                ProfilerDialogs.displayError(Bundle.VisualVMGoToSource_SourceNotFound(className));
            }
            
            return true;
        }

        @Override
        public boolean openFile(FileObject srcFile, int offset) {
            throw new UnsupportedOperationException("GoToSource: openFile not supported in VisualVM"); // NOI18N
        }
        
    }
    
}
