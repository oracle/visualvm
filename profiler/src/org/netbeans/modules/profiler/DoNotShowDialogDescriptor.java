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

package org.netbeans.modules.profiler;

import org.openide.DialogDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;


/**
 * A class representing operations with settings for predefined tasks in the IDE
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class DoNotShowDialogDescriptor extends DialogDescriptor {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String DO_NOT_SHOW_AGAIN_STRING = NbBundle.getMessage(DoNotShowDialogDescriptor.class,
                                                                               "DoNotShowDialogDescriptor_DoNotShowAgainString"); //NOI18N
                                                                                                                                  // -----
    private static final JCheckBox doNotShowAgain = new JCheckBox(DO_NOT_SHOW_AGAIN_STRING);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public DoNotShowDialogDescriptor(final Object innerPane, final String title, final String doNotShowKey) {
        super(createDoNotShowPanel(innerPane, doNotShowKey), title);
    }

    public DoNotShowDialogDescriptor(final Object innerPane, final String title, final boolean isModal, final ActionListener bl,
                                     final String doNotShowKey) {
        super(createDoNotShowPanel(innerPane, doNotShowKey), title, isModal, bl);
    }

    public DoNotShowDialogDescriptor(final Object innerPane, final String title, final boolean isModal, final int optionType,
                                     final Object initialValue, final ActionListener bl, final String doNotShowKey) {
        super(createDoNotShowPanel(innerPane, doNotShowKey), title, isModal, optionType, initialValue, bl);
    }

    public DoNotShowDialogDescriptor(final Object innerPane, final String title, final boolean modal, final Object[] options,
                                     final Object initialValue, final int optionsAlign, final HelpCtx helpCtx,
                                     final ActionListener bl, final String doNotShowKey) {
        super(createDoNotShowPanel(innerPane, doNotShowKey), title, modal, options, initialValue, optionsAlign, helpCtx, bl);
    }

    public DoNotShowDialogDescriptor(final Object innerPane, final String title, final boolean isModal, final int optionType,
                                     final Object initialValue, final int optionsAlign, final HelpCtx helpCtx,
                                     final ActionListener bl, final String doNotShowKey) {
        super(createDoNotShowPanel(innerPane, doNotShowKey), title, isModal, optionType, initialValue, optionsAlign, helpCtx, bl);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    private static Object createDoNotShowPanel(final Object innerPane, final String key) {
        if (!(innerPane instanceof Component)) {
            return innerPane;
        }

        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0, 5));
        panel.add((Component) innerPane, BorderLayout.CENTER);
        panel.add(doNotShowAgain, BorderLayout.SOUTH);

        return panel;
    }
}
