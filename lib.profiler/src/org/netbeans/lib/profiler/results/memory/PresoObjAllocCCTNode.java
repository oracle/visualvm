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

package org.netbeans.lib.profiler.results.memory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.utils.StringUtils;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import java.util.ResourceBundle;
import org.netbeans.lib.profiler.results.FilterSortSupport;
import org.netbeans.lib.profiler.utils.Wildcards;


/**
 * Presentation-Time Memory Profiling Calling Context Tree (CCT) Node. Used "as is" for Object Allocation
 * profiling, and used as a base class for PresoObjLivenessCCTNode. Contains additional functionality
 * to map jmethodIDs (integer identifiers automatically assigned to methods by the JVM, that are returned
 * by stack trace routines) to method names. This includes sending a request to the server to get method
 * names/signatures for given jmethodIDs.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 */
public class PresoObjAllocCCTNode extends CCTNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String VM_ALLOC_CLASS = "org.netbeans.lib.profiler.server.ProfilerRuntimeMemory"; // NOI18N
    public static final String VM_ALLOC_METHOD = "traceVMObjectAlloc"; // NOI18N
    private static final String VM_ALLOC_TEXT = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.memory.Bundle") // NOI18N
    .getString("PresoObjAllocCCTNode_VMAllocMsg"); // NOI18N
    private static final String UKNOWN_NODENAME = ResourceBundle.getBundle("org.netbeans.lib.profiler.results.memory.Bundle") // NOI18N
    .getString("PresoObjAllocCCTNode_UnknownMsg"); // NOI18N
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_ALLOC_OBJ_SIZE = 2;
    public static final int SORT_BY_ALLOC_OBJ_NUMBER = 3;
    
//    protected static final char MASK_FILTERED_NODE = 0x8;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public long nCalls;
    public long totalObjSize;
    protected PresoObjAllocCCTNode parent;
    String className;
    String methodName;
