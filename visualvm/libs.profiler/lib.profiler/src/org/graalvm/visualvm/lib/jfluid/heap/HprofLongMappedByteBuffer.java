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
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 *
 * @author Tomas Hurka
 */
class HprofLongMappedByteBuffer extends HprofByteBuffer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static int BUFFER_SIZE_BITS = 30;
    private static long BUFFER_SIZE = 1L << BUFFER_SIZE_BITS;
    private static int BUFFER_SIZE_MASK = (int) ((BUFFER_SIZE) - 1);
    private static int BUFFER_EXT = 32 * 1024;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private MappedByteBuffer[] dumpBuffer;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofLongMappedByteBuffer(File dumpFile) throws IOException {
        FileInputStream fis = new FileInputStream(dumpFile);
        FileChannel channel = fis.getChannel();
        length = channel.size();
        dumpBuffer = new MappedByteBuffer[(int) (((length + BUFFER_SIZE) - 1) / BUFFER_SIZE)];

        for (int i = 0; i < dumpBuffer.length; i++) {
            long position = i * BUFFER_SIZE;
            long size = Math.min(BUFFER_SIZE + BUFFER_EXT, length - position);
            dumpBuffer[i] = channel.map(FileChannel.MapMode.READ_ONLY, position, size);
        }

        channel.close();
        readHeader();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    char getChar(long index) {
        return dumpBuffer[getBufferIndex(index)].getChar(getBufferOffset(index));
    }

    double getDouble(long index) {
        return dumpBuffer[getBufferIndex(index)].getDouble(getBufferOffset(index));
    }

    float getFloat(long index) {
        return dumpBuffer[getBufferIndex(index)].getFloat(getBufferOffset(index));
    }

    int getInt(long index) {
        return dumpBuffer[getBufferIndex(index)].getInt(getBufferOffset(index));
    }

    long getLong(long index) {
        return dumpBuffer[getBufferIndex(index)].getLong(getBufferOffset(index));
    }

    short getShort(long index) {
        return dumpBuffer[getBufferIndex(index)].getShort(getBufferOffset(index));
    }

    // delegate to MappedByteBuffer        
    byte get(long index) {
        return dumpBuffer[getBufferIndex(index)].get(getBufferOffset(index));
    }

    synchronized void get(long position, byte[] chars) {
        MappedByteBuffer buffer = dumpBuffer[getBufferIndex(position)];
        buffer.position(getBufferOffset(position));
        buffer.get(chars);
    }

    private int getBufferIndex(long index) {
        return (int) (index >> BUFFER_SIZE_BITS);
    }

    private int getBufferOffset(long index) {
        return (int) (index & BUFFER_SIZE_MASK);
    }
}
