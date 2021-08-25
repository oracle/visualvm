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

package org.graalvm.visualvm.lib.jfluid.heap;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.ResourceBundle;


/**
 *
 * @author Tomas Hurka
 */
abstract class HprofByteBuffer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Magic header
    static final String magic1 = "JAVA PROFILE 1.0.1"; // NOI18N
    static final String magic2 = "JAVA PROFILE 1.0.2"; // NOI18N
    static final String magic3 = "JAVA PROFILE 1.0.3"; // NOI18N
    static final int JAVA_PROFILE_1_0_1 = 1;
    static final int JAVA_PROFILE_1_0_2 = 2;
    static final int JAVA_PROFILE_1_0_3 = 3;
    static final int MINIMAL_SIZE = 30;
    static final boolean DEBUG = false;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    int idSize;
    int version;
    long headerSize;
    long length;
    long time;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    static HprofByteBuffer createHprofByteBuffer(File dumpFile)
                                          throws IOException {
        long fileLen = dumpFile.length();

        if (fileLen < MINIMAL_SIZE) {
            String errText = ResourceBundle.getBundle("org/graalvm/visualvm/lib/jfluid/heap/Bundle")
                                           .getString("HprofByteBuffer_ShortFile"); // NOI18N
            throw new IOException(errText);
        }

        try {
            if (fileLen < Integer.MAX_VALUE) {
                return new HprofMappedByteBuffer(dumpFile);
            } else {
                return new HprofLongMappedByteBuffer(dumpFile);
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof OutOfMemoryError) { // can happen on 32bit Windows, since there is only 2G for memory mapped data for whole java process.

                return new HprofFileBuffer(dumpFile);
            }

            throw ex;
        }
    }

    static HprofByteBuffer createHprofByteBuffer(ByteBuffer bb) throws IOException {
        return new HprofMappedByteBuffer(bb);
    }

    abstract char getChar(long index);

    abstract double getDouble(long index);

    abstract float getFloat(long index);

    long getHeaderSize() {
        return headerSize;
    }

    long getID(long offset) {
        if (idSize == 4) {
            return ((long)getInt(offset)) & 0xFFFFFFFFL;
        } else if (idSize == 8) {
            return getLong(offset);
        }
        assert false;

        return -1;
    }

    int getIDSize() {
        return idSize;
    }

    int getFoffsetSize() {
        return length<Integer.MAX_VALUE ? 4 : 8;        
    }
    
    abstract int getInt(long index);

    abstract long getLong(long index);

    abstract short getShort(long index);

    long getTime() {
        return time;
    }

    long capacity() {
        return length;
    }

    abstract byte get(long index);

    abstract void get(long position, byte[] chars);

    void readHeader() throws IOException {
        long[] offset = new long[1];
        String magic = readStringNull(offset, MINIMAL_SIZE);

        if (DEBUG) {
            System.out.println("Magic " + magic); // NOI18N
        }

        if (magic1.equals(magic)) {
            version = JAVA_PROFILE_1_0_1;
        } else if (magic2.equals(magic)) {
            version = JAVA_PROFILE_1_0_2;
        } else if (magic3.equals(magic)) {
            version = JAVA_PROFILE_1_0_3;
        } else {
            if (DEBUG) {
                System.out.println("Invalid version"); // NOI18N
            }

            String errText = ResourceBundle.getBundle("org/graalvm/visualvm/lib/jfluid/heap/Bundle")
                                           .getString("HprofByteBuffer_InvalidFormat");
            throw new IOException(errText);
        }

        idSize = getInt(offset[0]);
        offset[0] += 4;
        time = getLong(offset[0]);
        offset[0] += 8;

        if (DEBUG) {
            System.out.println("ID " + idSize); // NOI18N
        }

        if (DEBUG) {
            System.out.println("Date " + new Date(time).toString()); // NOI18N
        }

        headerSize = offset[0];
    }

    private String readStringNull(long[] offset, int len) {
        StringBuilder s = new StringBuilder(20);
        byte b = get(offset[0]++);

        for (; (b > 0) && (s.length() < len); b = get(offset[0]++)) {
            s.append((char) b);
        }

        return s.toString();
    }
}
