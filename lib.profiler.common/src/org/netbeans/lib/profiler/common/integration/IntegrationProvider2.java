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
import java.io.File;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.swing.JPanel;


/**
 * Interface to be implemented by each integration provider.
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 * @deprecated
 * @since 2006/04/04
 */
public interface IntegrationProvider2 {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public interface AutomaticIntegrationPanelListener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Notifies the listener that some options have changed
         */
        public void optionsChanged();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public abstract class AutomaticIntegrationPanel extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Vector listeners = new Vector();

        //~ Methods --------------------------------------------------------------------------------------------------------------

        /**
         * Should return last stored JavaPlatform name (Used name of IDE JavaPlatform)
         * @return last stored JavaPlatform name (Used name of IDE JavaPlatform)
         */
        public abstract String getJavaPlatform();

        /**
         * Sets array of Objects used for manipulating with additional options required by the IntegrationProvider2
         *
         * @param supportedTarget Supported target
         * @param targetOS Operating system of the target
         * @param targetJVM Java platform running the target
         * @param options additional options required by the IntIntegrationProvider2aram attachSettings Settings of target configuration
         */
        public abstract void setOptions(int supportedTarget, String targetOS, String targetJVM, Object[] selectedOptions,
                                        AttachSettings attachSettings);

        /**
         * Gets array of Objects used for manipulating with additional options required by the IntegrationProvider2
         *
         * @return array of Objects representing additional options required by the IntegrationProvider2
         */
        public abstract Object[] getOptions();

        /**
         * Checks if currently selected options are valid
         * @return <CODE>true</CODE> if currently selected options are valid
         */
        public abstract boolean areOptionsValid();

        /**
         * Called when the user changes Java platform for running the target
         *
         * @param supportedTarget Supported target
         * @param targetOS Operating system of the target
         * @param targetJVM Java platform running the target
         * @param options additional options required by the IntIntegrationProvider2aram attachSettings Settings of target configuration
         */
        public abstract void javaPlatformChanged(int supportedTarget, String targetOS, String targetJVM,
                                                 Object[] selectedOptions, AttachSettings attachSettings);

        /**
         * Here the IntegrationProvider2 can load last stored state (settings) of AutomaticIntegrationPanel
         *
         * @param supportedTarget Supported target
         * @param settingsDir Directory where the panel can store current options (can be null)
         */
        public abstract void loadCustomOptions(int supportedTarget, File settingsDir);

        /**
         * Here the IntegrationProvider2 can store current state (settings) of AutomaticIntegrationPanel
         *
         * @param supportedTarget Supported target
         * @param settingsDir Directory where the panel can store current options (can be null)
         * @param javaPlatform Name of Java platform which runs the server (Used name of IDE JavaPlatform)
         */
        public abstract void storeCustomOptions(int supportedTarget, File settingsDir, String javaPlatform);

