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

package org.netbeans.lib.profiler.results.cpu.cct;

import java.util.Collection;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ThreadCPUCCTNode;
import org.netbeans.lib.profiler.marker.Mark;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Jaroslav Bachorik
 */
public final class CCTResultsFilter extends CPUCCTVisitorAdapter {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Evaluator {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        boolean evaluate(Mark mark);
    }
    
    public static interface EvaluatorProvider {
        Set/*<Evaluator>*/ getEvaluators();
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(CCTResultsFilter.class.getName());

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set /*<Evaluator>*/ evaluators = null;
    private Set evaluatorProviders = new HashSet();

    private Stack passFlagStack;
    private boolean passingFilter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of CategoryFilter */
    public CCTResultsFilter() {
        evaluators = new HashSet /*<Evaluator>*/();
        passFlagStack = new Stack();
        doReset();
    }

    public void setEvaluators(Collection evaluatorProviders) {
        this.evaluatorProviders.clear();
        this.evaluatorProviders.addAll(evaluatorProviders);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public synchronized boolean passesFilter() {
        return passingFilter;
    }

    public void beforeWalk() {
        super.beforeWalk();
        evaluators.clear();
        
        for(Iterator iter = evaluatorProviders.iterator();iter.hasNext();) {
            evaluators.addAll(((EvaluatorProvider)iter.next()).getEvaluators());
        }
    }

    public void afterWalk() {
        evaluators.clear();
        super.afterWalk();
    }

    public void reset() {
        doReset();
    }

    public void visit(ThreadCPUCCTNode node) {
        LOGGER.finest("visiting thread node");
        passFlagStack.push(Boolean.valueOf(passingFilter));
        passingFilter = true;

        for (Iterator iter = evaluators.iterator(); iter.hasNext();) {
            Evaluator evaluator = (Evaluator) iter.next();
            passingFilter &= evaluator.evaluate(Mark.DEFAULT);
        }

        LOGGER.finest("Evaluator result: " + passingFilter);
        super.visit(node);
    }

    public void visit(MarkedCPUCCTNode node) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Entering a node marked " + node.getMark().getId()); // NOI18N
        }

        passFlagStack.push(Boolean.valueOf(passingFilter));
        passingFilter = true;

        for (Iterator iter = evaluators.iterator(); iter.hasNext();) {
            Evaluator evaluator = (Evaluator) iter.next();
            passingFilter &= evaluator.evaluate(node.getMark());
        }
    }

    public void visitPost(ThreadCPUCCTNode node) {
        super.visitPost(node);

        if (!passFlagStack.isEmpty()) {
            passingFilter = ((Boolean) passFlagStack.pop()).booleanValue();
        }
    }

    public void visitPost(MarkedCPUCCTNode node) {
        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Leaving a node marked " + node.getMark().getId()); // NOI18N
        }

        if (!passFlagStack.isEmpty()) {
            passingFilter = ((Boolean) passFlagStack.pop()).booleanValue();
        }
    }

    private void doReset() {
        passingFilter = false;
        passFlagStack.clear();
    }
}
