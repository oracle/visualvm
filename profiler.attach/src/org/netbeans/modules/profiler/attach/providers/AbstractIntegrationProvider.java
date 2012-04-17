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
package org.netbeans.modules.profiler.attach.providers;

import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.openide.util.NbBundle;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;

/**
 *
 * @author Jaroslav Bachorik
 */
@NbBundle.Messages({
    "ExportSetenvMessage=Depending on the version shell used, you may need to use \"<code>setenv</code>\" command instead of \"<code>export</code>\".",
    "ReduceOverheadMessage=When profiling CPU, you should set a meaningful instrumentation filter and/or select Part of Application option to reduce profiling overhead.",
    "RemoteAbsolutePathHint=<code>{0}</code> must be an absolute path",
    "SpacesInPathWarningMsg=On some systems/configurations, problems may occur when starting the profiler agent using parameters with spaces in the path. To fix these problems, please remove the quotes from agent parameters and modify these parameters to use 8.3 (DOS) path format.",
    "RemoteString=remote",
    "WorkDirMessage=Working directory is the directory from which the application will be started.",
    "ManualRemoteStep1Message=If you have not done it before click \"Generate Remote Pack ...\" button to generate an appropriate Remote pack. Once it is generated install it on the remote machine. The Remote pack root directory will be referred to as \"<code>{0}</code>\".",
    "ManualRemoteStep2Message=If you have not run profiling on this remote machine, run the <code>{0}</code> script first."
})
public abstract class AbstractIntegrationProvider implements WizardIntegrationProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    protected final String EXPORT_SETENV_MSG = Bundle.ExportSetenvMessage();
    protected final String REDUCE_OVERHEAD_MSG = Bundle.ReduceOverheadMessage();
    protected final String REMOTE_ABSOLUTE_PATH_HINT = Bundle.RemoteAbsolutePathHint("&lt;" + Bundle.RemoteString() + "&gt;"); // NOI18N
    protected final String SPACES_IN_PATH_WARNING_MSG = Bundle.SpacesInPathWarningMsg();
    protected WizardStep attachedWizard;
    private final String HTML_REMOTE_STRING = "&lt;" + Bundle.RemoteString() + "&gt;"; // NOI18N
    private final String WORK_DIR_MESSAGE = Bundle.WorkDirMessage();
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
    
    /**
     * @see org.netbeans.modules.profiler.attach.providers.WizardIntegrationProvider#getProfilerBinariesLink(attachSettings)
     */
    @Override
    final public String getProfilerBinariesLink(AttachSettings attachSettings) {
        if (needsSIPWorkaround()) {
            String agentArgs = IntegrationUtils.getProfilerAgentCommandLineArgs(attachSettings.getHostOS(), targetJava, attachSettings.isRemote(), attachSettings.getPort());
            return IntegrationUtils.getTemporaryBinariesLink(agentArgs);
        }
        return null;
    }

    protected abstract int getAttachWizardPriority();

    /**
     * 
     * @return Returns true if a provider requires space-in-path workaround in 
     * form of a temporary link (true is default)
     */
    protected boolean needsSIPWorkaround() {
        return true;
    }
    
    protected final String getManualRemoteStep1(final String targetOS) {
        return Bundle.ManualRemoteStep1Message(HTML_REMOTE_STRING);
    }

    protected final String getManualRemoteStep2(final String targetOS) {
        return Bundle.ManualRemoteStep2Message(IntegrationUtils.getRemoteCalibrateCommandString(targetOS, getTargetJava()));
    }
    
    @NbBundle.Messages({
        "# {0} - Actual profiler binaries link",
        "# {1} - The section to be updated (JAVA_OPTS, CATALINA_OPTS etc.)",
        "IntegrationProvider_TempLinkWarning=\"<i>{0}</i>\" is a temporary link. If you want to persist it across machine restarts copy the link to a permanent location and update the {1} section."
    })
    protected final void addLinkWarning(IntegrationHints instructions, String linkSection, AttachSettings attachSettings) {
        String link = getProfilerBinariesLink(attachSettings);
        if (link != null) {
            instructions.addWarning(Bundle.IntegrationProvider_TempLinkWarning(link, linkSection));
        }
    }

}
