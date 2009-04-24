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

package org.netbeans.modules.profiler.actions;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.PlatformsCustomizer;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * A panel to select Java Platform for profiling
 *
 * @author Ian Formanek
 */
public final class JavaPlatformSelector extends JPanel implements ListSelectionListener {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class JPListModel extends AbstractListModel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private java.util.List platforms;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private JPListModel(java.util.List platforms) {
            this.platforms = platforms;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Object getElementAt(int index) {
            return ((JavaPlatform) platforms.get(index)).getDisplayName();
        }

        public int getSize() {
            return platforms.size();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String OK_BUTTON_NAME = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                     "JavaPlatformSelector_OkButtonName"); // NOI18N
    private static final String USE_SEL_PLATFORM_CHCKBOX_NAME = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                    "JavaPlatformSelector_UseSelPlatformChckBoxName"); // NOI18N
    private static final String NO_SUPPORTED_PLATFORM_MSG = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                "JavaPlatformSelector_NoSupportedPlatformMsg"); // NOI18N
    private static final String SELECT_PLATFORM_CALIBRATE_MSG = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                    "JavaPlatformSelector_SelectPlatformCalibrateMsg"); // NOI18N
    private static final String SELECT_PLATFORM_CALIBRATE_DIALOG_CAPTION = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                               "JavaPlatformSelector_SelectPlatformCalibrateDialogCaption"); // NOI18N
    private static final String SELECT_PLATFORM_PROFILE_MSG = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                  "JavaPlatformSelector_SelectPlatformProfileMsg"); // NOI18N
    private static final String SELECT_PLATFORM_PROFILE_DIALOG_CAPTION = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                                             "JavaPlatformSelector_SelectPlatformProfileDialogCaption"); // NOI18N
    private static final String CANNOT_USE_PLATFORM_MSG = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                              "JavaPlatformSelector_CannotUsePlatform"); // NOI18N
    private static final String LIST_ACCESS_NAME = NbBundle.getMessage(JavaPlatformSelector.class,
                                                                       "JavaPlatformSelector_ListAccessName"); // NOI18N
                                                                                                               // -----
    private static JavaPlatformSelector defaultPlatform;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JLabel noteLabel;
    private JButton okButton = new JButton(OK_BUTTON_NAME);
    private JCheckBox alwaysCheckBox;
    private JList list;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private JavaPlatformSelector() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        list = new JList();
        list.setVisibleRowCount(6);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(this);
        list.getAccessibleContext().setAccessibleName(LIST_ACCESS_NAME);
        noteLabel = new JLabel();
        noteLabel.setLabelFor(list);
        noteLabel.setFocusable(false);        
        alwaysCheckBox = new JCheckBox(USE_SEL_PLATFORM_CHCKBOX_NAME, false);
        add(new JScrollPane(list), BorderLayout.CENTER);
        add(noteLabel, BorderLayout.NORTH);
        add(alwaysCheckBox, BorderLayout.SOUTH);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static synchronized JavaPlatformSelector getDefault() {
        if (defaultPlatform == null) {
            defaultPlatform = new JavaPlatformSelector();
        }

        return defaultPlatform;
    }

    public static java.util.List getSupportedPlatforms() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getPlatforms(null, new Specification("j2se", null)); // NOI18N
        ArrayList ret = new ArrayList();

        for (int i = 0; i < platforms.length; i++) {
            JavaPlatform platform = platforms[i];

            if (MiscUtils.isSupportedJVM(platform.getSystemProperties())) {
                ret.add(platforms[i]);
            }
        }

        return ret;
    }

    public JavaPlatform selectPlatformForCalibration() {
        java.util.List platforms = getSupportedPlatforms();

        if (platforms.size() == 0) {
            Profiler.getDefault().displayError(NO_SUPPORTED_PLATFORM_MSG);
            PlatformsCustomizer.showCustomizer(null);

            return null;
        }

        noteLabel.setText(SELECT_PLATFORM_CALIBRATE_MSG);
        noteLabel.getAccessibleContext().setAccessibleName(SELECT_PLATFORM_CALIBRATE_MSG);
        list.getAccessibleContext().setAccessibleDescription(SELECT_PLATFORM_CALIBRATE_MSG);
        list.setModel(new JPListModel(platforms));
        alwaysCheckBox.setVisible(false);

        DialogDescriptor dd = new DialogDescriptor(this, SELECT_PLATFORM_CALIBRATE_DIALOG_CAPTION, true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        list.setSelectedIndex(0);
        validateOKButton();
        Dialog selectDialog = ProfilerDialogs.createDialog(dd);
        selectDialog.getAccessibleContext().setAccessibleDescription(SELECT_PLATFORM_CALIBRATE_DIALOG_CAPTION);
        selectDialog.setVisible(true);

        if (dd.getValue() == okButton) {
            int idx = list.getSelectedIndex();

            return (JavaPlatform) platforms.get(idx);
        }

        return null;
    }

    public JavaPlatform selectPlatformToUse() {
        java.util.List platforms = getSupportedPlatforms();

        if (platforms.size() == 0) {
            Profiler.getDefault().displayError(NO_SUPPORTED_PLATFORM_MSG);
            PlatformsCustomizer.showCustomizer(null);

            return null;
        }

        noteLabel.setText(CANNOT_USE_PLATFORM_MSG + "<br>" // NOI18N
                          + SELECT_PLATFORM_PROFILE_MSG);
        noteLabel.getAccessibleContext().setAccessibleName(CANNOT_USE_PLATFORM_MSG + SELECT_PLATFORM_PROFILE_MSG);
        list.getAccessibleContext().setAccessibleDescription(CANNOT_USE_PLATFORM_MSG + SELECT_PLATFORM_PROFILE_MSG);
        list.setModel(new JPListModel(platforms));
        alwaysCheckBox.setSelected(false);
        alwaysCheckBox.setVisible(true);

        DialogDescriptor dd = new DialogDescriptor(this, SELECT_PLATFORM_PROFILE_DIALOG_CAPTION, true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        list.setSelectedIndex(0);
        validateOKButton();
        ProfilerDialogs.createDialog(dd).setVisible(true);

        if (dd.getValue() == okButton) {
            int idx = list.getSelectedIndex();
            JavaPlatform plat = (JavaPlatform) platforms.get(idx);

            if (alwaysCheckBox.isSelected()) {
                // in this case store the selected platform as global platform to use
                Profiler.getDefault().getGlobalProfilingSettings()
                        .setJavaPlatformForProfiling((plat == null) ? null : plat.getDisplayName());
            }

            return plat;
        }

        return null;
    }

    public void valueChanged(ListSelectionEvent e) {
        validateOKButton();
    }

    private void validateOKButton() {
        okButton.setEnabled(list.getSelectedIndex() != -1);
    }
}
