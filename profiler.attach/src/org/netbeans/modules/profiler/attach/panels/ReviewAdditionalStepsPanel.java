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

package org.netbeans.modules.profiler.attach.panels;

import javax.swing.JPanel;
import org.netbeans.modules.profiler.attach.spi.IntegrationProvider;
import org.netbeans.modules.profiler.attach.spi.RunException;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.HelpCtx;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ReviewAdditionalStepsPanel extends AttachWizardPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------


    /* default*/ class Model {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private boolean automation = false;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public IntegrationProvider.IntegrationHints getAdditionalSteps() {
            return getContext().getIntegrationProvider()
                       .getAfterInstallationHints(getContext().getAttachSettings(), this.automation);
        }

        public void setAutomaticStart(boolean value) {
            this.automation = value;
        }

        public boolean getAutomaticStart() {
            return this.automation;
        }

        public String getProviderName() {
            return getContext().getIntegrationProvider().getTitle();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "ReviewAdditionalStepsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Model model = null;
    private ReviewAdditionalStepsPanelUI panel = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of ReviewAdditionalStepsPanel */
    public ReviewAdditionalStepsPanel() {
        this.model = new Model();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelp() {
        return HELP_CTX;
    }

    public boolean isValid() {
        return true;
    }

    public boolean canBack(AttachWizardContext context) {
        return true;
    }

    public boolean canFinish(AttachWizardContext context) {
        return true;
    }

    public boolean canNext(AttachWizardContext context) {
        return true;
    }

    public boolean onCancel(AttachWizardContext context) {
        return true;
    }

    public void onEnter(AttachWizardContext context) {
    }

    public void onExit(AttachWizardContext context) {
    }

    public void onFinish(AttachWizardContext context) {
        if (this.model.getAutomaticStart()) {
            try {
                getContext().getIntegrationProvider().run(getContext().getAttachSettings());
            } catch (RunException e) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Message(e.getMessage(), NotifyDescriptor.WARNING_MESSAGE));
            }
        }
    }

    protected JPanel getRenderPanel() {
        if (panel == null) {
            panel = new ReviewAdditionalStepsPanelUI(model);
        }

        return panel;
    }

    protected void onPanelShow() {
        ((ReviewAdditionalStepsPanelUI) getRenderPanel()).refresh();
    }
}
