
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

package org.netbeans.modules.profiler.ui.stp;

import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.spi.ProjectTypeProfiler;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import org.netbeans.modules.profiler.attach.AttachWizard;
import org.netbeans.modules.profiler.projectsupport.utilities.ProjectUtilities;


/**
 *
 * @author Jiri Sedlacek
 */
public class Utils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static Dimension DIMENSION_SMALLEST = new Dimension(0, 0);
    private static int defaultSpinnerHeight = -1;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static AttachSettings getAttachSettings(Project project) {
        AttachSettings attachSettings = null;

        try {
            attachSettings = NetBeansProfiler.loadAttachSettings(project);
        } catch (IOException ex) {
        }

        return attachSettings;
    }

    //  public static boolean iAnalyzerSettings(ProfilingSettings settings) {
    //    if (settings == null) return false;
    //    return iAnalyzerSettings(settings.getProfilingType());
    //  }
    //  
    //  public static boolean iAnalyzerSettings(int type) {
    //    return type == ProfilingSettings.PROFILE_ANALYZE;
    //  }
    public static boolean isCPUSettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isCPUSettings(settings.getProfilingType());
    }

    public static boolean isCPUSettings(int type) {
        return (type == ProfilingSettings.PROFILE_CPU_ENTIRE) || (type == ProfilingSettings.PROFILE_CPU_PART)
               || (type == ProfilingSettings.PROFILE_CPU_STOPWATCH);
    }

    public static int getDefaultSpinnerHeight() {
        if (defaultSpinnerHeight == -1) {
            defaultSpinnerHeight = new JTextField().getPreferredSize().height;
        }

        return defaultSpinnerHeight;
    }

    public static boolean isMemorySettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isMemorySettings(settings.getProfilingType());
    }

    public static boolean isMemorySettings(int type) {
        return (type == ProfilingSettings.PROFILE_MEMORY_ALLOCATIONS) || (type == ProfilingSettings.PROFILE_MEMORY_LIVENESS);
    }

    public static boolean isMonitorSettings(ProfilingSettings settings) {
        if (settings == null) {
            return false;
        }

        return isMonitorSettings(settings.getProfilingType());
    }

    public static boolean isMonitorSettings(int type) {
        return type == ProfilingSettings.PROFILE_MONITOR;
    }

    public static String getProjectName(Project project) {
        if (project == null) {
            return SelectProfilingTask.EXTERNAL_APPLICATION_STRING;
        }

        ProjectInformation pi = ProjectUtils.getInformation(project);

        return pi.getDisplayName();
    }

    public static SelectProfilingTask.SettingsConfigurator getSettingsConfigurator(Project project) {
        ProjectTypeProfiler ptp = org.netbeans.modules.profiler.utils.ProjectUtilities.getProjectTypeProfiler(project);

        SelectProfilingTask.SettingsConfigurator configurator = ptp.getSettingsConfigurator();
        if (configurator == null) return DefaultSettingsConfigurator.SHARED_INSTANCE;

        return configurator;
    }

    public static JPanel createFillerPanel() {
        JPanel fillerPanel = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING)) {
            public Dimension getPreferredSize() {
                return DIMENSION_SMALLEST;
            }
        };

        fillerPanel.setOpaque(false);

        return fillerPanel;
    }

    public static JSeparator createHorizontalSeparator() {
        JSeparator horizontalSeparator = new JSeparator() {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };

        return horizontalSeparator;
    }

    public static AttachSettings selectAttachSettings(Project project) {
        AttachSettings attachSettings = getAttachSettings(project);

        if (attachSettings == null) {
            attachSettings = new AttachSettings();
        }

        return AttachWizard.getDefault().configure(attachSettings);
//        AttachWizard attachWizard = new AttachWizard();
//        attachWizard.init(attachSettings);
//
//        final WizardDescriptor wd = attachWizard.getWizardDescriptor();
//        final Dialog d = ProfilerDialogs.createDialog(wd);
//        d.pack();
//        d.setVisible(true);
//
//        if (wd.getValue() != WizardDescriptor.FINISH_OPTION) {
//            return null; // cancelled by the user
//        }
//
//        attachWizard.finish(); // wizard correctly finished
//
//        return attachWizard.getAttachSettings();
    }
}
