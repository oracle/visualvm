/*
 *  Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
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
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */
package com.sun.tools.visualvm.coredump.impl;

import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import com.sun.tools.visualvm.coredump.CoreDumpsContainer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.ImageIcon;
import org.openide.util.Utilities;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddVMCoredumpAction extends SingleDataSourceAction<CoreDumpsContainer> {
    
    private static final String ICON_PATH = "com/sun/tools/visualvm/coredump/resources/addCoredump.png";
    private static final Image ICON =  Utilities.loadImage(ICON_PATH);
    
    private boolean notSupported;
    private boolean tracksSelection = false;
    
    private static AddVMCoredumpAction alwaysEnabled;
    private static AddVMCoredumpAction toolbarInstance;
    private static AddVMCoredumpAction selectionAware;
    
    public static synchronized AddVMCoredumpAction alwaysEnabled() {
        if (alwaysEnabled == null) {
            alwaysEnabled = new AddVMCoredumpAction();
            alwaysEnabled.initialize();
    }
        return alwaysEnabled;
    }
    
    public static synchronized AddVMCoredumpAction toolbarInstance() {
        if (toolbarInstance == null) {
            toolbarInstance = new AddVMCoredumpAction();
            toolbarInstance.putValue(SMALL_ICON, new ImageIcon(ICON));
            toolbarInstance.putValue("iconBase", ICON_PATH);
            toolbarInstance.initialize();
    }
        return toolbarInstance;
    }
    
    public static synchronized AddVMCoredumpAction selectionAware() {
        if (selectionAware == null) {
            selectionAware = new AddVMCoredumpAction().trackSelection();
            selectionAware.initialize();
    }
        return selectionAware;
    }
    
    public void actionPerformed(CoreDumpsContainer contanier, ActionEvent e) {
        CoreDumpConfigurator newCoreDumpConfiguration = CoreDumpConfigurator.defineCoreDump();
        if (newCoreDumpConfiguration != null) {
            CoreDumpProvider.createCoreDump(newCoreDumpConfiguration.getCoreDumpFile(),
                    newCoreDumpConfiguration.getDisplayname(), newCoreDumpConfiguration.getJavaHome(),
                    newCoreDumpConfiguration.deleteSourceFile());
        }
    }
    
    
    protected boolean isEnabled(CoreDumpsContainer contanier) {
        return true;
    }
    
    protected void updateState(Set<CoreDumpsContainer> coreDumpsContainerSet) {
        if (notSupported) return;
        if (tracksSelection) super.updateState(coreDumpsContainerSet);
    }
    
    protected void initialize() {
        notSupported = Utilities.isWindows();
        setEnabled(!notSupported);
        super.initialize();
    }
    
    
    private AddVMCoredumpAction trackSelection() {
        tracksSelection = true;
        updateState(ActionUtils.getSelectedDataSources(CoreDumpsContainer.class));
        return this;
    }
    
    
    private AddVMCoredumpAction() {
        super(CoreDumpsContainer.class);
        putValue(NAME, "Add VM Coredump...");
        putValue(SHORT_DESCRIPTION, "Add VM Coredump");
    }
    
}
