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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.ui.swing.PopupButton;
import org.graalvm.visualvm.lib.ui.swing.SmallButton;
import org.graalvm.visualvm.gotosource.SourceHandle;
import org.netbeans.api.options.OptionsDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ExternalSourcesViewer_CommandHint=<select a predefined command or define a custom one>", // NOI18N
    "ExternalSourcesViewer_Name=External Viewer",                               // NOI18N
    "ExternalSourcesViewer_Description=custom command to launch an external viewer", // NOI18N
    "ExternalSourcesViewer_NotConfiguredCaption=Go To Source",     // NOI18N
    "ExternalSourcesViewer_NotConfigured=<html><br><b>Sources viewer has not been configured yet.</b><br><br>Use Options | Sources | Viewer to define the external IDE or editor to open the sources.<br><br>Customize a predefined template from the popup list or define a custom command using<br>the available parameters to launch the external sources viewer.<br><br>Alternatively choose to use a viewer registered in the OS (preselecting line not supported).</html>",     // NOI18N
    "# {0} - error message",
    "ExternalSourcesViewer_CommandFailed=Failed to open source in external viewer.\n\n{0}", // NOI18N
    "ExternalSourcesViewer_CommandLabel=&Command:",                             // NOI18N
    "ExternalSourcesViewer_RootsDialogCaption=Select File Or Directory",        // NOI18N
    "ExternalSourcesViewer_RootsDialogButton=Select",                           // NOI18N
    "ExternalSourcesViewer_OpenRootsDialogToolTip=Insert file or directory",    // NOI18N
    "ExternalSourcesViewer_InsertCommandToolTip=Insert predefined command or parameter" // NOI18N
})
@ServiceProvider(service=SourcesViewer.class, position = 300)
public final class ExternalSourcesViewer extends SourcesViewer {
    
    private static final Logger LOGGER = Logger.getLogger(ExternalSourcesViewer.class.getName());
    
    public static final String ID = "ExternalSourcesViewer";                    // NOI18N
    
    private static final String PROP_COMMAND = "prop_ExternalSourcesViewer_command"; // NOI18N
    private static final String DEFAULT_COMMAND = Bundle.ExternalSourcesViewer_CommandHint();
    
    
    private static enum IdePreset {
        
        NETBEANS("NetBeans", "netbeans " + SourceHandle.Feature.FILE.getCode() + ":" + SourceHandle.Feature.LINE.getCode()), // NOI18N
        ECLIPSE("Eclipse", "eclipse " + SourceHandle.Feature.FILE.getCode() + ":" + SourceHandle.Feature.LINE.getCode()), // NOI18N
        IDEA("IntelliJ IDEA", "idea --line " + SourceHandle.Feature.LINE.getCode() + " --column " + SourceHandle.Feature.COLUMN.getCode() + " " + SourceHandle.Feature.FILE.getCode()), // NOI18N
        VSCODE("Visual Studio Code", "code -g " + SourceHandle.Feature.FILE.getCode() + ":" + SourceHandle.Feature.LINE.getCode()), // NOI18N
        XCODE("Xcode", "open -a Xcode " + SourceHandle.Feature.FILE.getCode()); // NOI18N
        
        private final String name;
        private final String command;
        
        IdePreset(String name, String command) {
            this.name = name;
            this.command = command;
        }
        
        String getName() { return name; }
        String getCommand() { return command; }
        
        @Override public String toString() { return getName(); }
        
        static IdePreset[] sorted() {
            IdePreset[] commands = values();
            Arrays.sort(commands, new Comparator<IdePreset>() {
                @Override
                public int compare(IdePreset c1, IdePreset c2) {
                    return c1.name.compareTo(c2.name);
                }
            });
            return commands;
        }
        
    }

    private static enum ToolPreset {
        
        NOTEPAD("Notepad", "notepad.exe " + SourceHandle.Feature.FILE.getCode()), // NOI18N
        NOTEPADPP("Notepad++", "notepad++ -p" + SourceHandle.Feature.OFFSET.getCode() + " " + SourceHandle.Feature.FILE.getCode()), // NOI18N
        GEDIT("Gedit", "gedit +" + SourceHandle.Feature.LINE.getCode() + " " + SourceHandle.Feature.FILE.getCode()), // NOI18N
        EMACS("Emacs", "emacs +" + SourceHandle.Feature.LINE.getCode() + ":" + SourceHandle.Feature.COLUMN.getCode() + " " + SourceHandle.Feature.FILE.getCode()), // NOI18N
        KATE("Kate", "kate -l " + SourceHandle.Feature.LINE.getCode() + " -c " + SourceHandle.Feature.COLUMN.getCode() + " " + SourceHandle.Feature.FILE.getCode()); // NOI18N
        
