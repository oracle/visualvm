/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.results.memory;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
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

    static class JMethodIdTableEntry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        String className;
        String methodName;
        String methodSig;
        int methodId;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        JMethodIdTableEntry(int methodId) {
            this.methodId = methodId;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static JMethodIdTable defaultTable;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JMethodIdTableEntry[] entries;
    private boolean staticTable = false;
    private int incompleteEntries;
    private int nElements;
    private int size;
    private int threshold;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    JMethodIdTable() {
        size = 97;
        threshold = (size * 3) / 4;
        nElements = 0;
        entries = new JMethodIdTableEntry[size];
    }

    JMethodIdTable(int[] methodIds, String[][] methodNamesAndSigs) {
        staticTable = true;
        size = methodIds.length * 2; // TODO: check this
        threshold = (size * 3) / 4;
        nElements = 0;
        entries = new JMethodIdTableEntry[size];

        for (int i = 0; i < methodIds.length; i++) {
            addEntry(methodIds[i], methodNamesAndSigs[0][i], methodNamesAndSigs[1][i], methodNamesAndSigs[2][i]);
        }
    }
    
    JMethodIdTable(JMethodIdTable otherTable) {
        staticTable = true;
        threshold = otherTable.nElements + 1;
        size = (threshold * 4) / 3 ;
        nElements = 0;
        entries = new JMethodIdTableEntry[size];
        
        for (int i = 0; i < otherTable.entries.length; i++) {
            JMethodIdTableEntry entry = otherTable.entries[i];
            
            if (entry != null) {
                addEntry(entry.methodId, entry.className, entry.methodName, entry.methodSig);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static JMethodIdTable getDefault() {
        if (defaultTable == null) {
            defaultTable = new JMethodIdTable();
        }

        return defaultTable;
    }

    public static void reset() {
        defaultTable = null;
    }

    public String debug() {
        if (entries == null) {
            return "Entries = null, size = " + size + ", nElements = " + nElements + ", threshold = " // NOI18N
                   + threshold + ", incompleteEntries = " + incompleteEntries; // NOI18N
        } else {
            return "Entries.length = " + entries.length + ", size = " + size + ", nElements = " + nElements // NOI18N
                   + ", threshold = " + threshold + ", incompleteEntries = " + incompleteEntries; // NOI18N
        }
    }

    public void readFromStream(DataInputStream in) throws IOException {
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

            addEntry(methodId, className, methodName, methodSig);
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(size);
        out.writeInt(nElements);
        out.writeInt(threshold);

        int count = 0;

        for (int i = 0; i < entries.length; i++) {
            if (entries[i] != null) {
                count++;
            }
        }

        out.writeInt(count);

        for (int i = 0; i < entries.length; i++) {
            if (entries[i] != null) {
                out.writeInt(entries[i].methodId);
                out.writeUTF(entries[i].className);
                out.writeUTF(entries[i].methodName);
                out.writeUTF(entries[i].methodSig);
            }
        }
    }

    JMethodIdTableEntry getEntry(int methodId) {
        int pos = hash(methodId) % size;

        while ((entries[pos] != null) && (entries[pos].methodId != methodId)) {
            pos = (pos + 1) % size;
        }

        return entries[pos];
    }

    synchronized void getNamesForMethodIds(ProfilerClient profilerClient)
                                    throws ClientUtils.TargetAppOrVMTerminated {
        if (staticTable) {
            throw new IllegalStateException("Attempt to update snapshot JMethodIdTable"); // NOI18N
        }

        if (incompleteEntries == 0) {
            return;
        }

        int[] missingNameMethodIds = new int[incompleteEntries];
        int idx = 0;

        for (int i = 0; i < entries.length; i++) {
            if (entries[i] == null) {
                continue;
            }

            if (entries[i].className == null) {
                missingNameMethodIds[idx++] = entries[i].methodId;
            }
        }

        String[][] methodClassNameAndSig = profilerClient.getMethodNamesForJMethodIds(missingNameMethodIds);

        for (int i = 0; i < missingNameMethodIds.length; i++) {
            completeEntry(missingNameMethodIds[i], methodClassNameAndSig[0][i], methodClassNameAndSig[1][i],
                          methodClassNameAndSig[2][i]);
        }

        incompleteEntries = 0;
    }

    void addEntry(int methodId, String className, String methodName, String methodSig) {
        checkMethodId(methodId);
        completeEntry(methodId, className, methodName, methodSig);
    }

    synchronized void checkMethodId(int methodId) {
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

    private void completeEntry(int methodId, String className, String methodName, String methodSig) {
        int pos = hash(methodId) % size;

        while (entries[pos].methodId != methodId) {
            pos = (pos + 1) % size;
        }

        entries[pos].className = className;
        entries[pos].methodName = methodName;
        entries[pos].methodSig = methodSig;
    }

    private void growTable() {
        JMethodIdTableEntry[] oldEntries = entries;
        size = (size * 2) + 1;
        threshold = (size * 3) / 4;
        entries = new JMethodIdTableEntry[size];

        for (int i = 0; i < oldEntries.length; i++) {
            if (oldEntries[i] != null) {
                int pos = hash(oldEntries[i].methodId) % size;

                while (entries[pos] != null) {
                    pos = (pos + 1) % size;
                }

                entries[pos] = oldEntries[i];
            }
        }
    }

    private int hash(int x) {
        return ((x >> 2) * 123457) & 0xFFFFFFF;
    }
}
