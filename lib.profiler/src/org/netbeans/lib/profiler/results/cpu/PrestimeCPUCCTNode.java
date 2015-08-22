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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.tree.TreeNode;
import org.netbeans.lib.profiler.results.FilterSortSupport;


/**
 * Base Presentation-Time CPU Profiling Calling Context Tree (CCT) Node class.
 * Subclasses provide an implementation that is backed by the flattened tree data array in CPUCCTContainer
 * (PrestimeCPUCCTNodeBacked) and the one that contains all the data in the node itself (PrestimeCPUCCTNodeFree).
 *
 * @author Misha Dmitriev
 */
public abstract class PrestimeCPUCCTNode extends CCTNode implements Cloneable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SELF_TIME_STRING;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.cpu.Bundle"); // NOI18N
        SELF_TIME_STRING = messages.getString("PrestimeCPUCCTNode_SelfTimeString"); // NOI18N
    }
    
    protected static final char MASK_SELF_TIME_NODE = 0x1;
    protected static final char MASK_CONTEXT_CALLS_NODE = 0x2;
    protected static final char MASK_THREAD_NODE = 0x4;
//    protected static final char MASK_FILTERED_NODE = 0x8;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_TIME_0 = 2;
    public static final int SORT_BY_TIME_1 = 3;
    public static final int SORT_BY_INVOCATIONS = 4;
    
    
    public static final PrestimeCPUCCTNode EMPTY = new PrestimeCPUCCTNode() {
        PrestimeCPUCCTNode createCopy() { return null; }
        
        public PrestimeCPUCCTNode getChild(int index) { return null; }
        public PrestimeCPUCCTNode[] getChildren() { return new PrestimeCPUCCTNode[0]; }
        public int getIndexOfChild(Object child) { return -1; }
        public int getNChildren() { return 0; }
        public String getNodeName() { return ""; } // NOI18N
        public long getTime() { return 0; }
        public double getTimeInPerCent() { return 0; }
        public long getWaits() { return 0; }

        public int getMethodId() { return -1; }
        public int getNCalls() { return -1; }
        public long getSleepTime0() { return -1; }
        public int getThreadId() { return -1; }
        public long getTotalTime0() { return -1; }
        public float getTotalTime0InPerCent() { return -1; }
        public long getTotalTime1() { return -1; }
        public float getTotalTime1InPerCent() { return -1; }
        public long getWaitTime0() { return -1; }
        public void sortChildren(int sortBy, boolean sortOrder) {}
        public TreeNode getChildAt(int childIndex) { return null; }
        public int getChildCount() { return 0; }
        public CCTNode getParent() { return null; }
        public int getIndex(TreeNode node) { return -1; }
        public boolean getAllowsChildren() { return false; }
        public boolean isLeaf() { return true; }
        public Enumeration children() { return null; }
    };

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected CPUCCTContainer container;
    protected PrestimeCPUCCTNode parent;
    protected PrestimeCPUCCTNode[] children;
    protected char flags; // Non-zero for several special kinds of nodes, per MASK_* bit constants above
    
    private String nodeName;
    
    protected int methodId;
    protected int nCalls;
    protected long sleepTime0;

    /** The same class used for both standard and "extended" nodes (collecting one or two timestamps) */
    protected long totalTime0;
    protected long totalTime1;

    /** The same class used for both standard and "extended" nodes (collecting one or two timestamps) */
    protected long waitTime0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected PrestimeCPUCCTNode() {
    }

    /**
     * Constructor for creating normal nodes representing methods
     */
    protected PrestimeCPUCCTNode(CPUCCTContainer container, PrestimeCPUCCTNode parent, int methodId) {
        this.container = container;
        this.parent = parent;
        this.methodId = methodId;
    }
    
    // --- Cloning support ---
    
    PrestimeCPUCCTNode createCopy() {
        throw new UnsupportedOperationException("Cannot be called directly on " + getClass().getName()); // NOI18N
    };
    
    void setupCopy(PrestimeCPUCCTNode node) {
        node.container = container;
        node.parent = parent;
        node.children = children;
        
        node.flags = flags;

        node.nodeName = nodeName;
        node.methodId = methodId;
        
        node.nCalls = nCalls;
        
        node.sleepTime0 = sleepTime0;
        node.totalTime0 = totalTime0;
        node.totalTime1 = totalTime1;
        node.waitTime0 = waitTime0;
    }
    
    // --- Filtering support ---
    
    protected void setupFilteredNode(PrestimeCPUCCTNode filtered) {
        setupCopy(filtered);
        
        filtered.setFilteredNode();
        
        filtered.nodeName = null;
        filtered.methodId = -1;

        Collection<PrestimeCPUCCTNode> _childrenL = resolveChildren(this);
        int nChildren = _childrenL.size();
        filtered.children = _childrenL.toArray(new PrestimeCPUCCTNode[nChildren]);
    }
    
    public void merge(CCTNode node) {
        if (node instanceof PrestimeCPUCCTNode) {
            PrestimeCPUCCTNode _node = (PrestimeCPUCCTNode)node;
            
            addNCalls(_node.getNCalls());
            addSleepTime0(_node.getSleepTime0());
            addTotalTime0(_node.getTotalTime0());
            addTotalTime1(_node.getTotalTime1());
            addWaitTime0(_node.getWaitTime0());

            List<CCTNode> ch = new ArrayList();
            
            // Include current children
            PrestimeCPUCCTNode[] _children = (PrestimeCPUCCTNode[])getChildren();
            if (_children != null) for (PrestimeCPUCCTNode child : _children)
                    ch.add(child.createCopy());
            
            // Add or merge new children
            PrestimeCPUCCTNode[] __children = (PrestimeCPUCCTNode[])node.getChildren();
            if (__children != null) for (PrestimeCPUCCTNode child : __children) {
                if (child != null && !child.isSelfTimeNode()) {
                    int idx = ch.indexOf(child);
                    if (idx == -1) ch.add(child.createCopy());
                    else ch.get(idx).merge(child);
                }
            }
            
            children = ch.toArray(new PrestimeCPUCCTNode[ch.size()]);
        }
    }

    protected static Collection<PrestimeCPUCCTNode> resolveChildren(PrestimeCPUCCTNode node) {
        List<PrestimeCPUCCTNode> chldrn = new ArrayList();
        PrestimeCPUCCTNode[] chld = (PrestimeCPUCCTNode[])node.getChildren();
        if (chld != null) for (PrestimeCPUCCTNode chl : chld)
            if (!chl.isSelfTimeNode()) chldrn.add(chl);
        return chldrn;
    }
    
    // ---

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CCTNode getChild(int index) {
        return children[index];
    }

    public CCTNode[] getChildren() {
        return children;
    }
    
    public int getNChildren() {
        return (children != null) ? children.length : 0;
    }

    public CPUCCTContainer getContainer() {
        return container;
    }
    
    
    public int getMethodId() {
        return methodId;
    }
    
    public int getNCalls() {
        return nCalls;
    }
    
    public long getSleepTime0() {
        return sleepTime0;

        // TODO: [wait] self time node?
    }
    
    public int getThreadId() {
        return container.getThreadId();
    }
    
    public long getTotalTime0() {
        return totalTime0;
    }
    
    public long getTotalTime1() {
        return totalTime1;
    }
    
    public long getWaitTime0() {
        return waitTime0; // TODO [wait]
    }
    
    
    public void addNCalls(int addCalls) {
        nCalls += addCalls;
    }

    public void addSleepTime0(long addTime) {
        sleepTime0 += addTime;
    }

    public void addTotalTime0(long addTime) {
        totalTime0 += addTime;
    }

    public void addTotalTime1(long addTime) {
        totalTime1 += addTime;
    }

    public void addWaitTime0(long addTime) {
        waitTime0 += addTime;
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
    
    protected void resetChildren() {
        if (children != null)
            for (PrestimeCPUCCTNode n : children)
                n.resetChildren();
    }

//    public abstract int getMethodId();
//
//    public abstract int getNCalls();
//
//    public abstract int getNChildren();
    
    public String getNodeName() {
        if (nodeName == null) nodeName = computeNodeName();
        return nodeName;
    }

    protected String computeNodeName() {
        if (isFiltered()) {
            return FilterSortSupport.FILTERED_OUT_LBL;
        } else if (isSelfTimeNode()) {
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
        return MethodNameFormatterFactory.getDefault().getFormatter().formatMethodName(
                                          methodClassNameAndSig[0], methodClassNameAndSig[1],
                                          methodClassNameAndSig[2]).toFormatted();

//        if (isContextCallsNode()) {
//            return MessageFormat.format(FROM_MSG, new Object[] { res });
//        } else {
//            return res;
//        }
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof PrestimeCPUCCTNode)) return false;
        PrestimeCPUCCTNode oo = (PrestimeCPUCCTNode)o;
        
        // Handle root
        if (parent == null) return oo.parent == null;
        
        // Handle toplevel thread nodes
        if (isThreadNode()) return container.getThreadId() == oo.container.getThreadId();
        
        // Handle self time nodes
        if (isSelfTimeNode()) return oo.isSelfTimeNode();
        
        // Handle filtered-out containers
        if (isFiltered()) return oo.isFiltered();
        
        // Handle "when called from" containers
        if (isContextCallsNode()) return getMethodId() == oo.getMethodId();
        
        // Handle regular method nodes
        return getMethodId() == oo.getMethodId();
        
//        // Expected fallback for packages/classes view (no methodIDs)
//        return getNodeName().equals(((PrestimeCPUCCTNode)o).getNodeName());
    }
    
    public int hashCode() {        
        // Handle root
        if (parent == null) return 1;
        
        // Handle toplevel thread nodes
        if (isThreadNode()) return container.getThreadId();
        
        // Handle self time nodes
        if (isSelfTimeNode()) return -1;
        
        // Handle filtered-out containers
        if (isFiltered()) return -10;
        
        // Handle "when called from" containers
        if (isContextCallsNode()) return Integer.MIN_VALUE + getMethodId();
        
        // Handle regular method nodes
        return getMethodId();
        
//        // Expected fallback for packages/classes view (no methodIDs)
//        return getNodeName().hashCode();
    }

    public CCTNode getParent() {
        return parent;
    }

