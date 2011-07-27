/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.api;

import org.netbeans.modules.profiler.spi.java.GoToSourceProvider;
import java.text.MessageFormat;
import java.util.Collection;
import org.netbeans.lib.profiler.ProfilerLogger;
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
    
    private static void openSourceImpl(final Lookup.Provider project, final String className, final String methodName, final String signature, final int line) {
        // *** logging stuff ***
        ProfilerLogger.debug("Open Source: Project: " + project); // NOI18N
        ProfilerLogger.debug("Open Source: Class name: " + className); // NOI18N
        ProfilerLogger.debug("Open Source: Method name: " + methodName); // NOI18N
        ProfilerLogger.debug("Open Source: Method sig: " + signature); // NOI18N
        
        Collection<? extends GoToSourceProvider> implementations = Lookup.getDefault().lookupAll(GoToSourceProvider.class);
        
        String st = MessageFormat.format(NbBundle.getMessage(GoToSource.class, "OpeningSourceMsg"),  // NOI18N
                                                             new Object[] { className });
        final String finalStatusText = st + " ..."; // NOI18N
        StatusDisplayer.getDefault().setStatusText(finalStatusText);
        
        for(GoToSourceProvider impl : implementations) {
            try {
                if (impl.openSource(project, className, methodName, signature, line)) return;
            } catch (Exception e) {
                ProfilerLogger.log(e);
            }
        }
        
        ProfilerDialogs.displayError(MessageFormat.format(NbBundle.getMessage(GoToSource.class,
                                                                                "NoSourceFoundMessage"), // NOI18N
                                                                                new Object[] { className }));
    }
    
    private static void openFileImpl(FileObject srcFile, int offset) {
        // *** logging stuff ***
        ProfilerLogger.debug("Open Source: FileObject: " + srcFile); // NOI18N
        ProfilerLogger.debug("Open Source: Offset: " + offset); // NOI18N
        
        Collection<? extends GoToSourceProvider> implementations = Lookup.getDefault().lookupAll(GoToSourceProvider.class);
        
        String st = MessageFormat.format(NbBundle.getMessage(GoToSource.class, "OpeningFileMsg"),  // NOI18N
                                                             new Object[] { srcFile.getName() });
        final String finalStatusText = st + " ..."; // NOI18N
        StatusDisplayer.getDefault().setStatusText(finalStatusText);
        
        for(GoToSourceProvider impl : implementations) {
            try {
                if (impl.openFile(srcFile, offset)) return;
            } catch (Exception e) {
                ProfilerLogger.log(e);
            }
        }
        
        ProfilerDialogs.displayError(MessageFormat.format(NbBundle.getMessage(GoToSource.class,
                                                                                "OpenFileFailsMessage"), // NOI18N
                                                                                new Object[] { srcFile.getName(), offset }));
    }
}
