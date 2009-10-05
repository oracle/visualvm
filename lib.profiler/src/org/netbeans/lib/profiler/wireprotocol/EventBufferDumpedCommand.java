/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.wireprotocol;

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
    private byte[] buffer;
    private int startPos;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public EventBufferDumpedCommand(int bufSize, byte[] buf, int start) {
        super(EVENT_BUFFER_DUMPED);
        this.bufSize = bufSize;
        buffer = buf;
        startPos = start;
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
        return super.toString() + ", bufSize: " + bufSize; // NOI18N
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
        }
    }
}
