/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.locks;

import java.util.logging.Level;
import org.netbeans.lib.profiler.global.CommonConstants;

/**
 *
 * @author Tomas Hurka
 */
public class LockDataFrameProcessor extends AbstractLockDataFrameProcessor {

    @Override
    public void doProcessDataFrame(byte[] buffer) {
        int curPos = 0;
        int bufSize = buffer.length;

        do {
            byte eventType = buffer[curPos++];

            switch (eventType) {
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
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("Profile Point Hit " + id + ", threadId=" + id + ", timeStamp=" + timeStamp); // NOI18N
                    }

                    fireProfilingPoint(threadId, id, timeStamp);

                    break;
                }
                case CommonConstants.SET_FOLLOWING_EVENTS_THREAD: {
                    currentThreadId = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Change current thread , tId={0}", currentThreadId); // NOI18N
                    }

                    break;
                }
                case CommonConstants.NEW_THREAD: {
                    int threadId = (char) ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    int strLen = ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    String threadName = new String(buffer, curPos, strLen);
                    curPos += strLen;
                    strLen = ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));

                    String threadClassName = new String(buffer, curPos, strLen);
                    curPos += strLen;

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Creating new thread , tId={0}", threadId); // NOI18N
                    }

                    fireNewThread(threadId, threadName, threadClassName);
                    currentThreadId = threadId;

                    break;
                }
                case CommonConstants.NEW_MONITOR: {
                    int hash = (((int) buffer[curPos++] & 0xFF) << 24) 
                         | (((int) buffer[curPos++] & 0xFF) << 16)
                         | (((int) buffer[curPos++] & 0xFF) << 8) 
                         | ((int) buffer[curPos++] & 0xFF);
                    int strLen = ((((int) buffer[curPos++] & 0xFF) << 8) | ((int) buffer[curPos++] & 0xFF));
                    String className = new String(buffer, curPos, strLen);
                    curPos += strLen;

                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Creating new monitor , mId={0} , className={1}", new Object[] {hash, className}); // NOI18N
                    }

                    fireNewMonitor(hash, className);
                    break;
                }
                case CommonConstants.METHOD_ENTRY_MONITOR:
                case CommonConstants.METHOD_EXIT_MONITOR: {
                    long timeStamp0 = (((long) buffer[curPos++] & 0xFF) << 48) | (((long) buffer[curPos++] & 0xFF) << 40)
                                 | (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                 | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                 | ((long) buffer[curPos++] & 0xFF);
                    long timeStamp1 = -1;
                    int hash = (((int) buffer[curPos++] & 0xFF) << 24) 
                         | (((int) buffer[curPos++] & 0xFF) << 16)
                         | (((int) buffer[curPos++] & 0xFF) << 8) 
                         | ((int) buffer[curPos++] & 0xFF);
                    
                    if (eventType == CommonConstants.METHOD_ENTRY_MONITOR) {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "Monitor entry , tId={0} , monitorId={1}", new Object[]{currentThreadId,hash}); // NOI18N
                        }

                        fireMonitorEntry(currentThreadId, timeStamp0, timeStamp1, hash);
                    }
                    if (eventType == CommonConstants.METHOD_EXIT_MONITOR) {
                        if (LOGGER.isLoggable(Level.FINEST)) {
                            LOGGER.log(Level.FINEST, "Monitor exit , tId={0} , monitorId={1}", new Object[]{currentThreadId,hash}); // NOI18N
                        }

                        fireMonitorExit(currentThreadId, timeStamp0, timeStamp1, hash);
                    }
                    break;
                }
                case CommonConstants.ADJUST_TIME: {
                    long timeStamp0 = (((long) buffer[curPos++] & 0xFF) << 48) | (((long) buffer[curPos++] & 0xFF) << 40)
                                 | (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                 | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                 | ((long) buffer[curPos++] & 0xFF);
                    long timeStamp1 = (((long) buffer[curPos++] & 0xFF) << 48) | (((long) buffer[curPos++] & 0xFF) << 40)
                                 | (((long) buffer[curPos++] & 0xFF) << 32) | (((long) buffer[curPos++] & 0xFF) << 24)
                                 | (((long) buffer[curPos++] & 0xFF) << 16) | (((long) buffer[curPos++] & 0xFF) << 8)
                                 | ((long) buffer[curPos++] & 0xFF);
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.log(Level.FINEST, "Adjust time , tId={0}", currentThreadId); // NOI18N
                    }

                    fireAdjustTime(currentThreadId, timeStamp0, timeStamp1);

                    break;
                }
                default: {
                    LOGGER.severe("*** Profiler Engine: internal error: got unknown event type in LockDataFrameProcessor: " // NOI18N
                                  + (int) eventType
                                  + " at " + curPos // NOI18N
                                  );

                    break;
                }
            }
        } while (curPos < bufSize);
    }
    
}
