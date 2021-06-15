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

package org.graalvm.visualvm.lib.jfluid.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
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

    private static final boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.jfluid.server.EventBufferManager") != null; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private File bufFile;
    private FileChannel bufFileChannel;
    private MappedByteBuffer mapByteBuf;
    private ProfilerServer profilerServer;
    private RandomAccessFile raFile;
    private String bufFileName = "";
    private boolean bufFileOk;
    private boolean bufFileSent;
    private boolean remoteProfiling;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public EventBufferManager(ProfilerServer server) {
        profilerServer = server;
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
            bufFileOk = profilerServer.sendEventBufferDumpedCommand(length, bufFileSent ? "": getBufferFileName());
            bufFileSent = true;
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
        remoteProfiling = ProfilerServer.getProfilingSessionStatus().remoteProfiling;
        if (remoteProfiling) {
            return;
        }

        if (bufFileOk) {
            return;
        }

        try {
            bufFileSent = false;
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
                    System.err.println("*** JFluid Warning: Failed to set access permissions on temporary buffer file, you may not be able to attach as a different user: " + e.getMessage()); // NOI18N
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
