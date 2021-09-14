/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * This is factory class for creating {@link Heap} from the file in Hprof dump format.
 * @author Tomas Hurka
 */
public class HeapFactory {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * this factory method creates {@link Heap} from a memory dump file in Hprof format.
     * <br>
     * Speed: slow
     * @param heapDump file which contains memory dump
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump) throws FileNotFoundException, IOException {
        return createHeap(heapDump, 0);
    }

    /**
     * this factory method creates {@link Heap} from a memory dump file in Hprof format.
     * If the memory dump file contains more than one dump, parameter segment is used to
     * select particular dump.
     * <br>
     * Speed: slow
     * @return implementation of {@link Heap} corresponding to the memory dump
     * passed in heapDump parameter
     * @param segment select corresponding dump from multi-dump file
     * @param heapDump file which contains memory dump
     * @throws java.io.FileNotFoundException if heapDump file does not exist
     * @throws java.io.IOException if I/O error occurred while accessing heapDump file
     */
    public static Heap createHeap(File heapDump, int segment)
                           throws FileNotFoundException, IOException {
        CacheDirectory cacheDir = CacheDirectory.getHeapDumpCacheDirectory(heapDump, segment);
        if (!cacheDir.isTemporary()) {
            File savedDump = cacheDir.getHeapDumpAuxFile();

            if (savedDump.exists() && savedDump.isFile() && savedDump.canRead()) {
                try {
                    return loadHeap(cacheDir);
                } catch (IOException ex) {
                    System.err.println("Loading heap dump "+heapDump+" from cache failed.");
                    ex.printStackTrace(System.err);
                    cacheDir.deleteAllCachedFiles();
                }
            }
        }
        return new HprofHeap(heapDump, segment, cacheDir);

    }
    
    static Heap loadHeap(CacheDirectory cacheDir)
                           throws FileNotFoundException, IOException {
        File savedDump = cacheDir.getHeapDumpAuxFile();
        InputStream is = new BufferedInputStream(new FileInputStream(savedDump), 64*1024);
        DataInputStream dis = new DataInputStream(is);
        Heap heap = new HprofHeap(dis, cacheDir);
        dis.close();
        return heap;
    }
    
}
