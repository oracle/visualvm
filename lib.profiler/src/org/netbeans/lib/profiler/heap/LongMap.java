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
import java.util.Collections;
import java.util.List;


/**
 * key - ID (long/int) of heap object
 * value (8/4) + 4 + 1 + (8/4)
 *  - offset (long/int) to dump file
 *  - instance index (int) - unique number of this {@link Instance} among all instances of the same Java Class
 *  - references flags (byte) - bit 0 set - has zero or one reference, bit 1 set - has GC root
 *  - ID/offset (long/int) - ID if reference flag bit 0 is set, otherwise offset to reference list file
 *
 * @author Tomas Hurka
 */
class LongMap {

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    class Entry {
        
        private static final byte NUMBER_LIST = 1;
        private static final byte GC_ROOT = 2;
        
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long offset;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Entry(long off) {
            offset = off;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setIndex(int index) {
            dumpBuffer.putInt(offset + KEY_SIZE + FOFFSET_SIZE, index);
        }

        int getIndex() {
            return dumpBuffer.getInt(offset + KEY_SIZE + FOFFSET_SIZE);
        }

        void setNearestGCRootPointer(long instanceId) {
            byte flags = (byte)(getFlags() | GC_ROOT);
            setFlags(flags);
            if ((flags & NUMBER_LIST) != 0) {   // put GC root on the first place in references list
                try {
                    referenceList.putFirst(getReferencesPointer(),instanceId);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        long getNearestGCRootPointer() {
            try {
                byte flag = getFlags();
                if ((flag & GC_ROOT) != 0) { // has GC root
                    long ref = getReferencesPointer();
                    if ((flag & NUMBER_LIST) != 0) { // get GC root from number list
                        return referenceList.getFirstNumber(ref);
                    }
                    return ref;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return 0L;
        }
        
        void addReference(long instanceId) {
            try {
                byte flags = getFlags();
                long ref = getReferencesPointer();
                if ((flags & NUMBER_LIST) == 0) { // reference list is not used
                    if (ref == 0L) {    // no reference was set
                        setReferencesPointer(instanceId);
                    } else if (ref != instanceId) {    // one reference was set, switch to reference list
                       setFlags((byte)(flags | NUMBER_LIST));
                       long list = referenceList.addFirstNumber(ref,instanceId);
                       setReferencesPointer(list);
                    }
                } else { // use reference list
                    long newRef = referenceList.addNumber(ref,instanceId);
                    if (newRef != ref) {
                        setReferencesPointer(newRef);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
        List getReferences() {
            byte flags = getFlags();
            long ref = getReferencesPointer();
            if ((flags & NUMBER_LIST) == 0) {
                if (ref == 0L) {
                    return Collections.EMPTY_LIST;
                } else {
                    return Collections.singletonList(new Long(ref));
                }
            } else {
                try {
                    return referenceList.getNumbers(ref);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return Collections.EMPTY_LIST;
        }
        
        long getOffset() {
            return getFoffset(offset + KEY_SIZE);
        }

        private void setReferencesPointer(long instanceId) {
            putID(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1, instanceId);
        }

        private long getReferencesPointer() {
            return getID(offset + KEY_SIZE + FOFFSET_SIZE + 4 + 1);
        }

        private void setFlags(byte flags) {
            dumpBuffer.putByte(offset + KEY_SIZE + FOFFSET_SIZE + 4, flags);
        }

        private byte getFlags() {
            return dumpBuffer.getByte(offset + KEY_SIZE + FOFFSET_SIZE + 4);
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final int KEY_SIZE;
    private final int VALUE_SIZE;
    private final int ENTRY_SIZE;
    private final int ID_SIZE;
    private final int FOFFSET_SIZE;
    private File tempFile;
    private Data dumpBuffer;
    private NumberList referenceList;
    private long fileSize;
    private long keys;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongMap(int size,int idSize,int foffsetSize) throws FileNotFoundException, IOException {
        assert idSize == 4 || idSize == 8;
        assert foffsetSize == 4 || foffsetSize == 8;
        keys = (size * 4L) / 3L;
        ID_SIZE = idSize;
        FOFFSET_SIZE = foffsetSize;
        KEY_SIZE = ID_SIZE;
        VALUE_SIZE = FOFFSET_SIZE + 4 + 1 + ID_SIZE;
        ENTRY_SIZE = KEY_SIZE + VALUE_SIZE;
        fileSize = keys * ENTRY_SIZE;
        tempFile = File.createTempFile("NBProfiler", ".map"); // NOI18N
        byte[] zeros = new byte[512*1024];

        RandomAccessFile file = new RandomAccessFile(tempFile, "rw"); // NOI18N
         while(file.length()<fileSize) {
            file.write(zeros);
        }
        file.setLength(fileSize);
        setDumpBuffer(file);
        tempFile.deleteOnExit();
        referenceList = new NumberList(ID_SIZE);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    protected void finalize() throws Throwable {
        tempFile.delete();
        super.finalize();
    }

    Entry get(long key) {
        long index = getIndex(key);

        while (true) {
            long mapKey = getID(index);

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
            if (getID(index) == 0L) {
                putID(index, key);
                putFoffset(index + KEY_SIZE, value);

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
            } else {
                throw ex;
            }
        }
    }

    private long getID(long index) {
        if (ID_SIZE == 4) {
            return ((long)dumpBuffer.getInt(index)) & 0xFFFFFFFFL;
        }
        return dumpBuffer.getLong(index);
    }
    
    private void putID(long index,long key) {
        if (ID_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
    }
    
    private long getFoffset(long index) {
        if (FOFFSET_SIZE == 4) {
            return dumpBuffer.getInt(index);
        }
        return dumpBuffer.getLong(index);
    }
    
    private void putFoffset(long index,long key) {
        if (FOFFSET_SIZE == 4) {
            dumpBuffer.putInt(index,(int)key);
        } else {
            dumpBuffer.putLong(index,key);
        }
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
    
    private interface Data {
        //~ Methods --------------------------------------------------------------------------------------------------------------
        
        byte getByte(long index);
        
        int getInt(long index);

        long getLong(long index);

        void putByte(long index, byte data);

        void putInt(long index, int data);

        void putLong(long index, long data);
    }

    private class FileData implements Data {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        RandomAccessFile file;
        byte[] buf;
        boolean bufferModified;
        long offset;
        final int BUFFER_SIZE = 128;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        FileData(RandomAccessFile f, long length) throws IOException {
            file = f;
            buf = new byte[ENTRY_SIZE*BUFFER_SIZE];
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
            int i = (int) (index % (ENTRY_SIZE * BUFFER_SIZE));
            long newOffset = index - i;

            if (offset != newOffset) {
                try {
                    if (bufferModified) {
                        file.seek(offset);
                        file.write(buf,0,getBufferSize(offset));
                        bufferModified = false;
                    }

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

    }

    private class MemoryMappedData implements Data {
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
    }
}
