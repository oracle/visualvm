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

import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTNodeFactory;
import org.netbeans.lib.profiler.results.cpu.cct.RuntimeCPUCCTNodeVisitor;
import org.netbeans.lib.profiler.results.cpu.cct.RuntimeCPUCCTNodeVisitorAdaptor;
import org.netbeans.lib.profiler.marker.Mark;


/**
 *
 * @author Jaroslav Bachorik
 */
public class MarkedCPUCCTNode extends TimedCPUCCTNode {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class Locator extends RuntimeCPUCCTNodeVisitorAdaptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private MarkedCPUCCTNode candidate = null;
        private Mark searchMark;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private Locator() {
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public static MarkedCPUCCTNode locate(Mark mark, RuntimeCPUCCTNode.Children nodes) {
            Locator instance = new Locator();

            return instance.doLocate(mark, nodes);
        }

        public void visit(MarkedCPUCCTNode node) {
            if (node.getMark().equals(searchMark)) {
                candidate = node;
            }
        }

        private MarkedCPUCCTNode doLocate(Mark mark, RuntimeCPUCCTNode.Children nodes) {
            candidate = null;
            searchMark = mark;
            nodes.accept(this);

            return candidate;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Mark mark;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of MarkedCPUCCTNode */
    public MarkedCPUCCTNode(CPUCCTNodeFactory factory, Mark mark, boolean collectingTwoTimeStamps) {
        super(factory, collectingTwoTimeStamps);
        this.mark = mark;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Mark getMark() {
        return mark;
    }

    public boolean isRoot() {
        return false;
    }

    public void accept(RuntimeCPUCCTNodeVisitor visitor) {
        visitor.visit(this);
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
        return getFactory().createCategory(mark);
    }
}
