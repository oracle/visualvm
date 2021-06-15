/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.threads;

import java.lang.management.ThreadInfo;
import java.util.Date;
import javax.management.openmbean.CompositeData;

/**
 *
 * @author Tomas Hurka
 */
public class ThreadDump {

    private final boolean jdk15;
    private final Date time;
    private Object[] cdThreads;
    private ThreadInfo[] tinfos;
    private final Object tinfoLock = new Object();

    public ThreadDump(boolean j15, Date t, Object[] th) {
        jdk15 = j15;
        time = t;
        cdThreads = th;
    }

    public boolean isJDK15() {
        return jdk15;
    }

    public Date getTime() {
        return time;
    }

    public ThreadInfo[] getThreads() {
        synchronized (tinfoLock) {
            if (tinfos == null) {
                int i = 0;
                tinfos = new ThreadInfo[cdThreads.length];
                for (Object cd : cdThreads) {
                    tinfos[i++] = ThreadInfo.from((CompositeData) cd);
                }
                cdThreads = null;
            }
            return tinfos;
        }
    }

    @Override
    public String toString() {
        return "Thread dump taken at:" + getTime() + " threads: " + getThreads().length;  // NOI18N
    }

}
