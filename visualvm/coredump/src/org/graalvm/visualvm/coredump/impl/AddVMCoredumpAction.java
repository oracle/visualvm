/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package org.graalvm.visualvm.coredump.impl;

import org.graalvm.visualvm.core.ui.actions.SingleDataSourceAction;
import org.graalvm.visualvm.coredump.CoreDumpsContainer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddVMCoredumpAction extends SingleDataSourceAction<CoreDumpsContainer> {
    
    private static final String ICON_PATH = "org/graalvm/visualvm/coredump/resources/addCoredump.png";    // NOI18N
    private static final Image ICON =  ImageUtilities.loadImage(ICON_PATH);
    
    private boolean tracksSelection = false;
    
    private static AddVMCoredumpAction alwaysEnabled;
    private static AddVMCoredumpAction selectionAware;
    
    
    public static synchronized AddVMCoredumpAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddVMCoredumpAction();
            alwaysEnabled.putValue(SMALL_ICON, new ImageIcon(ICON));
            alwaysEnabled.putValue("iconBase", ICON_PATH);  // NOI18N
        }
        return alwaysEnabled;
    }
    
    public static synchronized AddVMCoredumpAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddVMCoredumpAction();
            selectionAware.tracksSelection = true;
        }
        return selectionAware;
    }
    
    public void actionPerformed(CoreDumpsContainer container, ActionEvent e) {
        CoreDumpConfigurator newCoreDumpConfiguration = CoreDumpConfigurator.defineCoreDump();
        if (newCoreDumpConfiguration != null) {
            CoreDumpProvider.createCoreDump(newCoreDumpConfiguration.getCoreDumpFile(),
                    newCoreDumpConfiguration.getDisplayname(), newCoreDumpConfiguration.getJavaHome(),
                    newCoreDumpConfiguration.deleteSourceFile());
        }
    }
    
    
    protected boolean isEnabled(CoreDumpsContainer container) {
        return true;
    }
    
    protected void initialize() {
        if (tracksSelection) super.initialize();
    }
    
    
    private AddVMCoredumpAction() {
        super(CoreDumpsContainer.class);
        putValue(NAME, NbBundle.getMessage(AddVMCoredumpAction.class, "LBL_Add_VM_Coredump"));  // NOI18N
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(AddVMCoredumpAction.class, "ToolTip_Add_VM_Coredump")); // NOI18N
    }
    
}
