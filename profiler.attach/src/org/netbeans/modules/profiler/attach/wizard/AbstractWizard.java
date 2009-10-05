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

package org.netbeans.modules.profiler.attach.wizard;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;
import org.openide.WizardDescriptor;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class AbstractWizard implements WizardDescriptor.Iterator, ChangeListener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set<ChangeListener> listeners = null;
    volatile private WizardDescriptor wizardDescriptor = null;
    private WizardStep wizardModel = null;
    private boolean initialized = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AbstractWizard() {
        this.listeners = new HashSet<ChangeListener>(); // MUST be the first statement in the sequence; WizardDescriptor depends on it
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public WizardDescriptor getWizardDescriptor() {
        if (wizardDescriptor == null) {
            wizardDescriptor = new WizardDescriptor(this);
            this.init();
        }
            
        return wizardDescriptor;
    }

    public void addChangeListener(ChangeListener changeListener) {
        this.listeners.add(changeListener);
    }

    public WizardDescriptor.Panel current() {
        return (WizardDescriptor.Panel) wizardModel.getAttachedScreen();
    }

    public void finish() {
        getRootStep().onFinish();
    }

    public boolean hasNext() {
        return !wizardModel.isLast() && wizardModel.canNext();
    }

    public boolean hasPrevious() {
        return !wizardModel.isFirst() && wizardModel.canBack();
    }

    public final synchronized void init() {
        if (this.initialized) {
            return;
        }

        prepareWizardModel();

        getWizardDescriptor().setTitle(getTitle());
        getWizardDescriptor().setTitleFormat(new MessageFormat(getTitleFormat()));
        getWizardDescriptor().putProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.valueOf(isAutoWizard())); // NOI18N
                                                                                                           //    this.wizardDescriptor.putProperty(WizardDescriptor.PROP_HELP_DISPLAYED, Boolean.valueOf(isHelpDisplayed())); // NOI18N // Needs to be in default state to correctly display Help button

        getWizardDescriptor().putProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.valueOf(isContentDisplayed())); // NOI18N
        getWizardDescriptor().putProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.valueOf(isNumbered())); // NOI18N
        getWizardDescriptor().putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(0)); // NOI18N
        getWizardDescriptor().putProperty(WizardContext.CONTEXT_PROPERTY_NAME, getContext());

        this.wizardModel.setFirst();

        updateWizardSteps();

        this.initialized = true;
    }

    public String name() {
        return getTitle();
    }

    public void nextPanel() {
        wizardModel.setNext();
        updateWizardSteps();
    }

    public void previousPanel() {
        wizardModel.setPrevious();
        updateWizardSteps();
    }

    // <editor-fold defaultstate="collapsed" desc="WizardDescriptor.Iterator implementation">
    public void removeChangeListener(ChangeListener changeListener) {
        this.listeners.remove(changeListener);
    }

    // </editor-fold>

    public void stateChanged(ChangeEvent e) {
        updateWizardSteps();

        for (Iterator it = listeners.iterator(); it.hasNext();) {
            ChangeListener listener = (ChangeListener) it.next();
            listener.stateChanged(e);
        }
    }

    public synchronized void invalidate() {
        this.initialized = false;
        listeners.clear();
        wizardDescriptor = null;
    }

    protected abstract boolean isAutoWizard();

    protected abstract boolean isContentDisplayed();

    protected abstract WizardContext getContext();

    protected abstract boolean isHelpDisplayed();

    protected abstract boolean isNumbered();

    protected abstract WizardStep getRootStep();

    // <editor-fold defaultstate="collapsed" desc="Abstract methods">
    protected abstract String getTitle();

    protected abstract String getTitleFormat();

    protected abstract void onUpdateWizardSteps();
    
    private void prepareWizardModel() {
        if (this.wizardModel != null) {
            this.wizardModel.removeChangeListener(this);
        }

        this.wizardModel = this.getRootStep();
        this.wizardModel.addChangeListener(this);
    }

    // </editor-fold>
    private void updateWizardSteps() {
        onUpdateWizardSteps();

        TitleCollectingStepVisitor visitor = new TitleCollectingStepVisitor();
        wizardModel.accept(visitor, getContext(), 0);

        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_DATA, visitor.getTitleArray()); // NOI18N
        wizardDescriptor.putProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(visitor.getTitleIndex())); // NOI18N
        wizardDescriptor.setTitle(wizardModel.getTitle());
    }
}
