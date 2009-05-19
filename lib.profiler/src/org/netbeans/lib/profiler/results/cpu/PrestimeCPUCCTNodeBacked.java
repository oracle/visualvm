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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.results.CCTNode;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import java.text.NumberFormat;


/**
 * Presentation-Time CPU Profiling Calling Context Tree (CCT) Node backed by the flattened tree data array in
 * CPUCCTContainer. These nodes are constructed on demand, i.e. only when the user opens some node in the CCT on screen.
 * They contain minimum amount of data in the node instance itself. As a result, a tree constructed of such nodes has
 * a very small overhead on top of the flattened data that already exists (and has relatively low space consumption and
 * construction time). The drawback is that it's difficult to add elements to a tree represented in the flattened form.
 *
 * @author Misha Dmitriev
 */
public class PrestimeCPUCCTNodeBacked extends PrestimeCPUCCTNode {

    private static NumberFormat percentFormat=null;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int compactDataOfs;
    protected int nChildren;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for creating normal nodes representing methods
     */
    public PrestimeCPUCCTNodeBacked(CPUCCTContainer container, PrestimeCPUCCTNode parent, int compactDataOfs) {
        super(container, parent);
        this.compactDataOfs = compactDataOfs;
        this.container = container;
        nChildren = container.getNChildrenForNodeOfs(compactDataOfs);
    }

    /**
     * Constructor for creating a node that represent a whole thread
     */
    protected PrestimeCPUCCTNodeBacked(CPUCCTContainer container, PrestimeCPUCCTNode[] children) {
        super(container, null);
        setThreadNode();
        this.children = children;
        nChildren = children.length;

        for (int i = 0; i < nChildren; i++) {
            children[i].parent = this;
        }
    }

    protected PrestimeCPUCCTNodeBacked() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CCTNode getChild(int index) {
        getChildren();

        if (index < children.length) {
            return children[index];
        } else {
            return null;
        }
    }

    public CCTNode[] getChildren() {
        if (nChildren == 0) {
            return null;
        } else if (children != null) {
            return children;
        }

        // Otherwise need to construct children first
        int addChild = isThreadNode() ? 0 : 1; // There will be an additional "self time" node
        children = new PrestimeCPUCCTNode[nChildren + addChild];

        for (int i = 0; i < nChildren; i++) {
            int dataOfs = container.getChildOfsForNodeOfs(compactDataOfs, i);
            children[i + addChild] = new PrestimeCPUCCTNodeBacked(container, this, dataOfs);
        }

        if (addChild > 0) {
            children[0] = createSelfTimeNodeForThisNode();
            nChildren++;
        }

        // Now that children are created, sort them in the order previously used
        sortChildren(container.getCPUResSnapshot().getSortBy(), container.getCPUResSnapshot().getSortOrder());

        return children;
    }

    public int getMethodId() {
        return container.getMethodIdForNodeOfs(compactDataOfs);
    }

    public int getNCalls() {
        return container.getNCallsForNodeOfs(compactDataOfs);
    }

    public int getNChildren() {
        if (children == null) { // Actual array not yet initialized

            int addChild = (nChildren > 0) ? (isThreadNode() ? 0 : 1) : 0; // There will be an additional "self time" node

            return nChildren + addChild;
        } else {
            return nChildren;
        }
    }

    public long getSleepTime0() {
        return container.getSleepTime0ForNodeOfs(compactDataOfs);

        // TODO: [wait] self time node?
    }

    public int getThreadId() {
        return container.getThreadId();
    }

    public long getTotalTime0() {
        if (!isSelfTimeNode()) {
            return container.getTotalTime0ForNodeOfs(compactDataOfs);
        } else {
            return container.getSelfTime0ForNodeOfs(compactDataOfs);
        }
    }

    public float getTotalTime0InPerCent() {
        float result = (float) ((container.getWholeGraphNetTime0() > 0)
                                ? ((double) getTotalTime0() / (double) container.getWholeGraphNetTime0() * 100.0) : 0);

        return (result < 100) ? result : 100;
    }

    public long getTotalTime1() {
        if (!isSelfTimeNode()) {
            return container.getTotalTime1ForNodeOfs(compactDataOfs);
        } else {
            return container.getSelfTime1ForNodeOfs(compactDataOfs);
        }
    }

