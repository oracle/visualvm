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

package org.netbeans.lib.profiler.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;


/**
 * Target VM-side management of the shared-memory event buffer file, through which rough profiling data
 * is transmitted to the client.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class EventBufferManager implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final boolean DEBUG = System.getProperty("org.netbeans.lib.profiler.server.EventBufferManager") != null; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File bufFile;
    private FileChannel bufFileChannel;
    private MappedByteBuffer mapByteBuf;
    private ProfilerServer profilerServer;
    private RandomAccessFile raFile;
    private String bufFileName;
    private boolean bufFileOk;
    private boolean remoteProfiling;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public EventBufferManager(ProfilerServer server) {
        profilerServer = server;
        remoteProfiling = server.getProfilingSessionStatus().remoteProfiling;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getBufferFileName() {
        if (remoteProfiling) {
            return ""; // NOI18N
        } else {
            return bufFileName;
        }
    }

    public void eventBufferDumpHook(byte[] eventBuffer, int startPos, int curPtrPos) {
        int length = curPtrPos - startPos;

        if (!remoteProfiling) {
            if (!bufFileOk) {
                return;
            }

            if (DEBUG) {
                System.err.println("EventBufferManager.DEBUG: Dumping to file: startPos:" + startPos + ", length:" + length); // NOI18N
            }

            mapByteBuf.reset();
            mapByteBuf.put(eventBuffer, startPos, length);
            bufFileOk = profilerServer.sendEventBufferDumpedCommand(length, null, -1);
        } else {
            if (DEBUG) {
                System.err.println("EventBufferManager.DEBUG: Dumping to compressed wire: startPos:" + startPos + ", length:" + length); // NOI18N
            }
            profilerServer.sendEventBufferDumpedCommand(length, eventBuffer, startPos);
        }
    }

    public void freeBufferFile() {
        if (remoteProfiling) {
            return;
        }

        try {
            if (bufFileChannel != null) {
                mapByteBuf = null;
                bufFileChannel.close();
                raFile.close();
                System.gc(); // GCing mapBuf is the only way to free the buffer file.
                bufFileOk = false;
            }
        } catch (IOException ex) {
            System.err.println("Profiler Agent Error: internal error when closing temporary memory-mapped communication file"); // NOI18N
        }
    }

    public void openBufferFile(int sizeInBytes) throws IOException {
        if (remoteProfiling) {
            return;
        }

        if (bufFileOk) {
            return;
        }

        try {
            bufFile = File.createTempFile("jfluidbuf", null); // NOI18N
            bufFileName = bufFile.getCanonicalPath();

            // Bugfix: http://profiler.netbeans.org/issues/show_bug.cgi?id=59166
            // Summary: Temporary communication file should be accessible for all users
            // Bugfix details: As it does not seem to be possible to set the file permissions using Java code
            //                 we explicitely invoke chmod on the newly created buffer file if we are on UNIX
            if (Platform.isUnix()) {
                try {
                    Runtime.getRuntime().exec(new String[] { "chmod", "666", bufFileName }); // NOI18N
                } catch (Exception e) {
                    System.err.println("*** JFluid Warning: Failed to set access permissions on temporary buffer file, you may not be able to attach as a different user: "
                                       + e.getMessage()); // NOI18N
                }
            }

            raFile = new RandomAccessFile(bufFile, "rw"); // NOI18N
            bufFileChannel = raFile.getChannel();
            mapByteBuf = bufFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, sizeInBytes);
            mapByteBuf.rewind();
            mapByteBuf.mark();
            bufFileOk = true;
        } catch (FileNotFoundException ex1) {
            System.err.println("Profiler Agent Error: FileNotFoundException in EventBufferManager.openBufferFile - should not happen!"); // NOI18N

            return;
        } catch (IOException ex2) {
            System.err.println("Profiler Agent Error: Could not create temporary buffer file in the default temporary directory: "
                               + ex2.getMessage() + ": " + System.getProperty("java.io.tmpdir")); // NOI18N
            throw new IOException("Could not create temporary buffer file in the default temporary directory: "
                                  + ex2.getMessage() + ": " + System.getProperty("java.io.tmpdir")); // NOI18N
        }
    }
}
