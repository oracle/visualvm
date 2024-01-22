/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * This command is issued by the back end to signal that the root instrumentation class has been loaded.
 * It contains the information about all classes loaded by the target JVM by that time.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class RootClassLoadedCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] allLoadedClassLoaderIds;
    private String[] allLoadedClassNames;
    private byte[][] cachedClassFileBytes;
    private int[] allLoadedClassesSuper;
    private int[][] allLoadedClassesInterfaces;
    private int[] parentLoaderIds; // An index into this table is a loader id, and the value at this index is this loader's parent loader id.
    private int classCount;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RootClassLoadedCommand(String[] allLoadedClassNames, int[] loaderIds, byte[][] cachedClassFileBytes,
                                int[] loadedClassesSuper, int[][] loadedClassesInterfaces,int classCount,
                                int[] parentLoaderIds) {
        super(ROOT_CLASS_LOADED);
        this.allLoadedClassNames = allLoadedClassNames;
        this.allLoadedClassLoaderIds = loaderIds;
        this.cachedClassFileBytes = cachedClassFileBytes;
        this.allLoadedClassesSuper = loadedClassesSuper;
        this.allLoadedClassesInterfaces = loadedClassesInterfaces;
        this.classCount = classCount;
        this.parentLoaderIds = parentLoaderIds;
    }

    // Custom serialization support
    RootClassLoadedCommand() {
        super(ROOT_CLASS_LOADED);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getAllLoadedClassLoaderIds() {
        return allLoadedClassLoaderIds;
    }

    public String[] getAllLoadedClassNames() {
        return allLoadedClassNames;
    }

    public byte[][] getCachedClassFileBytes() {
        byte[][] res = cachedClassFileBytes;
        cachedClassFileBytes = null; // Save memory

        return res;
    }

    public int[] getAllLoaderSuperClassIds() {
        return allLoadedClassesSuper;
    }

    public int[][] getAllLoadedInterfaceIds() {
        return allLoadedClassesInterfaces;
    }
    
    public int[] getParentLoaderIds() {
        // Return a copy, just in case, since this instance of parentLoaderIds is reused when this command is received
        int[] newParentLoaderIds = new int[parentLoaderIds.length];
        System.arraycopy(parentLoaderIds, 0, newParentLoaderIds, 0, parentLoaderIds.length);

        return newParentLoaderIds;
    }

    // for debugging
    public String toString() {
        return super.toString() + ", classes: " + classCount;  // NOI18N
    }

    void readObject(ObjectInputStream gin) throws IOException {
        GZIPInputStream eix = new GZIPInputStream(gin, 32768);
        ObjectInputStream in = new ObjectInputStream(eix);
        byte[] EMPTY = new byte[0];
        classCount = in.readInt();
        allLoadedClassNames = new String[classCount];

        for (int i = 0; i < classCount; i++) {
            allLoadedClassNames[i] = in.readUTF().replace('.', '/').intern();   // NOI18N
        }

        allLoadedClassLoaderIds = new int[classCount];

        for (int i = 0; i < classCount; i++) {
            allLoadedClassLoaderIds[i] = in.readInt();
        }

        int len = in.readInt();

        if (len == 0) {
            cachedClassFileBytes = null;
        } else {
            cachedClassFileBytes = new byte[len][];

            for (int i = 0; i < len; i++) {
                int bytesLen = in.readInt();

                if (bytesLen == -1) {
                    continue;
                }
                if (bytesLen == 0) {
                   cachedClassFileBytes[i] = EMPTY;
                   continue;
                }
                cachedClassFileBytes[i] = new byte[bytesLen];
                in.readFully(cachedClassFileBytes[i]);
            }
        }

        allLoadedClassesSuper = new int[classCount];
        for (int i = 0; i < classCount; i++) {
            allLoadedClassesSuper[i] = in.readInt();
        }

        allLoadedClassesInterfaces = new int[classCount][];
        for (int i = 0; i < classCount; i++) {
            int ilen = in.readInt();
            allLoadedClassesInterfaces[i] = new int[ilen];
            for (int j = 0; j < ilen; j++) {
                allLoadedClassesInterfaces[i][j] = in.readInt();
            }
        }
        
        len = in.readInt();
        parentLoaderIds = new int[len];

        for (int i = 0; i < len; i++) {
            parentLoaderIds[i] = in.readInt();
        }
        int eof = in.read();
        if (eof != -1) throw new IOException("RootClassLoadedCommand EOF not found, read:"+eof);
        boolean failed = false;
        for (int i = 21; i<127; i+=7) {
            int inb = gin.read();
            if (inb != i) {
                failed = true;
                if (WireIO.DEBUG) System.out.print(i+"="+inb+"; ");
                if (inb%7 == 0) {
                    i = inb;
                }
            }
        }
        if (WireIO.DEBUG && failed) System.out.println("RootClassLoadedCommand fixed.");
    }

    void writeObject(ObjectOutputStream gout) throws IOException {
        GZIPOutputStream eox = new GZIPOutputStream(gout, 32768);
        ObjectOutputStream out = new ObjectOutputStream(eox);
        out.writeInt(classCount);

        for (int i = 0; i < classCount; i++) {
            out.writeUTF(allLoadedClassNames[i]);
        }

        for (int i = 0; i < classCount; i++) {
            int loaderId = allLoadedClassLoaderIds[i];

            // At the client side we treat classes loaded by the bootstrap and by the system classloaders in the same way
            if (loaderId == -1) {
                loaderId = 0;
            }

            out.writeInt(loaderId);
        }

        if (cachedClassFileBytes == null) {
            out.writeInt(0);
        } else {
            out.writeInt(classCount);

            for (int i = 0; i < classCount; i++) {
                if (cachedClassFileBytes[i] == null) {
                    out.writeInt(-1);
                } else {
                    out.writeInt(cachedClassFileBytes[i].length);
                    if (cachedClassFileBytes[i].length > 0) {
                        out.write(cachedClassFileBytes[i]);
                    }
                }
            }
        }

        for (int i = 0; i < classCount; i++) {
            out.writeInt(allLoadedClassesSuper[i]);
        }

        for (int i = 0; i < classCount; i++) {
            int[] ifacesIds = allLoadedClassesInterfaces[i];
            if (ifacesIds != null) {
                out.writeInt(ifacesIds.length);
                for (int j = 0; j < ifacesIds.length; j++) {
                    out.writeInt(ifacesIds[j]);
                }
            } else {
                out.writeInt(0);
            } 
        }
        
        out.writeInt(parentLoaderIds.length);

        for (int i = 0; i < parentLoaderIds.length; i++) {
            out.writeInt(parentLoaderIds[i]);
        }
        out.flush();
        eox.finish();
        gout.flush();
        for (int i = 7; i<127; i+=7) gout.write(i);

        // Free memory
        allLoadedClassNames = null;
        allLoadedClassLoaderIds = null;
        cachedClassFileBytes = null;
        allLoadedClassesSuper = null;
        allLoadedClassesInterfaces = null;
        parentLoaderIds = null;
    }
}
