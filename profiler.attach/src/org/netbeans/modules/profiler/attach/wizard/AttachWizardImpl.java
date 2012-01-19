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

package org.netbeans.modules.profiler.attach.wizard;

import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.attach.wizard.functors.ConditionalFunctor;
import org.netbeans.modules.profiler.attach.wizard.steps.CompositeWizardStep;
import org.netbeans.modules.profiler.attach.wizard.steps.ProxyWizardStep;
import org.netbeans.modules.profiler.attach.wizard.steps.SimpleWizardStep;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;
import org.netbeans.modules.profiler.attach.panels.AttachSettingsPanel;
import org.netbeans.modules.profiler.attach.panels.ManualIntegrationPanel;
import org.netbeans.modules.profiler.attach.panels.PerformIntegrationPanel;
import org.netbeans.modules.profiler.attach.panels.RemoteAttachSettingsPanel;
import org.netbeans.modules.profiler.attach.panels.ReviewAdditionalStepsPanel;
import org.netbeans.modules.profiler.attach.panels.ReviewSettingsPanel;
import org.netbeans.modules.profiler.attach.panels.SelectIntegrationTypePanel;
import org.openide.util.NbBundle;


/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "AttachWizard_AttachWizardCaption=Attach Wizard",
    "AttachWizard_ChooseIntegrationTypeString=Choose Integration Type",
    "AttachWizard_ReviewAttachSettingsString=Review Attach Settings",
    "AttachWizard_SelectTargetTypeString=Select Target Type",
    "AttachWizard_RemoteSystemString=Remote System",
    "AttachWizard_PerformIntegrationString=Review Integration",
    "AttachWizard_ReviewAdditionalStepsString=Review Additional Steps",
    "AttachWizard_ManualIntegrationStep=Manual Integration",
    "AttachWizard_AutomaticIntegrationStep=Automatic Integration",
    "AttachWizard_ProviderSpecificSettings=Provider specific settings",
    "AttachWizard_RefineAttachmentSettings=Refine attachments settings",
    "AttachWizard_IntegrateProfiler=Integrate profiler"
})
public class AttachWizardImpl extends AbstractWizard {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachWizardContext context = null;
    private ProxyWizardStep proxy = null;
    private WizardStep wizardModel = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AttachWizardImpl() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public AttachSettings getAttachSettings() {
        return ((AttachWizardContext) getContext()).getAttachSettings();
    }

    public void init(AttachSettings as) {
        ((AttachWizardContext) getContext()).setAttachSettings(as);
        super.init();
    }

    protected boolean isAutoWizard() {
        return true;
    }

    protected boolean isContentDisplayed() {
        return true;
    }

    protected WizardContext getContext() {
        if (this.context == null) {
            this.context = new AttachWizardContext();
        }

        return this.context;
    }

    protected boolean isHelpDisplayed() {
        return true;
    }

    protected boolean isNumbered() {
        return true;
    }

    protected WizardStep getRootStep() {
        if (this.wizardModel == null) {
            this.wizardModel = buildWizardModel();
        }

        return this.wizardModel;
    }

    protected String getTitle() {
        return Bundle.AttachWizard_AttachWizardCaption();
    }

    protected String getTitleFormat() {
        return "{0}"; // NOI18N
    }

    protected void onUpdateWizardSteps() {
        proxy.setWizardStep(((AttachWizardContext) getContext()).getIntegrationProvider().getAttachedWizard());
    }

    @Override
    public void invalidate() {
        super.invalidate();
        wizardModel = null;
    }

    private boolean isAutomationAllowed(AttachWizardContext ctx) {
        if (!ctx.getIntegrationProvider().supportsAutomation()) {
            return false;
        }

        //    if (ctx.getAttachSettings().isDynamic16()) return false;
        return ctx.getIntegrationProvider().supportsManual() && !ctx.getAttachSettings().isRemote();
    }