//    public abstract long getSleepTime0();
//
//    public abstract int getThreadId();

    public void setSelfTimeNode() {
        flags |= MASK_SELF_TIME_NODE;
    }

    public boolean isSelfTimeNode() {
        return (flags & MASK_SELF_TIME_NODE) != 0;
    }

    public void setThreadNode() {
        flags |= MASK_THREAD_NODE;
    }

    public boolean isThreadNode() {
        return (flags & MASK_THREAD_NODE) != 0;
    }
    
//    public void setFilteredNode() {
//        flags |= MASK_FILTERED_NODE;
//    }
//    
//    public void resetFilteredNode() {
//        flags &= ~MASK_FILTERED_NODE;
//    }
//
//    public boolean isFilteredNode() {
//        return (flags & MASK_FILTERED_NODE) != 0;
//    }

//    public abstract long getTotalTime0();

    public abstract float getTotalTime0InPerCent();

//    public abstract long getTotalTime1();

    public abstract float getTotalTime1InPerCent();

//    public abstract long getWaitTime0();

    public int getIndexOfChild(Object child) {
//        if (children == null) JOptionPane.showMessageDialog(null, "Node: " + this + "\nindex of child: " + child);\
        if (getNChildren() == 0) return -1;
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
    public void sortChildren(int sortBy, boolean sortOrder) {};
    
    public String toString() {
        return getNodeName();
    }

//    protected void doSortChildren(int sortBy, boolean sortOrder) {
//        int len = children.length;
//
//        for (int i = 0; i < len; i++) {
//            children[i].sortChildren(sortBy, sortOrder);
//        }
//
//        if (len > 1) {
//            switch (sortBy) {
//                case SORT_BY_NAME:
//                    sortChildrenByName(sortOrder);
//
//                    break;
//                case SORT_BY_TIME_0:
//                    sortChildrenByTime0(sortOrder);
//
//                    break;
//                case SORT_BY_TIME_1:
//                    sortChildrenByTime1(sortOrder);
//
//                    break;
//                case SORT_BY_INVOCATIONS:
//                    sortChildrenByInvocations(sortOrder);
//
//                    break;
//            }
//        }
//    }
//
//    protected void sortChildrenByInvocations(boolean sortOrder) {
//        int len = children.length;
//        int[] values = new int[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].getNCalls();
//        }
//
//        sortInts(values, sortOrder);
//    }
//
//    protected void sortChildrenByName(boolean sortOrder) {
//        int len = children.length;
//        String[] values = new String[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].getNodeName();
//        }
//
//        sortStrings(values, sortOrder);
//    }
//
//    protected void sortChildrenByTime0(boolean sortOrder) {
//        int len = children.length;
//        long[] values = new long[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].getTotalTime0();
//        }
//
//        sortLongs(values, sortOrder);
//    }
//
//    protected void sortChildrenByTime1(boolean sortOrder) {
//        int len = children.length;
//        long[] values = new long[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].getTotalTime1();
//        }
//
//        sortLongs(values, sortOrder);
//    }
//
//    protected void sortInts(int[] values, boolean sortOrder) {
//        int len = values.length;
//
//        // Just the insertion sort - we will never get really large arrays here
//        for (int i = 0; i < len; i++) {
//            for (int j = i; (j > 0) && ((sortOrder == false) ? (values[j - 1] < values[j]) : (values[j - 1] > values[j])); j--) {
//                int tmp = values[j];
//                values[j] = values[j - 1];
//                values[j - 1] = tmp;
//
//                PrestimeCPUCCTNode tmpCh = children[j];
//                children[j] = children[j - 1];
//                children[j - 1] = tmpCh;
//            }
//        }
//    }
//
//    protected void sortLongs(long[] values, boolean sortOrder) {
//        int len = values.length;
//
//        // Just the insertion sort - we will never get really large arrays here
//        for (int i = 0; i < len; i++) {
//            for (int j = i; (j > 0) && ((sortOrder == false) ? (values[j - 1] < values[j]) : (values[j - 1] > values[j])); j--) {
//                long tmp = values[j];
//                values[j] = values[j - 1];
//                values[j - 1] = tmp;
//
//                PrestimeCPUCCTNode tmpCh = children[j];
//                children[j] = children[j - 1];
//                children[j - 1] = tmpCh;
//            }
//        }
//    }
//
//    protected void sortStrings(String[] values, boolean sortOrder) {
//        int len = values.length;
//
//        // Just the insertion sort - we will never get really large arrays here
//        for (int i = 0; i < len; i++) {
//            for (int j = i;
//                     (j > 0)
//                     && ((sortOrder == false) ? (values[j - 1].compareTo(values[j]) < 0) : (values[j - 1].compareTo(values[j]) > 0));
//                     j--) {
//                String tmp = values[j];
//                values[j] = values[j - 1];
//                values[j - 1] = tmp;
//
//                PrestimeCPUCCTNode tmpCh = children[j];
//                children[j] = children[j - 1];
//                children[j - 1] = tmpCh;
//            }
//        }
//    }
}
