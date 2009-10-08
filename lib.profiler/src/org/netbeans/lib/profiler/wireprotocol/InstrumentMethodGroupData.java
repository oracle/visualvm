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

package org.netbeans.lib.profiler.wireprotocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * Conceptually, the base class for both InstrumentMethodGroupResponse and InstrumentMethodGroupCommand. However, we have to use
 * an instance of this class in each of the above, plus some delegation, instead of normal inheritance, since the above classes
 * have to extend Response and Command, respectively.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class InstrumentMethodGroupData {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int[] instrMethodClassLoaderIds;
    protected String[] instrMethodClasses;
    protected boolean[] instrMethodLeaf;
    protected byte[][] replacementClassFileBytes;
    protected int addInfo;
    protected int nClasses;
    protected int nMethods;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** 1.5-style RedefineClasses() instrumentation constructor */
    public InstrumentMethodGroupData(String[] instrMethodClasses, int[] instrMethodClassLoaderIds,
                                     byte[][] replacementClassFileBytes, boolean[] instrMethodLeaf, int addInfo) {
        nClasses = instrMethodClasses.length;
        nMethods = (instrMethodLeaf != null) ? instrMethodLeaf.length : 0;
        this.instrMethodClasses = instrMethodClasses;
        this.instrMethodClassLoaderIds = instrMethodClassLoaderIds;
        this.replacementClassFileBytes = replacementClassFileBytes;
        this.addInfo = addInfo;
    }

    // Custom serializaion support
    InstrumentMethodGroupData() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getAddInfo() {
        return addInfo;
    }

    public int[] getClassLoaderIds() {
        return instrMethodClassLoaderIds;
    }

    public boolean[] getInstrMethodLeaf() {
        return instrMethodLeaf;
    }

    public String[] getMethodClasses() {
        return instrMethodClasses;
    }

    public int getNClasses() {
        return nClasses;
    }

    public int getNMethods() {
        return nMethods;
    }

    public byte[][] getReplacementClassFileBytes() {
        return replacementClassFileBytes;
    }

    public void dump() {
        if (instrMethodClasses == null) {
            System.err.println("0 classes --"); // NOI18N

            return;
        } else {
            if (instrMethodClasses[0].startsWith("*FAKE")) { // NOI18N
                System.err.println("Fake InstrMethodGroupBase --"); // NOI18N

                return;
            }

            System.err.println(nClasses + " classes, " + nMethods + " methods --"); // NOI18N
        }

        int idx = 0;

        for (int i = 0; i < nClasses; i++) {
            System.err.print("--Class " + instrMethodClasses[i] + "," + instrMethodClassLoaderIds[i]); // NOI18N
            System.err.println();
        }
    }

    // ------------------------ Debugging -------------------------
    public String toString() {
        return ((instrMethodClasses != null) ? (instrMethodClasses.length) : 0) + " classes."; // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        nClasses = in.readInt();

        if (nClasses == 0) {
            return;
        }

        if ((instrMethodClasses == null) || (nClasses > instrMethodClasses.length)) {
            instrMethodClasses = new String[nClasses];
            instrMethodClassLoaderIds = new int[nClasses];
        }

        for (int i = 0; i < nClasses; i++) {
            instrMethodClasses[i] = in.readUTF();
            instrMethodClassLoaderIds[i] = in.readInt();
        }

        nMethods = in.readInt();

        int code = in.read();

        if (code != 0) {
            if ((instrMethodLeaf == null) || (nMethods > instrMethodLeaf.length)) {
                instrMethodLeaf = new boolean[nMethods];
            }

            for (int i = 0; i < nMethods; i++) {
                instrMethodLeaf[i] = in.readBoolean();
            }
        } else {
            instrMethodLeaf = null;
        }

        addInfo = in.readInt();

        if ((replacementClassFileBytes == null) || (nClasses > replacementClassFileBytes.length)) {
            replacementClassFileBytes = new byte[nClasses][];
        }

        for (int i = 0; i < nClasses; i++) {
            int len = in.readInt();

            if (len > 0) {
                replacementClassFileBytes[i] = new byte[len];
                in.readFully(replacementClassFileBytes[i]);
            }
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        if (instrMethodClasses == null) {
            out.writeInt(0);

            return;
        }

        out.writeInt(nClasses);

        for (int i = 0; i < nClasses; i++) {
            out.writeUTF(instrMethodClasses[i]);
            out.writeInt(instrMethodClassLoaderIds[i]);
        }

        out.writeInt(nMethods);

        if (instrMethodLeaf != null) {
            out.write(1);

            for (int i = 0; i < nMethods; i++) {
                out.writeBoolean(instrMethodLeaf[i]);
            }
        } else {
            out.write(0);
        }

        out.writeInt(addInfo);

        for (int i = 0; i < nClasses; i++) {
            if (replacementClassFileBytes[i] == null) {
                out.writeInt(0);
            } else {
                out.writeInt(replacementClassFileBytes[i].length);
                out.write(replacementClassFileBytes[i]);
            }
        }

        instrMethodClasses = null;
        instrMethodClassLoaderIds = null;
        instrMethodLeaf = null;
        replacementClassFileBytes = null;
    }
}
