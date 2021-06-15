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

package org.graalvm.visualvm.lib.jfluid.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.ProfilerLogger;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.graalvm.visualvm.lib.jfluid.wireprotocol.EventBufferDumpedCommand;


/**
 * Management of the shared-memory "event buffer" file, into which TA instrumentation writes rough profiling
 * data, and which is processed here at the client side.
 * So far it's deliberately allstatic. Can be made more object-style, but before doing that, check its current
 * usage in ProfilerClient and, as a superclass, in CPUCallGraphBuilder etc.
 *
 * @author Misha Dmitirev
 * @author Tomas Hurka
 */
public class EventBufferProcessor implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static ProfilingSessionStatus status;
    protected static ProfilerClient profilerClient;
    protected static MappedByteBuffer mapByteBuf;
    protected static File bufFile;
    protected static RandomAccessFile raFile;
    protected static FileChannel bufFileChannel;
    protected static boolean bufFileExists;
    protected static long startDataProcessingTime;
    protected static long dataProcessingTime;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * For statistics only
     */
    public static long getDataProcessingTime() {
        return dataProcessingTime;
    }

    public static boolean setEventBufferFile(String fileName) {
        if ((status != null) && status.remoteProfiling) {
            return true;
        }

        if (bufFile != null) {
            removeEventBufferFile();
        }

        try {
            bufFile = new File(fileName);
            raFile = new RandomAccessFile(bufFile, "rw"); // NOI18N
            bufFileChannel = raFile.getChannel();
            mapByteBuf = bufFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, EVENT_BUFFER_SIZE_IN_BYTES);
            mapByteBuf.rewind();
            mapByteBuf.mark();
            bufFileExists = true;
        } catch (FileNotFoundException ex1) {
            return false;
        } catch (IOException ex2) {
            ProfilerLogger.severe("internal error when opening memory-mapped temporary file"); // NOI18N
            ProfilerLogger.log(ex2);
            ProfilerLogger.severe(PLEASE_REPORT_PROBLEM);

            return false;
        }

        return true;
    }

    public static boolean bufFileExists() {
        return bufFileExists;
    }

    public static void initialize(ProfilerClient inProfilerClient) {
        profilerClient = inProfilerClient;
        status = profilerClient.getStatus();
    }

    public static synchronized byte[] readDataAndPrepareForProcessing(EventBufferDumpedCommand cmd) {
        byte[] buf;
        if (!status.remoteProfiling) {
            int bufSizeInBytes = cmd.getBufSize();
            buf = new byte[bufSizeInBytes];
            mapByteBuf.reset();
            mapByteBuf.get(buf, 0, bufSizeInBytes);
        } else {
            buf = cmd.getBuffer();
            assert buf != null;
            assert buf.length == cmd.getBufSize();
        }
        startDataProcessingTime = System.currentTimeMillis();
        return buf;
    }

    public static void removeEventBufferFile() {
        if ((status != null) && status.remoteProfiling) {
            return; // This may be called "uniformly" even during monitoring, when status isn't initialized
        }

        try {
            if (bufFile != null) {
                mapByteBuf = null;

                if (bufFileChannel != null) {
                    bufFileChannel.close(); // bufFileChannel can accidentally be null, if previous connection didn't quite succeed
                }

                if (raFile != null) {
                    raFile.close();
                }

                System.gc(); // Stupid - but that's the only way to GC mapBuf and thus to enable the buffer file deletion...
                             // Now try to remove the buffer file. If this doesn't happen immediately, try again - it may be that the
                             // target VM has not yet freed this file on its side. Repeat attempts for 2 seconds.

                for (int i = 0; i < 20; i++) {
                    if (bufFile.delete()) {
                        bufFile = null;
                        bufFileExists = false;

                        return;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ProfilerLogger.severe("internal error when closing memory-mapped temporary file"); // NOI18N
            ProfilerLogger.severe(PLEASE_REPORT_PROBLEM);
        } finally {
            bufFileExists = false;
        }
    }

    public static void reset() {
        dataProcessingTime = 0;

        // buf = null; // to cleanup memory allocated for the buffer - we cannot do this here, there may be events in the
        // buffer that are still unprocessed and the EventBufferProcessor in the cycle of processing it
        // see http://profiler.netbeans.org/issues/show_bug.cgi?id=69275
    }

    protected static synchronized void completeDataProcessing() {
        dataProcessingTime += (System.currentTimeMillis() - startDataProcessingTime);
    }
}
