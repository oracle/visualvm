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

package org.netbeans.lib.profiler.server.system;


/**
 * Provides methods for accessing various information related to threads.
 *
 * @author  Misha Dmitriev
 */
public class Threads {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns all live Java threads in this JVM. If the number of threads fits into the threads array, it is reused
     * (unused elements are filled with nulls). Otherwise, or if threads is null, a new array is created.
     */
    public static native Thread[] getAllThreads(Thread[] threads);

    //----------------- Miscellaneous
    public static native String getJVMArguments();

    public static native String getJavaCommand();

    /** For each passed thread, stores its status as defined in CommonConstants, in the status array. threads may contain nulls. */
    public static native void getThreadsStatus(Thread[] threads, int[] status);

    /** Returns the total number of live Java threads. */
    public static native int getTotalNumberOfThreads();

    /** Should be called at earliest possible time */
    public static void initialize() {
        // Doesn't do anything in this version
    }

    /**
     * Records a given thread as a profiler's own thread, so that targetAppTreadsExist() does not treat it as a
     * target app thread. Note that the current implementation allows only one additional profiler thread; if this
     * is called more than once, only the latest thread is remembered.
     */
    public static native void recordAdditionalProfilerOwnThread(Thread specialThread);

    /**
     * Record profiler's own threads. If excludeSpecialThread is true, recocrd all the Java threads currently existing
     * in this JVM, minus specialThread. Otherwise, record only the specialThread. Returns the number of recorded threads.
     */
    public static native int recordProfilerOwnThreads(boolean excludeSpecialThread, Thread specialThread);

    public static synchronized native void resumeTargetAppThreads(Thread excludedThread);

    public static synchronized native void suspendTargetAppThreads(Thread excludedThread);

    /**
     * Checks if any live target application threads still exist. A target application thread is any thread not recorded
     * previously by recordProfilerOwnThreads() or recordAdditionalProfilerOwnThread().
     */
    public static native boolean targetAppThreadsExist();

    public static void terminateTargetAppThreads() {
        terminateTargetAppThreads(new ThreadDeath());
    }

    public static native void terminateTargetAppThreads(Object exception);
}
