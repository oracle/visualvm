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
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import org.netbeans.modules.profiler.attach.providers.TargetPlatformEnum;
import org.netbeans.modules.profiler.attach.providers.WizardIntegrationProvider;


/**
 *
 * @author Jaroslav Bachorik
 */
public class AttachWizardContext implements WizardContext {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AttachSettings settings;
    private Map providerProperties = null;
    private TargetPlatformEnum selectedJvm = null;
    private WizardIntegrationProvider provider;
    private boolean automaticIntegration = false;
    private boolean configChanged;
    private boolean hideIntegration;
    private boolean manualIntegration = false;
    private boolean singularProviderSelection;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public AttachWizardContext() {
        this.settings = new AttachSettings();
        this.provider = new WizardIntegrationProvider.NullIntegrationProvider();
        this.providerProperties = new Hashtable();
        this.configChanged = false;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAttachSettings(AttachSettings as) {
        if (as == null) {
            this.settings.load(new Properties());
        } else {
            as.copyInto(this.settings);
        }
    }

    public AttachSettings getAttachSettings() {
        return this.settings;
    }

    public void setAutomatic(boolean value) {
        this.automaticIntegration = value;
    }

    public boolean isAutomatic() {
        return this.automaticIntegration;
    }

    public void setConfigChanged() {
        configChanged = true;
    }

    public void setHideIntegration(final boolean value) {
        hideIntegration = value;
    }

    public boolean isHideIntegration() {
        return hideIntegration;
    }

    public void setIntegrationProvider(WizardIntegrationProvider provider) {
        if (!this.provider.equals(provider)) {
            this.configChanged = true;
        }

        this.provider = provider;
    }

    public WizardIntegrationProvider getIntegrationProvider() {
        return this.provider;
    }

    public void setJvm(TargetPlatformEnum jvm) {
        this.selectedJvm = jvm;
    }

    public TargetPlatformEnum getJvm() {
        return this.selectedJvm;
    }

    public void setManual(boolean value) {
        this.manualIntegration = value;
    }

    public boolean isManual() {
        return this.manualIntegration;
    }

    public void setProviderSingular(final boolean value) {
        singularProviderSelection = value;
    }

    public boolean isProviderSingular() {
        return singularProviderSelection;
    }

    public boolean isReadyToPerform() {
        return configChanged;
    }

    public void resetConfigChanges() {
        configChanged = false;
    }
}
