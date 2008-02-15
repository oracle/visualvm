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

package com.sun.tools.visualvm.core.coredump;

import com.sun.tools.visualvm.core.datasource.AbstractSnapshot;
import com.sun.tools.visualvm.core.datasource.CoreDump;
import java.io.File;
import java.io.IOException;
import org.openide.util.Utilities;

/**
 *
 * @author Tomas Hurka
 */
class CoreDumpImpl extends AbstractSnapshot implements CoreDump {
    private String displayName;
    private final File jdkHome;
    
    public CoreDumpImpl(File file,String dName, String javaHomeName) throws IOException {
        super(file);
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File "+file.getAbsolutePath()+" does not exists");
        }
        if (javaHomeName != null && javaHomeName.length()>0) {
            jdkHome = new File(javaHomeName).getCanonicalFile();
            if (!jdkHome.exists() || !jdkHome.isDirectory()) {
                throw new IOException("Java Home "+javaHomeName+" does not exists");
            }
        } else {
            jdkHome = new File(System.getProperty("java.home")).getCanonicalFile();
        }
        displayName = dName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String newDisplayName) {
        if (displayName == null && newDisplayName == null) return;
        String oldDisplayName = displayName;
        displayName = newDisplayName;
        getChangeSupport().firePropertyChange(PROPERTY_DISPLAYNAME, oldDisplayName, newDisplayName);
    }
    
    public int hashCode() {
        return getFile().hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof CoreDumpImpl) {
            return getFile().equals(((CoreDumpImpl) obj).getFile());
        }
        return false;
    }
    
    String getId() {
        return Integer.toString(getFile().hashCode());
    }
    
    public String getExecutable() {
        String home = getJDKHome();
        
        String exec = home+File.separatorChar+"bin"+File.separatorChar+"java";
        if (Utilities.isWindows()) {
            exec +=".exe";
        }
        return exec;
    }
    
    public String getJDKHome() {
        return jdkHome.getAbsolutePath();
    }
    
    void finished() {
        setState(STATE_FINISHED); 
    }
    
}
