/*
 * Copyright (c) 1997, 2025, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * LongBuffer is a special kind of buffer for storing longs. It uses array of longs if there is only few longs
 * stored, otherwise longs are saved to backing temporary file.
 * @author Tomas Hurka
 */
class LongBuffer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DataInputStream readStream;
    private boolean readStreamClosed;
    private DataOutputStream writeStream;
    private File backingFile;
    private long[] buffer;
    private boolean useBackingFile;
    private int bufferSize;
    private int readOffset;
    private int longs;
    private CacheDirectory cacheDirectory;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongBuffer(int size, CacheDirectory cacheDir) {
        buffer = new long[size];
        cacheDirectory = cacheDir;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    void delete() {
        if (backingFile != null) {
            assert writeStream == null;
            assert readStreamClosed || readStream == null;
            backingFile.delete();
            useBackingFile = false;
            backingFile = null;
            longs = 0;
        }
    }

    boolean hasData() {
        return longs > 0;
    }

    long readLong() throws IOException {
        if (!useBackingFile) {
            if (readOffset < bufferSize) {
                return buffer[readOffset++];
            } else {
                return 0;
            }
        }
        if (readStreamClosed) {
            return 0;
        }
        try {
            return readStream.readLong();
        } catch (EOFException ex) {
            readStreamClosed = true;
            readStream.close();
            return 0L;
        }
    }

    void reset() throws IOException {
        bufferSize = 0;
        if (writeStream != null) {
            writeStream.close();
        }
        if (readStream != null) {
            readStream.close();
        }
        writeStream = null;
        readStream = null;
        readStreamClosed = false;
        longs = 0;
        useBackingFile = false;
        readOffset = 0;
    }

    void startReading() throws IOException {
        if (writeStream != null) {
            writeStream.close();
        }

        writeStream = null;
        rewind();
    }

    void rewind() {
        readOffset = 0;

        if (useBackingFile) {
            try {
                if (readStream != null) {
                    readStream.close();
                }
                readStream = new DataInputStream(new BufferedInputStream(new FileInputStream(backingFile), buffer.length * Long.BYTES));
                readStreamClosed = false;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    void writeLong(long data) throws IOException {
        longs++;
        if (bufferSize < buffer.length) {
            buffer[bufferSize++] = data;
            return;
        }

        if (backingFile == null) {
            backingFile = cacheDirectory.createTempFile("NBProfiler", ".gc"); // NOI18N
        }

        if (writeStream == null) {
            writeStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(backingFile), buffer.length * Long.BYTES));

            for (long l : buffer) {
                writeStream.writeLong(l);
            }

            useBackingFile = true;
        }

        writeStream.writeLong(data);
    }
    
    LongBuffer revertBuffer() throws IOException {
        LongBuffer reverted = new LongBuffer(buffer.length, cacheDirectory);
        
        if (bufferSize < buffer.length) {
            for (int i=0;i<bufferSize;i++) {
                reverted.writeLong(buffer[bufferSize - 1 - i]);
            }
        } else {
            if (writeStream != null) writeStream.flush();
            try (RandomAccessFile raf = new RandomAccessFile(backingFile,"r")) {
                long offset = raf.length();
                while(offset > 0) {
                    offset-=8;
                    raf.seek(offset);
                    reverted.writeLong(raf.readLong());
                }
            }
        }
        reverted.startReading();
        return reverted;
    }
    
    int getSize() {
        return longs;
    }
    
    // serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(bufferSize);
        out.writeInt(readOffset);
        out.writeInt(longs);
        out.writeInt(buffer.length);
        out.writeBoolean(useBackingFile);
        if (useBackingFile) {
            if (writeStream != null) writeStream.flush();
            out.writeUTF(backingFile.getAbsolutePath());
        } else {
            for (int i=0; i<bufferSize; i++) {
                out.writeLong(buffer[i]);
            }
        }
    }

    LongBuffer(DataInputStream dis, CacheDirectory cacheDir) throws IOException {
        bufferSize = dis.readInt();
        readOffset = dis.readInt();
        longs = dis.readInt();
        buffer = new long[dis.readInt()];
        useBackingFile = dis.readBoolean();
        if (useBackingFile) {
            backingFile = cacheDir.getCacheFile(dis.readUTF());
        } else {
            for (int i=0; i<bufferSize; i++) {
                buffer[i] = dis.readLong();
            }
        }
        cacheDirectory = cacheDir;
    } 
}
