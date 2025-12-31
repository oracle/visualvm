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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author Tomas Hurka
 */
class NumberList {

    private static final int NUMBERS_IN_BLOCK = 3;
    private final File dataFile;
    private final RandomAccessFile data;
    private final int numberSize;
    private final int blockSize;
    // Map <offset,block>
    private final Map<Long,byte[]> blockCache;
    private final Set<Long> dirtyBlocks;
    private long blocks;
    private MappedByteBuffer buf;
    private long mappedSize;
    private CacheDirectory cacheDirectory;

    NumberList(long dumpFileSize, CacheDirectory cacheDir) throws IOException {
        this(bytes(dumpFileSize), cacheDir);
    }

    NumberList(int elSize, CacheDirectory cacheDir) throws IOException {
        dataFile = cacheDir.createTempFile("NBProfiler", ".ref"); // NOI18N
        data = new RandomAccessFile(dataFile, "rw"); // NOI18N
        numberSize = elSize;
        blockCache = new BlockLRUCache<>();
        dirtyBlocks = new HashSet<>(100000);
        blockSize = (NUMBERS_IN_BLOCK + 1) * numberSize;
        cacheDirectory = cacheDir;
        addBlock(); // first block is unused, since it starts at offset 0
    }

    private static int bytes(long number) {
        if ((number & ~0xFFL) == 0L) {
            return 1;
        }
        if ((number & ~0xFFFFL) == 0L) {
            return 2;
        }
        if ((number & ~0xFFFFFFL) == 0L) {
            return 3;
        }
        if ((number & ~0xFFFFFFFFL) == 0L) {
            return 4;
        }
        if ((number & ~0xFFFFFFFFFFL) == 0L) {
            return 5;
        }
        if ((number & ~0xFFFFFFFFFFFFL) == 0L) {
            return 6;
        }
        if ((number & ~0xFFFFFFFFFFFFFFL) == 0L) {
            return 7;
        }
        return 8;
    }
    
    protected void finalize() throws Throwable {
        if (cacheDirectory.isTemporary()) {
            dataFile.delete();
        }
        super.finalize();
    }
    
