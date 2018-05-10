/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.coredump;

import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import org.graalvm.visualvm.core.snapshot.SnapshotsSupport;
import java.io.File;
import java.io.IOException;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * Abstract implementation of CoreDump.
 * Each coredump is defined by a coredump file and JDK_HOME directory of the JDK
 * which was running the original application.
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public abstract class CoreDump extends Snapshot {
    
    private final File jdkHome;
    
    
    /**
     * Creates new instance of a coredump.
     * 
     * @param file coredump file.
     * @param jdkHome JDK_HOME directory of the JDK which was running the original application.
     * @throws java.io.IOException if file or jdkHome are invalid.
     */
    public CoreDump(File file, File jdkHome) throws IOException {
        this(file, jdkHome, null);
    }
    
    /**
     * Creates new instance of a coredump.
     * 
     * @param file coredump file.
     * @param jdkHome JDK_HOME directory of the JDK which was running the original application.
     * @param master master DataSource for the coredump.
     * @throws java.io.IOException if file or jdkHome are invalid.
     */
    public CoreDump(File file, File jdkHome, DataSource master) throws IOException {
        super(file, CoreDumpSupport.getCategory(), master);
        
        if (!file.exists() || !file.isFile())
            throw new IOException("File " + file.getAbsolutePath() + " does not exist");    // NOI18N
        
        if (jdkHome != null) {
            if (!jdkHome.exists() || !jdkHome.isDirectory())
                throw new IOException("Java Home " + jdkHome.getAbsolutePath() + " does not exist");    // NOI18N
            this.jdkHome = jdkHome;
        } else {
            this.jdkHome = new File(System.getProperty("java.home")).getCanonicalFile();    // NOI18N
        }
    }
    
    /**
     * Returns the Java executable of the JDK which was running the original application.
     * 
     * @return the Java executable of the JDK which was running the original application.
     */
    public final String getExecutable() {
        String home = getJDKHome();
        
        String exec = home+File.separatorChar+"bin"+File.separatorChar+"java";  // NOI18N
        if (Utilities.isWindows()) {
            exec +=".exe";  // NOI18N
        }
        return exec;
    }
    
    /**
     * Returns JDK_HOME directory of the JDK which was running the original application.
     * 
     * @return JDK_HOME directory of the JDK which was running the original application.
     */
    public final String getJDKHome() {
        return jdkHome.getAbsolutePath();
    }
    
    public boolean supportsSaveAs() {
        return getFile() != null;
    }
    
    public void saveAs() {
        SnapshotsSupport.getInstance().saveAs(this, NbBundle.getMessage(CoreDump.class, "LBL_Save_Core_Dump_As"));  // NOI18N
    }

}
