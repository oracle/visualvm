/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes;

import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;


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
