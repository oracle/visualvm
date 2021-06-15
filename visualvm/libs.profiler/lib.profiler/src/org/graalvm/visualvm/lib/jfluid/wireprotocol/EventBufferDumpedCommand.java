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
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


/**
 * This command is issued by the profiling back-end when an event buffer is dumped into the shared-memory file
 * for natural reasons (capacity exceeded).
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 * @author Tomas Hurka
 */
public class EventBufferDumpedCommand extends Command {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int bufSize;
    private String eventBufferFileName;
    private byte[] buffer;
    private int startPos;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public EventBufferDumpedCommand(int bufSize, byte[] buf, int start) {
        super(EVENT_BUFFER_DUMPED);
        this.bufSize = bufSize;
        buffer = buf;
        startPos = start;
        eventBufferFileName = "";
    }

    public EventBufferDumpedCommand(int bufSize, String bufferName) {
        super(EVENT_BUFFER_DUMPED);
        this.bufSize = bufSize;
        buffer = null;
        startPos = -1;
        eventBufferFileName = bufferName;
    }

    // Custom serialization support
    EventBufferDumpedCommand() {
        super(EVENT_BUFFER_DUMPED);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getBufSize() {
        return bufSize;
    }

    public byte[] getBuffer() {
        return buffer;
    }

    // For debugging
    public String toString() {
        return super.toString() + ", bufSize: " + bufSize + (eventBufferFileName.length()>0 ? ", eventBufferFileName:" + eventBufferFileName : ""); // NOI18N
    }

    public String getEventBufferFileName() {
        return eventBufferFileName;
    }
    
    void readObject(ObjectInputStream in) throws IOException {
        boolean hasBuffer;
        
        bufSize = in.readInt();
        hasBuffer = in.readBoolean();
        if (hasBuffer) {
            int compressedSize = in.readInt();
            byte[] compressedBuf = new byte[compressedSize];
            Inflater decompressor = new Inflater();
            
            buffer = new byte[bufSize];
            in.readFully(compressedBuf);
            decompressor.setInput(compressedBuf);
            try {
                int originalSize = decompressor.inflate(buffer);
                assert originalSize==bufSize;
            } catch (DataFormatException ex) {
                throw new IOException(ex.getMessage());
            } finally {
                decompressor.end();
            }
            eventBufferFileName = "";
        } else {
            eventBufferFileName = in.readUTF();
        }
    }
    
    void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(bufSize);
        out.writeBoolean(buffer != null);
        if (buffer != null) {
            Deflater compressor = new Deflater();
            // for small buffers, the compressed size can be somewhat larger than the original  
            byte[] compressedBytes = new byte[bufSize + 32]; 
            int compressedSize;
            
            compressor.setInput(buffer,startPos,bufSize);
            compressor.finish();
            compressedSize = compressor.deflate(compressedBytes);
            out.writeInt(compressedSize);
            out.write(compressedBytes,0,compressedSize);
        } else {
            out.writeUTF(eventBufferFileName);
        }
    }
}
