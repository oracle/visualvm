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

import org.netbeans.lib.profiler.utils.FileOrZipEntry;
import org.netbeans.lib.profiler.utils.MiscUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * Fixed-size cache of binary classes (.class files). Used to avoid flooding memory with class files when performing intensive
 * method scanning, that may touch thousands of classes. Currently uses LRU eviction policy.
 * A separate, currently no-eviction cache, is maintained for classes supplied by the VM.
 *
 * @author Misha Dmitirev
 */
public class ClassFileCache {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static ClassFileCache defaultClassFileCache;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ClassPath classPath; // Used to quickly obtain an open JAR file for a given name
    private Hashtable vmSuppliedClassCache;
    private byte[][] classFileBytes;
    private String[] classNameAndLocation;
    private long[] lastTimeUsed;
    private int capacity;
    private int size;
    private int sizeLimit;
    private long timeCounter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //------------ We don't expect the below API to be used outside of this package, hence it's package-private ------------
    ClassFileCache() {
        capacity = 877; // FIXME: may be worth setting size flexibly, or adjusting inside cache if too many evictions happen
        size = 0;
        sizeLimit = (capacity * 3) / 4;
        classNameAndLocation = new String[capacity];
        classFileBytes = new byte[capacity][];
        lastTimeUsed = new long[capacity];

        vmSuppliedClassCache = new Hashtable();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static ClassFileCache getDefault() {
        if (defaultClassFileCache == null) {
            defaultClassFileCache = new ClassFileCache();
        }

        return defaultClassFileCache;
    }

    static void resetDefaultCache() {
        defaultClassFileCache = null;
    }

    byte[] getClassFile(String name, String location) throws IOException {
        String nameAndLocation = (name + "#" + location).intern(); // NOI18N
        byte[] res;

        if (location.startsWith(ClassRepository.LOCATION_VMSUPPLIED)) {
            res = (byte[]) vmSuppliedClassCache.get(nameAndLocation);
        } else {
            res = get(nameAndLocation);

            if (res == null) {
                if (size > sizeLimit) {
                    removeLRUEntry();
                }

                res = readAndPut(name, location, nameAndLocation);
            }
        }

        return res;
    }

    void addVMSuppliedClassFile(String name, int classLoaderId, byte[] buf) {
        String nameAndLocation = (name + "#" + ClassRepository.LOCATION_VMSUPPLIED + classLoaderId).intern(); // NOI18N
        vmSuppliedClassCache.put(nameAndLocation, buf);
    }

    /**
     * Returns the actual class loader id for the given class/loader pair, or -1 if class is not loaded.
     * The real loader may be the same as classLoaderId or its parent loader.
     */
    int hasVMSuppliedClassFile(String name, int classLoaderId) {
        do {
            // we are trying the whole classloader hierarchy up to the root system classloader with id=0
            boolean res = (vmSuppliedClassCache.containsKey((name + "#" + ClassRepository.LOCATION_VMSUPPLIED + classLoaderId)
                                                                                                                                            .intern())); // NOI18N

            if (res) {
                return classLoaderId;
            } else if (classLoaderId != 0) {
                classLoaderId = ClassLoaderTable.getParentLoader(classLoaderId);
            }

            if (classLoaderId == -1) {
                MiscUtils.printWarningMessage("Failed to lookup classloader for: " + name); // NOI18N

                return -1;
            }
        } while (classLoaderId != 0);

        return -1;
    }

    //---------------------------------------- Private implementation -------------------------------------------
    private byte[] get(String nameAndLocation) {
        int pos = (nameAndLocation.hashCode() & 0x7FFFFFFF) % capacity;

        while ((classNameAndLocation[pos] != null) && (classNameAndLocation[pos] != nameAndLocation)) {
            pos = (pos + 1) % capacity;
        }

        if (classNameAndLocation[pos] != null) {
            lastTimeUsed[pos] = ++timeCounter;

            return classFileBytes[pos];
        } else {
            return null;
        }
    }

    private byte[] readAndPut(String name, String classFileLocation, String nameAndLocation)
                       throws IOException {
        byte[] classFile = readClassFile(name, classFileLocation);
        int pos = (nameAndLocation.hashCode() & 0x7FFFFFFF) % capacity;

        while (classNameAndLocation[pos] != null) {
            pos = (pos + 1) % capacity;
        }

        classNameAndLocation[pos] = nameAndLocation;
        classFileBytes[pos] = classFile;
        lastTimeUsed[pos] = ++timeCounter;
        size++;

        return classFile;
    }

    private byte[] readClassFile(String name, String classFileLocation)
                          throws IOException {
        String classFileName = name + ".class"; // NOI18N
        File location = new File(classFileLocation);

        if (location.isDirectory()) {
            return MiscUtils.readFileIntoBuffer(new FileOrZipEntry(classFileLocation, classFileName));
        } else { // Should be .jar file
                 // The following code may be used at different stages of JFluid work, with different initialization states, so
                 // it's coded defensively. If it can use an available open ZipFile, it will use it, otherwise it will open its own.

            ZipFile zip = null;

            if (classPath == null) {
                classPath = ClassRepository.getClassPath();
            }

            if (classPath != null) {
                try {
                    zip = classPath.getZipFileForName(classFileLocation);
                } catch (ZipException e2) {
                    throw new IOException("Could not open archive " + classFileLocation); // NOI18N
                }
            } else {
                throw new IOException("Could not get classpath for " + classFileName + " in " + classFileLocation); // NOI18N
            }

            ZipEntry entry = zip.getEntry(classFileName);

            if (entry == null) {
                throw new IOException("Could not find entry for " + classFileName + " in " + classFileLocation); // NOI18N
            }

            int len = (int) entry.getSize();
            byte[] buf = new byte[len];
            InputStream in = zip.getInputStream(entry);
            int readBytes;
            int ofs = 0;
            int remBytes = len;

            do {
                readBytes = in.read(buf, ofs, remBytes);
                ofs += readBytes;
                remBytes -= readBytes;
            } while (ofs < len);

            in.close();

            return buf;
        }
    }

    private void removeLRUEntry() {
        long leastTime = 0x7FFFFFFFFFFFFFFFL;
        int pos = 0;

        for (int i = 0; i < capacity; i++) {
            if ((lastTimeUsed[i] > 0) && (lastTimeUsed[i] < leastTime)) {
                pos = i;
            }
        }

        classNameAndLocation[pos] = null;
        classFileBytes[pos] = null;
        lastTimeUsed[pos] = 0;
        size--;

        return;
    }
}
