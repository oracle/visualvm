/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.jmx.impl;

import org.graalvm.visualvm.core.properties.PropertiesPanel;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.jmx.JmxConnectionCustomizer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class JmxConnectionConfigurator extends JPanel {

    static Result getResult() {
        JmxConnectionConfigurator configurator = instance();
        configurator.warmup();

        final DialogDescriptor dd = new DialogDescriptor(configurator,
                NbBundle.getMessage(JmxConnectionConfigurator.class, "Title_Add_JMX_Connection"),   // NOI18N
                true, new Object[] { configurator.okButton, DialogDescriptor.CANCEL_OPTION},
                configurator.okButton, 0, null, null);
        dd.setAdditionalOptions(new Object[] { configurator.hintLabel });
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        configurator.updateWindowTitle(d);
        d.pack();
        d.setVisible(true);

        Result result = configurator.get(dd.getValue() == configurator.okButton);
        configurator.cleanup();

        return result;
    }



    public static synchronized JmxConnectionConfigurator instance() {
        if (INSTANCE == null) INSTANCE = new JmxConnectionConfigurator();
        return INSTANCE;
    }


    public void warmup() {
        // Resolve customizers
        customizers.addAll(JmxConnectionSupportImpl.getCustomizers());
        singleCustomizer = customizers.size() == 1;

        // Populate list, initialize panels
        JmxConnectionCustomizer itemToSelect = null;
        for (JmxConnectionCustomizer customizer : customizers) {
            connectionTypeListModel.addElement(customizer);
            customizerPanels.add(customizer.createPanel(null));
            if (customizer.toString().equals(lastSelectedItem))
                itemToSelect = customizer;
        }

        // Update selector visibility
        boolean selectorVisible = customizers.size() > 1;
        connectionTypeLabel.setVisible(selectorVisible);
        connectionTypeScroll.setVisible(selectorVisible);
        customizerPanelScroll.setBorder(selectorVisible ?
            BorderFactory.createEmptyBorder(5, 8, 0, 5) :
            BorderFactory.createEmptyBorder(15, 5, 0, 5));

        // Register selection listener
        connectionTypeList.addListSelectionListener(selectionListener);

        // Restore previously selected item
        if (itemToSelect == null) connectionTypeList.setSelectedIndex(0);
        else connectionTypeList.setSelectedValue(itemToSelect, true);

        // Update OK button state
        updateOkButton();

        // Restore previous dialog size
        if (lastSize != null) customizerPanelScroll.setPreferredSize(lastSize);
        else initializePreferredSize();
    }

    public Result get(boolean accepted) {
        JmxConnectionCustomizer.Setup setup = null;
        if (accepted && selectedCustomizer != null && displayedPanel != null)
            setup = selectedCustomizer.getConnectionSetup(displayedPanel);
        return new Result(setup, selectedCustomizer, customizers, customizerPanels);
    }

    public void cleanup() {
        // Save last selected item
        Object selectedItem = connectionTypeList.getSelectedValue();
        lastSelectedItem = selectedItem == null ? null : selectedItem.toString();

        // Unregister selection listener
        connectionTypeList.clearSelection();
        connectionTypeList.removeListSelectionListener(selectionListener);

        // Save dialog size
        lastSize = customizerPanelScroll.getSize();

        // Clear state
        customizers.clear();
        customizerPanels.clear();
        customizerPanel.removeAll();
        connectionTypeListModel.clear();
        selectedCustomizer = null;
    }

    public void updateWindowTitle(Window w) {
        if (singleCustomizer) return;
        String title = NbBundle.getMessage(JmxConnectionConfigurator.class,
                "Title_Add_JMX_Connection") + " - " + // NOI18N
                selectedCustomizer.getPropertiesName();
        if (w instanceof Dialog) ((Dialog)w).setTitle(title);
        else if (w instanceof Frame) ((Frame)w).setTitle(title);
    }


    private void updateSelectedCustomizer() {
        selectedCustomizer = connectionTypeList.getSelectedValue();
        if (displayedPanel != null) {
            displayedPanel.removeChangeListener(validityListener);
            customizerPanel.removeAll();
        }
        if (selectedCustomizer != null) {
            displayedPanel = customizerPanels.get(connectionTypeListModel.
                                                  indexOf(selectedCustomizer));
            updateWindowTitle(SwingUtilities.getWindowAncestor(this));
        }
        if (displayedPanel != null) {
            displayedPanel.addChangeListener(validityListener);
            customizerPanel.add(displayedPanel, BorderLayout.CENTER);
            customizerPanel.revalidate();
            customizerPanel.repaint();
            updateOkButton();
        }
    }

    private void updateOkButton() {
        okButton.setEnabled(displayedPanel != null &&
                displayedPanel.settingsValid() ? true : false);
    }

    private void updateHint() {
        String hintText = selectedCustomizer == null ? null :
                          selectedCustomizer.getPropertiesDescription();
        if (hintText != null && !singleCustomizer) {
            hintLabel.setIcon(INFO_ICON);
            hintLabel.setText(hintText);
        } else {
            hintLabel.setIcon(null);
            hintLabel.setText(""); // NOI18N
        }
    }

    private void initializePreferredSize() {
        Dimension preferredSize = new Dimension();
        for (PropertiesPanel panel : customizerPanels) {
            Dimension panelPref = panel.getPreferredSize();
            preferredSize.width = Math.max(preferredSize.width, panelPref.width);
            preferredSize.height = Math.max(preferredSize.height, panelPref.height);
        }
        Insets insets = customizerPanelScroll.getInsets();
        preferredSize.width += insets.left + insets.right;
        preferredSize.height += insets.top + insets.bottom;
        if (!singleCustomizer) preferredSize.height += 40; // Extra bottom space
        customizerPanelScroll.setPreferredSize(preferredSize);
    }

    private JmxConnectionCustomizer getCustomizer(Point location) {
        int index = connectionTypeList.locationToIndex(location);
        if (index == -1) return null;
        if (!connectionTypeList.getCellBounds(index, index).contains(location))
            return null;
        return connectionTypeListModel.getElementAt(index);
    }


    private class SelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                updateSelectedCustomizer();
                updateHint();
            }
        }
    }

    private class ValidityListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            updateOkButton();
        }
    }


    private void initComponents() {
        okButton = new JButton(NbBundle.getMessage(JmxConnectionConfigurator.class, "LBL_OK"));    // NOI18N

        hintLabel = new JLabel("") { // NOI18N
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.height = Math.max(d.height, okButton.getPreferredSize().height);
                return d;
            }
        };
        hintLabel.setForeground(UIManager.getColor("Label.disabledForeground")); // NOI18N

        setLayout(new BorderLayout());

        connectionTypeLabel = new JLabel();
        Mnemonics.setLocalizedText(connectionTypeLabel,
                NbBundle.getMessage(JmxConnectionConfigurator.class, "LBL_Connection_type")); // NOI18N
        createBorder(connectionTypeLabel, BorderFactory.createEmptyBorder(15, 10, 0, 10));
        add(connectionTypeLabel, BorderLayout.NORTH);

        connectionTypeListModel = new DefaultListModel<>();
        connectionTypeList = new JList<JmxConnectionCustomizer>(connectionTypeListModel) {
            public String getToolTipText(MouseEvent evt) {
                JmxConnectionCustomizer cust = getCustomizer(evt.getPoint());
                return cust == null ? null : cust.getPropertiesDescription();
            }

        };
        connectionTypeLabel.setLabelFor(connectionTypeList);
        connectionTypeList.setSelectionModel(new DefaultListSelectionModel() {
            public void removeSelectionInterval(int i1, int i2) {}
        });
        connectionTypeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        final ListCellRenderer defaultRenderer = connectionTypeList.getCellRenderer();
        Component c = defaultRenderer.getListCellRendererComponent(connectionTypeList, "X", 0, false, false); // NOI18N
        connectionTypeList.setFixedCellHeight(c.getPreferredSize().height + 2);
        connectionTypeList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                return defaultRenderer.getListCellRendererComponent(list, " " + value + " ", index, isSelected, cellHasFocus); // NOI18N
            }
        });
        connectionTypeScroll = new JScrollPane(connectionTypeList,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED) {
            public Dimension getPreferredSize() {
                Dimension preferredSize = super.getPreferredSize();
                preferredSize.width = Math.min(preferredSize.width, 300);
                preferredSize.width = Math.max(preferredSize.width, 120);
                return preferredSize;
            }
        };
        createBorder(connectionTypeScroll, BorderFactory.createEmptyBorder(5, 10, 0, 0));
        add(connectionTypeScroll, BorderLayout.WEST);

        customizerPanel = new JPanel(new BorderLayout());
        customizerPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        customizerPanelScroll = new ScrollableContainer(customizerPanel,
                ScrollableContainer.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollableContainer.HORIZONTAL_SCROLLBAR_NEVER);
        customizerPanelScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
        add(customizerPanelScroll, BorderLayout.CENTER);
    }

    private static void createBorder(JComponent component, Border border) {
        Border cBorder = component.getBorder();
        if (cBorder == null) component.setBorder(border);
        else component.setBorder(BorderFactory.createCompoundBorder(border, cBorder));
    }
    

    private JmxConnectionConfigurator() {
        initComponents();
    }


    private static final Icon INFO_ICON =
            ImageUtilities.loadImageIcon("org/graalvm/visualvm/jmx/resources/infoIcon.png", // NOI18N
            false);


    private boolean singleCustomizer;

    private JmxConnectionCustomizer selectedCustomizer;
    private PropertiesPanel displayedPanel;
    private List<JmxConnectionCustomizer> customizers = new ArrayList<>();
    private List<PropertiesPanel> customizerPanels = new ArrayList<>();

    private SelectionListener selectionListener = new SelectionListener();
    private ValidityListener validityListener = new ValidityListener();

    private String lastSelectedItem;
    private Dimension lastSize;

    private DefaultListModel<JmxConnectionCustomizer> connectionTypeListModel;

    private JButton okButton;
    private JLabel hintLabel;

    private JLabel connectionTypeLabel;
    private JList<JmxConnectionCustomizer> connectionTypeList;
    private JScrollPane connectionTypeScroll;
    private JPanel customizerPanel;
    private ScrollableContainer customizerPanelScroll;

    private static JmxConnectionConfigurator INSTANCE;


    public static final class Result {

        private final JmxConnectionCustomizer.Setup setup;
        private final JmxConnectionCustomizer selectedCustomizer;
        private final List<JmxConnectionCustomizer> customizers = new ArrayList<>();
        private final List<PropertiesPanel> customizerPanels = new ArrayList<>();


        public Result(JmxConnectionCustomizer.Setup setup,
                      JmxConnectionCustomizer selectedCustomizer,
                      List<JmxConnectionCustomizer> customizers,
                      List<PropertiesPanel> customizerPanels) {
            this.setup = setup;
            this.selectedCustomizer = selectedCustomizer;
            this.customizers.addAll(customizers);
            this.customizerPanels.addAll(customizerPanels);
        }


        public JmxConnectionCustomizer.Setup getSetup() { return setup; }

        public void accepted(JmxApplication application) {
            for (int i = 0; i < customizers.size(); i++) {
                JmxConnectionCustomizer c = customizers.get(i);
                if (c == selectedCustomizer) {
                    JmxPropertiesProvider.setCustomizer(application, selectedCustomizer);
                    c.propertiesDefined(customizerPanels.get(i), application);
                } else {
                    customizers.get(i).propertiesCancelled(customizerPanels.get(i), null);
                }
            }
        }

        public void cancelled() {
            for (int i = 0; i < customizers.size(); i++)
                customizers.get(i).propertiesCancelled(customizerPanels.get(i), null);
        }

    }

}
