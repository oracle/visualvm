/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This is issued by back end in response to GetMethodNamesForJMethodIdsCommand. It contains strings with methods'
 * classes, names and signatures, packed into a single byte[] array. At the client side this data is subsequently
 * unpacked (not in this class to avoid having unused code at the back end side).
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class MethodNamesResponse extends Response {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] packedArrayOffsets;
    private byte[] packedData;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MethodNamesResponse(byte[] packedData, int[] packedArrayOffsets) {
        super(true, METHOD_NAMES);
        this.packedData = packedData;
        this.packedArrayOffsets = packedArrayOffsets;
    }

    // Custom serialization support
    MethodNamesResponse() {
        super(true, METHOD_NAMES);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getPackedArrayOffsets() {
        return packedArrayOffsets;
    }

    public byte[] getPackedData() {
        return packedData;
    }

    void readObject(ObjectInputStream in) throws IOException {
        int len = in.readInt();
        packedData = new byte[len];
        in.readFully(packedData);
        len = in.readInt();
        packedArrayOffsets = new int[len];

        for (int i = 0; i < len; i++) {
            packedArrayOffsets[i] = in.readInt();
        }
    }

    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(packedData.length);
        out.write(packedData);
        out.writeInt(packedArrayOffsets.length);

        for (int i = 0; i < packedArrayOffsets.length; i++) {
            out.writeInt(packedArrayOffsets[i]);
        }

        packedData = null;
        packedArrayOffsets = null;
    }
}
