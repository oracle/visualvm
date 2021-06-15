/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.actions;

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
    "JavaPlatformSelector_UseSelPlatformChckBoxName=Always use the selected platform for profiling",
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
        alwaysCheckBox = new JCheckBox(Bundle.JavaPlatformSelector_UseSelPlatformChckBoxName(), false);
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

        if (platforms.isEmpty()) {
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

        if (platforms.isEmpty()) {
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
