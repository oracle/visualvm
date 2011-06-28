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

package org.netbeans.modules.profiler.utils;

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.modules.profiler.api.ProfilingSettingsManager;
import org.openide.DialogDisplayer;


/**
 * Utilities for interaction with the NetBeans IDE
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class IDEUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CREATE_NEW_CONFIGURATION_HINT = NbBundle.getMessage(IDEUtils.class,
                                                                                    "IDEUtils_CreateNewConfigurationHint"); // NOI18N
    private static final String SELECT_SETTINGS_CONFIGURATION_LABEL_TEXT = NbBundle.getMessage(IDEUtils.class,
                                                                                               "IDEUtils_SelectSettingsConfigurationLabelText"); // NOI18N
    private static final String SELECT_SETTINGS_CONFIGURATION_DIALOG_CAPTION = NbBundle.getMessage(IDEUtils.class,
                                                                                                   "IDEUtils_SelectSettingsConfigurationDialogCaption"); // NOI18N
    private static final String INVALID_TARGET_JVM_EXEFILE_ERROR = NbBundle.getMessage(IDEUtils.class,
                                                                                       "IDEUtils_InvalidTargetJVMExeFileError"); // NOI18N // TODO: move to this package's bundle
    private static final String ERROR_CONVERTING_PROFILING_SETTINGS_MESSAGE = NbBundle.getMessage(IDEUtils.class,
                                                                                                  "IDEUtils_ErrorConvertingProfilingSettingsMessage"); //NOI18N
    private static final String LIST_ACCESS_NAME = NbBundle.getMessage(IDEUtils.class, "IDEUtils_ListAccessName"); //NOI18N
    private static final String OK_BUTTON_TEXT = NbBundle.getMessage(IDEUtils.class, "IDEUtils_OkButtonText"); //NOI18N
                                                                                                               // -----
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String getAntProfilerStartArgument15(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_15_STRING);
    }

    public static String getAntProfilerStartArgument16(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_16_STRING);
    }

    public static String getAntProfilerStartArgument17(int port, int architecture) {
        return getAntProfilerStartArgument(port, architecture, CommonConstants.JDK_17_STRING);
    }

//    // Searches for a localized help. The default directory is <profiler_cluster>/docs/profiler,
//    // localized help is in <profiler_cluster>/docs/profiler_<locale_suffix> as obtained by NbBundle.getLocalizingSuffixes()
//    // see Issue 65429 (http://www.netbeans.org/issues/show_bug.cgi?id=65429)
//    public static String getHelpDir() {
//        Iterator suffixesIterator = NbBundle.getLocalizingSuffixes();
//        File localizedHelpDir = null;
//
//        while (suffixesIterator.hasNext() && (localizedHelpDir == null)) {
//            localizedHelpDir = InstalledFileLocator.getDefault()
//                                                   .locate("docs/profiler" + suffixesIterator.next(),
//                                                           "org.netbeans.modules.profiler", false); //NOI18N
//        }
//
//        if (localizedHelpDir == null) {
//            return null;
//        } else {
//            return localizedHelpDir.getPath();
//        }
//    }

    /**
     * Opens a dialog that allows the user to select one of existing profiling settings
     */
    public static ProfilingSettings selectSettings(int type, org.netbeans.lib.profiler.common.ProfilingSettings[] availableSettings, org.netbeans.lib.profiler.common.ProfilingSettings settingsToSelect) {
        Object[] settings = new Object[availableSettings.length + 1];

        for (int i = 0; i < availableSettings.length; i++) {
            settings[i] = availableSettings[i];
        }

        settings[availableSettings.length] = CREATE_NEW_CONFIGURATION_HINT;

        // constuct the UI
        final JLabel label = new JLabel(SELECT_SETTINGS_CONFIGURATION_LABEL_TEXT);
        final JButton okButton = new JButton(OK_BUTTON_TEXT);
        final JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(450, 250));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setLayout(new BorderLayout(0, 5));
        panel.add(label, BorderLayout.NORTH);

        final JList list = new JList(settings);
        label.setLabelFor(list);
        list.getAccessibleContext().setAccessibleName(LIST_ACCESS_NAME);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    okButton.setEnabled(list.getSelectedIndex() != -1);
                }
            });

        if (settingsToSelect != null) {
            list.setSelectedValue(settingsToSelect, true);
        } else {
            list.setSelectedIndex(0);
        }

        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        final DialogDescriptor dd = new DialogDescriptor(panel, SELECT_SETTINGS_CONFIGURATION_DIALOG_CAPTION, true,
                                                         new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton, 0,
                                                         null, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.setVisible(true);

        if (dd.getValue() == okButton) {
            final int selectedIndex = list.getSelectedIndex();

            if (selectedIndex != -1) { // TODO [ian]: do not allow this, disable OK button if there is no selection

                if (selectedIndex < (settings.length - 1)) {
                    ProfilingSettings selectedSettings = (ProfilingSettings) settings[selectedIndex];
                    selectedSettings.setProfilingType(type);

                    return selectedSettings;
                } else { // create a new setting

                    ProfilingSettings newSettings = ProfilingSettingsManager.createNewSettings(type, availableSettings);

                    if (newSettings == null) {
                        return null; // cancelled by the user
                    }

                    newSettings.setProfilingType(type);

                    return newSettings;
                }
            }
        }

        return null;
    }

    private static String getAntProfilerStartArgument(int port, int architecture, String jdkVersion) {
        String ld = Profiler.getDefault().getLibsDir();

        // -agentpath:D:/Testing/41 userdir/lib/deployed/jdk15/windows/profilerinterface.dll=D:\Testing\41 userdir\lib,5140
        return "-agentpath:" // NOI18N
               + Platform.getAgentNativeLibFullName(ld, false, jdkVersion, architecture) + "=" // NOI18N
               + ld + "," // NOI18N
               + port + "," // NOI18N
               + System.getProperty("profiler.agent.connect.timeout", "10"); // NOI18N // 10 seconds timeout by default
    }
    
}
