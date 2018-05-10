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

package org.netbeans.lib.profiler.results.cpu.cct.nodes;

import org.netbeans.lib.profiler.results.RuntimeCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public class ServletRequestCPUCCTNode extends TimedCPUCCTNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Locator {

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Locator() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static ServletRequestCPUCCTNode locate(int requestType, String servletPath, RuntimeCCTNode[] nodes) {
            for(RuntimeCCTNode n : nodes) {
                if (n instanceof ServletRequestCPUCCTNode) {
                    ServletRequestCPUCCTNode sn = (ServletRequestCPUCCTNode)n;
                    if (sn.getServletPath().equals(servletPath) && sn.getRequestType() == requestType) {
                        return sn;
                    }
                }
            }
            return null;
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
    public ServletRequestCPUCCTNode(int requestType, String path) {
        super();
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
        return new ServletRequestCPUCCTNode(requestType, servletPath);
    }
}
