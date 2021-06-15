/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.utilities;

import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;

/**
 * Miscellaneous utilities
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerUtils {

    private static RequestProcessor profilerRequestProcessor;
    private static ErrorManager profilerErrorManager;


    public static synchronized RequestProcessor getProfilerRequestProcessor() {
        if (profilerRequestProcessor == null)
            profilerRequestProcessor = new RequestProcessor("Profiler Request Processor", 1); // NOI18N
        return profilerRequestProcessor;
    }

    public static synchronized ErrorManager getProfilerErrorManager() {
        if (profilerErrorManager == null)
            profilerErrorManager = ErrorManager.getDefault().getInstance("org.graalvm.visualvm.lib.profiler"); // NOI18N
        return profilerErrorManager;
    }

    public static void runInProfilerRequestProcessor(Runnable r) {
        getProfilerRequestProcessor().post(r);
    }

    public static void runInProfilerRequestProcessor(Runnable r, int delay) {
        getProfilerRequestProcessor().post(r, delay);
    }
}
