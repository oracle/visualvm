/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource.options;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.ui.swing.SmallButton;
import org.graalvm.visualvm.gotosource.SourcesRoot;
import org.graalvm.visualvm.gotosource.impl.SourceRoots;
import org.graalvm.visualvm.gotosource.impl.SourceViewers;
import org.graalvm.visualvm.gotosource.SourcesViewer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SourcesOptionsPanel_DefinitionsCaption=Definitions",                       // NOI18N
    "SourcesOptionsPanel_Sources=&Source Roots:",                               // NOI18N
    "SourcesOptionsPanel_ViewerCaption=Viewer",                                 // NOI18N
    "SourcesOptionsPanel_OpenIn=&Open sources in:",                             // NOI18N
    "SourcesOptionsPanel_Config=Viewer settings:",                              // NOI18N
    "SourcesOptionsPanel_Add=Add new source root(s)",                              // NOI18N
    "SourcesOptionsPanel_Delete=Delete selected source root(s)",                  // NOI18N
    "SourcesOptionsPanel_MoveUp=Move selected source root up",                  // NOI18N
    "SourcesOptionsPanel_MoveDown=Move selected source root down",              // NOI18N
    "SourcesOptionsPanel_SelectRootsCaption=Select Source Roots",               // NOI18N
    "SourcesOptionsPanel_SelectButton=Select",                                  // NOI18N
    "SourcesOptionsPanel_SourceDirectoriesFilter=Directories or Archives",      // NOI18N
    "SourcesOptionsPanel_ForcedRoots=Source roots have been set automatically for this session", // NOI18N
    "SourcesOptionsPanel_ForcedViewer=Sources viewer has been set automatically for this session", // NOI18N
    "SourcesOptionsPanel_SourcesLocation=Sources Location:",                    // NOI18N
    "SourcesOptionsPanel_SelectedRootsChoice=&Selected root(s)",                // NOI18N
    "SourcesOptionsPanel_SelectedRootsToolTip=Sources are directly in the source root(s)",// NOI18N
    "SourcesOptionsPanel_SubdirectoriesChoice=S&ubdirectories:",                // NOI18N
    "SourcesOptionsPanel_SubdirectoriesToolTip=Sources are in the selected subdirectories of the source root(s)", // NOI18N
    "SourcesOptionsPanel_CustomSubpathsChoice=&Custom subpaths:",               // NOI18N
    "SourcesOptionsPanel_CustomSubpathsToolTip=<html>Sources are in the defined subpaths of the source root(s) - no wildcards allowed<br>Use <code>{0}</code> to search source root(s) for JDK sources, including module subfolders</html>", // NOI18N
    "SourcesOptionsPanel_SourcesEncoding=Sources Encoding:"                     // NOI18N
})
final class SourcesOptionsPanel extends JPanel {
    
    private static final String PROP_LAST_SOURCES_DIR = "prop_SourcesOptionsPanel_lastDir"; // NOI18N
    
    
    SourcesOptionsPanel() {
        initUI();
    }
    
    
    void load(Preferences settings) {
        rootsForcedHint.setVisible(SourceRoots.areForcedRoots());
        
        rootsListModel = new DefaultListModel();
        for (String root : SourceRoots.getRoots()) rootsListModel.addElement(root);
        rootsList.setModel(rootsListModel);
        rootsList.setEnabled(!rootsForcedHint.isVisible());
        updateRootsButtons();
        
        
        viewerForcedHint.setVisible(SourceViewers.isForcedViewer());
        
        Collection<? extends SourcesViewer> viewers = SourceViewers.getRegisteredViewers();
        viewerSelector.setModel(new DefaultComboBoxModel(viewers.toArray(new SourcesViewer[0])));
        viewerSelector.setEnabled(!viewerForcedHint.isVisible());
        
        SourcesViewer selected = SourceViewers.getSelectedViewer();
        if (selected == null && !viewers.isEmpty()) {
            selected = viewers.iterator().next();
            SourceViewers.saveSelectedViewer(selected);
        }
        
        for (int i = 0; i < viewerSelector.getItemCount(); i++)
            ((SourcesViewer)viewerSelector.getItemAt(i)).loadSettings();
        
        if (selected != null) {
            viewerSelector.setSelectedItem(selected);
            viewerSelected(selected);
        }
    }
    
