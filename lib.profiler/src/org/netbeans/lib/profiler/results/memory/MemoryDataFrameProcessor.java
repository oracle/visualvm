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

package org.netbeans.lib.profiler.results.memory;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.AbstractDataFrameProcessor;
import org.netbeans.lib.profiler.results.ProfilingResultListener;
import java.util.logging.Level;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MemoryDataFrameProcessor extends AbstractDataFrameProcessor {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void doProcessDataFrame(byte[] buffer) {
        int curPos = 0;
        int bufSize = buffer.length;
        int currentEpoch = -1;

        do {
            byte eventType = buffer[curPos++];

            switch (eventType) {
                case CommonConstants.OBJ_ALLOC_STACK_TRACE: {
                    char classId = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    long objSize = (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                   | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                   | ((long) buffer[curPos++] & 0xFF);
                    int depth = ((((int) buffer[curPos++]) & 0xFF) << 16) | ((((int) buffer[curPos++]) & 0xFF) << 8)
                                | (((int) buffer[curPos++]) & 0xFF);

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Allocation stack trace: classId=" + (int) classId + ", objSize=" + objSize + ", depth="
                                      + depth); // NOI18N
                    }

                    int[] methodIds = new int[depth];

                    for (int i = 0; i < depth; i++) {
                        methodIds[i] = (((int) buffer[curPos++] & 0xFF) << 24) | (((int) buffer[curPos++] & 0xFF) << 16)
                                       | (((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF);
                    }

                    fireAllocStackTrace(classId, objSize, methodIds);

                    break;
                }
                case CommonConstants.OBJ_LIVENESS_STACK_TRACE: {
                    char classId = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    int objEpoch = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));

                    if (objEpoch > currentEpoch) {
                        currentEpoch = objEpoch; // objEpoch may be < currentEpoch if e.g. the GC event is being processed
                    }

                    long objectId = ((((long) classId) & 0xFFFF) << 48) | ((((long) objEpoch) & 0xFFFF) << 32)
                                    | (((long) buffer[curPos++] & 0xFF) << 24) | (((long) buffer[curPos++] & 0xFF) << 16)
                                    | (((long) buffer[curPos++] & 0xFF) << 8) | ((long) buffer[curPos++] & 0xFF);
                    long objSize = (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                   | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                   | ((long) buffer[curPos++] & 0xFF);

                    int depth = ((((int) buffer[curPos++]) & 0xFF) << 16) | ((((int) buffer[curPos++]) & 0xFF) << 8)
                                | (((int) buffer[curPos++]) & 0xFF);

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Liveness stack trace: classId=" + (int) classId + ", objectId=" + objectId + ", objEpoch="
                                      + objEpoch + ", objSize=" + objSize + ", depth=" + depth); // NOI18N
                    }

                    int[] methodIds = new int[depth];

                    for (int i = 0; i < depth; i++) {
                        methodIds[i] = (((int) buffer[curPos++] & 0xFF) << 24) | (((int) buffer[curPos++] & 0xFF) << 16)
                                       | (((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF);
                    }

                    fireLivenessStackTrace(classId, objectId, objEpoch, objSize, methodIds);

                    break;
                }
                case CommonConstants.OBJ_GC_HAPPENED: {
                    char classId = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    int objEpoch = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));

                    if (objEpoch > currentEpoch) {
                        currentEpoch = objEpoch; // objEpoch may be < currentEpoch if e.g. the GC event is being processed
                    }

                    long objectId = ((((long) classId) & 0xFFFF) << 48) | ((((long) objEpoch) & 0xFFFF) << 32)
                                    | (((long) buffer[curPos++] & 0xFF) << 24) | (((long) buffer[curPos++] & 0xFF) << 16)
                                    | (((long) buffer[curPos++] & 0xFF) << 8) | ((long) buffer[curPos++] & 0xFF);

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("GC Performed: classId=" + (int) classId + ", objectId=" + objectId + ", objEpoch="
                                      + objEpoch); // NOI18N
                    }

                    fireGCPerformed(classId, objectId, objEpoch);

                    break;
                }
                case CommonConstants.RESET_COLLECTORS: {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Profiling data reset"); // NOI18N
                    }

                    fireReset();

                    break;
                }
                case CommonConstants.BUFFEREVENT_PROFILEPOINT_HIT: {
                    int id = (((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF);
                    long timeStamp = (((long) buffer[curPos++] & 0xFF) << 48) | (((long) buffer[curPos++] & 0xFF) << 40)
                                     | (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                     | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                     | ((long) buffer[curPos++] & 0xFF);
                    int threadId = (((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF);

                    fireProfilingPoint(threadId, id, timeStamp);

                    break;
                }
                default: {
                    LOGGER.severe("*** Profiler Engine: internal error: got unknown event type in MemoryDataFrameProcessor: "
                                  + (int) eventType // NOI18N
                                  + " at " + curPos // NOI18N
                                  );

                    break; // NOI18N
                }
            }
        } while (curPos < bufSize);
    }

    private void fireAllocStackTrace(final char classId, final long objSize, final int[] methodIds) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    try {
                        ((MemoryProfilingResultsListener) listener).onAllocStackTrace(classId, objSize, methodIds);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    private void fireGCPerformed(final char classId, final long objectId, final int objEpoch) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((MemoryProfilingResultsListener) listener).onGcPerformed(classId, objectId, objEpoch);
                }
            });
    }

    private void fireLivenessStackTrace(final char classId, final long objectId, final int objEpoch, final long objSize,
                                        final int[] methodIds) {
        foreachListener(new ListenerFunctor() {
                public void execute(ProfilingResultListener listener) {
                    ((MemoryProfilingResultsListener) listener).onLivenessStackTrace(classId, objectId, objEpoch, objSize,
                                                                                     methodIds);
                }
            });
    }
}
