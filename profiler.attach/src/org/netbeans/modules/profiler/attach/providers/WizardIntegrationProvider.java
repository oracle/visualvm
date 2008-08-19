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

import java.io.IOException;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.attach.spi.IntegrationProvider;
import org.netbeans.modules.profiler.attach.wizard.WizardContext;
import org.netbeans.modules.profiler.attach.wizard.steps.NullWizardStep;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;



/**
 *
 * @author Jaroslav Bachorik
 */
public interface WizardIntegrationProvider extends IntegrationProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    // <editor-fold defaultstate="collapsed" desc="NullIntegrationProvider">
    public class NullIntegrationProvider implements WizardIntegrationProvider {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public IntegrationProvider.IntegrationHints getAfterInstallationHints(AttachSettings attachSettings, boolean automation) {
            return new IntegrationProvider.IntegrationHints();
        }

        public WizardStep getAttachedWizard() {
            return new NullWizardStep();
        }

        public String getDynamicWorkingDirectoryHint(String targetOS, AttachSettings attachSettings) {
            return ""; // NOI18N
        }

        public IntegrationProvider.IntegrationHints getIntegrationReview(AttachSettings attachSettings) {
            return new IntegrationProvider.IntegrationHints();
        }

        public IntegrationProvider.IntegrationHints getModificationHints(AttachSettings attachSettings) {
            return new IntegrationProvider.IntegrationHints();
        }

        public SettingsPersistor getSettingsPersistor() {
            return SettingsPersistor.NullSettingsPersistor.getInstance();
        }

        public void setTargetJava(String javaName) {
        }

        public String getTargetJava() {
            return ""; // NOI18N
        }

        public void setTargetJavaHome(String path) {
        }

        public String getTargetJavaHome() {
            return ""; // NOI18N
        }

        public String getTitle() {
            return "<None>"; // NOI18N
        }

        public void setWizardContext(WizardContext context) {
        }

        public void categorize(IntegrationCategorizer categorizer) {
        }

        public void modify(AttachSettings attachSettings) {
        }

        public void run(AttachSettings attachSettings) {
        }

        public String exportRemotePack(AttachSettings settings, String exportPath) throws IOException {
            throw new IOException();
        }

        public boolean supportsAutomation() {
            return false;
        }

        public boolean supportsDirect() {
            return false;
        }

        public boolean supportsDynamic() {
            return false;
        }

        public boolean supportsDynamicPid() {
            return false;
        }

        public boolean supportsJVM(TargetPlatformEnum jvm, AttachSettings attachSettings) {
            return false;
        }

        public boolean supportsLocal() {
            return false;
        }

        public boolean supportsManual() {
            return false;
        }

        public boolean supportsRemote() {
            return false;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public WizardStep getAttachedWizard();

    public String getDynamicWorkingDirectoryHint(String targetOS, AttachSettings attachSettings);

    public SettingsPersistor getSettingsPersistor();

    public abstract void categorize(IntegrationCategorizer categorizer);

    public boolean supportsDynamicPid();

    public boolean supportsJVM(TargetPlatformEnum jvm, AttachSettings attachSettings);

    // </editor-fold>
}
