/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.api;

import org.graalvm.visualvm.lib.profiler.spi.java.GoToSourceProvider;
import java.util.Collection;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * GoToSource class allows to open source file at specified line number or method.
 *
 * @author Jaroslav Bachorik
 * @author Tomas Hurka
 */
final public class GoToSource {

    private static final RequestProcessor srcOpenerRP = new RequestProcessor("Profiler Source Opener"); // NOI18N

    /**
     * Returns true if at least one provider of GoToSource is available. This still
     * doesn't mean that opening a concrete source is supported, the provider(s)
     * may not support the source type.
     *
     * @return true if at least one provider of GoToSource is available, false otherwise
     */
    public static boolean isAvailable() {
        return Lookup.getDefault().lookup(GoToSourceProvider.class) != null;
    }

    /**
     * Open a source code file on a given position.
     * @param srcFile The source file to be opened
     * @param offset The position to open the file at
     * @return  Returns TRUE if such file exists and the offset is valid
     */
    public static void openFile(final FileObject srcFile, final int offset) {
        srcOpenerRP.post(new Runnable() {

            @Override
            public void run() {
                openFileImpl(srcFile, offset);
            }
        });
    }

    /**
     * Open a source specified by parameters.
     * @param project The associated project
     * @param className The class name
     * @param methodName The method name or NULL
     * @param signature The signature or NULL
     */
    public static void openSource(Lookup.Provider project, String className, String methodName, String methodSig) {
        openSource(project, className, methodName, methodSig, -1);
    }

    /**
     * Open a source specified by parameters.
     * @param project The associated project
     * @param className The class name
     * @param methodName The method name or NULL
     * @param line The line number or {@linkplain Integer#MIN_VALUE}
     */
    public static void openSource(Lookup.Provider project, String className, String methodName, int line) {
        openSource(project, className, methodName, null, line);
    }

    private static void openSource(final Lookup.Provider project, final String className, final String methodName, final String signature, final int line) {
        srcOpenerRP.post(new Runnable() {
            
            @Override
            public void run() {
                openSourceImpl(project, className, methodName, signature, line);
            }
        });
    }
    
    @NbBundle.Messages({
        "OpeningSourceMsg=Opening source for class {0}",
        "NoSourceFoundMessage=No source found for class {0}"
    })
    private static void openSourceImpl(Lookup.Provider project, String className, String methodName, String signature, int line) {
        int idx = methodName == null ? -1 : methodName.indexOf("[native]"); // NOI18N
        if (idx > -1) methodName = methodName.substring(0, idx);
        
        // *** logging stuff ***
        ProfilerLogger.debug("Open Source: Project: " + project); // NOI18N
        ProfilerLogger.debug("Open Source: Class name: " + className); // NOI18N
        ProfilerLogger.debug("Open Source: Method name: " + methodName); // NOI18N
        ProfilerLogger.debug("Open Source: Method sig: " + signature); // NOI18N
        
        Collection<? extends GoToSourceProvider> implementations = Lookup.getDefault().lookupAll(GoToSourceProvider.class);
        
        String st = Bundle.OpeningSourceMsg(className);
        final String finalStatusText = st + " ..."; // NOI18N
        StatusDisplayer.getDefault().setStatusText(finalStatusText);
        
        for(GoToSourceProvider impl : implementations) {
            try {
                if (impl.openSource(project, className, methodName, signature, line)) return;
            } catch (Exception e) {
                ProfilerLogger.log(e);
            }
        }
        
        ProfilerDialogs.displayError(Bundle.NoSourceFoundMessage(className));
    }
    
    @NbBundle.Messages({
        "OpeningFileMsg=Opening source file {0}",
        "OpenFileFailsMessage=File \"{0}\" does not exist or the offset \"{1}\" is out of range"
    })
    private static void openFileImpl(FileObject srcFile, int offset) {
        // *** logging stuff ***
        ProfilerLogger.debug("Open Source: FileObject: " + srcFile); // NOI18N
        ProfilerLogger.debug("Open Source: Offset: " + offset); // NOI18N
        
        Collection<? extends GoToSourceProvider> implementations = Lookup.getDefault().lookupAll(GoToSourceProvider.class);
        
        String st = Bundle.OpeningFileMsg(srcFile.getName());
        final String finalStatusText = st + " ..."; // NOI18N
        StatusDisplayer.getDefault().setStatusText(finalStatusText);
        
        for(GoToSourceProvider impl : implementations) {
            try {
                if (impl.openFile(srcFile, offset)) return;
            } catch (Exception e) {
                ProfilerLogger.log(e);
            }
        }
        
        ProfilerDialogs.displayError(Bundle.OpenFileFailsMessage(srcFile.getName(), offset));
    }
}
