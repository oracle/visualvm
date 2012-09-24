/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
package org.netbeans.modules.profiler.ui.panels;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;
import org.netbeans.modules.profiler.ui.ManualMethodSelect;
import org.openide.DialogDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
@NbBundle.Messages({
    "RootMethodsPanel_RootMethodsLabelText=&Selected Root Methods:",
    "RootMethodsPanel_AddManualButtonText=&Add...",
    "RootMethodsPanel_EditButtonText=&Edit...",
    "RootMethodsPanel_RemoveButtonText=&Remove",
    "RootMethodsPanel_MessageAreaText=<b>Note:</b> You can also specify a root method in the source editor by right-clicking the method and choosing \"Profiling > Add As Profiling Root Method...\".",
    "RootMethodsPanel_SpecifyRootMethodsDialogCaption=Edit Profiling Roots (Advanced)",
    "RootMethodsPanel_RootsListAccessName=List of selected profiling roots.",
    "RootMethodsPanel_AddManuallyButtonAccessDescr=Add new profiling root.",
    "RootMethodsPanel_EditButtonAccessDescr=Edit defined profiling root.",
    "RootMethodsPanel_RemoveButtonAccessDescr=Remove selected profiling roots.",
    "RootMethodsPanel_IncorrectManualRootMsg=<html><b>No method could be resolved based on the provided data.</b><br><br>Please make sure you have entered the method definition correctly.<br>Use <code>javap -s &lt;classname&gt;</code> for exact methods definitions in VM format.",
    "RootMethodsPanel_AddFromJarButtonText=Add &JAR/Folder...",
    "RootMethodsPanel_AddFromJarButtonAccessDescr=Add new profiling root from an external jar or folder.",
    "RootMethodsPanel_FoldersJarsFileFilter=Class folders/JARs"
})
public final class RootMethodsPanel extends JPanel implements ActionListener, ListSelectionListener, HelpCtx.Provider {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String HELP_CTX_KEY = "RootMethodsPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    private static RootMethodsPanel defaultInstance;
    private static MethodNameFormatterFactory formatterFactory = MethodNameFormatterFactory.getDefault(new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_FULLCLASSMETHOD));
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private ArrayList selectedRoots = new ArrayList();
    private DefaultListModel rootsListModel;
    private HTMLTextArea hintArea;
    private JButton addFromJarButton;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JList rootsList;
    private Lookup.Provider project;
    private boolean globalAttach = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    /**
     * Creates new form RootMethodsPanel
     */
    private RootMethodsPanel() {
        super();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    public static ClientUtils.SourceCodeSelection[] getSelectedRootMethods(ClientUtils.SourceCodeSelection[] roots,
            Lookup.Provider project) {
        final RootMethodsPanel rm = getDefault();
        rm.project = project;
        rm.globalAttach = rm.project == null;
        rm.addFromJarButton.setEnabled(rm.globalAttach);
        rm.refreshList(roots);

        return performDisplay(rm);
    }

    /**
     * Invoked when an action occurs.
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource() == addFromJarButton) {
            addFromJarButton.setEnabled(false);
            RequestProcessor.getDefault().post(new Runnable() {

                public void run() {
                    try {
                        FileChooserBuilder b = new FileChooserBuilder(RootMethodsPanel.class);
                        File jar = b.setFileFilter(new FileFilter() {

                            @Override
                            public boolean accept(File f) {
                                if (f.isDirectory()) {
                                    return true;
                                }
                                String ext = null;
                                String n = f.getName();
                                int index = n.lastIndexOf("."); // NOI18N
                                if (index > -1) {
                                    ext = n.substring(index + 1);
                                }
                                return ext != null && ext.equalsIgnoreCase("jar"); // NOI18N
                            }

                            @Override
                            public String getDescription() {
                                return Bundle.RootMethodsPanel_FoldersJarsFileFilter();
                            }
                        }).showOpenDialog();

                        if (jar == null) {
                            return;
                        }

                        final ClientUtils.SourceCodeSelection[] sel = FileSelectRootMethodsPanel.getDefault().getRootMethods(FileUtil.toFileObject(jar),
                                (ClientUtils.SourceCodeSelection[]) selectedRoots.toArray(new ClientUtils.SourceCodeSelection[]{}));

                        if (sel != null) {
                            addNewRootMethods(sel, true);
                        }
                    } finally {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                addFromJarButton.setEnabled(true);
                            }
                        });
                    }
                }
            });
        } else if (e.getSource() == addButton) {
            final ClientUtils.SourceCodeSelection scs = ManualMethodSelect.selectMethod();

            if (scs != null) {
                String newItem = null;
                try {
                    newItem = formatterFactory.getFormatter().formatMethodName(scs).toFormatted();
                    if (!selectedRoots.contains(scs)) {
                        selectedRoots.add(scs);

                        rootsListModel.addElement(newItem);
                        rootsList.setSelectedValue(newItem, true);
                    }
                } catch (Exception ex) {
                    ProfilerDialogs.displayError(Bundle.RootMethodsPanel_IncorrectManualRootMsg());
                }
            }
        } else if (e.getSource() == editButton) {
            ClientUtils.SourceCodeSelection sel =
                    (ClientUtils.SourceCodeSelection) selectedRoots.get(rootsList.getSelectedIndex());
            ClientUtils.SourceCodeSelection scs = ManualMethodSelect.selectMethod(sel);

            if (scs != null) {
                String newItem = null;
                try {
                    newItem = formatterFactory.getFormatter().formatMethodName(scs).toFormatted();
                    if (!selectedRoots.contains(scs)) {
                        int index = selectedRoots.indexOf(sel);
                        selectedRoots.remove(index);
                        selectedRoots.add(index, scs);

                        rootsListModel.remove(index);
                        rootsListModel.add(index, newItem);
                        rootsList.setSelectedIndex(index);
                    }
                } catch (Exception ex) {
                    ProfilerDialogs.displayError(Bundle.RootMethodsPanel_IncorrectManualRootMsg());
                }
            }
        } else if (e.getSource() == removeButton) {
            final int[] selectedSessionIndices = rootsList.getSelectedIndices();

            Object[] selectedItems = rootsList.getSelectedValues();
            for (Object selectedItem : selectedItems) {
                rootsListModel.removeElement(selectedItem);
            }

            ArrayList selRoots = new ArrayList();
            for (int i : selectedSessionIndices) {
                selRoots.add(selectedRoots.get(i));
            }
            selectedRoots.removeAll(selRoots);

            int toSelect = -1;

            if (selectedSessionIndices.length > 0) {
                toSelect = selectedSessionIndices[0];
            }

            if (toSelect >= rootsListModel.size()) {
                toSelect = rootsListModel.size() - 1;
            }

            rootsList.setSelectedIndex(toSelect);
            updateButtons();
        }
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    public void valueChanged(final ListSelectionEvent e) {
        updateButtons();
    }

    private static RootMethodsPanel getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new RootMethodsPanel();
        }

        return defaultInstance;
    }

    // ---------------------------------------------------------------------------
    private static ClientUtils.SourceCodeSelection[] performDisplay(final RootMethodsPanel rm) {
        final DialogDescriptor dd = new DialogDescriptor(rm, Bundle.RootMethodsPanel_SpecifyRootMethodsDialogCaption());
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);

        if (rm.addFromJarButton.isEnabled()) {
            rm.addFromJarButton.grabFocus();
        }

        d.setVisible(true);

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            ClientUtils.SourceCodeSelection[] ret = new ClientUtils.SourceCodeSelection[rm.selectedRoots.size()];
            rm.selectedRoots.toArray(ret);

            return ret;
        }

        return null;
    }

    private void addNewRootMethods(ClientUtils.SourceCodeSelection[] sel, boolean clean) {
        if (clean) {
            selectedRoots.clear();
            rootsListModel.clear();
        }

        MethodNameFormatter formatter = formatterFactory.getFormatter();

        for (int i = 0; i < sel.length; i++) {
            final ClientUtils.SourceCodeSelection scs = sel[i];

            if (!selectedRoots.contains(scs)) {
                selectedRoots.add(scs);
                rootsListModel.addElement(formatter.formatMethodName(scs).toFormatted());

                //          new MethodNameFormatter(
                //          scs.getClassName(), scs.getMethodName(), scs.getMethodSignature()
                //          ).getFullFormattedClassAndMethod()
                //          );
            }
        }
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(12, 12, 12, 12);

        JLabel label = new JLabel();
        JScrollPane rootsScrollPane = new JScrollPane();
        rootsList = new JList();

        JPanel buttonPanel = new JPanel();
        addFromJarButton = new JButton();
        addButton = new JButton();
        editButton = new JButton();
        removeButton = new JButton();
        hintArea = new HTMLTextArea() {

            public Dimension getPreferredSize() { // Workaround to force the text area not to consume horizontal space to fit the contents to just one line

                return new Dimension(1, super.getPreferredSize().height);
            }
        };

        org.openide.awt.Mnemonics.setLocalizedText(label, Bundle.RootMethodsPanel_RootMethodsLabelText());
        label.setLabelFor(rootsList);
        label.setIconTextGap(10);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(12, 12, 5, 12);
        add(label, gridBagConstraints);

        rootsList.getAccessibleContext().setAccessibleName(Bundle.RootMethodsPanel_RootsListAccessName());

        rootsScrollPane.setViewportView(rootsList);
        rootsScrollPane.setPreferredSize(new Dimension(330, rootsScrollPane.getPreferredSize().height));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(0, 24, 0, 12);
        add(rootsScrollPane, gridBagConstraints);

        buttonPanel.setLayout(new GridLayout(4, 1, 0, 5));

        buttonPanel.setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(addFromJarButton, Bundle.RootMethodsPanel_AddFromJarButtonText());
        addFromJarButton.getAccessibleContext().setAccessibleDescription(Bundle.RootMethodsPanel_AddFromJarButtonAccessDescr());
        buttonPanel.add(addFromJarButton);

        org.openide.awt.Mnemonics.setLocalizedText(addButton, Bundle.RootMethodsPanel_AddManualButtonText());
        addButton.getAccessibleContext().setAccessibleDescription(Bundle.RootMethodsPanel_AddManuallyButtonAccessDescr());
        buttonPanel.add(addButton);

        org.openide.awt.Mnemonics.setLocalizedText(editButton, Bundle.RootMethodsPanel_EditButtonText());
        editButton.getAccessibleContext().setAccessibleDescription(Bundle.RootMethodsPanel_EditButtonAccessDescr());
        buttonPanel.add(editButton);

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, Bundle.RootMethodsPanel_RemoveButtonText());
        removeButton.getAccessibleContext().setAccessibleDescription(Bundle.RootMethodsPanel_RemoveButtonAccessDescr());
        buttonPanel.add(removeButton);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        gridBagConstraints.insets = new Insets(0, 0, 0, 12);
        add(buttonPanel, gridBagConstraints);

        Color panelBackground = UIManager.getColor("Panel.background"); //NOI18N
        Color hintBackground = UIUtils.getSafeColor(panelBackground.getRed() - 10, panelBackground.getGreen() - 10,
                panelBackground.getBlue() - 10);

        // hintArea
        hintArea.setText(Bundle.RootMethodsPanel_MessageAreaText());
        hintArea.setEnabled(false);
        hintArea.setDisabledTextColor(Color.darkGray);
        hintArea.setBackground(hintBackground);
        hintArea.setBorder(BorderFactory.createMatteBorder(10, 10, 10, 10, hintBackground));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new Insets(12, 12, 0, 12);
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        add(hintArea, gridBagConstraints);

        addFromJarButton.addActionListener(this);
        addButton.addActionListener(this);
        editButton.addActionListener(this);
        removeButton.addActionListener(this);
        rootsList.addListSelectionListener(this);
    }

    private void refreshList(final ClientUtils.SourceCodeSelection[] roots) {
        selectedRoots.clear();

        for (int i = 0; i < roots.length; i++) {
            ClientUtils.SourceCodeSelection root = roots[i];

            if (root != null) {
                selectedRoots.add(root);
            }
        }

        rootsListModel = new DefaultListModel();
        updateList();
        rootsList.setModel(rootsListModel);

        updateButtons();
    }

    private void updateButtons() {
        editButton.setEnabled(rootsList.getSelectedIndices().length == 1);
        removeButton.setEnabled(rootsList.getSelectedIndices().length > 0);
    }

    private void updateList() {
        rootsListModel.removeAllElements();

        MethodNameFormatter formatter = formatterFactory.getFormatter();

        for (Iterator it = selectedRoots.iterator(); it.hasNext();) {
            final ClientUtils.SourceCodeSelection scs = (ClientUtils.SourceCodeSelection) it.next();
            rootsListModel.addElement(formatter.formatMethodName(scs).toFormatted());

            //        new MethodNameFormatter(
            //        scs.getClassName(), scs.getMethodName(), scs.getMethodSignature()
            //        ).getFullFormattedClassAndMethod()
            //        );
        }
    }
}
