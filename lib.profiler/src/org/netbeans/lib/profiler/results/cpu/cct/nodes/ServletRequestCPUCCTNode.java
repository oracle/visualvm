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

package org.netbeans.lib.profiler.results.cpu.cct.nodes;

import org.netbeans.lib.profiler.results.cpu.cct.*;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ServletRequestCPUCCTNode extends TimedCPUCCTNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Locator extends RuntimeCPUCCTNodeVisitorAdaptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ServletRequestCPUCCTNode cctNodeCandidate = null;
        private String servletPath;
        private int requestType;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Locator() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static ServletRequestCPUCCTNode locate(int requestType, String servletPath, RuntimeCPUCCTNode.Children nodes) {
            Locator instance = new Locator();

            return instance.doLocate(requestType, servletPath, nodes);
        }

        public void visit(ServletRequestCPUCCTNode node) {
            if (node.getServletPath().equals(servletPath) && (node.getRequestType() == requestType)) {
                cctNodeCandidate = node;
            }
        }

        private ServletRequestCPUCCTNode doLocate(int requestType, String servletPath, RuntimeCPUCCTNode.Children nodes) {
            cctNodeCandidate = null;
            this.requestType = requestType;
            this.servletPath = servletPath;
            nodes.accept(this);

            return cctNodeCandidate;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    int hashCode = 0;
    private final String servletPath;
    private final int requestType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of ServletRequestCPUCCTNode
     */
    public ServletRequestCPUCCTNode(CPUCCTNodeFactory factory, int requestType, String path, boolean collectingTwoTimestamps) {
        super(factory, collectingTwoTimestamps);
        this.servletPath = path;
        this.requestType = requestType;
        setFilteredStatus(FILTERED_YES); // boundary node is going to be filtered by default
        setNCalls(0);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getRequestType() {
        return requestType;
    }

    public boolean isRoot() {
        return false;
    }

    public String getServletPath() {
        return servletPath;
    }

    public void accept(RuntimeCPUCCTNodeVisitor visitor) {
        visitor.visit(this);
    }

    public boolean equals(Object otherNode) {
        if (otherNode == null) {
            return false;
        }

        if (!(otherNode instanceof ServletRequestCPUCCTNode)) {
            return false;
        }

        return servletPath.equals(((ServletRequestCPUCCTNode) otherNode).servletPath)
               && (requestType == ((ServletRequestCPUCCTNode) otherNode).requestType);
    }

    public int hashCode() {
        if (hashCode == 0) {
            hashCode = servletPath.hashCode() + (requestType * 18321);
        }

        return hashCode;
    }

    protected TimedCPUCCTNode createSelfInstance() {
        CPUCCTNodeFactory factory = getFactory();

        return (factory != null) ? factory.createServletRequestNode(requestType, servletPath) : null;
    }
}
