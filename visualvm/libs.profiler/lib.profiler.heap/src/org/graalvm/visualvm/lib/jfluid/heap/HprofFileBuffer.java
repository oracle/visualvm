/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 *
 * @author Tomas Hurka
 */
class HprofFileBuffer extends HprofByteBuffer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int MAX_bufferSizeBits = 17;
    private static final int MIN_bufferSizeBits = 7;
    private static final int MIN_bufferSize = 1 << MIN_bufferSizeBits;
    private static final int MIN_bufferSizeMask = MIN_bufferSize - 1;
    private static final int BUFFER_EXT = 8;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    RandomAccessFile fis;
    private byte[] dumpBuffer;
    private long bufferStartOffset;
    private int bufferSizeBits;
    private int bufferSize;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    HprofFileBuffer(File dumpFile) throws IOException {
        fis = new RandomAccessFile(dumpFile, "r");
        length = fis.length();
        bufferStartOffset = Long.MAX_VALUE;
        readHeader();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    synchronized char getChar(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i]) & 0xFF;

        return (char) ((ch1 << 8) + (ch2 << 0));
    }

    synchronized double getDouble(long index) {
        int i = loadBufferIfNeeded(index);

        return Double.longBitsToDouble(getLong(i));
    }

    synchronized float getFloat(long index) {
        int i = loadBufferIfNeeded(index);

        return Float.intBitsToFloat(getInt(i));
    }

    synchronized int getInt(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch3 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch4 = ((int) dumpBuffer[i]) & 0xFF;

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    synchronized long getLong(long index) {
        return ((long) (getInt(index)) << 32) + (getInt(index + 4) & 0xFFFFFFFFL);
    }

    synchronized short getShort(long index) {
        int i = loadBufferIfNeeded(index);
        int ch1 = ((int) dumpBuffer[i++]) & 0xFF;
        int ch2 = ((int) dumpBuffer[i]) & 0xFF;

        return (short) ((ch1 << 8) + (ch2 << 0));
    }

    // delegate to MappedByteBuffer    
    synchronized byte get(long index) {
        int i = loadBufferIfNeeded(index);

        return dumpBuffer[i];
    }

    synchronized void get(long position, byte[] chars) {
        int i = loadBufferIfNeeded(position);

        if ((i + chars.length) < dumpBuffer.length) {
            System.arraycopy(dumpBuffer, i, chars, 0, chars.length);
        } else {
            try {
                fis.seek(position);
                fis.readFully(chars);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setBufferSize(long newBufferStart) {
        if ((newBufferStart > bufferStartOffset) && (newBufferStart < (bufferStartOffset + (2 * bufferSize)))) { // sequential read -> increase buffer size

            if (bufferSizeBits < MAX_bufferSizeBits) {
                setBufferSize(bufferSizeBits + 1);
            }
        } else { // reset buffer size
            setBufferSize(MIN_bufferSizeBits);
        }
    }

    private void setBufferSize(int newBufferSizeBits) {
        bufferSizeBits = newBufferSizeBits;
        bufferSize = 1 << bufferSizeBits;
        dumpBuffer = new byte[bufferSize + BUFFER_EXT];
    }

    private int loadBufferIfNeeded(long index) {
        if ((index >= bufferStartOffset) && (index < (bufferStartOffset + bufferSize))) {
            return (int) (index - bufferStartOffset);
        }

        long newBufferStart = index & ~MIN_bufferSizeMask;
        setBufferSize(newBufferStart);

        try {
            fis.seek(newBufferStart);
            fis.readFully(dumpBuffer);

            //System.out.println("Reading at "+newBufferStart+" size "+dumpBuffer.length+" thread "+Thread.currentThread().getName());
        } catch (EOFException ex) {
            // ignore
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        bufferStartOffset = newBufferStart;

        return (int) (index - bufferStartOffset);
    }
}
