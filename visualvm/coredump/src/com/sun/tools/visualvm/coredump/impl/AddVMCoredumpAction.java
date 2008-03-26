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

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRoot;
import com.sun.tools.visualvm.core.ui.actions.ActionUtils;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
import com.sun.tools.visualvm.coredump.CoreDumpsContainer;
import java.awt.event.ActionEvent;
import java.util.Set;
import org.openide.util.Utilities;

    
/**
 *
 * @author Jiri Sedlacek
 */
class AddVMCoredumpAction extends SingleDataSourceAction<DataSource> {
    
    private boolean notSupported;
    private boolean tracksSelection = false;
    
    
    public static AddVMCoredumpAction alwaysEnabled() {
        AddVMCoredumpAction action = new AddVMCoredumpAction();
        action.initialize();
        return action;
    }
    
    public static AddVMCoredumpAction selectionAware() {
        AddVMCoredumpAction action = new AddVMCoredumpAction().trackSelection();
        action.initialize();
        return action;
    }
    
    public void actionPerformed(DataSource dataSource, ActionEvent e) {
        CoreDumpConfigurator newCoreDumpConfiguration = CoreDumpConfigurator.defineCoreDump();
        if (newCoreDumpConfiguration != null) {
            CoreDumpProvider.createCoreDump(newCoreDumpConfiguration.getCoreDumpFile(),
                    newCoreDumpConfiguration.getDisplayname(), newCoreDumpConfiguration.getJavaHome(),
                    newCoreDumpConfiguration.deleteSourceFile());
        }
    }
    
    
    protected boolean isEnabled(DataSource dataSource) {
        return dataSource instanceof DataSourceRoot || dataSource instanceof CoreDumpsContainer;
    }
    
    protected void updateState(Set<DataSource> selectedDataSources) {
        if (notSupported) return;
        if (tracksSelection) super.updateState(selectedDataSources);
    }
    
    protected void initialize() {
        super.initialize();
        notSupported = Utilities.isWindows();
        setEnabled(!notSupported);
    }
    
    
    private AddVMCoredumpAction trackSelection() {
        tracksSelection = true;
        updateState(ActionUtils.getSelectedDataSources());
        return this;
    }
    
    
    private AddVMCoredumpAction() {
        super(DataSource.class);
        putValue(NAME, "Add VM Coredump...");
        putValue(SHORT_DESCRIPTION, "Add VM Coredump");
    }
    
}
