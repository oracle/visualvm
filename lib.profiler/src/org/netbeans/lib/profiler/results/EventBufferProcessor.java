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

package org.netbeans.lib.profiler.results;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.ProfilerLogger;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.netbeans.lib.profiler.wireprotocol.EventBufferDumpedCommand;


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
    protected static byte[] buf;
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

    public static synchronized void readDataAndPrepareForProcessing(EventBufferDumpedCommand cmd) {
        if (!status.remoteProfiling) {
            int bufSizeInBytes = cmd.getBufSize();
            if ((buf == null) || (buf.length < bufSizeInBytes)) {
                buf = new byte[bufSizeInBytes];
            }
            mapByteBuf.reset();
            mapByteBuf.get(buf, 0, bufSizeInBytes);
        } else {
            buf = cmd.getBuffer();
            assert buf != null;
        }
        startDataProcessingTime = System.currentTimeMillis();
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