    void save(Preferences settings) {
        if (!SourceRoots.areForcedRoots()) SourceRoots.saveRoots(getDefinedRoots());
        
        if (!SourceViewers.isForcedViewer()) {
            SourceViewers.saveSelectedViewer(getSelectedViewer());
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (int i = 0; i < viewerSelector.getItemCount(); i++)
                        ((SourcesViewer)viewerSelector.getItemAt(i)).saveSettings();
                }
            });
        }
    }
    
    void cancel() {}
    
    boolean dirty(Preferences settings) {
        if (!SourceRoots.areForcedRoots()) {
            String[] definedStrings = SourceRoots.getRoots();
            String[] currentStrings = getDefinedRoots();
            if (!Arrays.equals(definedStrings, currentStrings)) return true;
        }
        
        if (!SourceViewers.isForcedViewer()) {
            SourcesViewer selectedViewer = SourceViewers.getSelectedViewer();
            String selectedViewerID = selectedViewer == null ? null : selectedViewer.getID();
            SourcesViewer currentlySelectedViewer = getSelectedViewer();
            String currentlySelectedViewerID = currentlySelectedViewer == null ? null : currentlySelectedViewer.getID();
            if (!Objects.equals(selectedViewerID, currentlySelectedViewerID)) return true;

            for (int i = 0; i < viewerSelector.getItemCount(); i++)
                if (((SourcesViewer)viewerSelector.getItemAt(i)).settingsDirty())
                    return true;
        }
        
        return false;
    }
    
    
    private String[] getDefinedRoots() {
        String[] roots = new String[rootsListModel.size()];
        rootsListModel.copyInto(roots);
        return roots;
    }
    
    
    private SourcesViewer getSelectedViewer() {
        return (SourcesViewer)viewerSelector.getSelectedItem();
    }
    
    
    private void viewerSelected(SourcesViewer viewer) {
        viewerDescription.setText(viewer.getDescription());
        
        viewerSettings.removeAll();
        JComponent settingsComponent = viewer.getSettingsComponent();
        if (settingsComponent != null) viewerSettings.add(settingsComponent, BorderLayout.NORTH);

        
        validate();
        repaint();
    }
    
    
    private void updateRootsButtons() {
        if (rootsForcedHint.isVisible()) {
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
            upButton.setEnabled(false);
            downButton.setEnabled(false);
        } else {
            int[] selectedRows = rootsList.getSelectedIndices();
            int selectedRow = selectedRows.length == 1 ? selectedRows[0] : -1;

            addButton.setEnabled(true);
            removeButton.setEnabled(selectedRows.length > 0);

            if (selectedRow == -1) {
                upButton.setEnabled(false);
                downButton.setEnabled(false);
            } else {
                Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

                upButton.setEnabled(selectedRow > 0);
                downButton.setEnabled(selectedRow < rootsListModel.size() - 1);

                if (upButton == focusOwner && !upButton.isEnabled() && downButton.isEnabled()) downButton.requestFocusInWindow();
                else if (downButton == focusOwner && !downButton.isEnabled() && upButton.isEnabled()) upButton.requestFocusInWindow();
            }
        }
    }
    
    
    private void initUI() {
        setLayout(new GridBagLayout());
        
        GridBagConstraints c;
        int y = 0;
        int htab = 15;
        int vgap = 5;
        
        SectionSeparator definitionsSection = UISupport.createSectionSeparator(Bundle.SourcesOptionsPanel_DefinitionsCaption());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, vgap * 3, 0);
        add(definitionsSection, c);
        
        JLabel definitionsCaption = new JLabel();
        Mnemonics.setLocalizedText(definitionsCaption, Bundle.SourcesOptionsPanel_Sources());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, htab, vgap, 0);
        add(definitionsCaption, c);
        
        rootsListModel = new DefaultListModel();
        rootsList = new JList(rootsListModel);
        rootsList.setVisibleRowCount(0);
        definitionsCaption.setLabelFor(rootsList);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = 5;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, htab, vgap * 3, 0);
        add(new JScrollPane(rootsList), c);
        
        addButton = new SmallButton(Icons.getIcon(GeneralIcons.ADD)) {
            {
                setToolTipText(Bundle.SourcesOptionsPanel_Add());
            }
            protected void fireActionPerformed(ActionEvent e) {
                super.fireActionPerformed(e);
                
                JFileChooser fileChooser = new JFileChooser((String)null);
                fileChooser.setDialogTitle(Bundle.SourcesOptionsPanel_SelectRootsCaption());
                fileChooser.setApproveButtonText(Bundle.SourcesOptionsPanel_SelectButton());
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.addChoosableFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".zip") || f.getName().endsWith(".jar"); // NOI18N
                    }
                    @Override
                    public String getDescription() {
                        return Bundle.SourcesOptionsPanel_SourceDirectoriesFilter() + " (*.zip, *.jar)"; // NOI18N
                    }
                });
                
                String aFile = System.getProperties().getProperty("netbeans.home"); // NOI18N
                Icon fileIcon = fileChooser.getIcon(new File(aFile));
                SourceRootsCustomizer customizer = new SourceRootsCustomizer(fileIcon);
                fileChooser.setAccessory(customizer);
                fileChooser.addPropertyChangeListener(JFileChooser.SELECTED_FILES_CHANGED_PROPERTY, customizer);

                String lastDirS = NbPreferences.forModule(SourcesOptionsPanel.class).get(PROP_LAST_SOURCES_DIR, null);
                File lastDir = lastDirS == null ? null : new File(lastDirS);
                if (lastDir != null && lastDir.isDirectory()) fileChooser.setCurrentDirectory(lastDir);

                if (fileChooser.showOpenDialog(WindowManager.getDefault().getMainWindow()) == JFileChooser.APPROVE_OPTION) {
                    String first = null;
                    String firstC = null;
                    for (File selected : fileChooser.getSelectedFiles()) {
                        String path = selected.getAbsolutePath();
                        String pathC = customizer.createRootString(path);
                        
                        if (first == null) {
                            first = path;
                            firstC = pathC;
                        }
                        
                        if (!rootsListModel.contains(pathC)) rootsListModel.addElement(pathC);
                    }
                    if (first != null) {
                        rootsList.setSelectedValue(firstC, true);
                        
                        File dir = new File(first).getParentFile();
                        String dirS = dir.isDirectory() ? dir.getAbsolutePath() : null;
                        if (dirS != null) NbPreferences.forModule(SourcesOptionsPanel.class).put(PROP_LAST_SOURCES_DIR, dirS);
                    }
                }
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(addButton, c);
        
        removeButton = new SmallButton(Icons.getIcon(GeneralIcons.REMOVE)) {
            {
                setToolTipText(Bundle.SourcesOptionsPanel_Delete());
            }
            protected void fireActionPerformed(ActionEvent e) {
                for (Object selected : rootsList.getSelectedValuesList())
                    rootsListModel.removeElement(selected);
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, vgap * 2, 0);
        add(removeButton, c);
        
        final boolean[] internalSelectionChange = new boolean[] { false };
        
        upButton = new SmallButton(Icons.getIcon(GeneralIcons.UP)) {
            {
                setToolTipText(Bundle.SourcesOptionsPanel_MoveUp());
            }
            protected void fireActionPerformed(ActionEvent e) {
                int selected = rootsList.getSelectedIndex();
                if (selected < 1) return;
                
                String selectedRoot = rootsListModel.get(selected);
                internalSelectionChange[0] = true;
                try {
                    rootsListModel.remove(selected);
                    rootsListModel.add(selected - 1, selectedRoot);
                } finally {
                    internalSelectionChange[0] = false;
                }
                rootsList.setSelectedValue(selectedRoot, true);
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(upButton, c);
        
        downButton = new SmallButton(Icons.getIcon(GeneralIcons.DOWN)) {
            {
                setToolTipText(Bundle.SourcesOptionsPanel_MoveDown());
            }
            protected void fireActionPerformed(ActionEvent e) {
                int selected = rootsList.getSelectedIndex();
                if (selected == -1 || selected > rootsListModel.size() - 2) return;
                
                String selectedRoot = rootsListModel.get(selected);
                internalSelectionChange[0] = true;
                try {
                    rootsListModel.remove(selected);
                    rootsListModel.add(selected + 1, selectedRoot);
                } finally {
                    internalSelectionChange[0] = false;
                }
                rootsList.setSelectedValue(selectedRoot, true);
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(downButton, c);
        
        rootsForcedHint = new JLabel(Bundle.SourcesOptionsPanel_ForcedRoots(), Icons.getIcon(GeneralIcons.INFO), JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(-vgap, htab, vgap * 3, 0);
        add(rootsForcedHint, c);
        
        SectionSeparator gotoSection = UISupport.createSectionSeparator(Bundle.SourcesOptionsPanel_ViewerCaption());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, vgap * 3, 0);
        add(gotoSection, c);
        
        final JPanel chooserPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, vgap * 2, 0);
        add(chooserPanel, c);
        
        JLabel openInLabel = new JLabel();
        Mnemonics.setLocalizedText(openInLabel, Bundle.SourcesOptionsPanel_OpenIn());
        chooserPanel.add(openInLabel);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, vgap);
        chooserPanel.add(openInLabel, c);
        
        viewerSelector = new JComboBox();
        openInLabel.setLabelFor(viewerSelector);
        viewerSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) viewerSelected((SourcesViewer)e.getItem());
            }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, htab);
        chooserPanel.add(viewerSelector, c);        
        
        viewerDescription = new JLabel();
        viewerDescription.setEnabled(false);
        c = new GridBagConstraints();
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1.0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        chooserPanel.add(viewerDescription, c);
        
        viewerSettings = new JPanel(new BorderLayout()) {
            public Dimension getMinimumSize() {
                Dimension dim = super.getMinimumSize();
                dim.height = getPreferredSize().height;
                return dim;
            }
            public Dimension getPreferredSize() {
                Dimension dim = super.getPreferredSize();
                dim.height = Math.max(dim.height, chooserPanel.getPreferredSize().height + 10);
                return dim;
            }
        };
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.weightx = 1.0;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(viewerSettings, c);
        
        viewerForcedHint = new JLabel(Bundle.SourcesOptionsPanel_ForcedViewer(), Icons.getIcon(GeneralIcons.INFO), JLabel.LEADING);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(viewerForcedHint, c);
        
        
        ListSelectionListener selection = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!internalSelectionChange[0] && !e.getValueIsAdjusting()) updateRootsButtons();
            }
        };
        rootsList.addListSelectionListener(selection);
        updateRootsButtons();
    }
    
    
    private DefaultListModel<String> rootsListModel;
    private JList<String> rootsList;
    private JButton addButton, removeButton, upButton, downButton;
    private JLabel rootsForcedHint;
    
    private JComboBox<SourcesViewer> viewerSelector;
    private JLabel viewerDescription;
    private JPanel viewerSettings;
    private JLabel viewerForcedHint;
    
    
    private static final class SourceRootsCustomizer extends JPanel implements PropertyChangeListener {
        
        private static final String PREDEFINED_JDKSRC_DIR1 = "java.base";       // NOI18N
        private static final String PREDEFINED_JDKSRC_DIR2 = "java.se";         // NOI18N
        
        private static final String PREDEFINED_SRC_DIR = "src";                 // NOI18N
        
        
        private RequestProcessor processor;
        
        
        SourceRootsCustomizer(Icon fileIcon) {
            super(null);
            
            initUI(fileIcon);
        }
        
        
        String createRootString(String root) {
            String[] subpaths;
            if (commonFolderChoice.isSelected()) {
                subpaths = subdirectoryList.getSelectedValuesList().toArray(new String[0]);
            } else if (customFolderChoice.isSelected()) {
                subpaths = customFolderField.getText().trim().replace(File.separator, "/") // NOI18N
                                                             .replace(File.pathSeparator, ":") // NOI18N
                                                             .split(":");        // NOI18N
            } else {
                subpaths = null;
            }
            
            String encoding = encodingSelector.getEditor().getItem().toString().trim();
            
            return SourcesRoot.createString(root, subpaths, encoding);
        }
        
        
        private void initUI(Icon fileIcon) {
            setLayout(new GridBagLayout());
            
            GridBagConstraints c;
            int y = 0;
            int htab = 15;
            int vgap = 5;
            
            SectionSeparator foldersSection = UISupport.createSectionSeparator(Bundle.SourcesOptionsPanel_SourcesLocation());
            foldersSection.setFont(foldersSection.getFont().deriveFont(Font.PLAIN));
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab, htab - vgap, vgap);
            add(foldersSection, c);
            
            selectedFolderChoice = new JRadioButton();
            Mnemonics.setLocalizedText(selectedFolderChoice, Bundle.SourcesOptionsPanel_SelectedRootsChoice());
            selectedFolderChoice.setToolTipText(Bundle.SourcesOptionsPanel_SelectedRootsToolTip());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab, vgap, vgap);
            add(selectedFolderChoice, c);
            
            int htab2 = htab + 16 + selectedFolderChoice.getIconTextGap(); // 16 is typical icon width
            
            commonFolderChoice = new JRadioButton();
            Mnemonics.setLocalizedText(commonFolderChoice, Bundle.SourcesOptionsPanel_SubdirectoriesChoice());
            commonFolderChoice.setToolTipText(Bundle.SourcesOptionsPanel_SubdirectoriesToolTip());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab, 0, vgap);
            add(commonFolderChoice, c);
            
            subdirectoryList = new JList();
            DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
                @Override public void setIcon(Icon icon) { if (icon != null) super.setIcon(icon); }
            };
            renderer.setIcon(fileIcon);
            subdirectoryList.setCellRenderer(renderer);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, htab2, htab - vgap, vgap);
            add(new JScrollPane(subdirectoryList), c);
            
            customFolderChoice = new JRadioButton();
            Mnemonics.setLocalizedText(customFolderChoice, Bundle.SourcesOptionsPanel_CustomSubpathsChoice());
            customFolderChoice.setToolTipText(Bundle.SourcesOptionsPanel_CustomSubpathsToolTip(SourcesRoot.MODULES_SUBPATH));
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab, 0, vgap);
            add(customFolderChoice, c);
            
            customFolderField = new JTextField();
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab2, htab + vgap, vgap);
            add(customFolderField, c);
            
            SectionSeparator encodingSection = UISupport.createSectionSeparator(Bundle.SourcesOptionsPanel_SourcesEncoding());
            encodingSection.setFont(foldersSection.getFont());
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab, htab - vgap, vgap);
            add(encodingSection, c);
            
            encodingSelector = new JComboBox(Charset.availableCharsets().keySet().toArray());
            encodingSelector.setSelectedItem(StandardCharsets.UTF_8.name());
            encodingSelector.setEditable(true);
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, htab2, htab + vgap, vgap);
            add(encodingSelector, c);
            
            ButtonGroup bg = new ButtonGroup();
            bg.add(selectedFolderChoice);
            bg.add(commonFolderChoice);
            bg.add(customFolderChoice);
            
            selectedFolderChoice.setSelected(true);
            
            subdirectoryList.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { commonFolderChoice.setSelected(true); }
            });
            
            customFolderField.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { customFolderChoice.setSelected(true); }
            });
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            boolean empty = subdirectoryList.getModel().getSize() == 0;
            final List<String> selected = empty ? null : subdirectoryList.getSelectedValuesList();
            subdirectoryList.setEnabled(false);
            
            final File[] subdirs = (File[])evt.getNewValue();
            
            if (processor == null) processor = new RequestProcessor("Source Roots Subfoldes Processor"); // NOI18N
            
            processor.post(new Runnable() {
                public void run() {
                    final List<String> subdirsL = getCommonSubDirs(subdirs);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            subdirectoryList.setListData(subdirsL.toArray(new String[0]));
                            subdirectoryList.setEnabled(true);
                            
                            if (selected != null && !selected.isEmpty()) {
                                List<Integer> sel = new ArrayList();
                                ListModel model = subdirectoryList.getModel();
                                for (int i = 0; i < model.getSize(); i++)
                                    if (selected.contains(model.getElementAt(i)))
                                        sel.add(i);

                                int[] selidx = new int[sel.size()];
                                for (int i = 0; i < selidx.length; i++)
                                    selidx[i] = sel.get(i);

                                subdirectoryList.setSelectedIndices(selidx);
                            }
                            
                            // Predefined patterns
                            if (subdirectoryList.getSelectedValue() == null) {
                                // JDK 9+ sources
                                if (subdirsL.contains(PREDEFINED_JDKSRC_DIR1) &&
                                    subdirsL.contains(PREDEFINED_JDKSRC_DIR2)) {
                                    subdirectoryList.setSelectionInterval(0, subdirectoryList.getModel().getSize() - 1);
                                // src subfolder
                                } else if (subdirsL.contains(PREDEFINED_SRC_DIR)) {
                                    subdirectoryList.setSelectedValue(PREDEFINED_SRC_DIR, false);
                                }
                            }
                        }
                    });
                }
            });
        }
        
        
        private static List<String> getCommonSubDirs(File[] roots) {
            if (roots == null || roots.length == 0) return Collections.EMPTY_LIST;
            
            List<String> subdirs = null;
            
            for (File root : roots) {
                List<String> rootSubdirs = getSubDirs(root);
                Collections.sort(rootSubdirs);
                if (subdirs == null) subdirs = rootSubdirs;
                else subdirs.retainAll(rootSubdirs);
            }
            
            return subdirs == null ? Collections.EMPTY_LIST : subdirs;
        }
        
        private static List<String> getSubDirs(File root) {
            if (root.isDirectory()) return getFolderSubDirs(root);
            else if (root.isFile()) return getArchiveSubDirs(root);
            else return Collections.EMPTY_LIST;
        }
        
        private static List<String> getFolderSubDirs(File root) {
            List<String> rootSubdirs = new ArrayList();
            
            File[] rootSubdirsF = root.listFiles(new java.io.FileFilter() {
                @Override public boolean accept(File f) { return f.isDirectory(); }
            });
            
            for (File f : rootSubdirsF) rootSubdirs.add(f.getName());
            
            return rootSubdirs;
        }
        
        private static List<String> getArchiveSubDirs(File root) {
            try (FileSystem archiveFileSystem = FileSystems.newFileSystem(root.toPath(), null)) {
                Path archive = archiveFileSystem.getRootDirectories().iterator().next();
                List<Path> subfolders = Files.walk(archive, 1).filter(Files::isDirectory).collect(Collectors.toList());

                List<String> rootSubDirs = new ArrayList();
                for (Path path : subfolders) rootSubDirs.add(path.toString().replace("/", "")); // NOI18N
                rootSubDirs.remove(0); // remove root

                return rootSubDirs;
            } catch (Exception e) {
                return Collections.EMPTY_LIST;
            }
        }
        
        
        private JRadioButton selectedFolderChoice;
        private JRadioButton commonFolderChoice;
        private JRadioButton customFolderChoice;
        
        private JList<String> subdirectoryList;
        private JTextField customFolderField;
        
        private JComboBox encodingSelector;
        
    }
    
}