        private final String name;
        private final String command;
        
        ToolPreset(String name, String command) {
            this.name = name;
            this.command = command;
        }
        
        String getName() { return name; }
        String getCommand() { return command; }
        
        @Override public String toString() { return getName(); }
        
        static ToolPreset[] sorted() {
            ToolPreset[] commands = values();
            Arrays.sort(commands, new Comparator<ToolPreset>() {
                @Override
                public int compare(ToolPreset c1, ToolPreset c2) {
                    return c1.name.compareTo(c2.name);
                }
            });
            return commands;
        }
        
    }


    private final String forcedCommand;
    
    private JPanel settingsPanel;
    private JTextField commandField;
    
    
    public ExternalSourcesViewer() {
        this(null);
    }
    
    public ExternalSourcesViewer(String forcedCommand) {
        super(ID, Bundle.ExternalSourcesViewer_Name(), Bundle.ExternalSourcesViewer_Description());
        this.forcedCommand = forcedCommand;
    }

    
    @Override
    public boolean open(SourceHandle handle) {
        String command = getCommand();
        
        if (command.isEmpty() || command.equals(DEFAULT_COMMAND)) configureCommand();
        else executeCommand(handle, command);
                
        return true;
    }
    
    
    @Override
    public void loadSettings() {
        if (forcedCommand == null && settingsPanel != null) commandField.setText(getCommand());
    }
    
    @Override
    public void saveSettings() {
        if (forcedCommand == null && settingsPanel != null) saveCommand(commandField.getText().trim());
    }
    
    @Override
    public boolean settingsDirty() {
        return forcedCommand == null && settingsPanel != null && !commandField.getText().trim().equals(getCommand());
    }
    
    
    private void saveCommand(String command) {
        if (forcedCommand == null) NbPreferences.forModule(ExternalSourcesViewer.class).put(PROP_COMMAND, command);
    }
    
    private String getCommand() {
        return forcedCommand == null ? NbPreferences.forModule(ExternalSourcesViewer.class).get(PROP_COMMAND, DEFAULT_COMMAND).trim() : forcedCommand;
    }
    
    
    private static void configureCommand() {
        ProfilerDialogs.displayWarning(Bundle.ExternalSourcesViewer_NotConfigured(), Bundle.ExternalSourcesViewer_NotConfiguredCaption(), null);
        OptionsDisplayer.getDefault().open("SourcesOptions");                   // NOI18N
    }
    
    private static void executeCommand(SourceHandle handle, String commandS) {
        List<String> commandL = ExternalViewerLauncher.getCommandStrings(commandS);

        for (int i = 0; i < commandL.size(); i++) {
            String commandI = commandL.get(i);
            if (i == 0) { // first command should be path to viewer executable
                if ((commandI.startsWith("'") && commandI.endsWith("'")) ||   // NOI18N
                    (commandI.startsWith("\"") && commandI.endsWith("\""))) {   // NOI18N
                    commandI = commandI.substring(1, commandI.length() - 1);
                    commandL.set(i, commandI);
                }
            } else { // other commands may be feature wildcards
                commandI = handle.expandFeatures(commandI);
                commandL.set(i, commandI);
            }
        }

        new ExternalViewerLauncher(commandL) {
            @Override protected void failed(IOException e)     {
                ProfilerDialogs.displayError(Bundle.ExternalSourcesViewer_CommandFailed(e.getMessage()));
                LOGGER.log(Level.INFO, "Opening external sources viewer failed", e); // NOI18N
            }
        }.run();
    }
    
    
    @Override
    public JComponent getSettingsComponent() {
        if (settingsPanel == null) {
            settingsPanel = new JPanel(null);
            settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.LINE_AXIS));
            settingsPanel.setOpaque(false);
            
            int tab = 15;
            int gap = 5;
            
