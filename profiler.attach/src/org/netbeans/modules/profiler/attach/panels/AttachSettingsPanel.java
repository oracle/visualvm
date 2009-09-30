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

import org.netbeans.lib.profiler.common.AttachSettings;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.NbBundle;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import org.netbeans.modules.profiler.attach.providers.IntegrationCategorizer;
import org.netbeans.modules.profiler.attach.providers.WizardIntegrationProvider;
import org.netbeans.modules.profiler.attach.spi.IntegrationProvider;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;

/**
 *
 * @author Jaroslav Bachorik
 */
public class AttachSettingsPanel extends AttachWizardPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public class PanelModel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Target target = null;
        private TargetGroup targetGroup = null;
        private WizardIntegrationProvider dummyProvider = new WizardIntegrationProvider.NullIntegrationProvider();
        private TargetGroup[] targets = null;
        private boolean automaticIntegration;
        private boolean directAttach;
        private boolean dynamicAttach16;
        private boolean local;
        private boolean manualIntegration;
        private boolean remote;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public PanelModel() {
            Collection<? extends IntegrationProvider> instances = Lookup.getDefault().lookupAll(IntegrationProvider.class);

            final TargetGroup application = new TargetGroup(APPLICATION_STRING, true);
            final TargetGroup applet = new TargetGroup(APPLET_STRING, true);
            final TargetGroup server = new TargetGroup(J2EE_STRING, false);

            IntegrationCategorizer myCategorizer = new IntegrationCategorizer() {
                public void addApplet(IntegrationProvider provider, int priority) {
                    applet.addTarget(new Target(provider.getTitle(), (WizardIntegrationProvider) provider, priority));
                }

                public void addApplication(IntegrationProvider provider, int priority) {
                    application.addTarget(new Target(provider.getTitle(), (WizardIntegrationProvider) provider, priority));
                }

                public void addAppserver(IntegrationProvider provider, int priority) {
                    server.addTarget(new Target(provider.getTitle(), (WizardIntegrationProvider) provider, priority));
                }
            };

            for (Iterator it = instances.iterator(); it.hasNext();) {
                WizardIntegrationProvider provider = (WizardIntegrationProvider) it.next();
                provider.categorize(myCategorizer);
                provider.getSettingsPersistor().loadSettings();
            }

            if (application.getTargets().length == 0) {
                application.addTarget(new Target(dummyProvider.getTitle(), dummyProvider, 0));
                application.setNull(true);
            }

            ;

            if (applet.getTargets().length == 0) {
                applet.addTarget(new Target(dummyProvider.getTitle(), dummyProvider, 0));
                applet.setNull(true);
            }

            if (server.getTargets().length == 0) {
                server.addTarget(new Target(dummyProvider.getTitle(), dummyProvider, 0));
                server.setNull(true);
            }

            targets = new TargetGroup[] { application, applet, server };
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setAutomaticIntegration(boolean value) {
            this.automaticIntegration = value;
            this.manualIntegration = !value;
            publishUpdate();
        }

        public boolean isAutomaticIntegration() {
            return automaticIntegration;
        }

        public void setDirectAttach(boolean value) {
            this.directAttach = value;
            this.dynamicAttach16 = !value;
            publishUpdate();
        }

        public boolean isDirectAttach() {
            return directAttach;
        }

        public boolean isDynamicAttach16() {
            return dynamicAttach16;
        }

        // <editor-fold defaultstate="collapsed" desc="hints">
        public String getHints() {
            StringBuffer hintBuffer = new StringBuffer();

            if (!targetGroup.isNull()) {
                if (isLocal()) {
                    hintBuffer.append(getLocalHint()).append("<br>"); // NOI18N
                }

                if (isRemote()) {
                    hintBuffer.append(getRemoteHint()).append("<br>"); // NOI18N
                }

                if (isDirectAttach()) {
                    hintBuffer.append(getDirectHint()).append("<br>"); // NOI18N
                }

                if (isDynamicAttach16()) {
                    hintBuffer.append(getDynamicHint()).append("<br>"); // NOI18N
                }
            } else {
                hintBuffer.append("No provider registered!"); // NOI18B
            }

            return hintBuffer.toString();
        }

        public void setLocal(boolean value) {
            this.local = value;
            this.remote = !value;
            publishUpdate();
        }

        public boolean isLocal() {
            return this.local;
        }

        public void setManualIntegration(boolean value) {
            this.manualIntegration = value;
            this.automaticIntegration = !value;
            publishUpdate();
        }

        public boolean isManualIntegration() {
            return manualIntegration;
        }

        public void setRemote(boolean value) {
            this.remote = value;
            this.local = !value;
            publishUpdate();
        }

        public boolean isRemote() {
            return this.remote;
        }

        public void setTarget(Target target) {
            this.target = target;
            publishUpdate();
        }

        public Target getTarget() {
            //      if (this.target == null && this.getTargetGroup().getTargets().length > 0)
            //        this.target = this.getTargetGroup().getTargets()[0];
            return this.target;
        }

        public void setTargetGroup(TargetGroup group) {
            this.targetGroup = group;
            publishUpdate();
        }

        public TargetGroup getTargetGroup() {
            //      if (this.targetGroup == null && this.targets.length > 0)
            //        this.targetGroup = targets[0];
            return this.targetGroup;
        }

        public TargetGroup[] getTargetGroups() {
            return targets;
        }

        void setDynamicAttach16(boolean value) {
            this.dynamicAttach16 = value;
            this.directAttach = !value;
            publishUpdate();
        }

        private String getDirectHint() {
            StringBuffer hint = new StringBuffer();

            if (model.isLocal()) { // don't display hints for disabled features

                if (getContext().getIntegrationProvider().supportsDirect()) {
                    hint.append(MessageFormat.format(DIRECT_HELP_STRING, new Object[] { getTargetGroup().getName() }));
                }

                if (hint.length() > 0) {
                    hint.append(' '); // NOI18N
                }

                if (getContext().getIntegrationProvider().supportsDynamic()) {
                    hint.append(MessageFormat.format(DYNAMIC_HELP_STRING, new Object[] { getTargetGroup().getName() }));
                } else {
                    hint.append(MessageFormat.format(DYNAMIC_NOSUPP_HELP_STRING, new Object[] { getTarget().getName() }));
                }
            }

            return hint.toString();
        }

        private String getDynamicHint() {
            return new StringBuffer(getDirectHint()).append(' ')
                                                    .append(MessageFormat.format(DYNAMIC_JVM_HELP_STRING,
                                                                                 new Object[] { "JDK 6.0/7.0" })).toString(); // NOI18N
        }

        private String getLocalHint() {
            StringBuffer hint = new StringBuffer();

            if (getContext().getIntegrationProvider().supportsLocal()) {
                hint.append(MessageFormat.format(LOCAL_HELP_STRING, new Object[] { getTargetGroup().getName() }));
            }

            if (hint.length() > 0) {
                hint.append(' '); // NOI18N
            }

            if (getContext().getIntegrationProvider().supportsRemote()) {
                hint.append(MessageFormat.format(REMOTE_HELP_STRING, new Object[] { getTargetGroup().getName() }));
            } else {
                hint.append(MessageFormat.format(REMOTE_NOSUPP_HELP_STRING, new Object[] { getTarget().getName() }));
            }

            return hint.toString();
        }

        private String getRemoteHint() {
            return new StringBuffer(getLocalHint()).append(' ').append(REMOTE_PACKS_HELP_STRING).toString(); // NOI18N
        }

        // </editor-fold>
    }

    public class Target implements Comparable {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String name;
        private WizardIntegrationProvider provider;
        private int priority;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Target(String name, WizardIntegrationProvider provider, int priority) {
            this.name = name;
            this.provider = provider;
            this.priority = priority;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getName() {
            return this.name;
        }

        public int getPriority() {
            return this.priority;
        }

        public WizardIntegrationProvider getProvider() {
            return this.provider;
        }

        public int compareTo(Object o) {
            if (o == null) {
                return 0;
            }

            if (!(o instanceof Target)) {
                return 0;
            }

            int priorityA = this.priority;
            int priorityB = ((Target) o).getPriority();

            if (priorityA > priorityB) {
                return 1;
            } else if (priorityA < priorityB) {
                return -1;
            } else {
                return 0;
            }
        }

        public boolean supportsAutomation() {
            return this.provider.supportsAutomation();
        }

        public boolean supportsDirectAttach() {
            return this.provider.supportsDirect();
        }

        public boolean supportsDynamicAttach() {
            return this.provider.supportsDynamic();
        }

        public boolean supportsLocalProfiling() {
            return this.provider.supportsLocal();
        }

        public boolean supportsRemoteProfiling() {
            return this.provider.supportsRemote();
        }

        public String toString() {
            return this.name;
        }
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Model implementation">
    public class TargetGroup {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private List /*<Target>*/ targetList;
        private String name;
        private boolean dirty;
        private boolean nullGroup;
        private boolean singular;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TargetGroup(String name, boolean singular) {
            this(name, new Target[] {  }, singular);
        }

        public TargetGroup(String name, Target[] targets, boolean singular) {
            this.name = name;
            this.singular = singular;
            this.targetList = new Vector();

            for (int i = 0; i < targets.length; i++) {
                this.targetList.add(targets[i]);
            }

            Collections.sort(this.targetList);
            this.nullGroup = false;
            this.dirty = true;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getName() {
            return this.name;
        }

        public void setNull(boolean value) {
            this.nullGroup = value;
        }

        public boolean isNull() {
            return this.nullGroup;
        }

        public boolean isSingular() {
            return this.singular;
        }

        public Target[] getTargets() {
            if (this.dirty) {
                Collections.sort(this.targetList);
                this.dirty = false;
            }

            return (Target[]) this.targetList.toArray(new Target[] {  });
        }

        public void addTarget(Target target) {
            this.targetList.add(target);
            this.dirty = true;
        }

        public String toString() {
            return this.name;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // <editor-fold defaultstate="collapsed" desc="I18N constants">
    private static final String LOCAL_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                        "TargetSettingsWizardPanelUI_LocalHelpString"); // NOI18N
    private static final String REMOTE_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                         "TargetSettingsWizardPanelUI_RemoteHelpString"); // NOI18N
    private static final String REMOTE_NOSUPP_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                                "TargetSettingsWizardPanelUI_RemoteNosuppHelpString"); // NOI18N
    private static final String REMOTE_PACKS_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                               "TargetSettingsWizardPanelUI_RemotePacksHelpString"); // NOI18N
    private static final String DIRECT_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                         "TargetSettingsWizardPanelUI_DirectHelpString"); // NOI18N
    private static final String DYNAMIC_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                          "TargetSettingsWizardPanelUI_DynamicHelpString"); // NOI18N
    private static final String DYNAMIC_NOSUPP_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                                 "TargetSettingsWizardPanelUI_DynamicNosuppHelpString"); // NOI18N
    private static final String DYNAMIC_JVM_HELP_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                              "TargetSettingsWizardPanelUI_DynamicJvmHelpString"); // NOI18N
    private static final String APPLICATION_STRING = NbBundle.getMessage(AttachSettingsPanel.class,
                                                                         "AttachWizard_GroupApplication"); // NOI18N
    private static final String APPLET_STRING = NbBundle.getMessage(AttachSettingsPanel.class, "AttachWizard_GroupApplet"); // NOI18N
    private static final String J2EE_STRING = NbBundle.getMessage(AttachSettingsPanel.class, "AttachWizard_GroupJ2EE"); // NOI18N

    // </editor-fold>
    private static final String HELP_CTX_KEY = "AttachSettingsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachSettingsPanelUI panel = null;
    private PanelModel model = new PanelModel();
    private WizardIntegrationProvider nullProvider = new WizardIntegrationProvider.NullIntegrationProvider();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of AttachSettingsPanel */
    public AttachSettingsPanel() {
        super();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelp() {
        return HELP_CTX;
    }

    public boolean isValid() {
        return (model.getTargetGroup() != null) && !model.getTargetGroup().isNull() && (model.getTarget() != null)
               && (model.isRemote() || (model.isLocal() && (model.isDirectAttach() || model.isDynamicAttach16())));
    }

    public boolean canBack(AttachWizardContext context) {
        return true;
    }

    public boolean canFinish(AttachWizardContext context) {
        return false;
    }

    public boolean canNext(AttachWizardContext context) {
        return isValid();
    }

    public boolean onCancel(AttachWizardContext context) {
        return true;
    }

    public void onEnter(AttachWizardContext context) {
        getContext().setHideIntegration(true);
        setTrackUpdates(false);

        String preselectedGroupName = context.getAttachSettings().getTargetType();
        String preselectedTargetName = context.getAttachSettings().getServerType();

        TargetGroup preselectedGroup = null;
        Target preselectedTarget = null;

        TargetGroup[] groups = model.getTargetGroups();
        outer: 
        for (int i = 0; i < groups.length; i++) {
            if (groups[i].getName().equals(preselectedGroupName)) {
                preselectedGroup = groups[i];
                preselectedGroupName = preselectedGroup.getName();

                if (preselectedGroup.isSingular()) {
                    preselectedTarget = preselectedGroup.getTargets()[0];
                    preselectedTargetName = preselectedTarget.getName();

                    break outer;
                }
            }

            for (int j = 0; j < groups[i].getTargets().length; j++) {
                if (groups[i].getTargets()[j].getProvider().equals(context.getIntegrationProvider())) {
                    preselectedGroup = groups[i];
                    preselectedTarget = groups[i].getTargets()[j];

                    break outer;
                }

                if (groups[i].getTargets()[j].getName().equals(preselectedTargetName)) {
                    preselectedTarget = groups[i].getTargets()[j];
                }
            }
        }

        model.setTargetGroup(preselectedGroup);
        model.setTarget(preselectedTarget);

        model.setAutomaticIntegration(context.isAutomatic());
        model.setManualIntegration(context.isManual());

        if (context.getAttachSettings().isDirect()) {
            model.setDirectAttach(true);
        } else if (context.getAttachSettings().isDynamic16()) {
            model.setDynamicAttach16(true);
        } else {
            throw new IllegalArgumentException("invalid attach settings " + context.getAttachSettings()); // NOI18N
        }

        model.setRemote(context.getAttachSettings().isRemote());

        ((AttachSettingsPanelUI) getRenderPanel()).loadModel();
        setTrackUpdates(true);
    }

    public void onExit(AttachWizardContext context) {
        storeSettings();
    }

    public void onFinish(AttachWizardContext context) {
    }

    public void onPanelShow() {
        // overtake the defaults set by the UI
        ((AttachSettingsPanelUI) getRenderPanel()).applyCombos();
        ((AttachSettingsPanelUI) getRenderPanel()).loadModel();
    }

    protected JPanel getRenderPanel() {
        if (panel == null) {
            panel = new AttachSettingsPanelUI(model);
        }

        return panel;
    }

    private void publishUpdate() {
        if (!isTrackUpdates()) {
            return;
        }

        if (storeSettings()) {
            publishUpdate(new ChangeEvent(this));
        }
    }

    private boolean storeSettings() {
        AttachWizardContext context = getContext();

        if (context == null) {
            return false;
        }

        if (model.getTarget() == null) {
            return true;
        }

        AttachSettings settings = context.getAttachSettings();

        if (model.isLocal() || model.isRemote()) {
            settings.setRemote(model.isRemote());
        }

        settings.setDirect(model.isDirectAttach());
        settings.setDynamic16(model.isDynamicAttach16());

        settings.setTargetType(model.getTargetGroup().getName());
        settings.setServerType(model.getTarget().getName());

        context.setIntegrationProvider(model.getTarget().getProvider());
        context.setAutomatic(context.getIntegrationProvider().supportsAutomation()
                             && !context.getIntegrationProvider().supportsManual());
        context.setManual(context.getIntegrationProvider().supportsManual()
                          && (!context.getIntegrationProvider().supportsAutomation()));
        context.setProviderSingular(model.getTargetGroup().isSingular());

        settings.setHostOS(null); // reset host os

        return true;
    }
}
