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
import javax.swing.event.ChangeEvent;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.openide.util.HelpCtx;

/**
 *
 * @author Jaroslav Bachorik
 */
public class SelectIntegrationTypePanel extends AttachWizardPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // <editor-fold defaultstate="collapsed" desc="Model">

    /* default */ class Model {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------


        /* default */ static final int AUTOMATIC_INTEGRATION = 1;

        /* default */ static final int MANUAL_INTEGRATION = 2;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private int integrationType = 0;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setIntegrationType(int type) {
            this.integrationType = type;
            publishUpdate();
        }

        public int getIntegrationType() {
            return this.integrationType;
        }

        public String getProviderName() {
            if ((getContext() != null) && (getContext().getIntegrationProvider() != null)) {
                return getContext().getIntegrationProvider().getTitle();
            } else {
                return null;
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "SelectIntegrationTypePanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // </editor-fold>
    private Model model;
    private SelectIntegrationTypePanelUI panel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SelectIntegrationTypePanel() {
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
        return false;
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
    }

    protected JPanel getRenderPanel() {
        if (this.panel == null) {
            this.panel = new SelectIntegrationTypePanelUI(model);
        }

        return this.panel;
    }

    protected void onPanelShow() {
        publishUpdate(); // overtake the values set by the GUI constraints
        panel.refresh();
    }

    private void publishUpdate() {
        if (getContext() == null) {
            return;
        }

        getContext().setAutomatic(this.model.integrationType == Model.AUTOMATIC_INTEGRATION);
        getContext().setManual(this.model.integrationType == Model.MANUAL_INTEGRATION);

        publishUpdate(new ChangeEvent(this));
    }
}
