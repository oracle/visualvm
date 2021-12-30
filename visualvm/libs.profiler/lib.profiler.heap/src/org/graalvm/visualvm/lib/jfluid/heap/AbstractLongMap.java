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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author Tomas Hurka
 */
abstract class AbstractLongMap {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    abstract class Entry {
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final int VALUE_SIZE;
    final int ENTRY_SIZE;
    long fileSize;
    private long keys;
    final int KEY_SIZE;
    final int ID_SIZE;
    final int FOFFSET_SIZE;
    Data dumpBuffer;
    CacheDirectory cacheDirectory;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    AbstractLongMap(int size,int idSize,int foffsetSize,int valueSize,CacheDirectory cacheDir) throws FileNotFoundException, IOException {
        assert idSize == 4 || idSize == 8;
        assert foffsetSize == 4 || foffsetSize == 8;
        keys = (size * 4L) / 3L;
        ID_SIZE = idSize;
        FOFFSET_SIZE = foffsetSize;
        KEY_SIZE = ID_SIZE;
        VALUE_SIZE = valueSize;
        ENTRY_SIZE = KEY_SIZE + VALUE_SIZE;
        fileSize = keys * ENTRY_SIZE;
        cacheDirectory = cacheDir;
        dumpBuffer = cacheDir.createDumpBuffer(fileSize, ENTRY_SIZE);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void finalize() throws Throwable {
        if (cacheDirectory.isTemporary()) {
            dumpBuffer.deleteFile();
        }
        super.finalize();
    }

    Entry get(long key) {
        long index = getIndex(key);

        while (true) {
            long mapKey = getID(index);

            if (mapKey == key) {
                return createEntry(index);
            }

            if (mapKey == 0L) {
                return null;
            }

            index = getNextIndex(index);
        }
    }

    Entry put(long key, long value) {
        long index = getIndex(key);

        while (true) {
            long mapKey = getID(index);
            if (mapKey == 0L) {
                putID(index, key);
                return createEntry(index,value);
            } else if (mapKey == key) {
                return createEntry(index);
            }

            index = getNextIndex(index);
        }
    }

    static Data getDumpBuffer(File f, RandomAccessFile file, int entrySize) throws IOException {
        long length = file.length();

        try {
            if (length > Integer.MAX_VALUE) {
                return new LongMemoryMappedData(f, file, length, entrySize);
            } else {
                return new MemoryMappedData(f, file, length);
            }
        } catch (IOException ex) {
            if (ex.getCause() instanceof OutOfMemoryError) {
                return new FileData(f, file, length, entrySize);
            } else {
                throw ex;
            }
        }
    }

    long getID(long index) {
        if (ID_SIZE == 4) {
            return ((long)dumpBuffer.getInt(index)) & 0xFFFFFFFFL;
        }
        return dumpBuffer.getLong(index);
    }
    
    void putID(long index,long key) {
        if (ID_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
    }
    
    long getFoffset(long index) {
        if (FOFFSET_SIZE == 4) {
            return dumpBuffer.getInt(index);
        }
        return dumpBuffer.getLong(index);
    }
    
    void putFoffset(long index,long key) {
        if (FOFFSET_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeLong(keys);
        out.writeInt(ID_SIZE);
        out.writeInt(FOFFSET_SIZE);
        out.writeInt(VALUE_SIZE);
        dumpBuffer.writeToStream(out);
    }

    AbstractLongMap(DataInputStream dis, CacheDirectory cacheDir) throws IOException {
        keys = dis.readLong();
        ID_SIZE = dis.readInt();
        FOFFSET_SIZE = dis.readInt();
        VALUE_SIZE = dis.readInt();
        
        KEY_SIZE = ID_SIZE;
        ENTRY_SIZE = KEY_SIZE + VALUE_SIZE;
        fileSize = keys * ENTRY_SIZE;
        dumpBuffer = Data.readFromStream(dis, cacheDir, ENTRY_SIZE);
        cacheDirectory = cacheDir;
    }
    
    private long getIndex(long key) {
        long hash = key & 0x7FFFFFFFFFFFFFFFL;
        return (hash % keys) * ENTRY_SIZE;
    }

    private long getNextIndex(long index) {
        index += ENTRY_SIZE;
        if (index >= fileSize) {
            index = 0;
        }
        return index;
    }
    
    private static boolean isLinux() {
        String osName = System.getProperty("os.name");  // NOI18N
        
        return osName.endsWith("Linux"); // NOI18N
    }

    abstract Entry createEntry(long index);
    
    abstract Entry createEntry(long index,long value);
    
    interface Data {
        //~ Methods --------------------------------------------------------------------------------------------------------------
        static Data readFromStream(DataInputStream dis, CacheDirectory cacheDir, int entrySize) throws IOException {
            File tempFile = cacheDir.getCacheFile(dis.readUTF());
            RandomAccessFile file = new RandomAccessFile(tempFile, "rw"); // NOI18N
            Data dumpBuffer = getDumpBuffer(tempFile, file, entrySize);
            file.close();
            return dumpBuffer;
        }
        
        byte getByte(long index);
        
        int getInt(long index);

        long getLong(long index);

        void putByte(long index, byte data);

        void putInt(long index, int data);

        void putLong(long index, long data);

        void force() throws IOException;

        void writeToStream(DataOutputStream out) throws IOException;

        void deleteFile();
    }

    private static abstract class AbstractData implements Data {

        File bufferFile;

        private AbstractData(File file) {
            bufferFile = file;
        }

        //---- Serialization support
        public void writeToStream(DataOutputStream out) throws IOException {
            out.writeUTF(bufferFile.getAbsolutePath());
            force();
        }

        public void deleteFile() {
            bufferFile.delete();
        }

    }

    private static class FileData extends AbstractData {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        RandomAccessFile file;
        byte[] buf;
        boolean bufferModified;
        long offset;
        int entrySize;
        long fileSize;
        final static int BUFFER_SIZE = 128;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        FileData(File fl, RandomAccessFile f, long length, int entry) throws IOException {
            super(fl);
            file = f;
            fileSize = length;
            entrySize = entry;
            buf = new byte[entrySize*BUFFER_SIZE];
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized byte getByte(long index) {
            int i = loadBufferIfNeeded(index);
            return buf[i];
        }

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

        public synchronized void putByte(long index, byte data) {
            int i = loadBufferIfNeeded(index);
            buf[i] = data;
            bufferModified = true;
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
            int i = (int) (index % (entrySize * BUFFER_SIZE));
            long newOffset = index - i;

            if (offset != newOffset) {
                try {
                    flush();
                    file.seek(newOffset);
                    file.readFully(buf,0,getBufferSize(newOffset));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                offset = newOffset;
            }

            return i;
        }

        private int getBufferSize(long off) {
            int size = buf.length;
            
            if (fileSize-off<buf.length) {
                size = (int)(fileSize-off);
            }
            return size;
        }

        private void flush() throws IOException {
            if (bufferModified) {
                file.seek(offset);
                file.write(buf,0,getBufferSize(offset));
                bufferModified = false;
            }
        }

        @Override
        public void force() throws IOException {
            flush();
        }
    }
    
    private static class MemoryMappedData extends AbstractData {
        
        private static final FileChannel.MapMode MAP_MODE = isLinux() ? FileChannel.MapMode.PRIVATE : FileChannel.MapMode.READ_WRITE;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        MappedByteBuffer buf;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        MemoryMappedData(File f, RandomAccessFile file, long length)
                  throws IOException {
            super(f);
            buf = createBuffer(file, length);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public byte getByte(long index) {
            return buf.get((int) index);
        }

        public int getInt(long index) {
            return buf.getInt((int) index);
        }

        public long getLong(long index) {
            return buf.getLong((int) index);
        }

        public void putByte(long index, byte data) {
            buf.put((int) index, data);
        }

        public void putInt(long index, int data) {
            buf.putInt((int) index, data);
        }

        public void putLong(long index, long data) {
            buf.putLong((int) index, data);
        }

        @Override
        public void force() throws IOException {
            if (MAP_MODE == FileChannel.MapMode.PRIVATE) {
                File newBufferFile = new File(bufferFile.getAbsolutePath()+".new"); // NOI18N
                int length = buf.capacity();
                new FileOutputStream(newBufferFile).getChannel().write(buf);
                buf = null;
                bufferFile.delete();
                newBufferFile.renameTo(bufferFile);
                buf = createBuffer(new RandomAccessFile(bufferFile, "rw"), length); // NOI18N
            } else {
                buf.force();
            }
        }

        private static MappedByteBuffer createBuffer(RandomAccessFile file, long length) throws IOException {
            FileChannel channel = file.getChannel();
            MappedByteBuffer buf = channel.map(MAP_MODE, 0, length);
            channel.close();
            return buf;
        }
    }

    private static class LongMemoryMappedData extends AbstractData {

        private static int BUFFER_SIZE_BITS = 30;
        private static long BUFFER_SIZE = 1L << BUFFER_SIZE_BITS;
        private static int BUFFER_SIZE_MASK = (int) ((BUFFER_SIZE) - 1);
        private static int BUFFER_EXT = 32 * 1024;

        //~ Instance fields ----------------------------------------------------------------------------------------------------------

        private MappedByteBuffer[] dumpBuffer;
        private final int entrySize;


        //~ Constructors ---------------------------------------------------------------------------------------------------------

        LongMemoryMappedData(File f, RandomAccessFile file, long length, int entry)
                  throws IOException {
            super(f);
            dumpBuffer = createBuffers(file, length);
            entrySize = entry;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public byte getByte(long index) {
            return dumpBuffer[getBufferIndex(index)].get(getBufferOffset(index));
        }

        public int getInt(long index) {
            return dumpBuffer[getBufferIndex(index)].getInt(getBufferOffset(index));
        }

        public long getLong(long index) {
            return dumpBuffer[getBufferIndex(index)].getLong(getBufferOffset(index));
        }

        public void putByte(long index, byte data) {
            dumpBuffer[getBufferIndex(index)].put(getBufferOffset(index),data);
        }

        public void putInt(long index, int data) {
            dumpBuffer[getBufferIndex(index)].putInt(getBufferOffset(index),data);
        }

        public void putLong(long index, long data) {
            dumpBuffer[getBufferIndex(index)].putLong(getBufferOffset(index),data);
        }

        private int getBufferIndex(long index) {
            return (int) (index >> BUFFER_SIZE_BITS);
        }

        private int getBufferOffset(long index) {
            return (int) (index & BUFFER_SIZE_MASK);
        }

        @Override
        public void force() throws IOException{
            if (MemoryMappedData.MAP_MODE == FileChannel.MapMode.PRIVATE) {
                File newBufferFile = new File(bufferFile.getAbsolutePath()+".new"); // NOI18N
                long length = bufferFile.length();
                FileChannel channel = new FileOutputStream(newBufferFile).getChannel();
                int offset_start = 0;

                for (int i = 0; i < dumpBuffer.length; i++) {
                    MappedByteBuffer buf = dumpBuffer[i];
                    long offset_end = (((i+1)*BUFFER_SIZE)/entrySize)*entrySize + entrySize;

                    if (offset_end > length) {
                        offset_end = length;
                    }
                    buf.limit((int)(offset_end - i*BUFFER_SIZE));
                    buf.position(offset_start);
                    channel.write(buf);
                    offset_start = (int)(offset_end - (i+1)*BUFFER_SIZE);
                }
                channel.close();
                dumpBuffer = null;
                bufferFile.delete();
                newBufferFile.renameTo(bufferFile);
                dumpBuffer = createBuffers(new RandomAccessFile(bufferFile, "rw"), length); // NOI18N
            } else {
                for (MappedByteBuffer buf : dumpBuffer) {
                    buf.force();
                }
            }
        }

        private static MappedByteBuffer[] createBuffers(RandomAccessFile file, long length) throws IOException {
            FileChannel channel = file.getChannel();
            MappedByteBuffer[] dumpBuffer = new MappedByteBuffer[(int) (((length + BUFFER_SIZE) - 1) / BUFFER_SIZE)];

            for (int i = 0; i < dumpBuffer.length; i++) {
                long position = i * BUFFER_SIZE;
                long size = Math.min(BUFFER_SIZE + BUFFER_EXT, length - position);
                dumpBuffer[i] = channel.map(MemoryMappedData.MAP_MODE, position, size);
            }
            channel.close();
            file.close();
            return dumpBuffer;
        }
    }
}
