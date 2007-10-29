/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.heap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 * key - ID (long) of heap object
 * value 8+4+8 = 20 bytes
 *  - offset (long) to dump file
 *  - instance index (int) - unique number of this {@link Instance} among all instances of the same Java Class
 *  - ID (long) to nearest GC root, 0 for GC root or if is not computed yet
 *
 * @author Tomas Hurka
 */
class LongMap {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    interface Data {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        int getInt(long index);

        long getLong(long index);

        void putInt(long index, int data);

        void putLong(long index, long data);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Entry {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long offset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Entry(long off) {
            offset = off;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setIndex(int index) {
            dumpBuffer.putInt(offset + KEY_SIZE + 8, index);
        }

        int getIndex() {
            return dumpBuffer.getInt(offset + KEY_SIZE + 8);
        }

        void setNearestGCRootPointer(long instanceId) {
            dumpBuffer.putLong(offset + KEY_SIZE + 8 + 4, instanceId);
        }

        long getNearestGCRootPointer() {
            return dumpBuffer.getLong(offset + KEY_SIZE + 8 + 4);
        }

        long getOffset() {
            return dumpBuffer.getLong(offset + KEY_SIZE);
        }
    }

    class FileData implements Data {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        RandomAccessFile file;
        byte[] buf;
        boolean bufferModified;
        long offset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        FileData(RandomAccessFile f, long length) throws IOException {
            file = f;
            buf = new byte[ENTRY_SIZE];
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized int getInt(long index) {
            int i = loadBufferIfNeeded(index);
            int ch1 = ((int) buf[i++]) & 0xFF;
            int ch2 = ((int) buf[i++]) & 0xFF;
            int ch3 = ((int) buf[i++]) & 0xFF;
            int ch4 = ((int) buf[i]) & 0xFF;

            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        }

        public synchronized long getLong(long index) {
           int i = loadBufferIfNeeded(index);
           return (((long)buf[i++] << 56) +
                  ((long)(buf[i++] & 255) << 48) +
                  ((long)(buf[i++] & 255) << 40) +
                  ((long)(buf[i++] & 255) << 32) +
                  ((long)(buf[i++] & 255) << 24) +
                  ((buf[i++] & 255) << 16) +
                  ((buf[i++] & 255) <<  8) +
                  ((buf[i++] & 255) <<  0));
        }

        public synchronized void putInt(long index, int data) {
            int i = loadBufferIfNeeded(index);
            buf[i++] = (byte) (data >>> 24);
            buf[i++] = (byte) (data >>> 16);
            buf[i++] = (byte) (data >>> 8);
            buf[i++] = (byte) (data >>> 0);
            bufferModified = true;
        }

        public synchronized void putLong(long index, long data) {
            int i = loadBufferIfNeeded(index);
            buf[i++] = (byte) (data >>> 56);
            buf[i++] = (byte) (data >>> 48);
            buf[i++] = (byte) (data >>> 40);
            buf[i++] = (byte) (data >>> 32);
            buf[i++] = (byte) (data >>> 24);
            buf[i++] = (byte) (data >>> 16);
            buf[i++] = (byte) (data >>> 8);
            buf[i++] = (byte) (data >>> 0);
            bufferModified = true;
        }

        private int loadBufferIfNeeded(long index) {
            int i = (int) (index % ENTRY_SIZE);
            long newOffset = index - i;

            if (offset != newOffset) {
                try {
                    if (bufferModified) {
                        file.seek(offset);
                        file.write(buf);
                        bufferModified = false;
                    }

                    file.seek(newOffset);
                    file.readFully(buf);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                offset = newOffset;
            }

            return i;
        }
    }

    class MemoryMappedData implements Data {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        MappedByteBuffer buf;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        MemoryMappedData(RandomAccessFile file, long length)
                  throws IOException {
            FileChannel channel = file.getChannel();
            buf = channel.map(FileChannel.MapMode.READ_WRITE, 0, length);
            channel.close();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getInt(long index) {
            return buf.getInt((int) index);
        }

        public long getLong(long index) {
            return buf.getLong((int) index);
        }

        public void putInt(long index, int data) {
            buf.putInt((int) index, data);
        }

        public void putLong(long index, long data) {
            buf.putLong((int) index, data);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int KEY_SIZE = 8;
    private static final int VALUE_SIZE = 8 + 4 + 8;
    private static final int ENTRY_SIZE = KEY_SIZE + VALUE_SIZE;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    File tempFile;
    private Data dumpBuffer;
    private long fileSize;
    private long keys;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongMap(int size) throws FileNotFoundException, IOException {
        keys = (size * 4L) / 3L;
        fileSize = keys * ENTRY_SIZE;
        tempFile = File.createTempFile("NBProfiler", ".map"); // NOI18N

        RandomAccessFile file = new RandomAccessFile(tempFile, "rw"); // NOI18N
        file.setLength(fileSize);
        setDumpBuffer(file);
        tempFile.deleteOnExit();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void finalize() throws Throwable {
        tempFile.delete();
        super.finalize();
    }

    Entry get(long key) {
        long index = getIndex(key);

        while (true) {
            long mapKey = dumpBuffer.getLong(index);

            if (mapKey == key) {
                return new Entry(index);
            }

            if (mapKey == 0L) {
                return null;
            }

            index = getNextIndex(index);
        }
    }

    void put(long key, long value) {
        long index = getIndex(key);

        while (true) {
            if (dumpBuffer.getLong(index) == 0L) {
                dumpBuffer.putLong(index, key);
                dumpBuffer.putLong(index + 8, value);

                return;
            }

            index = getNextIndex(index);
        }
    }

    private void setDumpBuffer(RandomAccessFile file) throws IOException {
        long length = file.length();

        try {
            if (length > Integer.MAX_VALUE) {
                dumpBuffer = new FileData(file, length);
            } else {
                dumpBuffer = new MemoryMappedData(file, length);
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof OutOfMemoryError) {
                dumpBuffer = new FileData(file, length);
            }

            throw ex;
        }
    }

    private long getIndex(long key) {
        long hash = ((key << 1) - (key << 8)) & 0x7FFFFFFFFFFFFFFFL;

        return (hash % keys) * ENTRY_SIZE;
    }

    private long getNextIndex(long index) {
        index += ENTRY_SIZE;

        if (index >= fileSize) {
            index = 0;
        }

        return index;
    }
}
