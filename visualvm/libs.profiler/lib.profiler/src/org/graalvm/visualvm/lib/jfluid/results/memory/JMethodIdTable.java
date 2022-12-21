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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * This class maps jmethodIds to (clazz, methodIdx) pairs
 *
 * @author Misha Dmitriev
 */
public class JMethodIdTable {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class JMethodIdTableEntry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public String className;
        public String methodName;
        public String methodSig;
        public transient boolean isNative;
        int methodId;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        JMethodIdTableEntry(int methodId) {
            this.methodId = methodId;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static String NATIVE_SUFFIX = "[native]";   // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JMethodIdTableEntry[] entries;
    private boolean staticTable = false;
    private int incompleteEntries;
    private int nElements;
    private int size;
    private int threshold;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JMethodIdTable() {
        size = 97;
        threshold = (size * 3) / 4;
        nElements = 0;
        entries = new JMethodIdTableEntry[size];
    }

    public JMethodIdTable(JMethodIdTable otherTable) {
        staticTable = true;
        threshold = otherTable.nElements + 1;
        size = (threshold * 4) / 3 ;
        nElements = 0;
        entries = new JMethodIdTableEntry[size];
        
        for (JMethodIdTableEntry entry : otherTable.entries) {
            if (entry != null) {
                addEntry(entry.methodId, entry.className, entry.methodName, entry.methodSig, entry.isNative);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    synchronized public String debug() {
        if (entries == null) {
            return "Entries = null, size = " + size + ", nElements = " + nElements + ", threshold = " // NOI18N
                   + threshold + ", incompleteEntries = " + incompleteEntries; // NOI18N
        } else {
            return "Entries.length = " + entries.length + ", size = " + size + ", nElements = " + nElements // NOI18N
                   + ", threshold = " + threshold + ", incompleteEntries = " + incompleteEntries; // NOI18N
        }
    }

    synchronized public void readFromStream(DataInputStream in) throws IOException {
        size = in.readInt();
        nElements = in.readInt();
        threshold = in.readInt();

        entries = new JMethodIdTableEntry[size];

        int count = in.readInt();

        for (int i = 0; i < count; i++) {
            int methodId = in.readInt();
            String className = in.readUTF();
            String methodName = in.readUTF();
            String methodSig = in.readUTF();
            boolean isNative = false;
            
            if (methodName.endsWith(NATIVE_SUFFIX)) {
                methodName = methodName.substring(0, methodName.length() - NATIVE_SUFFIX.length());
                isNative = true;
            }
            addEntry(methodId, className, methodName, methodSig, isNative);
        }
    }

    synchronized public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(size);
        out.writeInt(nElements);
        out.writeInt(threshold);

        int count = 0;

        for (JMethodIdTableEntry entrie : entries) {
            if (entrie != null) {
                count++;
            }
        }

        out.writeInt(count);

        for (JMethodIdTableEntry entry : entries) {
            if (entry != null) {
                out.writeInt(entry.methodId);
                out.writeUTF(entry.className);
                out.writeUTF(entry.isNative ? entry.methodName.concat(NATIVE_SUFFIX) : entry.methodName);
                out.writeUTF(entry.methodSig);
            }
        }
    }

    synchronized public JMethodIdTableEntry getEntry(int methodId) {
        int pos = hash(methodId) % size;

        while ((entries[pos] != null) && (entries[pos].methodId != methodId)) {
            pos = (pos + 1) % size;
        }

        return entries[pos];
    }

    synchronized public void getNamesForMethodIds(ProfilerClient profilerClient)
                                    throws ClientUtils.TargetAppOrVMTerminated {
        if (staticTable) {
            throw new IllegalStateException("Attempt to update snapshot JMethodIdTable"); // NOI18N
        }

        if (incompleteEntries == 0) {
            return;
        }

        int[] missingNameMethodIds = new int[incompleteEntries];
        int idx = 0;

        for (JMethodIdTableEntry entrie : entries) {
            if (entrie == null) {
                continue;
            }
            if (entrie.className == null) {
                missingNameMethodIds[idx++] = entrie.methodId;
            }
        }

        String[][] methodClassNameAndSig = profilerClient.getMethodNamesForJMethodIds(missingNameMethodIds);

        for (int i = 0; i < missingNameMethodIds.length; i++) {
            completeEntry(missingNameMethodIds[i], methodClassNameAndSig[0][i], methodClassNameAndSig[1][i],
                          methodClassNameAndSig[2][i], getBoolean(methodClassNameAndSig[3][i]));
        }

        incompleteEntries = 0;
    }

    void addEntry(int methodId, String className, String methodName, String methodSig, boolean isNative) {
        checkMethodId(methodId);
        completeEntry(methodId, className, methodName, methodSig, isNative);
    }

    synchronized public void checkMethodId(int methodId) {
        int pos = hash(methodId) % size;

        while (entries[pos] != null) {
            if (entries[pos].methodId == methodId) {
                return;
            }

            pos = (pos + 1) % size;
        }

        if (nElements < threshold) {
            entries[pos] = new JMethodIdTableEntry(methodId);
            nElements++;
            incompleteEntries++;

            return;
        } else {
            growTable();
            checkMethodId(methodId);
        }
    }

    synchronized private void completeEntry(int methodId, String className, String methodName, String methodSig, boolean isNative) {
        int pos = hash(methodId) % size;

        while (entries[pos].methodId != methodId) {
            pos = (pos + 1) % size;
        }

        entries[pos].className = className;
        entries[pos].methodName = methodName;
        entries[pos].methodSig = methodSig;
        entries[pos].isNative = isNative;
    }

    private void growTable() {
        JMethodIdTableEntry[] oldEntries = entries;
        size = (size * 2) + 1;
        threshold = (size * 3) / 4;
        entries = new JMethodIdTableEntry[size];

        for (JMethodIdTableEntry oldEntry : oldEntries) {
            if (oldEntry != null) {
                int pos = hash(oldEntry.methodId) % size;

                while (entries[pos] != null) {
                    pos = (pos + 1) % size;
                }

                entries[pos] = oldEntry;
            }
        }
    }

    private int hash(int x) {
        return ((x >> 2) * 123457) & 0xFFFFFFF;
    }
    
    private boolean getBoolean(String boolStr) {
        return "1".equals(boolStr);       // NOI18N
    }
}
