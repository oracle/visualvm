/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.customtype.actions;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.application.type.DefaultApplicationType;
import org.graalvm.visualvm.modules.customtype.ApplicationTypeManager;
import org.graalvm.visualvm.modules.customtype.ui.ApplicationTypeForm;
import org.graalvm.visualvm.core.ui.actions.DataSourceAction;
import org.graalvm.visualvm.modules.customtype.ApplicationType;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Set;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 *
 * @author Jaroslav Bachorik
 */
public class NewApplicationTypeAction extends DataSourceAction<Application> {
    private Application selectedApp = null;

    final private static class Singleton {

        final private static NewApplicationTypeAction INSTANCE = new NewApplicationTypeAction();
    }

    final public static NewApplicationTypeAction getDefault() {
        return Singleton.INSTANCE;
    }

    private NewApplicationTypeAction() {
        super(Application.class);
        putValue(NAME, "Create New Application Type...");
    }

    @Override
    protected void updateState(Set<Application> selectedApps) {
        if (selectedApps.size() == 1) {
            selectedApp = selectedApps.iterator().next();
            org.graalvm.visualvm.application.type.ApplicationType at = ApplicationTypeFactory.getApplicationTypeFor(selectedApp);
            if (at instanceof DefaultApplicationType) {
                if (!JvmFactory.getJVMFor(selectedApp).getMainClass().isEmpty()) {
                    setEnabled(true);
                    return;
                }
            }
            selectedApp = null;
            setEnabled(false);
        } else {
            selectedApp = null;
            setEnabled(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Jvm jvm = JvmFactory.getJVMFor(selectedApp);
        if (jvm != null) {
            ApplicationType eat = ApplicationTypeManager.getDefault().newType(jvm.getMainClass());
            final ApplicationTypeForm form = new ApplicationTypeForm(eat);

            final DialogDescriptor[] dd = new DialogDescriptor[1];

            dd[0] = new DialogDescriptor(form, "New Application Type Details",  true, new Object[]{form.getValidationSupport().getOkButton(), DialogDescriptor.CANCEL_OPTION}, form.getValidationSupport().getOkButton(), DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource().equals(form.getValidationSupport().getOkButton()) && form.storeData()) {
                        dd[0].setClosingOptions(new Object[] {form.getValidationSupport().getOkButton()});
                    }
                }
            });

            dd[0].setClosingOptions(new Object[] {DialogDescriptor.CANCEL_OPTION});

            Dialog dlg = DialogDisplayer.getDefault().createDialog(dd[0]);
            dlg.setVisible(true);
            if (dd[0].getValue() == form.getValidationSupport().getOkButton()) {
                try {
                    ApplicationTypeManager.getDefault().storeType(eat);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

}
