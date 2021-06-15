/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;


/**
 * Results snapshot for Sampled Memory Profiling.
 *
 * @author Ian Formanek
 * @author Tomas Hurka
 */
public class SampledMemoryResultsSnapshot extends MemoryResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MEMORY_SAMPLED_MSG = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.memory.Bundle").getString("SampledMemoryResultsSnapshot_MemorySamledMsg"); // NOI18N
                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] liveObjectsCounts;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SampledMemoryResultsSnapshot() {
    } // No-arg constructor needed for above serialization methods to work

    public SampledMemoryResultsSnapshot(long beginTime, long timeTaken, ProfilerClient client)
                               throws ClientUtils.TargetAppOrVMTerminated {
        super(beginTime, timeTaken, null, client);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getObjectsCounts() {
        return liveObjectsCounts;
    }

    public void performInit(ProfilerClient client, MemoryCCTProvider provider)
                     throws ClientUtils.TargetAppOrVMTerminated {
        HeapHistogram histo = client.getHeapHistogram();
        Set<HeapHistogram.ClassInfo> info = histo.getHeapHistogram();
        
        nProfiledClasses = info.size();
        classNames = new String[nProfiledClasses];
        objectsSizePerClass = new long[nProfiledClasses];
        liveObjectsCounts = new int[nProfiledClasses];
        int i = 0;
        for (HeapHistogram.ClassInfo ci : info) {
            classNames[i] = ci.getName();
            objectsSizePerClass[i] = ci.getBytes();
            liveObjectsCounts[i] = (int)ci.getInstancesCount();
            i++;
        }
    }
    
    public SampledMemoryResultsSnapshot createDiff(MemoryResultsSnapshot snapshot) {
        if (!(snapshot instanceof SampledMemoryResultsSnapshot)) return null;
        return new SampledMemoryResultsDiff(this, (SampledMemoryResultsSnapshot)snapshot);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        int len = in.readInt();
        liveObjectsCounts = new int[len];

        for (int i = 0; i < len; i++) {
            liveObjectsCounts[i] = in.readInt();
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    public String toString() {
        return MessageFormat.format(MEMORY_SAMPLED_MSG, new Object[] { super.toString() });
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(liveObjectsCounts.length);

        for (int i = 0; i < liveObjectsCounts.length; i++) {
            out.writeInt(liveObjectsCounts[i]);
        }
    }

    protected PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                         boolean dontShowZeroLiveObjAllocPaths) {
        return PresoObjAllocCCTNode.createPresentationCCTFromSnapshot(getJMethodIdTable(), rootNode, getClassName(classId));
    }

    void debugValues() {
        super.debugValues();
        LOGGER.finest("objectsCounts.length: " + debugLength(liveObjectsCounts)); // NOI18N
    }
}
