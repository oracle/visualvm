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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * LongBuffer is a special kind of buffer for storing longs. It uses array of longs if there is only few longs
 * stored, otherwise longs are saved to backing temporary file.
 * @author Tomas Hurka
 */
class LongBuffer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DataInputStream readStream;
    private DataOutputStream writeStream;
    private File backedFile;
    private long[] buffer;
    private boolean hasData;
    private boolean useBackingFile;
    private int bufferSize;
    private int readOffset;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    LongBuffer(int size) {
        buffer = new long[size];
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    void delete() {
        if (backedFile != null) {
            backedFile.delete();
        }
    }

    boolean hasData() {
        return hasData;
    }

    long readLong() throws IOException {
        if (!useBackingFile) {
            if (readOffset < bufferSize) {
                return buffer[readOffset++];
            } else {
                return 0;
            }
        }

        try {
            return readStream.readLong();
        } catch (EOFException ex) {
            return 0L;
        }
    }

    void reset() {
        bufferSize = 0;
        writeStream = null;
        readStream = null;
        hasData = false;
        useBackingFile = false;
        readOffset = 0;
    }

    void startReading() {
        if (useBackingFile) {
            try {
                writeStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        writeStream = null;
        readOffset = 0;

        if (useBackingFile) {
            try {
                readStream = new DataInputStream(new BufferedInputStream(new FileInputStream(backedFile), buffer.length * 8));
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    void writeLong(long data) throws IOException {
        hasData = true;

        if (bufferSize < buffer.length) {
            buffer[bufferSize++] = data;

            return;
        }

        if (backedFile == null) {
            backedFile = File.createTempFile("NBProfiler", ".gc"); // NOI18N
        }

        if (writeStream == null) {
            writeStream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(backedFile), buffer.length * 8));

            for (int i = 0; i < buffer.length; i++) {
                writeStream.writeLong(buffer[i]);
            }

            useBackingFile = true;
        }

        writeStream.writeLong(data);
    }
}