        public void addOptionsChangeListener(AutomaticIntegrationPanelListener listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        public void removeOptionsChangeListener(AutomaticIntegrationPanelListener listener) {
            if (listeners.contains(listener)) {
                listeners.remove(listener);
            }
        }

        protected void fireOptionsChangedEvent() {
            for (int i = 0; i < listeners.size(); i++) {
                ((AutomaticIntegrationPanelListener) listeners.get(i)).optionsChanged();
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    static final ResourceBundle messages = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.integration.Bundle"); // NOI18N

    /**
     * Integration with Java Application
     */
    public static final String TARGET_TYPE_APPLICATION = messages.getString("IntegrationProvider_TargetTypeApplication"); // NOI18N

    /**
     * Integration with Java Applet
     */
    public static final String TARGET_TYPE_APPLET = messages.getString("IntegrationProvider_TargetTypeApplet"); // NOI18N

    /**
     * Integration with J2EE Web/Application Server
     */
    public static final String TARGET_TYPE_J2EEAPPLICATION = messages.getString("IntegrationProvider_TargetTypeJ2SeApplication"); // NOI18N

    /**
     * Integration with Database
     */
    public static final String TARGET_TYPE_DATABASE = messages.getString("IntegrationProvider_TargetTypeDatabase"); // NOI18N
                                                                                                                    // -----

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the HTML-formatted description of additional steps needed for finishing integration with supported target
     *
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param targetJavaHomePath JAVA_HOME of target JVM
     * @param selectedOptions other InteIntegrationProvider2cific parameters (typically configuration files to modify)
     * @param attachSettings Settings of target configuration
     * @return HTML-formatted description of additional steps needed for finishing integration with supported target
     */
    public String getAdditionalStepsInstructions(int supportedTarget, String targetOS, String targetJVM,
                                                 String targetJavaHomePath, Object[] selectedOptions,
                                                 AttachSettings attachSettings);

    /**
     * Gets panel for selecting parameters required for automatic integration.
     * This method must be implemented by every IntegrationProvider2 which supports automatic integration.
     *
     * @return panel for selecting parameters required for automatic integration
     */
    public AutomaticIntegrationPanel getAutomaticIntegrationPanel();

    /**
     * Returns number used for sorting Targets of the same Target Type
     * @return number used for sorting Targets of the same Target Type
     */
    public int getIntegrationProviderNumber();

    /**
     * Gets the HTML-formatted summary of integration steps that will be automatically performed
     *
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param targetJavaHomePath JAVA_HOME of target JVM
     * @param selectedOptions other InteIntegrationProvider2cific parameters (typically configuration files to modify)
     * @param attachSettings Settings of target configuration
     * @return HTML-formatted summary of integration steps that will be automatically performed
     */
    public String getIntegrationReviewText(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                           Object[] selectedOptions, AttachSettings attachSettings);

    /**
     * Gets the textual description of last error
     * @return textual description of last error
     */
    public String getLastErrorMessage();

    /**
     * Gets the HTML-formatted description of manual steps needed for integration with supported target
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param attachSettings Settings of target configuration
     * @return HTML-formatted description of manual steps needed for integration with supported target
     */
    public String getManualIntegrationStepsInstructions(int supportedTarget, String targetOS, String targetJVM,
                                                        AttachSettings attachSettings);

    /**
     * Checks if this provider is the only one for its target type (Java Application, Applet)
     * @return <CODE>true</CODE> if this provider is the only one for its target type
     */
    public boolean isSingular();

    /**
     * Returns supported target (defined by concrete IntegrationProvider2 implementation)
     *
     * @param supportedTargetIndex Index of supported target
     * @return Supported target
     */
    public int getSupportedTarget(int supportedTargetIndex);

    /**
     * Returns supported target
     * @param supportedTargetName Name of supported target
     * @return Supported target
     */
    public int getSupportedTarget(String supportedTargetName);

    /**
     * Returns name of supported target
     * @param supportedTarget Supported target
     * @return Name of supported target
     */
    public String getSupportedTargetName(int supportedTarget);

    /**
     * Returns number of targets supported by integration provider
     * @return Number of supported targets
     */
    public int getSupportedTargetsCount();

    /**
     * Returns type of targets supported by integration provider
     * @return type of supported target
     */
    public String getSupportedTargetsType();

    /**
     * Performs automatic integration
     *
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param targetJavaHomePath JAVA_HOME of target JVM
     * @param selectedOptions other InteIntegrationProvider2cific parameters (typically configuration files to modify)
     * @param attachSettings Settings of target configuration
     * @return <CODE>true</CODE> if automatic integration was successful
     */
    public boolean performIntegration(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                      Object[] selectedOptions, AttachSettings attachSettings);

    /**
     * Automatically starts the target in separate thread
     *
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param targetJavaHomePath JAVA_HOME of target JVM
     * @param selectedOptions other InteIntegrationProvider2cific parameters (typically configuration files to modify)
     * @param attachSettings Settings of target configuration
     */
    public void startTarget(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                            Object[] selectedOptions, AttachSettings attachSettings);

    /**
     * Checks if automatic integration with target configuration is supported by the integration provider
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param attachSettings Settings of target configuration
     * @return <CODE>true</CODE> if this target configuration is supported
     */
    public boolean supportsAutomaticIntegration(int supportedTarget, String targetOS, AttachSettings attachSettings);

    /**
     * Checks if provided java platform is supported by the IntegrationProvider2
     *
     * @param supportedTarget Supported target
     * @param targetJVM Java platform running the target
     * @param attachSettings Settings of target configuration
     * @return <CODE>true</CODE> if provided java platform is supported by the IntIntegrationProvider2
     */
    public boolean supportsJavaPlatform(int supportedTarget, String targetJVM, AttachSettings attachSettings);

    /**
     * Checks if automatic target startup is supported by the IntegrationProvider2
     *
     * @param supportedTarget Supported target
     * @param targetOS Operating system of the target
     * @param targetJVM Java platform running the target
     * @param targetJavaHomePath JAVA_HOME of target JVM
     * @param selectedOptions other InteIntegrationProvider2cific parameters (typically configuration files to modify)
     * @param attachSettings Settings of target configuration
     * @return <CODE>true</CODE> if automatic target startup is supported by the IntegrIntegrationProvider2
     */
    public boolean supportsTargetStartup(int supportedTarget, String targetOS, String targetJVM, String targetJavaHomePath,
                                         Object[] selectedOptions, AttachSettings attachSettings);
}
