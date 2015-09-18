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

import org.netbeans.lib.profiler.ProfilerClient;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.ExportDataDumper;


/**
 * Presentation-Time Object Liveness Profiling Calling Context Tree (CCT) Node.
 *
 * @author Misha Dmitriev
 */
public class PresoObjLivenessCCTNode extends PresoObjAllocCCTNode {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static int currentEpoch;
    public static final int SORT_BY_LIVE_OBJ_SIZE = 1;
    public static final int SORT_BY_LIVE_OBJ_NUMBER = 2;
    public static final int SORT_BY_ALLOC_OBJ = 3;
    public static final int SORT_BY_AVG_AGE = 4;
    public static final int SORT_BY_SURV_GEN = 5;
    public static final int SORT_BY_NAME = 6;
    public static final int SORT_BY_TOTAL_ALLOC_OBJ = 7;
    private static boolean dontShowZeroLiveObjNodes;
//    public final NumberFormat decimalFormat = NumberFormat.getInstance(Locale.ENGLISH);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    public float avgObjectAge;
    public int nLiveObjects;
    public int survGen;
    public int nTotalAllocObjects = -1; // Only populated for root nodes

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public static PresoObjLivenessCCTNode rootNode(PresoObjLivenessCCTNode[] children) {
        PresoObjLivenessCCTNode root = new PresoObjLivenessCCTNode();
        root.setChildren(children);
        return root;
    }
    
    public PresoObjLivenessCCTNode(String className, long nCalls, long totalObjSize, int nLiveObjects, int nTotalAllocObjects, float avgObjectAge, int survGen) {
        super(className, nCalls, totalObjSize);
        this.nLiveObjects = nLiveObjects;
        this.nTotalAllocObjects = nTotalAllocObjects;
        this.avgObjectAge = avgObjectAge;
        this.survGen = survGen;
    }
    
    PresoObjLivenessCCTNode() {}
    
    protected PresoObjLivenessCCTNode(RuntimeMemoryCCTNode rtNode) {
        super(rtNode);
    }
    
    
    // --- Filtering support
    
    public PresoObjLivenessCCTNode createFilteredNode() {
        PresoObjLivenessCCTNode filtered = new PresoObjLivenessCCTNode();
        setupFilteredNode(filtered);        
        return filtered;
    }
    
     protected void setupFilteredNode(PresoObjLivenessCCTNode filtered) {
        super.setupFilteredNode(filtered);
        
        filtered.nLiveObjects = nLiveObjects;
        filtered.avgObjectAge = avgObjectAge;
        filtered.survGen = survGen;
    }
    
    public void merge(CCTNode node) {
        if (node instanceof PresoObjLivenessCCTNode) {
            PresoObjLivenessCCTNode _node = (PresoObjLivenessCCTNode)node;
            
            nLiveObjects += _node.nLiveObjects;
            // TODO: use a more precise aggregation algorithm!!!
            avgObjectAge = Math.max(avgObjectAge, _node.avgObjectAge);
            survGen = Math.max(survGen, _node.survGen);
            
            super.merge(node);
        }
    }
    
    // ---

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static PresoObjLivenessCCTNode createPresentationCCTFromSnapshot(LivenessMemoryResultsSnapshot snapshot,
                                                                         RuntimeMemoryCCTNode rootRuntimeNode,
                                                                         String classTypeName, int curEpoch,
                                                                         boolean dontShowZeroLiveObjAllocPaths) {
        currentEpoch = curEpoch;
        dontShowZeroLiveObjNodes = dontShowZeroLiveObjAllocPaths;

        SurvGenSet survGens = new SurvGenSet();

        PresoObjLivenessCCTNode rootNode = generateMirrorNode(rootRuntimeNode, survGens);

        if (rootNode != null) { // null means there are no live objects for any allocation path
            assignNamesToNodesFromSnapshot(snapshot, rootNode, classTypeName);
        }

        return rootNode;
    }

