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

package org.netbeans.modules.profiler.attach.wizard.steps;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.attach.wizard.WizardContext;
import org.netbeans.modules.profiler.attach.wizard.functors.ConditionalFunctor;
import org.netbeans.modules.profiler.attach.wizard.functors.TrueConditionalFunctor;
import org.netbeans.modules.profiler.attach.wizard.screen.WizardScreen;


/**
 *
 * @author Jaroslav Bachorik
 */
public class CompositeWizardStep implements WizardStep {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int currentIndex;
    private ConditionalFunctor condition;
    private List /*<WizardStep>*/ steps;
    private String title;
    private WizardContext context;
    private WizardStep nullStep = new NullWizardStep();
    private boolean currentFlag;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public CompositeWizardStep(String title) {
        this(null, title);
    }

    public CompositeWizardStep(WizardContext context, String title) {
        this(context, title, new TrueConditionalFunctor());
    }

    public CompositeWizardStep(WizardContext context, String title, ConditionalFunctor functor) {
        this.title = title;
        this.steps = new Vector(50);
        this.condition = functor;
        this.currentIndex = 0;
        this.context = context;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public WizardScreen getAttachedScreen() {
        return getCurrentStep().getAttachedScreen();
    }

    public boolean isBegin() {
        return currentIndex == -1;
    }

    public void setCurrent() {
    }

    public boolean isCurrent() {
        return this.currentFlag;
    }

    public boolean isEnd() {
        return currentIndex == steps.size();
    }

    public void setFirst() {
        if (this.canHandle()) {
            int iterIndex = 0;

            while (iterIndex < steps.size()) {
                WizardStep iterStep = getStepByIndex(iterIndex);
                iterStep.setFirst();

                if (!iterStep.isEnd()) {
                    break;
                }

                iterIndex++;
            }

            if (iterIndex < steps.size()) {
                this.currentFlag = true;
                this.currentIndex = iterIndex;
            } else {
                this.currentIndex = steps.size();
            }
        } else {
            currentIndex = steps.size();
        }
    }

    public boolean isFirst() {
        // is current step the first one?
        boolean first = getCurrentStep().isFirst();

        if (first) {
            // if it is make sure there are no other preceding steps being able to handle the context
            for (int index = this.currentIndex - 1; index >= 0; index--) {
                if (getStepByIndex(index).canHandle()) {
                    // if a preceding step can handle the context we are not at the beginning yet
                    first = false;

                    break;
                }
            }
        }

        return first;
    }

    public void setLast() {
        if (this.canHandle()) {
            int iterIndex = steps.size() - 1;

            while (iterIndex >= 0) {
                WizardStep iterStep = getStepByIndex(iterIndex);
                iterStep.setLast();

                if (!iterStep.isBegin()) {
                    break;
                }

                iterIndex--;
            }

            if (iterIndex >= 0) {
                this.currentFlag = true;
                currentIndex = iterIndex;
            } else {
                currentIndex = -1;
            }
        } else {
            currentIndex = -1;
        }
    }

    public boolean isLast() {
        // is current step the last one?
        boolean last = getCurrentStep().isLast();

        if (last) {
            // if it si make sure there aro no succeeding  steps being able to handle the context
            for (int index = this.currentIndex + 1; index < steps.size(); index++) {
                if (getStepByIndex(index).canHandle()) {
                    // if a succeeding step can handle the context we are not at the end yet
                    last = false;

                    break;
                }
            }
        }

        return last;
    }

    public void setNext() {
        getCurrentStep().setNext();

        if (getCurrentStep().isEnd()) {
            int iterIndex = this.currentIndex + 1;

            while (iterIndex < steps.size()) {
                WizardStep iterStep = getStepByIndex(iterIndex);
                iterStep.setFirst();

                if (!iterStep.isEnd()) {
                    break;
                }

                iterIndex++;
            }

            if (iterIndex < steps.size()) {
                this.currentIndex = iterIndex;
            } else {
                this.currentFlag = false;
                this.currentIndex = steps.size();
            }
        }
    }

    public void setPrevious() {
        getCurrentStep().setPrevious();

        if (getCurrentStep().isBegin()) {
            int iterIndex = this.currentIndex - 1;

            while (iterIndex >= 0) {
                WizardStep iterStep = getStepByIndex(iterIndex);
                iterStep.setLast();

                if (!iterStep.isBegin()) {
                    break;
                }

                iterIndex--;
            }

            if (iterIndex >= 0) {
                this.currentIndex = iterIndex;
            } else {
                this.currentFlag = false;
                this.currentIndex = -1;
            }
        }
    }

    public int getStepIndex() {
        return currentIndex + getCurrentStep().getStepIndex();
    }

    public String getTitle() {
        return this.title;
    }

    public void setWizardContext(WizardContext context) {
        this.context = context;

        for (Iterator it = steps.iterator(); it.hasNext();) {
            WizardStep step = (WizardStep) it.next();
            step.setWizardContext(context);
        }
    }

    public void accept(WizardStepVisitor visitor, WizardContext context, int level) {
        if (canHandle()) {
            //            visitor.visit(this, context, level);
            //            if (isCurrent()) {
            for (Iterator it = steps.iterator(); it.hasNext();) {
                WizardStep step = (WizardStep) it.next();
                step.accept(visitor, context, level + 1);
            }

            //            }
        }
    }

    public void addChangeListener(ChangeListener listener) {
        for (Iterator it = steps.iterator(); it.hasNext();) {
            WizardStep step = (WizardStep) it.next();
            step.addChangeListener(listener);
        }
    }

    public WizardStep addStep(String title, WizardScreen attachedScreen) {
        return addStep(title, attachedScreen, new TrueConditionalFunctor());
    }

    public WizardStep addStep(String title, WizardScreen attachedScreen, ConditionalFunctor condition) {
        WizardStep step = new SimpleWizardStep(this.context, title, attachedScreen, condition);
        this.steps.add(step);

        return step;
    }

    public void addStep(WizardStep step) {
        this.steps.add(step);
    }

    public boolean canBack() {
        return getCurrentStep().canBack();
    }

    public boolean canFinish() {
        return getCurrentStep().canFinish();
    }

    public boolean canHandle() {
        if (context == null) {
            return false;
        }

        return this.condition.evaluate(this.context);
    }

    public boolean canNext() {
        return getCurrentStep().canNext();
    }

    public boolean onCancel() {
        return getCurrentStep().onCancel();
    }

    public void onFinish() {
        getCurrentStep().onFinish();
    }

    public void removeChangeListener(ChangeListener listener) {
        for (Iterator it = steps.iterator(); it.hasNext();) {
            WizardStep step = (WizardStep) it.next();
            step.removeChangeListener(listener);
        }
    }

    protected WizardStep getCurrentStep() {
        if ((currentIndex >= 0) && (currentIndex < this.steps.size())) {
            return (WizardStep) this.steps.get(currentIndex);
        } else {
            return nullStep;
        }
    }

    private WizardStep getStepByIndex(int index) {
        index = (index < 0) ? 0 : index;
        index = (index > this.steps.size()) ? this.steps.size() : index;

        return (WizardStep) steps.get(index);
    }
}
