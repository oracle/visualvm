/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
import org.netbeans.lib.profiler.global.ProfilingSessionStatus;
import org.netbeans.lib.profiler.results.ResultsSnapshot;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.logging.Level;


/**
 * Results snapshot for Memory Profiling.
 *
 * @author Ian Formanek
 */
public abstract class MemoryResultsSnapshot extends ResultsSnapshot {
    /***************************************************************************
    +------------------------------------------------------------------------------+
    | Profiler memory snapshot format description                                  |
    +------------------------------------------------------------------------------+
    int         version
    long        timestamp
    long        duration
    int         # profiled classes
    ===> for(# profiled classes)
    string      class name
    long        object size per class
    <===
    boolean     contains stacktraces
    int         # stacktraces
    ===> for(# stacktraces)
    :::> load node
    int         type (RuntimeMemoryCCTNode.TYPE_RuntimeMemoryCCTNode,
                      RuntimeMemoryCCTNode.TYPE_RuntimeObjAllocTermCCTNode,
                      RuntimeMemoryCCTNode.RuntimeObjLivenessTermCCTNode)
    int         methodId
    int         # children
    ======> for(# children)
    >load node<
    <=====
    <::: load node
    <===
    ***************************************************************************/

    private JMethodIdTable table;
    private String[] classNames;

    /** [0-nProfiledClasses] total size in bytes for tracked instances of this class */
    private long[] objectsSizePerClass;

    /** [0-nProfiledClasses] class Id -> root of its allocation traces tree */
    private RuntimeMemoryCCTNode[] stacksForClasses;

    /** total number of profiled classes */
    private int nProfiledClasses;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public MemoryResultsSnapshot() {
    } // No-arg constructor needed for above serialization methods to work