            JLabel parametersCaption = new JLabel();
            Mnemonics.setLocalizedText(parametersCaption, Bundle.ExternalSourcesViewer_CommandLabel());
            settingsPanel.add(parametersCaption);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            commandField = new JTextField(getCommand());
            parametersCaption.setLabelFor(commandField);
            settingsPanel.add(commandField);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            SmallButton fileChooser = new SmallButton("...") {                  // NOI18N
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    
                    JFileChooser fileChooser = new JFileChooser((String)null);
                    fileChooser.setDialogTitle(Bundle.ExternalSourcesViewer_RootsDialogCaption());
                    fileChooser.setApproveButtonText(Bundle.ExternalSourcesViewer_RootsDialogButton());
                    fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                    
                    if (fileChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION)
                        insertFile(commandField, fileChooser.getSelectedFile());
                }
            };
            fileChooser.setToolTipText(Bundle.ExternalSourcesViewer_OpenRootsDialogToolTip());
            settingsPanel.add(fileChooser);
            
            settingsPanel.add(Box.createHorizontalStrut(gap));
            
            PopupButton parametersPopup = new PopupButton() {
                protected void populatePopup(JPopupMenu popup) {
                    IdePreset[] ides = IdePreset.sorted();
                    for (IdePreset ide : ides) {
                        final String command = ide.getCommand();
                        JMenuItem item = new JMenuItem(ide.getName()) {
                            protected void fireActionPerformed(ActionEvent e) {
                                super.fireActionPerformed(e);
                                commandField.setText(command);
                            }
                        };
                        popup.add(item);
                    }
                    
                    popup.addSeparator();
                    
                    ToolPreset[] tools = ToolPreset.sorted();
                    for (ToolPreset tool : tools) {
                        final String command = tool.getCommand();
                        JMenuItem item = new JMenuItem(tool.getName()) {
                            protected void fireActionPerformed(ActionEvent e) {
                                super.fireActionPerformed(e);
                                commandField.setText(command);
                            }
                        };
                        popup.add(item);
                    }
                    
                    popup.addSeparator();
                    
                    SourceHandle.Feature[] features = SourceHandle.Feature.values();
                    
                    int longestCode = 0;
                    for (SourceHandle.Feature feature : features)
                        longestCode = Math.max(longestCode, feature.getCode().length());
                    
                    for (SourceHandle.Feature feature : features) {
                        final String parameter = feature.getCode();
                        String val = "<html><code>" + appendSpaces(parameter, longestCode) + "&nbsp;&nbsp;</code>" + feature.getName() + "</html>"; // NOI18N
                        JMenuItem item = new JMenuItem(val) {
                            protected void fireActionPerformed(ActionEvent e) {
                                super.fireActionPerformed(e);
                                insertParameter(commandField, parameter);
                            }
                        };
                        popup.add(item);
                    }
                }
            };
            parametersPopup.setToolTipText(Bundle.ExternalSourcesViewer_InsertCommandToolTip());
            parametersPopup.setPopupAlign(SwingConstants.NORTH);
            settingsPanel.add(parametersPopup);
            
            Dimension dim = parametersPopup.getPreferredSize();
            fileChooser.setPreferredSize(dim);
            fileChooser.setMinimumSize(dim);
            fileChooser.setMaximumSize(dim);

            if (forcedCommand != null) {
                commandField.setEditable(false);
                fileChooser.setEnabled(false);
                parametersPopup.setEnabled(false);
            }
        }
        
        return settingsPanel;
    }
    
    
    private static String appendSpaces(String string, int targetLength) {
        int spacesToAdd = targetLength - string.length();
        for (int i = 0; i < spacesToAdd; i++) string += "&nbsp;";               // NOI18N
        return string;
    }
    
    private static void insertFile(JTextField textField, File file) {
        String path = file.getAbsolutePath();
        if (path.contains(" ")) path = "\"" + path + "\"";                      // NOI18N
        
//        try { textField.getDocument().insertString(textField.getCaretPosition(), path, null); }
        try {
            textField.getDocument().insertString(0, path, null);
            textField.select(0, path.length());
            textField.requestFocusInWindow();
        } catch (BadLocationException ex) {}
    }
    
    private static void insertParameter(JTextField textField, String parameter) {
        Document document = textField.getDocument();
        int length = document.getLength();
        int position = textField.getCaretPosition();
        
        try { 
            if (position > 0 && !" ".equals(document.getText(position - 1, 1))) // NOI18N
                parameter = " " + parameter;                                    // NOI18N
            if (position < length - 1 && !" ".equals(document.getText(position, 1))) // NOI18N
                parameter = parameter + " ";                                    // NOI18N
            
            textField.getDocument().insertString(position, parameter, null);
        } catch (BadLocationException ex) {}
    }
    
}
