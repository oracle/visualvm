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
 * Request from client to back end to instrument a group of TA methods.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class InstrumentMethodGroupCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private InstrumentMethodGroupData b;
    private int instrType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** 1.5-style RedefineClasses() instrumentation constructor */
    public InstrumentMethodGroupCommand(int instrType, String[] instrMethodClasses, int[] instrMethodClassLoaderIds,
                                        byte[][] replacementClassFileBytes, boolean[] instrMethodLeaf, int addInfo) {
        super(INSTRUMENT_METHOD_GROUP);
        this.instrType = instrType;
        b = new InstrumentMethodGroupData(instrMethodClasses, instrMethodClassLoaderIds, replacementClassFileBytes,
                                          instrMethodLeaf, addInfo);
    }

    /** This is used just to send "empty" commands, meaning no methods are instrumented */
    public InstrumentMethodGroupCommand(Object dummy) {
        super(INSTRUMENT_METHOD_GROUP);
        instrType = -1;
    }

    // Custom serializaion support
    InstrumentMethodGroupCommand() {
        super(INSTRUMENT_METHOD_GROUP);
        b = new InstrumentMethodGroupData();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public InstrumentMethodGroupData getBase() {
        return b;
    }

    public int[] getClassLoaderIds() {
        return b.instrMethodClassLoaderIds;
    }

    public boolean isEmpty() {
        return instrType == -1;
    }

    public boolean[] getInstrMethodLeaf() {
        return b.instrMethodLeaf;
    }

    public int getInstrType() {
        return instrType;
    }

    public String[] getMethodClasses() {
        return b.instrMethodClasses;
    }

    public byte[][] getReplacementClassFileBytes() {
        return b.replacementClassFileBytes;
    }

    public void dump() {
        System.err.print("-- InstrumentMethodGroupCommand: "); // NOI18N

        if (b != null) {
            b.dump();
        }
    }

    // ------------------------ Debugging -------------------------
    public String toString() {
        return "InstrumentMethodGroupCommand " + ((b != null) ? b.toString() : "null"); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        instrType = in.readInt();

        if (!isEmpty()) {
            b.readObject(in);
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(instrType);

        if (!isEmpty()) {
            b.writeObject(out);
        }
    }
}
