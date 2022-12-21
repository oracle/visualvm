/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.jfluid.global.ProfilingSessionStatus;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.marker.Marker;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MarkingEngine {
    private static String INVALID_MID = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.cpu.Bundle").getString("MSG_INVALID_METHODID"); // NOI18N

    private static Logger LOGGER = Logger.getLogger(MarkingEngine.class.getName());

    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface StateObserver {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        void stateChanged(MarkingEngine instance);
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static MarkingEngine instance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object markGuard = new Object();

    final private MarkMapper mapper;

    // @GuardedBy markGuard
    private MarkMapping[] marks;

    private Set observers = new HashSet();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MarkingEngine
     */
    private MarkingEngine() {
        mapper = new MarkMapper();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized MarkingEngine getDefault() {
        if (instance == null) {
            instance = new MarkingEngine();
        }

        return instance;
    }

    // configure the engine for a given set of {@linkplain MarkMapping}
    public synchronized void configure(MarkMapping[] mappings, Collection observers) {
        setMarks(mappings != null ? mappings : Marker.DEFAULT.getMappings());
        this.observers.clear();
        this.observers.add( mapper );
        this.observers.addAll(observers);
    }

    public synchronized void deconfigure() {
        setMarks(Marker.DEFAULT.getMappings());
    }

    public ClientUtils.SourceCodeSelection[] getMarkerMethods() {
        synchronized (markGuard) {
            if (marks == null) {
                return new ClientUtils.SourceCodeSelection[0];
            }

            ClientUtils.SourceCodeSelection[] methods = new ClientUtils.SourceCodeSelection[marks.length];

            for (int i = 0; i < marks.length; i++) {
                methods[i] = marks[i].markMask;
            }

            return methods;
        }
    }

    public int getNMarks() {
        synchronized (markGuard) {
            return (marks != null) ? marks.length : 0;
        }
    }

    public Mark markMethod(int methodId, ProfilingSessionStatus status) {
        synchronized(mapper) {
            return mapper.getMark(methodId, status);
        }
    }

    Mark mark(int methodId, ProfilingSessionStatus status) {
        ClientUtils.SourceCodeSelection method = null;

        synchronized (markGuard) {
            if (marks == null || marks.length == 0 || status == null) {
                return Mark.DEFAULT;
            }

            status.beginTrans(false);

            try {
                String[] cNames = status.getInstrMethodClasses();
                String[] mNames = status.getInstrMethodNames();
                String[] sigs = status.getInstrMethodSignatures();
                
                if (mNames.length <= methodId || cNames.length <= methodId || sigs.length <= methodId) {
                    int maxMid = Math.min(Math.min(mNames.length, cNames.length), sigs.length);
                    LOGGER.log(Level.WARNING, INVALID_MID, new Object[]{methodId, maxMid});
                } else {
                    method = new ClientUtils.SourceCodeSelection(cNames[methodId],
                                                                 mNames[methodId],
                                                                 sigs[methodId]);
                }
            } finally {
                status.endTrans();
            }

            if (method != null) {
                String methodSig = method.toFlattened();

                for (MarkMapping mark : marks) {
                    if (methodSig.startsWith(mark.markSig)) {
                        return mark.mark;
                    }
                }
            }

            return Mark.DEFAULT;
        }
    }

    private void setMarks(MarkMapping[] marks) {
        boolean stateChange = false;

        synchronized (markGuard) {
            stateChange = !Arrays.equals(this.marks,marks);
            this.marks = marks;
        }
        if (stateChange) {
            fireStateChanged();
        }
    }

    private void fireStateChanged() {
        for (Iterator iter = observers.iterator(); iter.hasNext();) {
            ((StateObserver) iter.next()).stateChanged(this);
        }
    }
}
