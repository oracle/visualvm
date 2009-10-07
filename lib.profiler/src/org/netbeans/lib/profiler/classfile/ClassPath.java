/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.classfile;

import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/**
 * Class path, that can be set containing both directories and .jar files, and then used to read a .class (.java)
 * file with a specified fully qualified name.
 *
 * @author Misha Dmitirev
 * @author Tomas Hurka
 */
public class ClassPath {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private abstract static class PathEntry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected HashSet entries;
        protected int hits; // This is done to avoid indexing of the JAR files too early and all at once
        protected int threshHits; // This is done to avoid indexing of the JAR files too early and all at once

        //~ Methods --------------------------------------------------------------------------------------------------------------

        abstract String getLocationForClassFile(String fileName);
    }

    private static class Dir extends PathEntry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private File dir;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Dir(File dirF) {
            dir = dirF;
            threshHits = 100 + (int) (40 * Math.random());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        String getLocationForClassFile(String fileName) {
            if (entries != null) {
                if (entries.contains(fileName)) {
                    return dir.getAbsolutePath();
                } else {
                    return null;
                }
            } else {
                if (++hits >= threshHits) {
                    entries = new HashSet();
                    MiscUtils.getAllClassesInDir(dir.getAbsolutePath(), "", false, entries); // NOI18N

                    return getLocationForClassFile(fileName);
                } else {
                    File file = new File(dir, fileName);

                    //System.err.println("*** Trying file " + file.getAbsolutePath() + " in PathEntry = " + dir.getAbsolutePath());
                    if (file.exists()) {
                        return dir.getAbsolutePath();
                    } else {
                        return null;
                    }
                }
            }
        }
    }

    private class Zip extends PathEntry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String zipFilePath;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Zip(String path) {
            zipFilePath = path;
            threshHits = 50 + (int) (20 * Math.random());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        String getLocationForClassFile(String fileName) {
            if (entries != null) {
                if (entries.contains(fileName)) {
                    return zipFilePath;
                } else {
                    return null;
                }
            } else {
                if (++hits >= threshHits) {
                    entries = new HashSet();
                    MiscUtils.getAllClassesInJar(zipFilePath, false, entries);
                    return getLocationForClassFile(fileName);
                } else {
                    ZipFile zip;
                    try {
                        zip = getZipFileForName(zipFilePath);
                    } catch (IOException ex) {
                        System.err.println("Warning: CLASSPATH component " + zipFilePath + ": " + ex); // NOI18N
                        return null;
                    }
                    ZipEntry entry = zip.getEntry(fileName);

                    if (entry != null) {
                        return zipFilePath;
                    } else {
                        return null;
                    }
                }
            }
        }
    }
    
    private static class JarLRUCache extends LinkedHashMap {
        private static final int MAX_CAPACITY = 100;
        
        private JarLRUCache() {  
            super(10, 0.75f, true); 
        }
        
        protected boolean removeEldestEntry(Map.Entry eldest) {
            if (size()>MAX_CAPACITY) {
                try {
                    ((ZipFile)eldest.getValue()).close();
                } catch (IOException ex) {
                    // ignore
                }
                return true;
            }
            return false;
        }

    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JarLRUCache zipFileNameToFile;
    private PathEntry[] paths;
    private boolean isCP; // True for a class path, false for a source path

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ClassPath(String classPath, boolean isCP) {
        this.isCP = isCP;
        List vec = new ArrayList();
        zipFileNameToFile = new JarLRUCache();

        for (StringTokenizer tok = new StringTokenizer(classPath, File.pathSeparator); tok.hasMoreTokens();) {
            String path = tok.nextToken();

            if (!path.equals("")) { // NOI18N
                File file = new File(path);

                if (file.exists()) {
                    if (file.isDirectory()) {
                        vec.add(new Dir(file));
                    } else {
                        vec.add(new Zip(file.getPath()));
                    }
                }
            }
        }

        paths = new PathEntry[vec.size()];
        vec.toArray(paths);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Searches for the class on this class path, reads it if found, and returns the DynamicClassInfo for it.
     * If class is not found, returns null. Exceptions are thrown if class file is found but something goes wrong when reading it.
     */
    public DynamicClassInfo getClassInfoForClass(String className, int classLoaderId)
                                          throws IOException, ClassFormatError {
        String slashedClassName = className.replace('.', '/'); // NOI18N
                                                               //System.err.println("*** Requested " + slashedClassName);

        String dirOrJar = getLocationForClass(slashedClassName);

        //if (dirOrJar == null) System.err.println("*** Unsuccessful for " + slashedClassName);
        if (dirOrJar == null) {
            return null;
        }

        return new DynamicClassInfo(slashedClassName, classLoaderId, dirOrJar);
    }

    /** Requires "slashed" class name. Returns the directory or .jar name where this class is located, or null if not found. */
    public String getLocationForClass(String slashedClassName) {
        String fileName = slashedClassName + (isCP ? ".class" : ".java"); // NOI18N

        for (int i = 0; i < paths.length; i++) {
            String location = paths[i].getLocationForClassFile(fileName);

            if (location != null) {
                return location;
            }
        }

        return null;
    }

    /** This is used to avoid repetitive creation of ZipFiles in the code that reads files from JARs given just the name of the latter */
    public ZipFile getZipFileForName(String zipFileName) throws IOException {
        ZipFile zip = (ZipFile) zipFileNameToFile.get(zipFileName);
        if (zip == null) {
            zip = new ZipFile(zipFileName);
            zipFileNameToFile.put(zipFileName,zip);
        }
        return zip;
    }

    public void close() {
        // close all ZipFiles in ClassPath, the files on disk would otherwise be locked
        // this is a bugfix for http://profiler.netbeans.org/issues/show_bug.cgi?id=61849
        for (Iterator it = zipFileNameToFile.values().iterator(); it.hasNext();) {
            try {
                ((ZipFile) it.next()).close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    //------------------------------------------ Debugging -----------------------------------------
    public String toString() {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < paths.length; i++) {
            buf.append((paths[i] instanceof Dir) ? ((Dir) paths[i]).dir.getAbsolutePath() : ((Zip) paths[i]).zipFilePath);
            buf.append(File.pathSeparatorChar);
        }

        return buf.toString();
    }
}