    private WizardStep buildWizardModel() {
        this.proxy = prepareProviderProxyStep();

        CompositeWizardStep rootStep = new CompositeWizardStep(getContext(), Bundle.AttachWizard_AttachWizardCaption());
        rootStep.addStep(Bundle.AttachWizard_SelectTargetTypeString(), new AttachSettingsPanel());
        rootStep.addStep(prepareAdditionalSettingsStep());
        rootStep.addStep(Bundle.AttachWizard_ReviewAttachSettingsString(), new ReviewSettingsPanel());
        //    rootStep.addStep(new SimpleWizardStep(getContext(), "...", new NullWizardScreen(), new ConditionalFunctor() {
        //      public boolean evaluate(WizardContext context) {
        //        return ((AttachWizardContext)context).isHideIntegration();
        //      }
        //    }));
        rootStep.addStep(prepareIntegrationStep());

        return rootStep;
    }

    private WizardStep prepareAdditionalSettingsStep() {
        CompositeWizardStep additionalSettings = new CompositeWizardStep(
            getContext(), 
            Bundle.AttachWizard_RefineAttachmentSettings(),
            new ConditionalFunctor() { // NOI18N
                public boolean evaluate(WizardContext context) {
                    AttachSettings settings = ((AttachWizardContext) context).getAttachSettings();

                    return settings.isRemote() || (!settings.isDirect() && !settings.isDynamic16());
                }
            }
        );

        WizardStep remoteAttach = new SimpleWizardStep(
            getContext(), 
            Bundle.AttachWizard_RemoteSystemString(), 
            new RemoteAttachSettingsPanel(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachSettings settings = ((AttachWizardContext) context).getAttachSettings();

                    return settings.isRemote();
                }
            }
        );
        additionalSettings.addStep(remoteAttach);

        return additionalSettings;
    }

    private WizardStep prepareIntegrationStep() {
        CompositeWizardStep integrationStep = new CompositeWizardStep(
            getContext(), 
            Bundle.AttachWizard_IntegrateProfiler(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;
                    AttachSettings settings = ctx.getAttachSettings();

                    return ctx.getIntegrationProvider().supportsManual() || ctx.getIntegrationProvider().supportsAutomation();
                }
            }
        );

        integrationStep.addStep(
            Bundle.AttachWizard_ChooseIntegrationTypeString(), 
            new SelectIntegrationTypePanel(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return isAutomationAllowed(ctx);
                }
            }
        );

        CompositeWizardStep automaticIntegration = new CompositeWizardStep(
            getContext(), 
            Bundle.AttachWizard_AutomaticIntegrationStep(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isAutomatic();
                }
            }
        );

        automaticIntegration.addStep(this.proxy);
        automaticIntegration.addStep(
            Bundle.AttachWizard_PerformIntegrationString(), 
            new PerformIntegrationPanel(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    //        AttachWizardContext ctx = (AttachWizardContext)context;
                    //        return ctx.isReadyToPerform();
                    return true;
                }
            });
        automaticIntegration.addStep(Bundle.AttachWizard_ReviewAdditionalStepsString(), new ReviewAdditionalStepsPanel());

        integrationStep.addStep(automaticIntegration);

        integrationStep.addStep(prepareManualIntegrationStep());

        return integrationStep;
    }

    private WizardStep prepareManualIntegrationStep() {
        return new SimpleWizardStep(
            getContext(), 
            Bundle.AttachWizard_ManualIntegrationStep(), 
            new ManualIntegrationPanel(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isManual();
                }
            }
        );
    }

    private ProxyWizardStep prepareProviderProxyStep() {
        return new ProxyWizardStep(
            getContext(), 
            Bundle.AttachWizard_ProviderSpecificSettings(),
            new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isAutomatic();
                }
            }
        ) {
            public void setNext() {
                super.setNext();

                if (canHandle()) {
                    ((AttachWizardContext) getContext()).getIntegrationProvider().getSettingsPersistor().storeSettings();
                }
            }
        };
    }
}
