/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.profiler.attach.providers;

import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.openide.util.NbBundle;
import java.text.MessageFormat;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class AbstractIntegrationProvider implements WizardIntegrationProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    protected final String EXPORT_SETENV_MSG = NbBundle.getMessage(AbstractIntegrationProvider.class, "ExportSetenvMessage"); // NOI18N
    protected final String REDUCE_OVERHEAD_MSG = NbBundle.getMessage(AbstractIntegrationProvider.class, "ReduceOverheadMessage"); // NOI18N
    protected final String REMOTE_ABSOLUTE_PATH_HINT = MessageFormat.format(NbBundle.getMessage(AbstractIntegrationProvider.class,
            "RemoteAbsolutePathHint"), // NOI18N
            new Object[]{
                "&lt;" + NbBundle.getMessage(AbstractIntegrationProvider.class,
                "RemoteString") + "&gt;"
            } // NOI18N
            );
    protected final String SPACES_IN_PATH_WARNING_MSG = NbBundle.getMessage(AbstractIntegrationProvider.class,
            "SpacesInPathWarningMsg"); // NOI18N
    protected WizardStep attachedWizard;
    private final String HTML_REMOTE_STRING = "&lt;" + NbBundle.getMessage(AbstractIntegrationProvider.class, "RemoteString") + "&gt;"; // NOI18N
    private final String WORK_DIR_MESSAGE = NbBundle.getMessage(AbstractIntegrationProvider.class, "WorkDirMessage"); // NOI18N
    private String targetJava = ""; // NOI18N
    private String targetJavaHomePath = ""; // NOI18N

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public AbstractIntegrationProvider() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public WizardStep getAttachedWizard() {
        return this.attachedWizard;
    }

    public String getDynamicWorkingDirectoryHint(String targetOS, AttachSettings attachSettings) {
        return WORK_DIR_MESSAGE;
    }

    public final void setTargetJava(String javaName) {
        this.targetJava = javaName;
    }

    public final String getTargetJava() {
        return this.targetJava;
    }

    public final void setTargetJavaHome(String path) {
        this.targetJavaHomePath = path;
    }

    public final String getTargetJavaHome() {
        return this.targetJavaHomePath;
    }

    public final void setTargetPlatform(TargetPlatform platform) {
        if (platform == null) {
            this.targetJava = ""; // NOI18N
            this.targetJavaHomePath = ""; // NOI18N
        } else {
            this.targetJava = platform.getName();
            this.targetJavaHomePath = platform.getHomePath();
        }
    }

    /**
     * NULL implementation of SettingsPersistor
     * Subclasses will override this method to expose their specific settings persistence behavior
     */
    public SettingsPersistor getSettingsPersistor() {
        return SettingsPersistor.NullSettingsPersistor.getInstance();
    }

    public boolean supportsAutomation() {
        return true;
    }

    public boolean supportsDirect() {
        return true;
    }

    public boolean supportsDynamic() {
        return true;
    }

    public boolean supportsDynamicPid() {
        return true;
    }

    public boolean supportsJVM(TargetPlatformEnum jvm, AttachSettings attachSettings) {
        if (jvm.equals(TargetPlatformEnum.JDK_CVM)) {
            return false;
        }
        return true;
    }

    public boolean supportsLocal() {
        return true;
    }

    public boolean supportsManual() {
        return true;
    }

    public boolean supportsRemote() {
        return true;
    }

    protected abstract int getAttachWizardPriority();

    protected final String getManualRemoteStep1(final String targetOS) {
        return MessageFormat.format(NbBundle.getMessage(AbstractIntegrationProvider.class, "ManualRemoteStep1Message"),
                new Object[]{"JDK 5.0/6.0", targetOS, HTML_REMOTE_STRING}); //NOI18N
    }

    protected final String getManualRemoteStep2(final String targetOS) {
        return MessageFormat.format(NbBundle.getMessage(AbstractIntegrationProvider.class, "ManualRemoteStep2Message"),
                new Object[]{IntegrationUtils.getRemoteCalibrateCommandString(targetOS)}); // NOI18N
    }
}
