/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.profiler.spi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerStorageProvider {
    
    // --- Global storage ------------------------------------------------------
    
    /**
     * Returns FileObject which can be used as a general settings storage.
     * @param create If <code>true</code> the folder will be created if it doesn't exist yet
     * @return FileObject which can be used as a general settings storage
     * @throws IOException 
     */
    public abstract FileObject getGlobalFolder(boolean create) throws IOException;
        
    /**
     * Loads the provided Properties from a global storage directory.
     * @param properties Properties instance to load
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public abstract void loadGlobalProperties(Properties properties, String filename) throws IOException;
    
    /**
     * Saves the provided Properties to a global storage directory.
     * @param properties Properties instance to save
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public abstract void saveGlobalProperties(Properties properties, String filename) throws IOException;
    
    public abstract void deleteGlobalProperties(String filename) throws IOException;
    
    // --- Project storage -----------------------------------------------------
    
    /**
     * Returns FileObject which can be used as a settings storage for the provided project or null if not available.
     * 
     * @param project project context, for null GlobalStorage.getSettingsFolder(create) will be called
     * @param create if <code>true</code> the storage will be created if not already available
     * @return FileObject which can be used as a settings storage for the provided project or null if not available
     * @throws IOException 
     */
    public abstract FileObject getProjectFolder(Lookup.Provider project, boolean create) throws IOException;
    
    /**
     * Returns project context for the provided settings storage FileObject or null if not resolvable.
     * 
     * @param settingsFolder settings storage
     * @return  project context for the provided settings storage FileObject or null if not resolvable
     */
    public abstract Lookup.Provider getProjectFromFolder(FileObject settingsFolder);
    
    /**
     * Loads the provided Properties from the project storage directory.
     * @param properties Properties instance to load
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public abstract void loadProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException;
    
    /**
     * Saves the provided Properties to the project storage directory.
     * @param properties Properties instance to save
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public abstract void saveProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException;
    
    public abstract void deleteProjectProperties(Lookup.Provider project, String filename) throws IOException;
    
    
    // --- Base implementation -------------------------------------------------
    
    public static abstract class Abstract extends ProfilerStorageProvider {
        
        protected String EXT = "xml"; // NOI18N
        
        // --- Global storage --------------------------------------------------
        
        @Override
        public void loadGlobalProperties(Properties properties, String filename) throws IOException {
            FileObject folder = getGlobalFolder(false);
            if (folder == null) return;

            FileObject fo = folder.getFileObject(filename, EXT);
            if (fo == null) return;

            loadProperties(properties, fo);
        }

        @Override
        public void saveGlobalProperties(Properties properties, String filename) throws IOException {
            FileObject folder = getGlobalFolder(true);
            FileObject fo = folder.getFileObject(filename, EXT);
            if (fo == null) fo = folder.createData(filename, EXT);

            saveProperties(properties, fo);
        }

        @Override
        public void deleteGlobalProperties(String filename) throws IOException {
            FileObject folder = getGlobalFolder(false);
            FileObject fo = folder == null ? null : folder.getFileObject(filename, EXT);
            if (fo != null) deleteProperties(fo);
        }
        
        // --- Project storage -------------------------------------------------
        
        @Override
        public void loadProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException {
            FileObject folder = getProjectFolder(project, false);
            if (folder == null) return;

            FileObject fo = folder.getFileObject(filename, EXT);
            if (fo == null) {
                // Convert PP storage .pp to .xml
                fo = folder.getFileObject(filename, "pp"); // NOI18N
                if (fo != null) {
                    FileLock fol = fo.lock();
                    try { fo.rename(fol, filename, EXT); }
                    finally { fol.releaseLock(); }
                }
            }
            if (fo == null) return;

            loadProperties(properties, fo);
        }

        @Override
        public void saveProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException {
            FileObject folder = getProjectFolder(project, true);
            FileObject fo = folder.getFileObject(filename, EXT);
            if (fo == null) fo = folder.createData(filename, EXT);

            saveProperties(properties, fo);
        }

        @Override
        public void deleteProjectProperties(Lookup.Provider project, String filename) throws IOException {
            FileObject folder = getProjectFolder(project, false);
            FileObject fo = folder == null ? null : folder.getFileObject(filename, EXT);
            if (fo != null) deleteProperties(fo);
        }
        
        // --- Implementation --------------------------------------------------
    
        protected void loadProperties(Properties properties, FileObject storage) throws IOException {
            synchronized (this) {
                InputStream is = storage.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);
                try {
                    properties.loadFromXML(bis);
                } finally {
                    bis.close();
                }
            }
        }

        protected void saveProperties(Properties properties, FileObject storage) throws IOException {
            synchronized (this) {
                OutputStream os = storage.getOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(os);
                try {
                    properties.storeToXML(bos, ""); // NOI18N
                } finally {
                    if (bos != null) bos.close();
                }
            }
        }

        protected void deleteProperties(FileObject storage) throws IOException {
            synchronized (this) {
                storage.delete();
            }
        }
        
    }
    
}