    public static PresoObjAllocCCTNode createPresentationCCTFromVM(ProfilerClient profilerClient,
                                                                   RuntimeMemoryCCTNode rootRuntimeNode, String classTypeName,
                                                                   int curEpoch, boolean dontShowZeroLiveObjAllocPaths)
        throws ClientUtils.TargetAppOrVMTerminated {
        currentEpoch = curEpoch;
        dontShowZeroLiveObjNodes = dontShowZeroLiveObjAllocPaths;

        SurvGenSet survGens = new SurvGenSet();

        PresoObjAllocCCTNode rootNode = generateMirrorNode(rootRuntimeNode, survGens);

        if (rootNode != null) { // null means there are no live objects for any allocation path
            assignNamesToNodesFromVM(profilerClient, rootNode, classTypeName);
        }

        return rootNode;
    }
    
    
    
//    void merge(PresoObjAllocCCTNode node) {
//        PresoObjLivenessCCTNode nodel = (PresoObjLivenessCCTNode)node;
//        nLiveObjects += nodel.nLiveObjects;
//        // TODO: use a more precise aggregation algorithm!!!
//        avgObjectAge = Math.max(avgObjectAge, nodel.avgObjectAge);
//        survGen = Math.max(survGen, nodel.survGen);
//        
//        super.merge(node);
//    }

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
//                case SORT_BY_LIVE_OBJ_SIZE:
//                    sortChildrenByLiveObjSize(sortOrder);
//
//                    break;
//                case SORT_BY_LIVE_OBJ_NUMBER:
//                    sortChildrenByLiveObjNumber(sortOrder);
//
//                    break;
//                case SORT_BY_ALLOC_OBJ:
//                    sortChildrenByAllocObjNumber(sortOrder);
//
//                    break;
//                case SORT_BY_AVG_AGE:
//                    sortChildrenByAvgAge(sortOrder);
//
//                    break;
//                case SORT_BY_SURV_GEN:
//                    sortChildrenBySurvGen(sortOrder);
//
//                    break;
//                case SORT_BY_NAME:
//                    sortChildrenByName(sortOrder);
//
//                    break;
//            }
//        }
    }

    protected static PresoObjLivenessCCTNode generateMirrorNode(RuntimeMemoryCCTNode rtNode, SurvGenSet survGens) {
        PresoObjLivenessCCTNode thisNode = null;

        if (rtNode instanceof RuntimeObjLivenessTermCCTNode) { // A "terminal" node may occur even in the middle of the call chain

            RuntimeObjLivenessTermCCTNode rtTermNode = (RuntimeObjLivenessTermCCTNode) rtNode;
            int nLiveObjects = rtTermNode.calculateTotalNLiveObjects();

            if (dontShowZeroLiveObjNodes && (nLiveObjects == 0) && (rtNode.children == null)) {
                return null;
            }

            thisNode = new PresoObjLivenessCCTNode(rtNode);
            thisNode.nLiveObjects = nLiveObjects;
            //thisNode.survGen = RuntimeObjLivenessTermCCTNode.calculateTotalNumberOfSurvGensForAllPaths(rtTermNode);
            RuntimeObjLivenessTermCCTNode.calculateTotalNumberOfSurvGens(rtTermNode, survGens);
            thisNode.survGen = survGens.getTotalNoOfAges();
            thisNode.avgObjectAge = RuntimeObjLivenessTermCCTNode.calculateAvgObjectAgeForAllPaths(rtTermNode, currentEpoch);

            if (rtNode.children != null) {
                int len = (rtNode.children instanceof RuntimeMemoryCCTNode) ? 1 : ((RuntimeMemoryCCTNode[]) rtNode.children).length;
                thisNode.children = new PresoObjAllocCCTNode[len];
            }
        }

        Object nodeChildren = rtNode.children;

        if (nodeChildren != null) {
            RuntimeMemoryCCTNode[] ar;

            if (nodeChildren instanceof RuntimeMemoryCCTNode) {
                ar = new RuntimeMemoryCCTNode[1];
                ar[0] = (RuntimeMemoryCCTNode) nodeChildren;
            } else {
                ar = (RuntimeMemoryCCTNode[]) nodeChildren;
            }

            int nChildren = ar.length;

            if (nChildren > 0) {
                double avgAge = 0;
                int childIdx = 0;

                for (int i = 0; i < nChildren; i++) {
                    SurvGenSet subNodeSurvGens = (nChildren == 1) ? survGens : new SurvGenSet();
                    PresoObjLivenessCCTNode child = (PresoObjLivenessCCTNode) generateMirrorNode(ar[i], subNodeSurvGens);

                    if (child != null) {
                        if (thisNode == null) {
                            thisNode = new PresoObjLivenessCCTNode(rtNode);
                            thisNode.children = new PresoObjAllocCCTNode[nChildren];
                        }
                    } else {
                        continue;
                    }

                    thisNode.children[childIdx++] = child;
                    child.parent = thisNode;
                    thisNode.nCalls += child.nCalls;
                    thisNode.totalObjSize += child.totalObjSize;
                    thisNode.nLiveObjects += child.nLiveObjects;
                    avgAge += (child.avgObjectAge * child.nLiveObjects);

                    if (nChildren > 1) {
                        survGens.mergeWith(subNodeSurvGens);
                    }
                }

                if (dontShowZeroLiveObjNodes && ((thisNode == null) || (thisNode.nLiveObjects == 0))) {
                    return null;
                }

                if (childIdx < nChildren) {
                    PresoObjAllocCCTNode[] newChildren = new PresoObjAllocCCTNode[childIdx];
                    System.arraycopy(thisNode.children, 0, newChildren, 0, childIdx);
                    thisNode.children = newChildren;
                }

                thisNode.avgObjectAge = (thisNode.nLiveObjects > 0) ? (float) (avgAge / thisNode.nLiveObjects) : 0;
                thisNode.survGen = survGens.getTotalNoOfAges();
            }
        }

        return thisNode;
    }

