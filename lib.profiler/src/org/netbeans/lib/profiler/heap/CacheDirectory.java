/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
 */
package org.netbeans.lib.profiler.heap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 *
 * @author Tomas Hurka
 */
class CacheDirectory {
    
    private static final String DIR_EXT = ".hwcache";   // NOI18N
    private static final String DUMP_AUX_FILE = "NBProfiler.nphd";   // NOI18N
    
    private File cacheDirectory;
    
    static CacheDirectory getHeapDumpCacheDirectory(File heapDump) {
        String dumpName = heapDump.getName();
        File parent = heapDump.getParentFile();
        File dir = new File(parent, dumpName+DIR_EXT);
        return new CacheDirectory(dir);
    }
    
    CacheDirectory(File cacheDir) {
        cacheDirectory = cacheDir;
        if (cacheDir != null) {
            if (!cacheDir.exists()) {
                if (!cacheDir.mkdir()) {
                    cacheDirectory = null;
                }
            }
        }
        if (cacheDirectory != null) {
            assert cacheDirectory.isDirectory() && cacheDirectory.canRead() && cacheDirectory.canWrite();            
        }
    }
    
    File createTempFile(String prefix, String suffix) throws IOException {
        File newFile;
        
        if (isTemporary()) {
            newFile = File.createTempFile(prefix, suffix);
            newFile.deleteOnExit();
        } else {
            newFile = File.createTempFile(prefix, suffix, cacheDirectory);
        }
        return newFile;
    }
    
    File getHeapDumpAuxFile() {
        assert !isTemporary();
        return new File(cacheDirectory, DUMP_AUX_FILE);
    }
    
    boolean isTemporary() {
        return cacheDirectory == null;
    }

    File getCacheFile(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        if (isFileRW(f)) {
            return f;
        }
        // try to find file in cache directory
        f = new File(cacheDirectory, f.getName());
        if (isFileRW(f)) {
            return f;
        }
        throw new FileNotFoundException(fileName);
    }

    File getHeapFile(String fileName) throws FileNotFoundException {
        File f = new File(fileName);
        if (isFileR(f)) {
            return f;
        }
        // try to find heap dump file next to cache directory
        f = new File(cacheDirectory.getParentFile(), f.getName());
        if (isFileR(f)) {
            return f;
        }
        throw new FileNotFoundException(fileName);        
    }
    
    private static boolean isFileR(File f) {
        return f.exists() && f.isFile() && f.canRead();
    }
    
    private static boolean isFileRW(File f) {
        return isFileR(f) && f.canWrite();
    }
}
