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

package org.netbeans.lib.profiler.results.cpu.marking;

import org.netbeans.lib.profiler.marker.Mark;
import org.netbeans.lib.profiler.global.TransactionalSupport;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTVisitorAdapter;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MarkedCPUCCTNode;
import java.util.Stack;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class MarkBasedNodeVisitor extends CPUCCTVisitorAdapter implements MarkingEngine.StateObserver {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    volatile boolean resetFlag = false;
    private Mark parentMark = null;
    private Stack markStack;
    private final TransactionalSupport transaction = new TransactionalSupport();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of MarkTimer
     */
    public MarkBasedNodeVisitor() {
        this.markStack = new Stack();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void afterWalk() {
        markStack.clear();
        parentMark = null;
        transaction.endTrans();
    }

    public void beforeWalk() {
        transaction.beginTrans(true);
        parentMark = null;
        resetFlag = false;
    }

    public void beginTrans(final boolean mutable) {
        transaction.beginTrans(mutable);
    }

    public void endTrans() {
        if (resetFlag) {
            this.markStack = new Stack();
            resetFlag = false;
        }

        transaction.endTrans();
    }

    public synchronized void onReset() {
        resetFlag = true;
        transaction.endTrans();
    }

    public void stateChanged(MarkingEngine instance) {
        reset();
    }

    public void visit(final MarkedCPUCCTNode node) {
        parentMark = (Mark) (markStack.isEmpty() ? null : markStack.peek());
        markStack.push(node.getMark());
    }

    public void visitPost(final MarkedCPUCCTNode node) {
        markStack.pop();
        parentMark = (Mark) (markStack.isEmpty() ? null : markStack.peek());
    }

    protected final Mark getCurrentMark() {
        return (Mark) (markStack.isEmpty() ? Mark.DEFAULT : markStack.peek());
    }

    protected final Mark getParentMark() {
        return (parentMark != null) ? parentMark : Mark.DEFAULT;
    }

    protected synchronized boolean isReset() {
        return resetFlag;
    }

    private void reset() {
        resetFlag = true;
    }
}
