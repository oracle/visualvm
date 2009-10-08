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
import org.netbeans.lib.profiler.results.ExportDataDumper;
import java.text.NumberFormat;


/**
 * Presentation-Time CPU Profiling Calling Context Tree (CCT) Node that contains all necessary data in its data fields.
 * These objects are used for reverse CCTs and other trees that have to be constructed incrementally, so that backing by
 * flattened data cannot be easily implemented.
 *
 * @author Misha Dmitriev
 */
public class PrestimeCPUCCTNodeFree extends PrestimeCPUCCTNode {

    private static NumberFormat percentFormat=null;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int methodId;
    protected int nCalls;
    protected long sleepTime0;

    /** The same class used for both standard and "extended" nodes (collecting one or two timestamps) */
    protected long totalTime0;
    protected long totalTime1;

    /** The same class used for both standard and "extended" nodes (collecting one or two timestamps) */
    protected long waitTime0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Constructor for creating normal nodes representing methods
     */
    protected PrestimeCPUCCTNodeFree(CPUCCTContainer container, PrestimeCPUCCTNode parent, int methodId) {
        super(container, parent);
        this.methodId = methodId;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public CCTNode getChild(int index) {
        return children[index];
    }

    public CCTNode[] getChildren() {
        return children;
    }

    public void setMethodId(int methodId) {
        this.methodId = methodId;
    }

    public int getMethodId() {
        return methodId;
    }

    public int getNCalls() {
        return nCalls;
    }

    public int getNChildren() {
        return (children != null) ? children.length : 0;
    }

    public long getSleepTime0() {
        return 0; // TODO [wait]
    }

    public int getThreadId() {
        return container.getThreadId();
    }

    public long getTotalTime0() {
        return totalTime0;
    }

    public float getTotalTime0InPerCent() {
        float result = (float) ((double) totalTime0 / container.getWholeGraphNetTime0() * 100);

        return (result < 100) ? result : 100f;
    }

    public long getTotalTime1() {
        return totalTime1;
    }

    public float getTotalTime1InPerCent() {
        return (float) ((double) totalTime1 / container.getWholeGraphNetTime1() * 100);
    }

    public long getWaitTime0() {
        return 0; // TODO [wait]
    }

    public void addChild(PrestimeCPUCCTNodeFree node) {
        if (children == null) {
            children = new PrestimeCPUCCTNodeFree[1];
        } else {
            PrestimeCPUCCTNodeFree[] newch = new PrestimeCPUCCTNodeFree[children.length + 1];
            System.arraycopy(children, 0, newch, 0, children.length);
            children = newch;
        }

        children[children.length - 1] = node;
    }

    /**
     * Methods used during node merging
     */
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

    /**
     * Create a copy of this node, but with zero children
     */
    public PrestimeCPUCCTNodeFree createChildlessCopy() {
        try {
            PrestimeCPUCCTNodeFree res = (PrestimeCPUCCTNodeFree) this.clone();
            res.children = null;

            return res;
        } catch (CloneNotSupportedException ex) {
            return null; /* Shouldn't happen */
        }
    }

    public void sortChildren(int sortBy, boolean sortOrder) {
        if (children != null) {
            doSortChildren(sortBy, sortOrder);
        }
    }

    public void exportXMLData(ExportDataDumper eDD,String indent) {
        String newline = System.getProperty("line.separator"); // NOI18N
        StringBuffer result = new StringBuffer(indent+"<node>"+newline); //NOI18N
        result.append(indent+" <Name>"+replaceHTMLCharacters(getNodeName())+"</Name>"+newline); //NOI18N
        result.append(indent+" <Parent>"+replaceHTMLCharacters((getParent()==null)?("none"):(((PrestimeCPUCCTNodeFree)getParent()).getNodeName()))+"</Parent>"+newline); //NOI18N
        result.append(indent+" <Time_Relative>"+percentFormat.format(((double)getTotalTime0InPerCent())/100)+"</Time_Relative>"+newline); //NOI18N
        result.append(indent+" <Time>"+getTotalTime0()+"</Time>"+newline); //NOI18N
        result.append(indent+" <Invocations>"+getNCalls()+"</Invocations>"+newline); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < getNChildren(); i++) {
                ((PrestimeCPUCCTNodeFree)children[i]).exportXMLData(eDD, indent+"  "); //NOI18N
            }
        }
        result=new StringBuffer(indent+"</node>"); //NOI18N
        eDD.dumpData(result);
    }

    public void exportHTMLData(ExportDataDumper eDD, int depth) {
        StringBuffer result = new StringBuffer("<tr><td class=\"method\"><pre class=\"method\">"); //NOI18N
        for (int i=0; i<depth; i++) {
            result.append("."); //NOI18N
        }
        result.append(replaceHTMLCharacters(getNodeName())+"</pre></td><td class=\"right\">"+percentFormat.format(((double)getTotalTime0InPerCent())/100)+"</td><td class=\"right\">"+getTotalTime0()+"</td><td class=\"right\">"+getNCalls()+"</td></tr>"); //NOI18N
        eDD.dumpData(result); //dumps the current row
        // children nodes
        if (children!=null) {
            for (int i = 0; i < getNChildren(); i++) {
                ((PrestimeCPUCCTNodeFree)children[i]).exportHTMLData(eDD, depth+1);
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
            for (int i = 0; i < getNChildren(); i++) {
                ((PrestimeCPUCCTNodeFree)children[i]).exportCSVData(separator, depth+1, eDD);
            }
        }
    }

    public static void setPercentFormat(NumberFormat percentFormat) {
        PrestimeCPUCCTNodeFree.percentFormat = percentFormat;
    }
}