    public float getTotalTime1InPerCent() {
        return (float) ((container.getWholeGraphNetTime1() > 0)
                        ? ((double) getTotalTime1() / (double) container.getWholeGraphNetTime1() * 100.0) : 0);
    }

    public long getWaitTime0() {
        return container.getWaitTime0ForNodeOfs(compactDataOfs);

        // TODO: [wait] self time node?
    }

    public void sortChildren(int sortBy, boolean sortOrder) {
        container.getCPUResSnapshot().saveSortParams(sortBy, sortOrder);

        // We don't eagerly initialize children for sorting
        if ((nChildren == 0) || (children == null)) {
            return;
        }

        doSortChildren(sortBy, sortOrder);
    }

    protected PrestimeCPUCCTNode createSelfTimeNodeForThisNode() {
        PrestimeCPUCCTNodeBacked selfTimeChild;

        selfTimeChild = new PrestimeCPUCCTNodeBacked();
        selfTimeChild.setSelfTimeNode();

        selfTimeChild.compactDataOfs = compactDataOfs;
        selfTimeChild.container = container;
        selfTimeChild.parent = this;

        return selfTimeChild;
    }
    
    public void exportXMLData(ExportDataDumper eDD,String indent) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer(indent+"<node>"+newline); //NOI18N
        result.append(indent+" <Name>"+replaceHTMLCharacters(getNodeName())+"</Name>"+newline); //NOI18N
        result.append(indent+" <Parent>"+replaceHTMLCharacters((getParent()==null)?("none"):(((PrestimeCPUCCTNodeBacked)getParent()).getNodeName()))+"</Parent>"+newline); //NOI18N
        result.append(indent+" <Time_Relative>"+percentFormat.format(((double) getTotalTime0InPerCent())/100)+"</Time_Relative>"+newline); //NOI18N
        result.append(indent+" <Time>"+getTotalTime0()+"</Time>"+newline); //NOI18N
        result.append(indent+" <Invocations>"+getNCalls()+"</Invocations>"+newline); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportXMLData(eDD, indent+"  "); //NOI18N
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportXMLData(eDD, indent+"  "); //NOI18N
                }
                tempChildren=null;
                nChildren=tempNChildren;
            }
        }
        result=new StringBuffer(indent+"</node>"); //NOI18N
        eDD.dumpData(result);
    }

    public void exportHTMLData(ExportDataDumper eDD, int depth) {
        StringBuffer result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">."); //NOI18N
        for (int i=0; i<depth; i++) {
            result.append("."); //NOI18N
        }
        result.append(replaceHTMLCharacters(getNodeName())+"</pre></td><td class=\"right\">"+percentFormat.format(((double) getTotalTime0InPerCent())/100)+"</td><td class=\"right\">"+getTotalTime0()+"</td><td class=\"right\">"+getNCalls()+"</td></tr>"); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportHTMLData(eDD, depth+1);
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportHTMLData(eDD, depth+1);
                }
                tempChildren=null;
                nChildren=tempNChildren;
            }
        }
    }

    private String replaceHTMLCharacters(String s) {
        StringBuffer sb = new StringBuffer();
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
        result.append(getNodeName() + quote + separator);
        result.append(quote+getTotalTime0InPerCent()+quote+separator);
        result.append(quote+getTotalTime0()+quote+separator);
        result.append(quote+getNCalls()+quote+newLine);
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < nChildren; i++) {
                ((PrestimeCPUCCTNodeBacked)children[i]).exportCSVData(separator, depth+1, eDD);
            }
        } else {
            if (nChildren>0) {
                int tempNChildren=nChildren;
                PrestimeCPUCCTNode[] tempChildren=(PrestimeCPUCCTNode[]) getChildren();
                children=null;
                for (int i = 0; i < nChildren; i++) {
                    ((PrestimeCPUCCTNodeBacked)tempChildren[i]).exportCSVData(separator, depth+1, eDD);
                }
                tempChildren=null;
                nChildren=tempNChildren;
            }
        }
    }

    public static void setPercentFormat(NumberFormat percentFormat) {
        PrestimeCPUCCTNodeBacked.percentFormat = percentFormat;
    }
}