    public MemoryResultsSnapshot(long beginTime, long timeTaken, MemoryCCTProvider provider, ProfilerClient client)
                          throws ClientUtils.TargetAppOrVMTerminated {
        super(beginTime, timeTaken);

        // TODO [performance]: profile for performance - specifically which of the actions below is most time consuming
        ProfilingSessionStatus status = client.getStatus();
        status.beginTrans(false);

        try {
            performInit(client, provider);
            
            nProfiledClasses = provider.getNProfiledClasses();

            int len = 0;

            if (provider.getObjectsSizePerClass() != null) {
                //System.err.println("mcgb.objectsSizePerClass len is: "+mcgb.objectsSizePerClass.length);
                len = provider.getObjectsSizePerClass().length;
                objectsSizePerClass = new long[len];
                System.arraycopy(provider.getObjectsSizePerClass(), 0, objectsSizePerClass, 0, len);
            } /*else {
               System.err.println("mcgb.objectsSizePerClass is NULL");
               }   */
            String[] s_classNames = status.getClassNames();
            //      len = s_classNames.length;
            len = nProfiledClasses;
            //System.err.println("status.classNames.length is: "+len );
            classNames = new String[len];
            System.arraycopy(s_classNames, 0, classNames, 0, len);

            //      System.out.println("Created snapshot [" + timeTaken + "] with " + classNames.length + " classes; nProfiledClasses = " + nProfiledClasses);
            if ((provider.getStacksForClasses() != null) && checkContainsStacks(provider.getStacksForClasses())) {
                stacksForClasses = new RuntimeMemoryCCTNode[provider.getStacksForClasses().length];

                for (int i = 0; i < stacksForClasses.length; i++) {
                    if (provider.getStacksForClasses()[i] != null) {
                        stacksForClasses[i] = (RuntimeMemoryCCTNode) provider.getStacksForClasses()[i].clone();
                    }
                }

                table = new JMethodIdTable(JMethodIdTable.getDefault());
            }
        } finally {
            status.endTrans();

            if (LOGGER.isLoggable(Level.FINEST)) {
                debugValues();
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getClassName(int classId) {
        return classNames[classId];
    }

    public String[] getClassNames() {
        return classNames;
    }

    public JMethodIdTable getJMethodIdTable() {
        return table;
    }

    public int getNProfiledClasses() {
        return nProfiledClasses;
    }

    public long[] getObjectsSizePerClass() {
        return objectsSizePerClass;
    }

    public boolean containsStacks() {
        return stacksForClasses != null;
    }

    /**
     * Creates a presentation-time allocation stack traces CCT for given classId.
     *
     * @param classId                       Class ID of the class whose allocation stack traces we request
     * @param dontShowZeroLiveObjAllocPaths If true, allocation paths with zero live objects will not be included in CCT
     * @return presentation-time CCT with allocation stack traces or null if none are available
     */
    public PresoObjAllocCCTNode createPresentationCCT(int classId, boolean dontShowZeroLiveObjAllocPaths) {
        if (stacksForClasses == null) {
            return null;
        }

        RuntimeMemoryCCTNode rootNode = stacksForClasses[classId];

        if (rootNode == null) {
            return null;
        }

        return createPresentationCCT(rootNode, classId, dontShowZeroLiveObjAllocPaths);
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        nProfiledClasses = in.readInt();
        classNames = new String[nProfiledClasses];
        objectsSizePerClass = new long[nProfiledClasses];

        for (int i = 0; i < nProfiledClasses; i++) {
            classNames[i] = in.readUTF();
            objectsSizePerClass[i] = in.readLong();
        }

        if (in.readBoolean()) {
            int len = in.readInt();
            //System.err.println("Read len: " +len);
            stacksForClasses = new RuntimeMemoryCCTNode[len];

            for (int i = 0; i < len; i++) {
                int type = in.readInt();

                //System.err.println("  [" + i + "] = " + type);
                if (type != 0) {
                    stacksForClasses[i] = RuntimeMemoryCCTNode.create(type);
                    stacksForClasses[i].readFromStream(in);
                }
            }

            if (in.readBoolean()) {
                table = new JMethodIdTable();
                table.readFromStream(in);
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            debugValues();
        }
    }

    //---- Serialization support
    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        out.writeInt(nProfiledClasses);

        for (int i = 0; i < nProfiledClasses; i++) {
            out.writeUTF(classNames[i]);
            out.writeLong(objectsSizePerClass[i]);
        }

        out.writeBoolean(stacksForClasses != null);

        if (stacksForClasses != null) {
            out.writeInt(stacksForClasses.length);

            //.err.println("Stored len: " +stacksForClasses.length);
            for (int i = 0; i < stacksForClasses.length; i++) {
                if (stacksForClasses[i] == null) {
                    //System.err.println("  [" + i + "] = 0");
                    out.writeInt(0);
                } else {
                    out.writeInt(stacksForClasses[i].getType());
                    //System.err.println("  [" + i + "] = " + stacksForClasses[i].getType());
                    stacksForClasses[i].writeToStream(out);
                }
            }

            out.writeBoolean(table != null);

            if (table != null) {
                table.writeToStream(out);
            }
        }
    }

    /**
     * Will create presentation CCT for call stacks for given root node.
     *
     * @param rootNode                      The root node that contains allocation stack traces data
     * @param classId                       Id of class whose allocations we are requesting
     * @param dontShowZeroLiveObjAllocPaths if true, allocation paths with zero live objects will not be included
     * @return a non-null instance of the root of presentation-time allocations CCT
     */
    protected abstract PresoObjAllocCCTNode createPresentationCCT(RuntimeMemoryCCTNode rootNode, int classId,
                                                                  boolean dontShowZeroLiveObjAllocPaths);

    protected abstract void performInit(ProfilerClient client, MemoryCCTProvider provider)
                                 throws ClientUtils.TargetAppOrVMTerminated;

    private boolean checkContainsStacks(RuntimeMemoryCCTNode[] stacksForClasses) {
        for (int i = 0; i < stacksForClasses.length; i++) {
            RuntimeMemoryCCTNode stacksForClass = stacksForClasses[i];

            if (stacksForClass == null) {
                continue;
            }

            if (stacksForClass instanceof RuntimeObjAllocTermCCTNode) {
                continue;
            }

            if (stacksForClass instanceof RuntimeObjLivenessTermCCTNode) {
                continue;
            }

            return true;
        }

        return false; // no data but term nodes or nulls
    }

    void debugValues() {
        LOGGER.finest("nProfiledClasses: " + nProfiledClasses); // NOI18N
        LOGGER.finest("stacksForClasses.length: " + debugLength(stacksForClasses)); // NOI18N
        LOGGER.finest("objectsSizePerClass.length: " + debugLength(objectsSizePerClass));
        LOGGER.finest("classNames.length: " + debugLength(classNames)); // NOI18N
        LOGGER.finest("table: " + ((table == null) ? "null" : table.debug())); // NOI18N
    }
}
