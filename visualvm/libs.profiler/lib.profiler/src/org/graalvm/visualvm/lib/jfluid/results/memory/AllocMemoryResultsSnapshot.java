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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.graalvm.visualvm.lib.jfluid.ProfilerClient;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;


/**
 * Results snapshot for Allocations Memory Profiling.
 *
 * @author Ian Formanek
 */
public class AllocMemoryResultsSnapshot extends MemoryResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MEMORY_ALLOC_MSG = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.memory.Bundle").getString("AllocMemoryResultsSnapshot_MemoryAllocMsg"); // NOI18N
                                                                                                                    // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] objectsCounts;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AllocMemoryResultsSnapshot() {
    } // No-arg constructor needed for above serialization methods to work

    public AllocMemoryResultsSnapshot(long beginTime, long timeTaken, MemoryCCTProvider provider, ProfilerClient client)
                               throws ClientUtils.TargetAppOrVMTerminated {
        super(beginTime, timeTaken, provider, client);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int[] getObjectsCounts() {
        return objectsCounts;
    }

    public void performInit(ProfilerClient client, MemoryCCTProvider provider)
                     throws ClientUtils.TargetAppOrVMTerminated {
        super.performInit(client, provider);
        
        int[] cnts = client.getAllocatedObjectsCountResults();
        objectsCounts = new int[cnts.length];
        System.arraycopy(cnts, 0, objectsCounts, 0, cnts.length);

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }
    
    public AllocMemoryResultsSnapshot createDiff(MemoryResultsSnapshot snapshot) {
        if (!(snapshot instanceof AllocMemoryResultsSnapshot)) return null;
        return new AllocMemoryResultsDiff(this, (AllocMemoryResultsSnapshot)snapshot);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        int len = in.readInt();
        objectsCounts = new int[len];

        for (int i = 0; i < len; i++) {
            objectsCounts[i] = in.readInt();
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    public String toString() {
        return MessageFormat.format(MEMORY_ALLOC_MSG, new Object[] { super.toString() });
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(objectsCounts.length);

        for (int i = 0; i < objectsCounts.length; i++) {
            out.writeInt(objectsCounts[i]);
        }
    }

    protected PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                         boolean dontShowZeroLiveObjAllocPaths) {
        return PresoObjAllocCCTNode.createPresentationCCTFromSnapshot(getJMethodIdTable(), rootNode, getClassName(classId));
    }

    void debugValues() {
        super.debugValues();
        LOGGER.finest("objectsCounts.length: " + debugLength(objectsCounts)); // NOI18N
    }
}
