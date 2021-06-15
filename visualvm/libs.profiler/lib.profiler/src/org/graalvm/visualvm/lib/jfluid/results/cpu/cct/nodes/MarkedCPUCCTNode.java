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

import org.graalvm.visualvm.lib.jfluid.marker.Mark;
import org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MarkedCPUCCTNode extends TimedCPUCCTNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Locator {

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Locator() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static MarkedCPUCCTNode locate(Mark mark, RuntimeCCTNode[] nodes) {
            for(RuntimeCCTNode n : nodes) {
                if (n instanceof MarkedCPUCCTNode && ((MarkedCPUCCTNode)n).getMark().equals(mark)) {
                    return (MarkedCPUCCTNode)n;
                }
            }
            return null;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Mark mark;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MarkedCPUCCTNode */
    public MarkedCPUCCTNode(Mark mark) {
        super();
        this.mark = mark;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Mark getMark() {
        return mark;
    }

    public boolean isRoot() {
        return false;
    }

    public boolean equals(Object otherNode) {
        if (otherNode == null) {
            return false;
        }

        if (!(otherNode instanceof MarkedCPUCCTNode)) {
            return false;
        }

        return mark.equals(((MarkedCPUCCTNode) otherNode).getMark());
    }

    public int hashCode() {
        return (mark == null) ? 0 : mark.hashCode();
    }

    protected TimedCPUCCTNode createSelfInstance() {
        return new MarkedCPUCCTNode(mark);
    }
}
