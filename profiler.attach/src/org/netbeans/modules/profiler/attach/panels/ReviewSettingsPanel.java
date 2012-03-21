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

package org.netbeans.modules.profiler.attach.panels;

import javax.swing.JPanel;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "AttachWizard_DirectAttachString=Direct attach",
    "AttachWizard_DynamicAttachString=Dynamic attach",
    "AttachWizard_LocalMachineString=Local machine",
    "AttachWizard_RemoteSystemOsString=Remote system OS:",
    "AttachWizard_RemoteSystemHostNameString=Remote system hostname:",
    "AttachWizard_TargetNameLocationString=Target location:",
    "AttachWizard_RemoteSystemString=Remote system:",
    "AttachWizard_TargetNameTypeString=Target:",
    "AttachWizard_TargetTypeString=Target type:"
})
public class ReviewSettingsPanel extends AttachWizardPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public class Model {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String summary;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getProviderName() {
            return getContext().getIntegrationProvider().getTitle();
        }

        public void setSummary(String htmlText) {
            this.summary = htmlText;
        }

        public String getSummary() {
            if (this.summary == null) {
                return "";
            }

            return this.summary;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "ReviewSettingsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Model model;
    private ReviewSettingsPanelUI panel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ReviewSettingsPanel() {
        model = new Model();
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
        if (getContext().getAttachSettings().isRemote()) {
            // remote profiling doesn't allow automatic integration
            getContext().setManual(true);
            getContext().setAutomatic(false);
        } else {
            if ((getContext().getIntegrationProvider() != null) && getContext().getIntegrationProvider().supportsManual()) {
                getContext().setAutomatic(false);
            }

            if ((getContext().getIntegrationProvider() != null) && getContext().getIntegrationProvider().supportsAutomation()) {
                getContext().setManual(false);
            }
        }

        getContext().setHideIntegration(true);
    }

    public void onExit(AttachWizardContext context) {
        boolean isModified = getContext().getAttachSettings().commit();

        if (isModified) {
            getContext().setConfigChanged();
        }

        getContext().setHideIntegration(false);
    }

    public void onFinish(AttachWizardContext context) {
    }

    public void onPanelShow() {
        this.model.setSummary(buildSummary(getContext().getAttachSettings()));
        this.panel.refresh();
    }

    protected JPanel getRenderPanel() {
        if (panel == null) {
            panel = new ReviewSettingsPanelUI(model);
        }

        return panel;
    }

    private String buildSummary(AttachSettings attachSettings) {
        StringBuilder attachSettingsSummaryBuffer = new StringBuilder();
        attachSettingsSummaryBuffer.append("<b>"); // NOI18N
        attachSettingsSummaryBuffer.append(Bundle.AttachWizard_TargetTypeString());
        attachSettingsSummaryBuffer.append("</b> "); // NOI18N
        attachSettingsSummaryBuffer.append(attachSettings.getTargetType());
        attachSettingsSummaryBuffer.append("<br>"); // NOI18N

        if (!getContext().isProviderSingular()) {
            attachSettingsSummaryBuffer.append("<b>"); // NOI18N
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_TargetNameTypeString());
            attachSettingsSummaryBuffer.append("</b> "); // NOI18N
            attachSettingsSummaryBuffer.append(attachSettings.getServerType());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
        }

        attachSettingsSummaryBuffer.append("<br>"); // NOI18N
        attachSettingsSummaryBuffer.append("<b>"); // NOI18N
        attachSettingsSummaryBuffer.append(Bundle.AttachWizard_TargetNameLocationString());
        attachSettingsSummaryBuffer.append("</b> "); // NOI18N

        if (attachSettings.isRemote()) {
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_RemoteSystemString());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
            attachSettingsSummaryBuffer.append("<b>"); // NOI18N
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_RemoteSystemHostNameString());
            attachSettingsSummaryBuffer.append("</b> "); // NOI18N
            attachSettingsSummaryBuffer.append(attachSettings.getHost());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
            attachSettingsSummaryBuffer.append("<b>"); // NOI18N
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_RemoteSystemOsString());
            attachSettingsSummaryBuffer.append("</b> "); // NOI18N
            attachSettingsSummaryBuffer.append(attachSettings.getHostOS());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
        } else {
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_LocalMachineString());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
            attachSettingsSummaryBuffer.append("<b>"); // NOI18N
            attachSettingsSummaryBuffer.append(Bundle.AttachWizard_AttachMethodString());
            attachSettingsSummaryBuffer.append("</b> "); // NOI18N
            attachSettingsSummaryBuffer.append(attachSettings.isDirect()
                                               ? Bundle.AttachWizard_DirectAttachString()
                                               : Bundle.AttachWizard_DynamicAttachString());
            attachSettingsSummaryBuffer.append("<br>"); // NOI18N
        }

        return attachSettingsSummaryBuffer.toString();
    }
}
