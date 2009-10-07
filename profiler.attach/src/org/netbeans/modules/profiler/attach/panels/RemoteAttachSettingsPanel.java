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

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.openide.util.HelpCtx;

/**
 *
 * @author Jaroslav Bachorik
 */
public class RemoteAttachSettingsPanel extends AttachWizardPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public class Model {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Collection availableOsList;
        private String remoteHost;
        private String remoteOs;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Model() {
            this.availableOsList = new Vector();
            this.availableOsList.add(IntegrationUtils.PLATFORM_WINDOWS_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_LINUX_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_LINUX_AMD64_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS);
            this.availableOsList.add(IntegrationUtils.PLATFORM_MAC_OS);

            this.remoteOs = null;
            this.remoteHost = null;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Vector getAvailableOsList() {
            Vector immutable = new Vector();

            for (Iterator it = this.availableOsList.iterator(); it.hasNext();) {
                immutable.add(it.next());
            }

            return immutable;
        }

        public void setRemoteHost(String host) {
            this.remoteHost = host;
            publishUpdate(new ChangeEvent(this));
        }

        public String getRemoteHost() {
            return this.remoteHost;
        }

        public void setRemoteOs(String os) {
            if (this.availableOsList.contains(os)) {
                this.remoteOs = os;
                publishUpdate(new ChangeEvent(this));
            }
        }

        public String getRemoteOs() {
            return this.remoteOs;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "RemoteAttachSettingsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private RemoteAttachSettingsPanel.Model model;
    private RemoteAttachSettingsPanelUI panel;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RemoteAttachSettingsPanel() {
        this.model = new RemoteAttachSettingsPanel.Model();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public HelpCtx getHelp() {
        return HELP_CTX;
    }

    public boolean isValid() {
        return (model.getRemoteHost() != null) && (model.getRemoteHost().length() > 0) && (model.getRemoteOs() != null)
               && (model.getRemoteOs().length() > 0);
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
        model.setRemoteHost(context.getAttachSettings().getHost());

        if ((model.getRemoteHost() != null) && (model.getRemoteHost().length() > 0)) {
            model.setRemoteOs(context.getAttachSettings().getHostOS());
        }
    }

    public void onExit(AttachWizardContext context) {
        AttachSettings settings = context.getAttachSettings();

        if (model.getRemoteHost() != null) {
            settings.setHost(model.getRemoteHost());
        }

        if (model.getRemoteOs() != null) {
            settings.setHostOS(model.getRemoteOs());
        }
    }

    public void onFinish(AttachWizardContext context) {
    }

    public void onPanelShow() {
        panel.refresh();
    }

    protected JPanel getRenderPanel() {
        if (panel == null) {
            panel = new RemoteAttachSettingsPanelUI(this.model);
        }

        return panel;
    }
}
