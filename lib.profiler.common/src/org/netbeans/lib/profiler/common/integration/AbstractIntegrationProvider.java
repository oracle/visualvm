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

package org.netbeans.lib.profiler.common.integration;

import org.netbeans.lib.profiler.common.AttachSettings;
import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * Abstract implementation of IntegrationProvider2.
 *
 * @author Jiri Sedlacek
 */
public abstract class AbstractIntegrationProvider implements IntegrationProvider2 {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.integration.Bundle"); // NOI18N
    private static final String WORKING_DIRECTORY_HINT = messages.getString("AbstractIntegrationProvider_WorkingDirectoryHint"); // NOI18N
                                                                                                                                 // -----
    public static final int SUPPORTED_TARGET_NONE = -1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String lastErrorMessage = ""; //NOI18N
    protected int[] supportedTargets;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    protected AbstractIntegrationProvider() {
        // other possible initialization comes here
    }

    protected AbstractIntegrationProvider(int[] supportedTargets) {
        this();
        this.supportedTargets = supportedTargets;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getAdditionalStepsInstructions(int supportedTarget, String targetOS, String targetJVM,
                                                 String targetJavaHomePath, Object[] selectedOptions,
                                                 AttachSettings attachSettings) {
        return ""; //NOI18N
    }

    public AutomaticIntegrationPanel getAutomaticIntegrationPanel() {
        return null;
    }

    public String getIntegrationReviewText(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                           Object[] selectedOptions, AttachSettings attachSettings) {
        return ""; //NOI18N
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public String getManualIntegrationStepsInstructions(int supportedTarget, String targetOS, String targetJVM,
                                                        AttachSettings attachSettings) {
        return ""; //NOI18N
    }

    public int getSupportedTarget(int supportedTargetIndex) {
        if ((supportedTargetIndex < 0) || (supportedTargetIndex >= getSupportedTargetsCount())) {
            return SUPPORTED_TARGET_NONE;
        }

        return supportedTargets[supportedTargetIndex];
    }

    public int getSupportedTarget(String supportedTargetName) {
        for (int i = 0; i < getSupportedTargetsCount(); i++) {
            if (getSupportedTargetName(getSupportedTarget(i)).equals(supportedTargetName)) {
                return getSupportedTarget(i);
            }
        }

        return SUPPORTED_TARGET_NONE;
    }

    // --- Shared implementation -------------------------------------------------
    public int getSupportedTargetsCount() {
        if (supportedTargets == null) {
            return 0;
        }

        return supportedTargets.length;
    }

    // --- Abstract methods to be implemented by descendants of this class -------
    public abstract String getSupportedTargetsType();

    public int getIntegrationProviderNumber() {
        return Integer.MAX_VALUE;
    }

    // --- Default implementation of IntegrationProvider2 methods -----------------
    public boolean isSingular() {
        return false;
    }

    public String getSupportedTargetName(int supportedTarget) {
        return ""; //NOI18N
    }

    public boolean performIntegration(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                      Object[] selectedOptions, AttachSettings attachSettings) {
        return false;
    }

    public void startTarget(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                            Object[] selectedOptions, AttachSettings attachSettings) {
    }

    public boolean supportsAutomaticIntegration(int supportedTarget, String targetOS, AttachSettings attachSettings) {
        return false;
    }

    public boolean supportsJavaPlatform(int supportedTarget, String targetJVM, AttachSettings attachSettings) {
        if (!attachSettings.isRemote() && !attachSettings.isDirect()) {
            return targetJVM != IntegrationUtils.PLATFORM_JAVA_50;
        }

        return true;
    }

    public boolean supportsTargetStartup(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                         Object[] selectedOptions, AttachSettings attachSettings) {
        return false;
    }
}
