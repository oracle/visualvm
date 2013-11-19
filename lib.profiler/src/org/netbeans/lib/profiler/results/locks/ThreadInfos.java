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

/**
 *
 * @author Tomas Hurka
 */
final class ThreadInfos {

    ThreadInfo[] threadInfos;
    int threadInfosLastIdx;
    ThreadInfo unknowThread = new ThreadInfo(-1, "Unknown", "N/A");     // NOI18N

    ThreadInfos() {
        reset();
    }

    boolean isEmpty() {
        if ((threadInfos == null) || (threadInfos.length == 0)) {
            return true;
        }
        for (int i = 0; i < threadInfos.length; i++) {
            if (threadInfos[i] != null) {
                return false;
            }
        }
        return true;
    }

    void newThreadInfo(int threadId, String threadName, String threadClassName) {
        if ((threadId > threadInfosLastIdx) || (threadInfos == null)) {
            int newLen = threadId + 1;
            ThreadInfo[] newInfos = new ThreadInfo[newLen];
            if (threadInfos != null) {
                System.arraycopy(threadInfos, 0, newInfos, 0, threadInfos.length);
            }
            threadInfos = newInfos;
            threadInfosLastIdx = threadId;
        }
        threadInfos[threadId] = new ThreadInfo(threadId, threadName, threadClassName);
    }

    void reset() {
        threadInfos = null;
        threadInfosLastIdx = -1;
    }

    ThreadInfo getThreadInfo(int threadId) {
        if (threadId == -1) return unknowThread;
        if (!isEmpty()) {
            return threadInfos[threadId];
        }
        return null;
    }
}
