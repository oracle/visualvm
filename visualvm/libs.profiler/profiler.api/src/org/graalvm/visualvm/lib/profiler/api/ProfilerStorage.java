/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Properties;
import org.graalvm.visualvm.lib.profiler.spi.ProfilerStorageProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerStorage {

    // --- Global storage ------------------------------------------------------

    /**
     * Returns FileObject which can be used as a general settings storage.
     * @param create If <code>true</code> the folder will be created if it doesn't exist yet
     * @return FileObject which can be used as a general settings storage
     * @throws IOException
     */
    public static FileObject getGlobalFolder(boolean create) throws IOException {
        ProfilerStorageProvider p = provider();
        if (p != null) return p.getGlobalFolder(create);
        else return null;
    }

    /**
     * Loads the provided Properties from a global storage directory.
     * @param properties Properties instance to load
     * @param filename name of the file containing the persisted properties
     * @throws IOException
     */
    public static void loadGlobalProperties(Properties properties, String filename) throws IOException {
        ProfilerStorageProvider p = provider();
        if (p != null) p.loadGlobalProperties(properties, filename);
    }

    /**
     * Saves the provided Properties to a global storage directory.
     * @param properties Properties instance to save
     * @param filename name of the file containing the persisted properties
     * @throws IOException
     */
    public static void saveGlobalProperties(Properties properties, String filename) throws IOException {
        ProfilerStorageProvider p = provider();
        if (p != null) p.saveGlobalProperties(properties, filename);
    }
    
    public static void deleteGlobalProperties(String filename) throws IOException {
        ProfilerStorageProvider p = provider();
        if (p != null) p.deleteGlobalProperties(filename);
    }
    
    // --- Project storage -----------------------------------------------------
    
    /**
     * Returns FileObject which can be used as a settings storage for the provided project, or global storage for null project.
     * 
     * @param project project context
     * @param create if <code>true</code> the storage will be created if not already available
     * @return FileObject which can be used as a settings storage for the provided project or null if not available
     * @throws IOException 
     */
    public static FileObject getProjectFolder(Lookup.Provider project, boolean create)
            throws IOException {
        if (project == null) return getGlobalFolder(create);
        ProfilerStorageProvider p = provider();
        if (p != null) return p.getProjectFolder(project, create);
        else return null;
    }
    
    /**
     * Returns project context for the provided settings storage FileObject or null if not resolvable.
     * 
     * @param settingsFolder settings storage
     * @return  project context for the provided settings storage FileObject or null if not resolvable
     */
    public static Lookup.Provider getProjectFromFolder(FileObject settingsFolder) {
        ProfilerStorageProvider p = provider();
        if (p != null) return p.getProjectFromFolder(settingsFolder);
        else return null;
    }
    
    /**
     * Loads the provided Properties from the project (or global for null project) storage directory.
     * @param properties Properties instance to load
     * @param project project context
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public static void loadProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException {
        if (project == null) {
            loadGlobalProperties(properties, filename);
        } else {
            ProfilerStorageProvider p = provider();
            if (p != null) p.loadProjectProperties(properties, project, filename);
        }
    }
    
    /**
     * Saves the provided Properties to the project (or global for null project) storage directory.
     * @param properties Properties instance to save
     * @param project project context
     * @param filename name of the file containing the persisted properties
     * @throws IOException 
     */
    public static void saveProjectProperties(Properties properties, Lookup.Provider project, String filename) throws IOException {
        if (project == null) {
            saveGlobalProperties(properties, filename);
        } else {
            ProfilerStorageProvider p = provider();
            if (p != null) p.saveProjectProperties(properties, project, filename);
        }
    }
    
    public static void deleteProjectProperties(Lookup.Provider project, String filename) throws IOException {
        if (project == null) {
            deleteGlobalProperties(filename);
        } else {
            ProfilerStorageProvider p = provider();
            if (p != null) p.deleteProjectProperties(project, filename);
        }
    }
    
    // --- Implementation ------------------------------------------------------
    
    private static ProfilerStorageProvider provider() {
        return Lookup.getDefault().lookup(ProfilerStorageProvider.class);
    }
    
}
