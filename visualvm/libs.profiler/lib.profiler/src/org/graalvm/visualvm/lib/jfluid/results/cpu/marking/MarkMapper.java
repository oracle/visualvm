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

package org.graalvm.visualvm.lib.jfluid.results.cpu.marking;

import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * @author Jaroslav Bachorik
 */
class MarkMapper implements MarkingEngine.StateObserver {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // @GuardedBy marksGuard
    private final Map markMap = new HashMap();
    private final Object marksGuard = new Object();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Mark getMark(int methodId, ProfilingSessionStatus status) {
        if (status == null) {
            return Mark.DEFAULT;
        }

        synchronized (marksGuard) {
            Mark mark = (Mark) markMap.get(Integer.valueOf(methodId));

            if (mark == null) {
                mark = MarkingEngine.getDefault().mark(methodId, status); // do mark the method
                markMap.put(Integer.valueOf(methodId), mark);
            }

            return mark;
        }
    }

    public void stateChanged(MarkingEngine instance) {
        reset();
    }

    private void reset() {
        synchronized (marksGuard) {
            markMap.clear();
        }
    }
}