//    String methodSig;
    String nodeName;
    protected PresoObjAllocCCTNode[] children;
    int methodId;
    JMethodIdTable.JMethodIdTableEntry entry;
    
    protected char flags;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    
    public static PresoObjAllocCCTNode rootNode(PresoObjAllocCCTNode[] children) {
        PresoObjAllocCCTNode root = new PresoObjAllocCCTNode();
        root.setChildren(children);
        return root;
    }
    
    public PresoObjAllocCCTNode(String className, long nCalls, long totalObjSize) {
        this.className = className;
        this.nCalls = nCalls;
        this.totalObjSize = totalObjSize;
        
        methodName = Wildcards.ALLWILDCARD;
    }
    
    PresoObjAllocCCTNode() {}
    
    protected PresoObjAllocCCTNode(RuntimeMemoryCCTNode rtNode) {
        methodId = rtNode.methodId;

        if (rtNode instanceof RuntimeObjAllocTermCCTNode) {
            RuntimeObjAllocTermCCTNode rtTermNode = (RuntimeObjAllocTermCCTNode) rtNode;
            nCalls += rtTermNode.nCalls;
            totalObjSize += rtTermNode.totalObjSize;
        }
    }
    
    
    protected final void setChildren(PresoObjAllocCCTNode[] children) {
        this.children = children;
        for (PresoObjAllocCCTNode child : children) child.parent = this;
    }
    
    
    // --- Filtering support
    
    public CCTNode createFilteredNode() {
        PresoObjAllocCCTNode filtered = new PresoObjAllocCCTNode();
        setupFilteredNode(filtered);        
        return filtered;
    }
    
    protected void setupFilteredNode(PresoObjAllocCCTNode filtered) {
        filtered.setFilteredNode();
         
        filtered.parent = parent;

        filtered.nCalls = nCalls;
        filtered.totalObjSize = totalObjSize;

        Collection<PresoObjAllocCCTNode> _childrenL = resolveChildren(this);
        filtered.children = _childrenL.toArray(new PresoObjAllocCCTNode[_childrenL.size()]);
    }
    
    public void merge(CCTNode node) {
        if (node instanceof PresoObjAllocCCTNode) {
            PresoObjAllocCCTNode _node = (PresoObjAllocCCTNode)node;
            
            nCalls += _node.nCalls;
            totalObjSize += _node.totalObjSize;

            List<CCTNode> ch = new ArrayList();
            
            // Include current children
            if (children != null) ch.addAll(Arrays.asList(children));
            
            // Add or merge new children
            for (PresoObjAllocCCTNode child : resolveChildren(_node)) {
                int idx = ch.indexOf(child);
                if (idx == -1) ch.add(child);
                else ch.get(idx).merge(child);
            }
            
            children = ch.toArray(new PresoObjAllocCCTNode[ch.size()]);
        }
    }

    protected static Collection<PresoObjAllocCCTNode> resolveChildren(PresoObjAllocCCTNode node) {
        PresoObjAllocCCTNode[] chldrn = (PresoObjAllocCCTNode[])node.getChildren();
        return chldrn == null ? Collections.EMPTY_LIST : Arrays.asList(chldrn);
    }
    
    // ---

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void getNamesForMethodIdsFromVM(ProfilerClient profilerClient, RuntimeMemoryCCTNode[] allStackRoots)
                                           throws ClientUtils.TargetAppOrVMTerminated {
        if (allStackRoots == null) {
            return; // Can happen if this is called too early
        }

        for (int i = 0; i < allStackRoots.length; i++) {
            if (allStackRoots[i] != null) {
                checkMethodIdForNodeFromVM(allStackRoots[i]);
            }
        }

        JMethodIdTable.getDefault().getNamesForMethodIds(profilerClient);
    }

    public static PresoObjAllocCCTNode createPresentationCCTFromSnapshot(MemoryResultsSnapshot snapshot,
                                                                         RuntimeMemoryCCTNode rootRuntimeNode,
                                                                         String classTypeName) {
        PresoObjAllocCCTNode rootNode = generateMirrorNode(rootRuntimeNode);
        assignNamesToNodesFromSnapshot(snapshot, rootNode, classTypeName);

        return rootNode;
    }

    public static PresoObjAllocCCTNode createPresentationCCTFromVM(ProfilerClient profilerClient,
                                                                   RuntimeMemoryCCTNode rootRuntimeNode, String classTypeName)
        throws ClientUtils.TargetAppOrVMTerminated {
        PresoObjAllocCCTNode rootNode = generateMirrorNode(rootRuntimeNode);
        assignNamesToNodesFromVM(profilerClient, rootNode, classTypeName);

        return rootNode;
    }

    public CCTNode getChild(int index) {
        if (index < children.length) {
            return children[index];
        } else {
            return null;
        }
    }

    public CCTNode[] getChildren() {
        return children;
    }

    public int getIndexOfChild(Object child) {
        for (int i = 0; i < children.length; i++) {
            if ((PresoObjAllocCCTNode) child == children[i]) {
                return i;
            }
        }

        return -1;
    }

    public String[] getMethodClassNameAndSig() {
        return new String[] { getClassName(), getMethodName(), getMethodSig()};
    }

    public int getNChildren() {
        if (children != null) {
            return children.length;
        } else {
            return 0;
        }
    }

    public String getNodeName() {
        if (nodeName == null) {
            if (isFiltered()) {
                nodeName = FilterSortSupport.FILTERED_OUT_LBL;
            } else if (methodId != 0) {
                if (VM_ALLOC_CLASS.equals(getClassName()) && VM_ALLOC_METHOD.equals(getMethodName())) { // special handling of ProfilerRuntimeMemory.traceVMObjectAlloc
                    nodeName = VM_ALLOC_TEXT;
                } else {
                    nodeName = MethodNameFormatterFactory.getDefault().getFormatter().formatMethodName(
                                        getClassName(), getMethodName(), getMethodSig()).toFormatted();
                }
            } else if (getClassName() != null) {
                nodeName = getClassName();
            } else {
                nodeName = UKNOWN_NODENAME;
            }
        }
        
        return nodeName;
    }

    public CCTNode getParent() {
        return parent;
    }

    public void sortChildren(int sortBy, boolean sortOrder) {
//        int nChildren = getNChildren();
//
//        if (nChildren == 0) {
//            return;
//        }
//
//        for (int i = 0; i < nChildren; i++) {
//            children[i].sortChildren(sortBy, sortOrder);
//        }
//
//        if (nChildren > 1) {
//            switch (sortBy) {
//                case SORT_BY_NAME:
//                    sortChildrenByName(sortOrder);
//
//                    break;
//                case SORT_BY_ALLOC_OBJ_SIZE:
//                    sortChildrenByAllocObjSize(sortOrder);
//
//                    break;
//                case SORT_BY_ALLOC_OBJ_NUMBER:
//                    sortChildrenByAllocObjNumber(sortOrder);
//
//                    break;
//            }
//        }
    }

    public String toString() {
        return getNodeName();
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
    
//    void merge(PresoObjAllocCCTNode node) {
//        nCalls += node.nCalls;
//        totalObjSize += totalObjSize;
//        
//        if (node.children != null) {
//            for (PresoObjAllocCCTNode ch : node.children)
//                ch.parent = this;
//            
//            int chl = children == null ? 0 : children.length;
//            int newchl = node.children.length;
//            PresoObjAllocCCTNode[] newch = new PresoObjAllocCCTNode[chl + newchl];
//            if (children != null) System.arraycopy(children, 0, newch, 0, chl);
//            System.arraycopy(node.children, 0, newch, chl, newchl);
//            children = newch;
//        }
//    }
    
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PresoObjAllocCCTNode)) return false;
        PresoObjAllocCCTNode other = (PresoObjAllocCCTNode)o;
        if (isFiltered()) {
            return other.isFiltered();
        }
        if (other.isFiltered()) {
            return false;
        }
        if (methodId == 0) {
            return getNodeName().equals(other.getNodeName());
        }
        if (other.methodId == 0) {
            return false;
        }
        return entry.className.equals(other.entry.className) &&
               entry.methodName.equals(other.entry.methodName) &&
               entry.methodSig.equals(other.entry.methodSig);
    }
    
    public int hashCode() {
        if (methodId == 0 || isFiltered()) {
            return getNodeName().hashCode();
        }
        return entry.className.hashCode() ^ entry.methodName.hashCode() ^ entry.methodSig.hashCode();
    }

    protected static void assignNamesToNodesFromSnapshot(MemoryResultsSnapshot snapshot, PresoObjAllocCCTNode rootNode,
                                                         String classTypeName) {
        rootNode.className = StringUtils.userFormClassName(classTypeName);
        rootNode.setFullClassAndMethodInfo(snapshot.getJMethodIdTable());
    }

    protected static void assignNamesToNodesFromVM(ProfilerClient profilerClient, PresoObjAllocCCTNode rootNode,
                                                   String classTypeName)
                                            throws ClientUtils.TargetAppOrVMTerminated {
        JMethodIdTable.getDefault().getNamesForMethodIds(profilerClient);
        rootNode.className = StringUtils.userFormClassName(classTypeName);
        rootNode.setFullClassAndMethodInfo(JMethodIdTable.getDefault());
    }

    protected static PresoObjAllocCCTNode generateMirrorNode(RuntimeMemoryCCTNode rtNode) {
        PresoObjAllocCCTNode thisNode = new PresoObjAllocCCTNode(rtNode);
        Object nodeChildren = rtNode.children;

        if (nodeChildren != null) {
            if (nodeChildren instanceof RuntimeMemoryCCTNode) {
                thisNode.children = new PresoObjAllocCCTNode[1];

                PresoObjAllocCCTNode child = generateMirrorNode((RuntimeMemoryCCTNode) nodeChildren);
                thisNode.children[0] = child;
                child.parent = thisNode;
                thisNode.nCalls += child.nCalls;
                thisNode.totalObjSize += child.totalObjSize;
            } else {
                RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) nodeChildren;
                int nChildren = ar.length;

                if (nChildren > 0) {
                    thisNode.children = new PresoObjAllocCCTNode[nChildren];

                    for (int i = 0; i < nChildren; i++) {
                        PresoObjAllocCCTNode child = generateMirrorNode(ar[i]);
                        thisNode.children[i] = child;
                        child.parent = thisNode;
                        thisNode.nCalls += child.nCalls;
                        thisNode.totalObjSize += child.totalObjSize;
                    }
                }
            }
        }

        return thisNode;
    }

    protected boolean setFullClassAndMethodInfo(JMethodIdTable methodIdTable) {
        if (methodId != 0) {
            entry = methodIdTable.getEntry(methodId);
        }

        // If any object allocations that happen in our own code are caught (which shouldn't happen),
        // make sure to conceal this data here.
        boolean thisNodeOk = entry!=null && !"org/netbeans/lib/profiler/server/ProfilerServer".equals(entry.className); // NOI18N
        boolean childrenOk = true;

        if (children != null) {
            for (int i = 0; i < children.length; i++) {
                if (!children[i].setFullClassAndMethodInfo(methodIdTable)) {
                    childrenOk = false;
                    children[i] = null;
                }
            }
        }

        if (!childrenOk) {
            // Determine the number of non-null children and create a new children array
            int newLen = 0;

            for (int i = 0; i < children.length; i++) {
                newLen += ((children[i] != null) ? 1 : 0);
            }

            boolean hasNonNullChildren = (newLen > 0);

            if (!hasNonNullChildren) {
                children = null;
            } else {
                PresoObjAllocCCTNode[] newChildren = new PresoObjAllocCCTNode[newLen];
                int idx = 0;

                for (int i = 0; i < children.length; i++) {
                    if (children[i] != null) {
                        newChildren[idx++] = children[i];
                    }
                }

                children = newChildren;
            }

            if ((getMethodName() == null) || (getMethodName().equals("main") && getMethodSig() // NOI18N
                .equals("([Ljava/lang/String;)V"))) { // NOI18N

                return true;
            } else {
                return hasNonNullChildren;
            }
        } else {
            return thisNodeOk;
        }
    }

    protected static void checkMethodIdForNodeFromVM(RuntimeMemoryCCTNode rtNode) {
        if (rtNode.methodId != 0) {
            JMethodIdTable.getDefault().checkMethodId(rtNode.methodId);
        }

        Object nodeChildren = rtNode.children;

        if (nodeChildren != null) {
            if (nodeChildren instanceof RuntimeMemoryCCTNode) {
                checkMethodIdForNodeFromVM((RuntimeMemoryCCTNode) nodeChildren);
            } else {
                RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) nodeChildren;

                for (int i = 0; i < ar.length; i++) {
                    checkMethodIdForNodeFromVM(ar[i]);
                }
            }
        }
    }

