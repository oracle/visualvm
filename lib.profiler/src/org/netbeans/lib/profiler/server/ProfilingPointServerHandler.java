/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.server;

import org.netbeans.lib.profiler.server.system.Timers;
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
