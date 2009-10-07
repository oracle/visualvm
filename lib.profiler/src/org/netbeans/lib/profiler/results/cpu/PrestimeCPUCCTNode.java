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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * Base Presentation-Time CPU Profiling Calling Context Tree (CCT) Node class.
 * Subclasses provide an implementation that is backed by the flattened tree data array in CPUCCTContainer
 * (PrestimeCPUCCTNodeBacked) and the one that contains all the data in the node itself (PrestimeCPUCCTNodeFree).
 *
 * @author Misha Dmitriev
 */
public abstract class PrestimeCPUCCTNode implements CCTNode, Cloneable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle"); // NOI18N
    private static final String SELF_TIME_STRING = messages.getString("PrestimeCPUCCTNode_SelfTimeString"); // NOI18N
    private static final String FROM_MSG = messages.getString("PrestimeCPUCCTNode_FromMsg"); // NOI18N
                                                                                             // -----
    protected static final char MASK_SELF_TIME_NODE = 0x1;
    protected static final char MASK_CONTEXT_CALLS_NODE = 0x2;
    protected static final char MASK_THREAD_NODE = 0x4;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_TIME_0 = 2;
    public static final int SORT_BY_TIME_1 = 3;
    public static final int SORT_BY_INVOCATIONS = 4;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUCCTContainer container;
    protected PrestimeCPUCCTNode parent;
    protected PrestimeCPUCCTNode[] children;
    protected char flags; // Non-zero for several special kinds of nodes, per MASK_* bit constants above

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected PrestimeCPUCCTNode() {
    }

    /**
     * Constructor for creating normal nodes representing methods
     */
    protected PrestimeCPUCCTNode(CPUCCTContainer container, PrestimeCPUCCTNode parent) {
        this.container = container;
        this.parent = parent;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public abstract CCTNode getChild(int index);

    public abstract CCTNode[] getChildren();

    public CPUCCTContainer getContainer() {
        return container;
    }

    public void setContextCallsNode() {
        flags = MASK_CONTEXT_CALLS_NODE;
    }

    public boolean isContextCallsNode() {
        return (flags & MASK_CONTEXT_CALLS_NODE) != 0;
    }

    public String[] getMethodClassNameAndSig() {
        if (!isThreadNode()) {
            return container.getMethodClassNameAndSig(getMethodId());
        } else {
            return new String[] { container.getThreadName(), "", "" }; // NOI18N
        }
    }

    public abstract int getMethodId();

    public abstract int getNCalls();

    public abstract int getNChildren();

    public String getNodeName() {
        if (isSelfTimeNode()) {
            return SELF_TIME_STRING;
        } else if (isThreadNode()) {
            return container.getThreadName();
        }

        int methodId = getMethodId();
        String[] methodClassNameAndSig = container.getMethodClassNameAndSig(methodId);

        //    PlainFormattableMethodName format = new PlainFormattableMethodName(
        //        methodClassNameAndSig[0], methodClassNameAndSig[1], methodClassNameAndSig[2]
        //    );
        //    String res = format.getFormattedClassAndMethod();
        String res = MethodNameFormatterFactory.getDefault().getFormatter()
                                               .formatMethodName(methodClassNameAndSig[0], methodClassNameAndSig[1],
                                                                 methodClassNameAndSig[2]).toFormatted();

        if (isContextCallsNode()) {
            return MessageFormat.format(FROM_MSG, new Object[] { res });
        } else {
            return res;
        }
    }

    public CCTNode getParent() {
        return parent;
    }

    public abstract long getSleepTime0();

    public abstract int getThreadId();

    public void setSelfTimeNode() {
        flags = MASK_SELF_TIME_NODE;
    }

    public boolean isSelfTimeNode() {
        return (flags & MASK_SELF_TIME_NODE) != 0;
    }

    public void setThreadNode() {
        flags = MASK_THREAD_NODE;
    }

    public boolean isThreadNode() {
        return (flags & MASK_THREAD_NODE) != 0;
    }

    public abstract long getTotalTime0();

    public abstract float getTotalTime0InPerCent();

    public abstract long getTotalTime1();

    public abstract float getTotalTime1InPerCent();

    public abstract long getWaitTime0();

    public int getIndexOfChild(Object child) {
        for (int i = 0; i < children.length; i++) {
            if ((PrestimeCPUCCTNode) child == children[i]) {
                return i;
            }
        }

        return -1;
    }

    /**
     * This is not equal to doSortChildren below, because the real implementation of sortChildren may need to do some
     * more things, such as generating the children, or deciding to return immediately.
     */
    public abstract void sortChildren(int sortBy, boolean sortOrder);

    public String toString() {
        return getNodeName();
    }

    protected void doSortChildren(int sortBy, boolean sortOrder) {
        int len = children.length;

        for (int i = 0; i < len; i++) {
            children[i].sortChildren(sortBy, sortOrder);
        }

        if (len > 1) {
            switch (sortBy) {
                case SORT_BY_NAME:
                    sortChildrenByName(sortOrder);

                    break;
                case SORT_BY_TIME_0:
                    sortChildrenByTime0(sortOrder);

                    break;
                case SORT_BY_TIME_1:
                    sortChildrenByTime1(sortOrder);

                    break;
                case SORT_BY_INVOCATIONS:
                    sortChildrenByInvocations(sortOrder);

                    break;
            }
        }
    }

    protected void sortChildrenByInvocations(boolean sortOrder) {
        int len = children.length;
        int[] values = new int[len];

        for (int i = 0; i < len; i++) {
            values[i] = children[i].getNCalls();
        }

        sortInts(values, sortOrder);
    }

    protected void sortChildrenByName(boolean sortOrder) {
        int len = children.length;
        String[] values = new String[len];

        for (int i = 0; i < len; i++) {
            values[i] = children[i].getNodeName();
        }

        sortStrings(values, sortOrder);
    }

    protected void sortChildrenByTime0(boolean sortOrder) {
        int len = children.length;
        long[] values = new long[len];

        for (int i = 0; i < len; i++) {
            values[i] = children[i].getTotalTime0();
        }

        sortLongs(values, sortOrder);
    }

    protected void sortChildrenByTime1(boolean sortOrder) {
        int len = children.length;
        long[] values = new long[len];

        for (int i = 0; i < len; i++) {
            values[i] = children[i].getTotalTime1();
        }

        sortLongs(values, sortOrder);
    }

    protected void sortInts(int[] values, boolean sortOrder) {
        int len = values.length;

        // Just the insertion sort - we will never get really large arrays here
        for (int i = 0; i < len; i++) {
            for (int j = i; (j > 0) && ((sortOrder == false) ? (values[j - 1] < values[j]) : (values[j - 1] > values[j])); j--) {
                int tmp = values[j];
                values[j] = values[j - 1];
                values[j - 1] = tmp;

                PrestimeCPUCCTNode tmpCh = children[j];
                children[j] = children[j - 1];
                children[j - 1] = tmpCh;
            }
        }
    }

    protected void sortLongs(long[] values, boolean sortOrder) {
        int len = values.length;

        // Just the insertion sort - we will never get really large arrays here
        for (int i = 0; i < len; i++) {
            for (int j = i; (j > 0) && ((sortOrder == false) ? (values[j - 1] < values[j]) : (values[j - 1] > values[j])); j--) {
                long tmp = values[j];
                values[j] = values[j - 1];
                values[j - 1] = tmp;

                PrestimeCPUCCTNode tmpCh = children[j];
                children[j] = children[j - 1];
                children[j - 1] = tmpCh;
            }
        }
    }

    protected void sortStrings(String[] values, boolean sortOrder) {
        int len = values.length;

        // Just the insertion sort - we will never get really large arrays here
        for (int i = 0; i < len; i++) {
            for (int j = i;
                     (j > 0)
                     && ((sortOrder == false) ? (values[j - 1].compareTo(values[j]) < 0) : (values[j - 1].compareTo(values[j]) > 0));
                     j--) {
                String tmp = values[j];
                values[j] = values[j - 1];
                values[j - 1] = tmp;

                PrestimeCPUCCTNode tmpCh = children[j];
                children[j] = children[j - 1];
                children[j - 1] = tmpCh;
            }
        }
    }
}