//    protected void sortChildrenByAllocObjNumber(boolean sortOrder) {
//        int len = children.length;
//        long[] values = new long[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].nCalls;
//        }
//
//        sortLongs(values, sortOrder);
//    }
//
//    protected void sortChildrenByAllocObjSize(boolean sortOrder) {
//        int len = children.length;
//        long[] values = new long[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = children[i].totalObjSize;
//        }
//
//        sortLongs(values, sortOrder);
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
//    protected void sortFloats(float[] values, boolean sortOrder) {
//        int len = values.length;
//
//        // Just the insertion sort - we will never get really large arrays here
//        for (int i = 0; i < len; i++) {
//            for (int j = i; (j > 0) && ((sortOrder == false) ? (values[j - 1] < values[j]) : (values[j - 1] > values[j])); j--) {
//                float tmp = values[j];
//                values[j] = values[j - 1];
//                values[j - 1] = tmp;
//
//                PresoObjAllocCCTNode tmpCh = children[j];
//                children[j] = children[j - 1];
//                children[j - 1] = tmpCh;
//            }
//        }
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
//                PresoObjAllocCCTNode tmpCh = children[j];
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
//                PresoObjAllocCCTNode tmpCh = children[j];
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
//                PresoObjAllocCCTNode tmpCh = children[j];
//                children[j] = children[j - 1];
//                children[j - 1] = tmpCh;
//            }
//        }
//    }

    public void exportXMLData(ExportDataDumper eDD,String indent) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer(indent+"<Node>"+newline); //NOI18N
        result.append(indent).append(" <Name>").append(replaceHTMLCharacters(getNodeName())).append("<Name>").append(newline); //NOI18N
        result.append(indent).append(" <Parent>").append(replaceHTMLCharacters((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName()))).append("<Parent>").append(newline); //NOI18N
        result.append(indent).append(" <Bytes_Allocated>").append(totalObjSize).append("</Bytes_Allocated>").append(newline); //NOI18N
        result.append(indent).append(" <Objects_Allocated>").append(nCalls).append("</Objects_Allocated>").append(newline); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < getNChildren(); i++) {
                children[i].exportXMLData(eDD, indent+" "); //NOI18N
            }
        }
        result=new StringBuffer(indent+"</Node>"); //NOI18N
        eDD.dumpData(result);
    }

    public void exportHTMLData(ExportDataDumper eDD, int depth) {
        StringBuffer result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">"); //NOI18N
        for (int i=0; i<depth; i++) {
            result.append("."); //NOI18N
        }
        result.append(replaceHTMLCharacters(getNodeName())).append("</pre></td><td class=\"right\">").append(totalObjSize).append("</td><td class=\"right\">").append(nCalls).append("</td><td class=\"parent\"><pre class=\"parent\">").append(replaceHTMLCharacters((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName()))).append("</pre></td></tr>"); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < children.length; i++) {
                children[i].exportHTMLData(eDD, depth+1);
            }
        }
    }

    private String replaceHTMLCharacters(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          char c = s.charAt(i);
          switch (c) {
              case '<': sb.append("&lt;"); break; // NOI18N
              case '>': sb.append("&gt;"); break; // NOI18N
              case '&': sb.append("&amp;"); break; // NOI18N
              case '"': sb.append("&quot;"); break; // NOI18N
              default: sb.append(c); break;
          }
        }
        return sb.toString();
    }

    public void exportCSVData(String separator, int depth, ExportDataDumper eDD) {
        StringBuffer result = new StringBuffer();
        String newLine = "\r\n"; // NOI18N
        String quote = "\""; // NOI18N
        String indent = " "; // NOI18N

        // this node
        result.append(quote);
        for (int i=0; i<depth; i++) {
            result.append(indent); // to simulate the tree structure in CSV
        }
        result.append(getNodeName()).append(quote).append(separator);
        result.append(quote).append(totalObjSize).append(quote).append(separator);
        result.append(quote).append(nCalls).append(quote).append(separator);
        result.append(quote).append((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName())).append(newLine); // NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < children.length; i++) {
                children[i].exportCSVData(separator, depth+1, eDD);
            }
        }
    }

    String getClassName() {
        if (className == null && entry != null) {
            className = entry.className.replace('/', '.'); // NOI18N
        }
        return className;
    }

    String getMethodName() {
        if (methodName == null && entry != null) {
            methodName = entry.methodName;
            if (entry.isNative) {
                methodName = methodName.concat(JMethodIdTable.NATIVE_SUFFIX);
            }   
        }
        return methodName;
    }

    String getMethodSig() {
        if (entry != null) {
            return entry.methodSig;
        }
        return null;
    }
    
    static class Handle {

        final PresoObjAllocCCTNode node;
        
        Handle(PresoObjAllocCCTNode n) {
            node = n;
        }

        public int hashCode() {
            return node.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj == this) return true;
            
            return node.equals(((Handle)obj).node);
        }
        
        
        
    }
}
