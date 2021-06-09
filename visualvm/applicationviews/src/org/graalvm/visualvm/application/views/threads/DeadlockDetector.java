/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.views.threads;

import java.lang.management.ThreadMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.logging.Logger;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
class DeadlockDetector {
    private static final Logger LOGGER = Logger.getLogger(DeadlockDetector.class.getName());
    private static int deadlockNumber = 10;
    private ThreadMXBean threadBean;
    private boolean deadlockDectionDisabled;
    private boolean findDeadlockedThreadsSupported;
    private int countDown;
    
    DeadlockDetector(ThreadMXBean tb) {
        threadBean = tb;
        findDeadlockedThreadsSupported = true;
    }
    
    long[] detectDeadlock() {
        long threadIds[];
        
        if (deadlockDectionDisabled) {
            return null;
        }
        if (countDown++ % deadlockNumber != 0) {
            return null;
        }
        try {
            if (findDeadlockedThreadsSupported) {
                threadIds = threadBean.findDeadlockedThreads();
            } else {
                threadIds = threadBean.findMonitorDeadlockedThreads();                
            }
        } catch (SecurityException ex) {
            LOGGER.throwing(ThreadMXBeanDataManager.class.getName(), "detectDeadlock", ex); // NOI18N
            deadlockDectionDisabled = true;
            return null;
        } catch (UnsupportedOperationException ex) {
            tryJdk15(ex);
            return null;
        } catch (UndeclaredThrowableException ex) {
            Throwable t = ex.getUndeclaredThrowable();
            if (t instanceof ReflectionException) {
                ReflectionException re = (ReflectionException) t;
                if (re.getTargetException() instanceof NoSuchMethodException) {
                    tryJdk15(ex);
                }
            }
            return null;
        }
        if (threadIds != null) { // Deadlock
            assert threadIds.length>0;
            // LOGGER.info("Deadlock "+Arrays.toString(threadIds));
        }
        return threadIds;
    }

    private void tryJdk15(Exception ex) {
        if (findDeadlockedThreadsSupported) {
            findDeadlockedThreadsSupported = false;
            countDown--;
            detectDeadlock();
        } else {
            LOGGER.throwing(ThreadMXBeanDataManager.class.getName(), "detectDeadlock", ex); // NOI18N
            deadlockDectionDisabled = true;
        }
    }
}