    long addNumber(long startOffset,long number) throws IOException {
        int slot;
        byte[] block = getBlock(startOffset);
        for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
            long el = readNumber(block,slot);
            if (el == 0L) {
                writeNumber(startOffset,block,slot,number);
                return startOffset;
            }
            if (el == number) { // number is already in the list
                return startOffset; // do nothing
            }
        }
        long nextBlock = addBlock(); // create next blok
        block = getBlock(nextBlock);
        writeNumber(nextBlock,block,slot,startOffset); // put next block in front of old block
        writeNumber(nextBlock,block,0,number); // write number to first position in the new block
        return nextBlock;
    }
    
    long addFirstNumber(long number1,long number2) throws IOException {
        long blockOffset = addBlock();
        byte[] block = getBlock(blockOffset);
        writeNumber(blockOffset,block,0,number1);
        writeNumber(blockOffset,block,1,number2);
        return blockOffset;
    }
    
    void putFirst(long startOffset,long number) throws IOException {
        int slot;
        long offset = startOffset;
        long movedNumber = 0;
        for(;;) {
            byte[] block = getBlock(offset);
            for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
                long el = readNumber(block,slot);
                if (offset == startOffset && slot == 0) { // first block
                    if (number == el) { // already first element 
                        return;
                    }
                    movedNumber = el;
                    writeNumber(offset,block,slot,number);
                } else if (el == 0L) { // end of the block, move to next one
                    break;
                } else if (el == number) { // number is already in the list
                    writeNumber(offset,block,slot,movedNumber);    // replace number and return                
                    return;
                }
            }
            offset = getOffsetToNextBlock(block);
            if (offset == 0L) {
                System.out.println("Error - number not found at end");
                return;
            }
        }
    }
    
    long getFirstNumber(long startOffset) throws IOException {
        byte[] block = getBlock(startOffset);
        return readNumber(block,0);
    }
    
    LongIterator getNumbersIterator(long startOffset) throws IOException {
        return new NumberIterator(startOffset);
    }

    List<Long> getNumbers(long startOffset) throws IOException {
        int slot;
        List<Long> numbers = new ArrayList<>();
        
        for(;;) {
            byte[] block = getBlock(startOffset);
            for (slot=0;slot<NUMBERS_IN_BLOCK;slot++) {
                long el = readNumber(block,slot);
                if (el == 0L) {     // end of the block, move to next one
                    break;
                }
                numbers.add(new Long(el));
            }
            long nextBlock = getOffsetToNextBlock(block);
            if (nextBlock == 0L) {
                return numbers;
            }
            startOffset = nextBlock;
        }
    }
    
    private void mmapData() {
        if (buf == null) {
            try {
                mappedSize = Math.min(blockSize*blocks, Integer.MAX_VALUE-blockSize+1);
                buf = data.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, mappedSize);
            } catch (IOException ex) {
                // map() failed
                mappedSize = 0;
                ex.printStackTrace();
            }
        }
    }
    
    void flush() {
        try {
            flushDirtyBlocks();
            blockCache.clear();
            mmapData();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    private long getOffsetToNextBlock(byte[] block) {
        return readNumber(block,NUMBERS_IN_BLOCK);
    }
    
    private long readNumber(byte[] block,int slot) {
        int offset = slot*numberSize;
        long el = 0;
//        for (int i=0;i<numberSize;i++) {
//            el <<= 8;
//            el |= ((int)block[offset+i]) & 0xFF;
//        }    
        if (numberSize == 4) {
            return ((long)getInt(block,offset)) & 0xFFFFFFFFL;
        } else if (numberSize == 8) {
            return getLong(block,offset);
        }
        return el;
    }

    private int getInt(byte[] buf, int i) {
        int ch1 = ((int) buf[i++]) & 0xFF;
        int ch2 = ((int) buf[i++]) & 0xFF;
        int ch3 = ((int) buf[i++]) & 0xFF;
        int ch4 = ((int) buf[i]) & 0xFF;

        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }

    private long getLong(byte[] buf, int i) {
       return (((long)buf[i++] << 56) +
              ((long)(buf[i++] & 255) << 48) +
              ((long)(buf[i++] & 255) << 40) +
              ((long)(buf[i++] & 255) << 32) +
              ((long)(buf[i++] & 255) << 24) +
              ((buf[i++] & 255) << 16) +
              ((buf[i++] & 255) <<  8) +
              ((buf[i++] & 255) <<  0));
    }
        
    private synchronized void writeNumber(long blockOffset,byte[] block,int slot,long element) throws IOException {
        if (blockOffset < mappedSize) {
            long offset = blockOffset+slot*numberSize;
            buf.position((int)offset);
            for (int i=numberSize-1;i>=0;i--) {
                byte el = (byte)(element >> (i*8));
                
                buf.put(el);
            }            
        } else {
            Long offsetObj = new Long(blockOffset);
            int offset = slot*numberSize;
            for (int i=numberSize-1;i>=0;i--) {
                byte el = (byte)(element >> (i*8));
                block[offset++]=el;
            }
            dirtyBlocks.add(offsetObj);
            if (dirtyBlocks.size()>10000) {
                flushDirtyBlocks();
            }
        }
    }
    
    private synchronized byte[] getBlock(long offset) throws IOException {
        byte[] block;
        if (offset < mappedSize) {
            block = new byte[blockSize];
            buf.position((int)offset);
            buf.get(block);
            return block;
        } else {
            Long offsetObj = new Long(offset);

            block = blockCache.get(offsetObj);
            if (block == null) {
                block = new byte[blockSize];
                data.seek(offset);
                data.readFully(block);
                blockCache.put(offsetObj,block);
            }
            return block;
        }
    }

    private long addBlock() throws IOException {
        long offset=blocks*blockSize;
        blockCache.put(new Long(offset),new byte[blockSize]);
        blocks++;
        return offset;
    }

    private void flushDirtyBlocks() throws IOException {
        if (dirtyBlocks.isEmpty()) {
            return;
        }
        Long[] dirty=dirtyBlocks.toArray(new Long[0]);
        Arrays.sort(dirty);
        byte blocks[] = new byte[1024*blockSize];
        int dataOffset = 0;
        long lastBlockOffset = 0;
        for (Long blockOffsetLong : dirty) {
            byte[] block = blockCache.get(blockOffsetLong);
            long blockOffset = blockOffsetLong.longValue();
            if (lastBlockOffset+dataOffset==blockOffset && dataOffset <= blocks.length - blockSize) {
                System.arraycopy(block,0,blocks,dataOffset,blockSize);
                dataOffset+=blockSize;
            } else {
                data.seek(lastBlockOffset);
                data.write(blocks,0,dataOffset);
                dataOffset = 0;
                System.arraycopy(block,0,blocks,dataOffset,blockSize);
                dataOffset+=blockSize;                
                lastBlockOffset = blockOffset;
            }
        }
        data.seek(lastBlockOffset);
        data.write(blocks,0,dataOffset);
        dirtyBlocks.clear();
    }

    //---- Serialization support
    void writeToStream(DataOutputStream out) throws IOException {
        out.writeUTF(dataFile.getAbsolutePath());
        out.writeInt(numberSize);
        out.writeLong(blocks);
        out.writeBoolean(buf != null);        
    }

    NumberList(DataInputStream dis, CacheDirectory cacheDir) throws IOException {
        boolean mmaped;
        
        cacheDirectory = cacheDir;
        dataFile = cacheDirectory.getCacheFile(dis.readUTF());
        data = new RandomAccessFile(dataFile, "rw"); // NOI18N
        numberSize = dis.readInt();
        blocks = dis.readLong();
        mmaped = dis.readBoolean();
        blockCache = new BlockLRUCache<>();
        dirtyBlocks = new HashSet<>(100000);
        blockSize = (NUMBERS_IN_BLOCK + 1) * numberSize;
        if (mmaped) {
            mmapData();
        }
    }    
    
    private class NumberIterator extends LongIterator {
        private int slot;
        private byte[] block;
        private long nextNumber;

        private NumberIterator(long startOffset) throws IOException {
            slot = 0;
            block = getBlock(startOffset);
            nextNumber();
        }

        @Override
        boolean hasNext() {
            return nextNumber != 0;
        }

        @Override
        long next() {
            if (hasNext()) {
                long num = nextNumber;
                try {
                    nextNumber();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    nextNumber = 0;
                }
                return num;
            }
            throw new NoSuchElementException();
        }

        private void nextNumber() throws IOException {
            if (slot < NUMBERS_IN_BLOCK) {
                long nextNum = readNumber(block,slot++);
                if (nextNum == 0) {     // end of the block, move to next one
                    nextBlock();
                } else {
                    nextNumber = nextNum;
                }
            } else {
               nextBlock();
            }
        }

        private void nextBlock() throws IOException {
            long nextBlock = getOffsetToNextBlock(block);

            if (nextBlock == 0) { // end of list
                nextNumber = 0;
                return;
            }
            block = getBlock(nextBlock);
            slot = 0;
            nextNumber();
        }
    }

    private class BlockLRUCache<V> extends LinkedHashMap<Long,V> {
        
        private static final int MAX_CAPACITY = 10000;
        
        private BlockLRUCache() {
            super(MAX_CAPACITY,0.75f,true);
        }

        protected boolean removeEldestEntry(Map.Entry<Long,V> eldest) {
            if (size()>MAX_CAPACITY) {
                Long key = eldest.getKey();
                if (!dirtyBlocks.contains(key)) {
                    return true;
                }
                get(key);
            }
            return false;
        }

    }
}