//    protected void sortChildrenByAvgAge(boolean sortOrder) {
//        int len = children.length;
//        float[] values = new float[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = ((PresoObjLivenessCCTNode) children[i]).avgObjectAge;
//        }
//
//        sortFloats(values, sortOrder);
//    }
//
//    protected void sortChildrenByLiveObjNumber(boolean sortOrder) {
//        int len = children.length;
//        int[] values = new int[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = ((PresoObjLivenessCCTNode) children[i]).nLiveObjects;
//        }
//
//        sortInts(values, sortOrder);
//    }
//
//    protected void sortChildrenByLiveObjSize(boolean sortOrder) {
//        sortChildrenByAllocObjSize(sortOrder);
//    }
//
//    protected void sortChildrenBySurvGen(boolean sortOrder) {
//        int len = children.length;
//        int[] values = new int[len];
//
//        for (int i = 0; i < len; i++) {
//            values[i] = ((PresoObjLivenessCCTNode) children[i]).survGen;
//        }
//
//        sortInts(values, sortOrder);
//    }

    @Override
    public void exportXMLData(ExportDataDumper eDD,String indent) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer(indent+"<Node>"+newline); //NOI18N
        result.append(indent).append(" <Name>").append(replaceHTMLCharacters(getNodeName())).append("</Name>").append(newline); //NOI18N
        result.append(indent).append(" <Parent>").append(replaceHTMLCharacters((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName()))).append("</Parent>").append(newline); //NOI18N
        result.append(indent).append(" <Live_Bytes>").append(totalObjSize).append("</Live_Bytes>").append(newline); //NOI18N
        result.append(indent).append(" <Live_Objects>").append(nLiveObjects).append("</Live_Objects>").append(newline); //NOI18N
        result.append(indent).append(" <Allocated_Objects>").append(nCalls).append("</Allocated_Objects>").append(newline); //NOI18N
        result.append(indent).append(" <Avg_Age>").append(avgObjectAge).append("</Avg_Age>").append(newline); //NOI18N
        result.append(indent).append(" <Generations>").append(survGen).append("</Generations>").append(newline); //NOI18N
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

    @Override
    public void exportHTMLData(ExportDataDumper eDD, int depth) {
        StringBuffer result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">"); //NOI18N
        for (int i=0; i<depth; i++) {
            result.append("."); //NOI18N
        }
        result.append(replaceHTMLCharacters(getNodeName())).append("</pre></td><td class=\"right\">").append(totalObjSize).append("</td><td class=\"right\">").append(nLiveObjects).append("</td><td class=\"right\">").append(nCalls).append("</td><td class=\"right\">").append(avgObjectAge).append("</td><td class=\"right\">").append(survGen).append("</td><td class=\"parent\"><pre class=\"parent\">").append(replaceHTMLCharacters((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName()))).append("</pre></td></tr>"); //NOI18N
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

    @Override
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
        result.append(quote).append(nLiveObjects).append(quote).append(separator);
        result.append(quote).append(nCalls).append(quote).append(separator);
//        result.append(quote).append(decimalFormat.format(avgObjectAge)).append(quote).append(separator);
        result.append(quote).append(survGen).append(quote).append(separator);
        result.append(quote).append((getParent()==null)?("none"):(((PresoObjAllocCCTNode)getParent()).getNodeName())).append(newLine); // NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < children.length; i++) {
                ((PresoObjLivenessCCTNode) children[i]).exportCSVData(separator, depth+1, eDD);
            }
        }
    }

    public void setDecimalFormat() {
//        decimalFormat.setMinimumFractionDigits(3);
//        decimalFormat.setMaximumFractionDigits(3);
    }
}
