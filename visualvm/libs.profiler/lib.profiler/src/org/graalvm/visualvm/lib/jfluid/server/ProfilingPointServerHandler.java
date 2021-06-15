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

package org.graalvm.visualvm.lib.jfluid.server;

import org.graalvm.visualvm.lib.jfluid.server.system.Timers;
import java.lang.reflect.Method;
import java.util.Arrays;


/**
 * Base class for custom profiling point server handlers. Default implementation just writes timestamped
 * event to the event buffer to be later processed by the client executor. Handlers are expected to be
 * singletons accessible via static getInstance() method.
 *
 * @author Tomas Hurka
 * @author Maros Sandor
 */
public class ProfilingPointServerHandler {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static ProfilingPointServerHandler[] profilingPointHandlers;
    private static int[] profilingPointIDs;
    private static ProfilingPointServerHandler instance;

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public static synchronized ProfilingPointServerHandler getInstance(String clientInfo) {
        if (instance == null) {
            instance = new ProfilingPointServerHandler();
        }

        return instance;
    }

    public static synchronized void initInstances(int[] ppIDs, String[] handlerClassNames, String[] handlersInfo) {
        profilingPointIDs = ppIDs;
        profilingPointHandlers = getInstances(handlerClassNames, handlersInfo);
    }

    public static ProfilingPointServerHandler getHandler(char handlerId) {
        int idx = Arrays.binarySearch(profilingPointIDs, handlerId);
        if (idx >= 0) {
            return profilingPointHandlers[idx];
        }
        return null;
    }

    private static ProfilingPointServerHandler[] getInstances(String[] handlerClassNames, String[] handlersInfo) {
        ProfilingPointServerHandler[] handlers = new ProfilingPointServerHandler[handlerClassNames.length];

        for (int i = 0; i < handlerClassNames.length; i++) {
            try {
                Method method = Class.forName(handlerClassNames[i]).getMethod("getInstance", new Class[] { String.class }); //NOI18N
                handlers[i] = (ProfilingPointServerHandler) method.invoke(null, new Object[] { handlersInfo[i] });
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }

        return handlers;
    }

    /**
     * Invoked by the JFluid server when the profiling point hits.
     *
     * @param id unique ID of the profiling point
     */
    public void profilingPointHit(int id) {
        long absTimeStamp = Timers.getCurrentTimeInCounts();
        profilingPointHit(id, absTimeStamp);
    }

    /**
     * Invoked by the JFluid server when the profiling point hits.
     *
     * @param id unique ID of the profiling point
     */
    public void profilingPointHit(int id, long absTimeStamp) {
        ProfilerRuntime.writeProfilingPointHitEvent(id, absTimeStamp);
    }
}
