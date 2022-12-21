/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.appui.options;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@OptionsPanelController.TopLevelRegistration(
        id = "FiltersOptions",
        categoryName = "#FiltersOptionsCategory_Name", 
        iconBase = "org/graalvm/visualvm/modules/appui/options/filters.png",
        position = 1550)
@NbBundle.Messages({
        "FiltersOptionsCategory_Name=Filters"
}) 
public final class FiltersOptionsCategory extends OptionsPanelController {
    
    private static final HelpCtx HELP_CTX = null;
//    private static final HelpCtx HELP_CTX = new HelpCtx("HeapViewerOptions.Help"); // NOI18N
    
    public static final String OPTIONS_HANDLE = "HeapViewerOptions"; // NOI18N
    
    private FiltersOptionsPanel settingsPanel;
    private JComponent settingsComponent;

    
    private FiltersOptionsPanel getPanel() {
        if (settingsPanel == null) settingsPanel = new FiltersOptionsPanel();
        return settingsPanel;
    }
    
    public JComponent getComponent(Lookup lookup) {
        if (settingsComponent == null) settingsComponent = UISupport.createScrollableContainer(getPanel());
        return settingsComponent;
    }

    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }
    
    public boolean isChanged() {
        if (settingsPanel == null) return false;
        return !settingsPanel.equalsTo(ProfilerIDESettings.getInstance());
    }

    public boolean isValid() {
        return true;
    }

    public void applyChanges() {
        if (settingsPanel == null) return;
        settingsPanel.storeTo(ProfilerIDESettings.getInstance());
    }

    public void cancel() {
    }

    public void update() {
        if (settingsPanel == null) return;
        settingsPanel.loadFrom(ProfilerIDESettings.getInstance());
    }
    
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }
    
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
    
}
