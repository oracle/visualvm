/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource.viewer;

import org.graalvm.visualvm.gotosource.SourcesViewer;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.gotosource.SourceHandle;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "RegisteredSourcesViewer_Name=Registered Viewer",                           // NOI18N
    "RegisteredSourcesViewer_Description=viewer registered in the operating system", // NOI18N
    "# {0} - error message",
    "RegisteredSourcesViewer_CommandFailed=Failed to open source in registered viewer.\n\n{0}", // NOI18N
    "RegisteredSourcesViewer_ModeLabel=Mode:",                                  // NOI18N
    "RegisteredSourcesViewer_OpenChoice=O&pen",                                 // NOI18N
    "RegisteredSourcesViewer_EditChoice=&Edit"                                  // NOI18N
})
@ServiceProvider(service=SourcesViewer.class, position = 200)
public final class RegisteredSourcesViewer extends SourcesViewer {
    
    private static final Logger LOGGER = Logger.getLogger(RegisteredSourcesViewer.class.getName());
    
    private static final String ID = "RegisteredSourcesViewer";                 // NOI18N
    
    private static final String PROP_MODE = "prop_RegisteredSourcesViewer_mode"; // NOI18N
    private static final boolean DEFAULT_MODE = Boolean.FALSE;
    
    
    private JPanel settingsPanel;
    private JRadioButton openChoice;
    private JRadioButton editChoice;
    
    
    public RegisteredSourcesViewer() {
        super(ID, Bundle.RegisteredSourcesViewer_Name(), Bundle.RegisteredSourcesViewer_Description());
    }

    
    @Override
    public boolean open(SourceHandle handle) {
        try {
            if (isEdit()) Desktop.getDesktop().edit(new File(handle.getSourceFile()));
            else Desktop.getDesktop().open(new File(handle.getSourceFile()));
        } catch (IOException ex) {
            ProfilerDialogs.displayError(Bundle.RegisteredSourcesViewer_CommandFailed(ex.getMessage()));
            LOGGER.log(Level.INFO, "Failed to open source " + handle.getSourceFile(), ex); // NOI18N
        }
        
        return true;
    }
    
    
    @Override
    public void loadSettings() {
        if (settingsPanel != null) {
            boolean edit = isEdit();
            openChoice.setSelected(!edit);
            editChoice.setSelected(edit);
        }
    }
    
    @Override
    public void saveSettings() {
        if (settingsPanel != null) saveEdit(editChoice.isSelected());
    }
    
    @Override
    public boolean settingsDirty() {
        return settingsPanel != null && isEdit() != editChoice.isSelected();
    }
    
    
    private void saveEdit(boolean edit) {
        NbPreferences.forModule(RegisteredSourcesViewer.class).putBoolean(PROP_MODE, edit);
    }
    
    private boolean isEdit() {
        return NbPreferences.forModule(RegisteredSourcesViewer.class).getBoolean(PROP_MODE, DEFAULT_MODE);
    }
    
    
    @Override
    public JComponent getSettingsComponent() {
        if (settingsPanel == null) {
            settingsPanel = new JPanel(null);
            settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.LINE_AXIS));
            settingsPanel.setOpaque(false);
            
            int tab = 15;
            int gap = 5;
            
            JLabel modeCaption = new JLabel();
            Mnemonics.setLocalizedText(modeCaption, Bundle.RegisteredSourcesViewer_ModeLabel());
            settingsPanel.add(modeCaption);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            openChoice = new JRadioButton();
            Mnemonics.setLocalizedText(openChoice, Bundle.RegisteredSourcesViewer_OpenChoice());
            settingsPanel.add(openChoice);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            editChoice = new JRadioButton();
            Mnemonics.setLocalizedText(editChoice, Bundle.RegisteredSourcesViewer_EditChoice());
            settingsPanel.add(editChoice);
            
            ButtonGroup bg = new ButtonGroup();
            bg.add(openChoice);
            bg.add(editChoice);
            
            boolean edit = isEdit();
            openChoice.setSelected(!edit);
            editChoice.setSelected(edit);
        }
        
        return settingsPanel;
    }
    
}
