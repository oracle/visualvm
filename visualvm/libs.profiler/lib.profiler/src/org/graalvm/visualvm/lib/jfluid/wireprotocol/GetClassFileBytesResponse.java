/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @author Tomas Hurka
 */
public class GetClassFileBytesResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private byte[][] classBytes;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public GetClassFileBytesResponse(byte[][] bytes) {
        this();
        classBytes = bytes;
    }

    // Custom serialization support
    GetClassFileBytesResponse() {
        super(true, GET_CLASS_FILE_BYTES_RESPONSE);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public byte[][] getClassBytes() {
        return classBytes;
    }

    // For debugging
    public String toString() {
        return "GetClassFileBytesResponse, classes: " + classBytes.length + ", " + super.toString(); // NOI18N
    }

    void readObject(ObjectInputStream in) throws IOException {
        int nClasses = in.readInt();

        if (nClasses == 0) {
            return;
        }

        classBytes = new byte[nClasses][];

        for (int i = 0; i < nClasses; i++) {
            int len = in.readInt();

            if (len > 0) {
                classBytes[i] = new byte[len];
                in.readFully(classBytes[i]);
            }
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(classBytes.length);

        for (int i = 0; i < classBytes.length; i++) {
            if (classBytes[i] == null) {
                out.writeInt(0);
            } else {
                out.writeInt(classBytes[i].length);
                out.write(classBytes[i]);
            }
        }
    }

}
