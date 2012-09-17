/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;


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
    private static final String MEMORY_SAMPLED_MSG = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.memory.Bundle").getString("SampledMemoryResultsSnapshot_MemorySamledMsg"); // NOI18N
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
        return PresoObjAllocCCTNode.createPresentationCCTFromSnapshot(this, rootNode, getClassName(classId));
    }

    void debugValues() {
        super.debugValues();
        LOGGER.finest("objectsCounts.length: " + debugLength(liveObjectsCounts)); // NOI18N
    }
}
