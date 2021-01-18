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

package org.graalvm.visualvm.lib.profiler.actions;

import org.graalvm.visualvm.lib.profiler.api.JavaPlatform;
import org.graalvm.visualvm.lib.common.Profiler;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.graalvm.visualvm.lib.profiler.api.JavaPlatform;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;


/**
 * A panel to select Java Platform for profiling
 *
 * @author Ian Formanek
 */
@NbBundle.Messages({
    "JavaPlatformSelector_OkButtonName=OK",
    "JavaPlatformSelector_UseSelPlatformCheckBoxName=Always use the selected platform for profiling",
    "JavaPlatformSelector_NoSupportedPlatformMsg=None of the installed Java Platforms can be used for profiling.\nNetBeans Profiler requires JDK 5.0 Update 4 and newer.\n\nPlease install a suitable Java Platform and run calibration again.",
    "JavaPlatformSelector_SelectPlatformCalibrateMsg=Select Java Platform to calibrate:",
    "JavaPlatformSelector_SelectPlatformCalibrateDialogCaption=Select Java Platform to calibrate",
    "JavaPlatformSelector_SelectPlatformProfileMsg=Please select Java Platform to use:",
    "JavaPlatformSelector_SelectPlatformProfileDialogCaption=Select Java Platform for Profiling",
    "JavaPlatformSelector_CannotUsePlatform=The Java Platform this project runs on cannot be used for profiling.",
    "JavaPlatformSelector_ListAccessName=List of Java Platforms available for profiling"
})
public final class JavaPlatformSelector extends JPanel implements ListSelectionListener, HelpCtx.Provider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class JPListModel extends AbstractListModel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private java.util.List<JavaPlatform> platforms;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        private JPListModel(java.util.List platforms) {
            this.platforms = platforms;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Object getElementAt(int index) {
            return platforms.get(index).getDisplayName();
        }

        public int getSize() {
            return platforms.size();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY = "JavaPlatformSelector.HelpCtx";
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static JavaPlatformSelector defaultPlatform;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JLabel noteLabel;
    private JButton okButton = new JButton(Bundle.JavaPlatformSelector_OkButtonName());
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
        list.getAccessibleContext().setAccessibleName(Bundle.JavaPlatformSelector_ListAccessName());
        noteLabel = new JLabel();
        noteLabel.setLabelFor(list);
        noteLabel.setFocusable(false);        
        alwaysCheckBox = new JCheckBox(Bundle.JavaPlatformSelector_UseSelPlatformCheckBoxName(), false);
        add(new JScrollPane(list) {
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = Math.max(d.width, 400);
                return d;
            }
        }, BorderLayout.CENTER);
        add(noteLabel, BorderLayout.NORTH);
        add(alwaysCheckBox, BorderLayout.SOUTH);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    @Override
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static synchronized JavaPlatformSelector getDefault() {
        if (defaultPlatform == null) {
            defaultPlatform = new JavaPlatformSelector();
        }

        return defaultPlatform;
    }

    public JavaPlatform selectPlatformForCalibration() {
        java.util.List<JavaPlatform> platforms = JavaPlatform.getPlatforms();

        if (platforms.size() == 0) {
            ProfilerDialogs.displayError(Bundle.JavaPlatformSelector_NoSupportedPlatformMsg());
            JavaPlatform.showCustomizer();

            return null;
        }

        noteLabel.setText(Bundle.JavaPlatformSelector_SelectPlatformCalibrateMsg());
        noteLabel.getAccessibleContext().setAccessibleName(Bundle.JavaPlatformSelector_SelectPlatformCalibrateMsg());
        list.getAccessibleContext().setAccessibleDescription(Bundle.JavaPlatformSelector_SelectPlatformCalibrateMsg());
        list.setModel(new JPListModel(platforms));
        alwaysCheckBox.setVisible(false);

        DialogDescriptor dd = new DialogDescriptor(this, Bundle.JavaPlatformSelector_SelectPlatformCalibrateDialogCaption(), true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        list.setSelectedIndex(0);
        validateOKButton();
        Dialog selectDialog = DialogDisplayer.getDefault().createDialog(dd);
        selectDialog.getAccessibleContext().setAccessibleDescription(Bundle.JavaPlatformSelector_SelectPlatformCalibrateDialogCaption());
        selectDialog.setVisible(true);

        if (dd.getValue() == okButton) {
            int idx = list.getSelectedIndex();

            return (JavaPlatform) platforms.get(idx);
        }

        return null;
    }

    public JavaPlatform selectPlatformToUse() {
        java.util.List platforms = JavaPlatform.getPlatforms();

        if (platforms.size() == 0) {
            ProfilerDialogs.displayError(Bundle.JavaPlatformSelector_NoSupportedPlatformMsg());
            JavaPlatform.showCustomizer();

            return null;
        }

        noteLabel.setText("<html>"+Bundle.JavaPlatformSelector_CannotUsePlatform() + "<br>" // NOI18N
                          + Bundle.JavaPlatformSelector_SelectPlatformProfileMsg()+"</html>"); // NOI18N
        noteLabel.getAccessibleContext().setAccessibleName(
            Bundle.JavaPlatformSelector_CannotUsePlatform() + 
            Bundle.JavaPlatformSelector_SelectPlatformProfileMsg());
        list.getAccessibleContext().setAccessibleDescription(
            Bundle.JavaPlatformSelector_CannotUsePlatform() + Bundle.JavaPlatformSelector_SelectPlatformCalibrateMsg());
        list.setModel(new JPListModel(platforms));
        alwaysCheckBox.setSelected(false);
        alwaysCheckBox.setVisible(true);

        DialogDescriptor dd = new DialogDescriptor(this, Bundle.JavaPlatformSelector_SelectPlatformProfileDialogCaption(), true,
                                                   new Object[] { okButton, DialogDescriptor.CANCEL_OPTION }, okButton,
                                                   DialogDescriptor.BOTTOM_ALIGN, null, null);
        list.setSelectedIndex(0);
        validateOKButton();
        DialogDisplayer.getDefault().createDialog(dd).setVisible(true);

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
