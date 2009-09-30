/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.results.memory;

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;


/**
 * Results snapshot for Allocations Memory Profiling.
 *
 * @author Ian Formanek
 */
public class AllocMemoryResultsSnapshot extends MemoryResultsSnapshot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.memory.Bundle"); // NOI18N
    private static final String MEMORY_ALLOC_MSG = messages.getString("AllocMemoryResultsSnapshot_MemoryAllocMsg"); // NOI18N
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
        int[] cnts = client.getAllocatedObjectsCountResults();
        objectsCounts = new int[cnts.length];
        System.arraycopy(cnts, 0, objectsCounts, 0, cnts.length);

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
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
        return PresoObjAllocCCTNode.createPresentationCCTFromSnapshot(this, rootNode, getClassName(classId));
    }

    void debugValues() {
        super.debugValues();
        LOGGER.finest("objectsCounts.length: " + debugLength(objectsCounts)); // NOI18N
    }
}
