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
        return super.toString() + " " + ((b != null) ? b.toString() : "null"); // NOI18N
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
