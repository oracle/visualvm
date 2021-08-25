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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 *
 * @author Tomas Hurka
 */
class HprofMappedByteBuffer extends HprofByteBuffer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final ByteBuffer dumpBuffer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofMappedByteBuffer(File dumpFile) throws IOException {
        this(mmap(dumpFile));
    }

    HprofMappedByteBuffer(ByteBuffer buffer) throws IOException {
        this.dumpBuffer = buffer;
        this.length = buffer.capacity();
        readHeader();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    char getChar(long index) {
        return dumpBuffer.getChar((int) index);
    }

    double getDouble(long index) {
        return dumpBuffer.getDouble((int) index);
    }

    float getFloat(long index) {
        return dumpBuffer.getFloat((int) index);
    }

    int getInt(long index) {
        return dumpBuffer.getInt((int) index);
    }

    long getLong(long index) {
        return dumpBuffer.getLong((int) index);
    }

    short getShort(long index) {
        return dumpBuffer.getShort((int) index);
    }

    // delegate to MappedByteBuffer
    byte get(long index) {
        return dumpBuffer.get((int) index);
    }

    synchronized void get(long position, byte[] chars) {
        dumpBuffer.position((int) position);
        dumpBuffer.get(chars);
    }

    private static MappedByteBuffer mmap(File dumpFile) throws IOException, FileNotFoundException {
        FileInputStream fis = new FileInputStream(dumpFile);
        FileChannel channel = fis.getChannel();
        MappedByteBuffer d = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        channel.close();
        return d;
    }
}
