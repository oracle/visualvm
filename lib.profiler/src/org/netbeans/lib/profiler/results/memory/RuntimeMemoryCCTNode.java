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

import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A node of the run time Memory Profiling Calling Context Tree (CCT). Unlike the presentation-time CCT, this one
 * contains information in the form that is quickly updateable at run time, but needs further processing for
 * proper presentation. Instances of class RuntimeMemoryCCTNode are used only as non-terminal nodes, and contain
 * minimum information to save space. The information such as the total number of calls, size of allocated objects,
 * etc., which can be calculated  for intermediate nodes if known for terminal nodes, is contained, in runtime CCT,
 * only in specialized terminal nodes (instances of classes RuntimeObjAllocTermCCTNode and
 * RuntimeObjLivenessTermCCTNode).
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class RuntimeMemoryCCTNode implements Cloneable, RuntimeCCTNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    protected static final int TYPE_RuntimeMemoryCCTNode = 1;
    protected static final int TYPE_RuntimeObjAllocTermCCTNode = 2;
    protected static final int TYPE_RuntimeObjLivenessTermCCTNode = 3;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /** Children nodes in the forward stack trace tree. This fiels can have three different values depending on the
     * number of children:
     *   null if there are no children
     *   instance of RuntimeMemoryCCTNode if there is exactly one child
     *   instance of RuntimeMemoryCCTNode[] if there are multiple children
     * This is purely a memory consumption optimization, which typically saves about 80% of memory, since most allocation
     * stack traces are a sequence of single-child nodes, and in such case we remove the need to create a one-item array
     */
    Object children;

    /** unique Id of method - jMethodId from JVM (see MemoryCallGraphBuilder.getNamesForJMethodIds) */
    int methodId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** For I/O only */
    protected RuntimeMemoryCCTNode() {
    }

    RuntimeMemoryCCTNode(int methodId) {
        this.methodId = methodId;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getType() {
        return TYPE_RuntimeMemoryCCTNode;
    }

    public static RuntimeMemoryCCTNode create(int type) {
        switch (type) {
            case TYPE_RuntimeMemoryCCTNode:
                return new RuntimeMemoryCCTNode();
            case TYPE_RuntimeObjAllocTermCCTNode:
                return new RuntimeObjAllocTermCCTNode();
            case TYPE_RuntimeObjLivenessTermCCTNode:
                return new RuntimeObjLivenessTermCCTNode();
        }

        throw new IllegalArgumentException("Illegal type: " + type); // NOI18N
    }

    public RuntimeMemoryCCTNode addNewChild(int methodId) {
        if (children == null) {
            children = new RuntimeMemoryCCTNode(methodId);

            return (RuntimeMemoryCCTNode) children;
        } else {
            RuntimeMemoryCCTNode[] ar = addChildEntry();

            return (ar[ar.length - 1] = new RuntimeMemoryCCTNode(methodId));
        }
    }

    public void attachNodeAsChild(RuntimeMemoryCCTNode node) {
        if (children == null) {
            children = node;
        } else {
            RuntimeMemoryCCTNode[] ar = addChildEntry();
            ar[ar.length - 1] = node;
        }
    }

    public Object clone() {
        try {
            RuntimeMemoryCCTNode ret = (RuntimeMemoryCCTNode) super.clone();

            if (children != null) {
                if (children instanceof RuntimeMemoryCCTNode) {
                    ret.children = ((RuntimeMemoryCCTNode) children).clone();
                } else {
                    RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) children;
                    ret.children = new RuntimeMemoryCCTNode[ar.length];

                    for (int i = 0; i < ar.length; i++) {
                        ((RuntimeMemoryCCTNode[]) ret.children)[i] = (RuntimeMemoryCCTNode) ar[i].clone();
                    }
                }
            }

            return ret;
        } catch (CloneNotSupportedException e) {
            throw new InternalError("Clone should never fail"); // NOI18N
        }
    }

    public void readFromStream(DataInputStream in) throws IOException {
        methodId = in.readInt();

        int len = in.readInt();

        if (len == 0) {
            children = null;
        } else if (len == 1) {
            int type = in.readInt();
            children = RuntimeMemoryCCTNode.create(type);
            ((RuntimeMemoryCCTNode) children).readFromStream(in);
        } else {
            RuntimeMemoryCCTNode[] ar = new RuntimeMemoryCCTNode[len];

            for (int i = 0; i < len; i++) {
                int type = in.readInt();
                ar[i] = RuntimeMemoryCCTNode.create(type);
                ar[i].readFromStream(in);
            }

            children = ar;
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        out.writeInt(methodId);

        if (children == null) {
            out.writeInt(0);
        } else if (children instanceof RuntimeMemoryCCTNode) {
            out.writeInt(1);
            out.writeInt(((RuntimeMemoryCCTNode) children).getType());
            ((RuntimeMemoryCCTNode) children).writeToStream(out);
        } else {
            RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) children;
            out.writeInt(ar.length);

            for (int i = 0; i < ar.length; i++) {
                out.writeInt(ar[i].getType());
                ar[i].writeToStream(out);
            }
        }
    }

    private RuntimeMemoryCCTNode[] addChildEntry() {
        assert (children != null);

        if (children instanceof RuntimeMemoryCCTNode) {
            // currently just single child
            RuntimeMemoryCCTNode[] ret = new RuntimeMemoryCCTNode[2];
            ret[0] = (RuntimeMemoryCCTNode) children;
            children = ret;

            return ret;
        } else {
            RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) children;
            RuntimeMemoryCCTNode[] newchildren = new RuntimeMemoryCCTNode[ar.length + 1];
            System.arraycopy(ar, 0, newchildren, 0, ar.length);
            children = newchildren;

            return newchildren;
        }
    }
}
