/*
 * Copyright (c) 2008, 2009, Oracle and/or its affiliates. All rights reserved.
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
package org.visualvm.demoapplicationtype;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import javax.swing.JOptionPane;

public class AnagramAction extends SingleDataSourceAction<Application> {

    public AnagramAction() {
        super(Application.class);
        putValue(Action.NAME, "Show Anagram PID");
        putValue(Action.SHORT_DESCRIPTION, "Demoes a menu item");
    }

    @Override
    protected void actionPerformed(Application application, ActionEvent arg1) {
        JOptionPane.showMessageDialog(null, application.getPid());
    }

    //Here you can determine whether the menu item is enabled,
    //depending on the data source type that is selected. In this
    //example, the menu item is enabled for all types within
    //the current data source:
    @Override
    protected boolean isEnabled(Application application) {
        if (ApplicationTypeFactory.getApplicationTypeFor(application) instanceof AnagramApplicationType) {
            return true;
        }
        return false;
    }
}

