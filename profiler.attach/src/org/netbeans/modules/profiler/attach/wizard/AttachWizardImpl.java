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
public class AttachWizardImpl extends AbstractWizard {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // I18N String constants
    private static final String ATTACH_WIZARD_CAPTION = NbBundle.getMessage(AttachWizardImpl.class, "AttachWizard_AttachWizardCaption"); // NOI18N
    private static final String STEP_INTEGRATION_TYPE = NbBundle.getMessage(AttachWizardImpl.class,
                                                                            "AttachWizard_ChooseIntegrationTypeString"); // NOI18N
    private static final String STEP_REVIEW_ATTACHSETTINGS = NbBundle.getMessage(AttachWizardImpl.class,
                                                                                 "AttachWizard_ReviewAttachSettingsString"); // NOI18N
    private static final String STEP_SELECT_TARGETTYPE = NbBundle.getMessage(AttachWizardImpl.class,
                                                                             "AttachWizard_SelectTargetTypeString"); // NOI18N
    private static final String STEP_REMOTE_SETTINGS = NbBundle.getMessage(AttachWizardImpl.class, "AttachWizard_RemoteSystemString"); // NOI18N
    private static final String STEP_DYNAMIC_SETTINGS = NbBundle.getMessage(AttachWizardImpl.class, "AttachWizard_DynamicAttachString"); // NOI18N
    private static final String STEP_PERFORM_INTEGRATION = NbBundle.getMessage(AttachWizardImpl.class,
                                                                               "AttachWizard_PerformIntegrationString"); // NOI18N
    private static final String STEP_REVIEW_ADDITIONALSTEPS = NbBundle.getMessage(AttachWizardImpl.class,
                                                                                  "AttachWizard_ReviewAdditionalStepsString"); // NOI18N
    private static final String STEP_MANUAL_INTEGRATION = NbBundle.getMessage(AttachWizardImpl.class,
                                                                              "AttachWizard_ManualIntegrationStep"); // NOI18N
    private static final String STEP_AUTOMATIC_INTEGRATION = NbBundle.getMessage(AttachWizardImpl.class,
                                                                                 "AttachWizard_AutomaticIntegrationStep"); // NOI18N
    private static final String PROVIDER_SPECIFIC_SETTINGS = NbBundle.getMessage(AttachWizardImpl.class,
                                                                                 "AttachWizard_ProviderSpecificSettings"); // NOI18N
    private static final String REFINE_ATTACHMENT_SETTINGS = NbBundle.getMessage(AttachWizardImpl.class,
                                                                                 "AttachWizard_RefineAttachmentSettings"); // NOI18N
    private static final String INTEGRATE_PROFILER = NbBundle.getMessage(AttachWizardImpl.class, "AttachWizard_IntegrateProfiler"); // NOI18N

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
        return ATTACH_WIZARD_CAPTION;
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

        CompositeWizardStep rootStep = new CompositeWizardStep(getContext(), ATTACH_WIZARD_CAPTION);
        rootStep.addStep(STEP_SELECT_TARGETTYPE, new AttachSettingsPanel());
        rootStep.addStep(prepareAdditionalSettingsStep());
        rootStep.addStep(STEP_REVIEW_ATTACHSETTINGS, new ReviewSettingsPanel());
        //    rootStep.addStep(new SimpleWizardStep(getContext(), "...", new NullWizardScreen(), new ConditionalFunctor() {
        //      public boolean evaluate(WizardContext context) {
        //        return ((AttachWizardContext)context).isHideIntegration();
        //      }
        //    }));
        rootStep.addStep(prepareIntegrationStep());

        return rootStep;
    }

    private WizardStep prepareAdditionalSettingsStep() {
        CompositeWizardStep additionalSettings = new CompositeWizardStep(getContext(), REFINE_ATTACHMENT_SETTINGS,
                                                                         new ConditionalFunctor() { // NOI18N
                public boolean evaluate(WizardContext context) {
                    AttachSettings settings = ((AttachWizardContext) context).getAttachSettings();

                    return settings.isRemote() || (!settings.isDirect() && !settings.isDynamic16());
                }
            });

        WizardStep remoteAttach = new SimpleWizardStep(getContext(), STEP_REMOTE_SETTINGS, new RemoteAttachSettingsPanel(),
                                                       new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachSettings settings = ((AttachWizardContext) context).getAttachSettings();

                    return settings.isRemote();
                }
            });
        additionalSettings.addStep(remoteAttach);

        return additionalSettings;
    }

    private WizardStep prepareIntegrationStep() {
        CompositeWizardStep integrationStep = new CompositeWizardStep(getContext(), INTEGRATE_PROFILER,
                                                                      new ConditionalFunctor() { // NOI18N
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;
                    AttachSettings settings = ctx.getAttachSettings();

                    return ctx.getIntegrationProvider().supportsManual() || ctx.getIntegrationProvider().supportsAutomation();
                }
            });

        integrationStep.addStep(STEP_INTEGRATION_TYPE, new SelectIntegrationTypePanel(),
                                new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return isAutomationAllowed(ctx);
                }
            });

        CompositeWizardStep automaticIntegration = new CompositeWizardStep(getContext(), STEP_AUTOMATIC_INTEGRATION,
                                                                           new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isAutomatic();
                }
            });

        automaticIntegration.addStep(this.proxy);
        automaticIntegration.addStep(STEP_PERFORM_INTEGRATION, new PerformIntegrationPanel(),
                                     new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    //        AttachWizardContext ctx = (AttachWizardContext)context;
                    //        return ctx.isReadyToPerform();
                    return true;
                }
            });
        automaticIntegration.addStep(STEP_REVIEW_ADDITIONALSTEPS, new ReviewAdditionalStepsPanel());

        integrationStep.addStep(automaticIntegration);

        integrationStep.addStep(prepareManualIntegrationStep());

        return integrationStep;
    }

    private WizardStep prepareManualIntegrationStep() {
        return new SimpleWizardStep(getContext(), STEP_MANUAL_INTEGRATION, new ManualIntegrationPanel(),
                                    new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isManual();
                }
            });
    }

    private ProxyWizardStep prepareProviderProxyStep() {
        return new ProxyWizardStep(getContext(), PROVIDER_SPECIFIC_SETTINGS,
                                   new ConditionalFunctor() {
                public boolean evaluate(WizardContext context) {
                    AttachWizardContext ctx = (AttachWizardContext) context;

                    return ctx.isAutomatic();
                }
            }) {
                public void setNext() {
                    super.setNext();

                    if (canHandle()) {
                        ((AttachWizardContext) getContext()).getIntegrationProvider().getSettingsPersistor().storeSettings();
                    }
                }
            };
    }
}
