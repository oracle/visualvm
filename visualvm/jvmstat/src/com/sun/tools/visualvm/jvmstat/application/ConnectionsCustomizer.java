/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.jvmstat.application;

import com.sun.tools.visualvm.core.options.GlobalPreferences;
import com.sun.tools.visualvm.core.properties.PropertiesPanel;
import com.sun.tools.visualvm.core.ui.components.ScrollableContainer;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
class ConnectionsCustomizer extends PropertiesPanel {

    private static final Border SELECTED_BORDER = selectedBorder();
    private static final Border EMPTY_BORDER = emptyBorder(SELECTED_BORDER);
    private static final ConnectionDescriptor DEFAULT_CONNECTION =
            ConnectionDescriptor.createDefault();

    private static final String DATA_VIEW = "DATA_VIEW"; // NOI18N
    private static final String NO_DATA_VIEW = "NO_DATA_VIEW"; // NOI18N

    private static int TABLE_WIDTH = -1;
    private static int ROW_HEIGHT  = -1;

    private final DefaultTableModel model;


    public ConnectionsCustomizer(Set<ConnectionDescriptor> descriptors) {
        this.model = getModel(descriptors);
        initComponents();
        update();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (model.getRowCount() > 0)
                    table.getSelectionModel().setSelectionInterval(0, 0);
            }
        });
    }


    public Set<ConnectionDescriptor> getDescriptors() {
        return getDescriptors(model);
    }


    private static DefaultTableModel getModel(Set<ConnectionDescriptor> descriptors) {
        DefaultTableModel model = new DefaultTableModel(new Object[] { "Connections" }, 0); // NOI18N
        for (ConnectionDescriptor descriptor : descriptors)
                model.addRow(new Object[] { descriptor });
        return model;
    }

    private static Set<ConnectionDescriptor> getDescriptors(DefaultTableModel model) {
        Set<ConnectionDescriptor> descriptors = new HashSet();
        for (int i = 0; i < model.getRowCount(); i++)
            descriptors.add((ConnectionDescriptor)model.getValueAt(i, 0));
        return descriptors;
    }


    private static Border selectedBorder() {
        Border b = UIManager.getBorder("Table.focusSelectedCellHighlightBorder"); // NOI18N
        if (b == null) b = UIManager.getBorder("Table.focusCellHighlightBorder"); // NOI18N
        return b;
    }

    private static Border emptyBorder(Border selectedBorder) {
        Insets i = selectedBorder == null ? new Insets(0, 0, 0, 0) :
                   selectedBorder.getBorderInsets(new JTextField());
        return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right);
    }


    private void addDefault() {
        model.addRow(new Object[] { ConnectionDescriptor.createDefault() });
        int row = table.getRowCount() - 1;
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    private void addCustom() {
        ConnectionDescriptor d = new ConnectionDescriptor(getUnusedPort(), GlobalPreferences.sharedInstance().getMonitoredHostPoll());
        model.addRow(new Object[] { d });
        int row = table.getRowCount() - 1;
        table.getSelectionModel().setSelectionInterval(row, row);
    }

    private void removeSelected() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;
        table.clearSelection();
        model.removeRow(selectedRow);
        if (selectedRow < table.getRowCount()) table.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
        else if (selectedRow > 0) table.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
    }

    private void update() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                CardLayout cl = (CardLayout)viewPanel.getLayout();
                cl.show(viewPanel, model.getRowCount() > 0 ? DATA_VIEW : NO_DATA_VIEW);
                addDefault.setEnabled(!containsConnection(DEFAULT_CONNECTION));
                remove.setEnabled(table.getSelectedRow() != -1);
            }
        });
    }


    private boolean containsConnection(ConnectionDescriptor d) {
        for (int i = 0; i < table.getRowCount(); i++)
            if (table.getValueAt(i, 0).equals(d)) return true;
        return false;
    }

    private int getUnusedPort() {
        Set<Integer> ports = new HashSet();
        for (int i = 0; i < table.getRowCount(); i++)
            ports.add(((ConnectionDescriptor)table.getValueAt(i, 0)).getPort());
        for (int i = ConnectionDescriptor.createDefault().getPort() + 1; i < 65536; i++)
            if (!ports.contains(i)) return i;
        return -1;
    }


    private void initComponents() {
        setLayout(new GridBagLayout());

        GridBagConstraints c;

        CellRenderer renderer = new CellRenderer();
        CellEditor editor = new CellEditor(new Runnable() {
            public void run() { update(); }
        });

        table = new ConnectionsTable(model, renderer, editor) {
            public int getRowHeight() {
                return ROW_HEIGHT;
            }
            public Dimension getPreferredSize() {
                Dimension ps = super.getPreferredSize();
                ps.width = TABLE_WIDTH;
                return ps;
            }
            public Dimension getMinimumSize() {
                Dimension ms = super.getMinimumSize();
                ms.width = TABLE_WIDTH;
                return ms;
            }
        };
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension());
        table.setFillsViewportHeight(true);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                update();
            }
        });

        if (TABLE_WIDTH == -1) {
            TABLE_WIDTH = editor.getTableCellEditorComponent(table, new
                          ConnectionDescriptor(1, 1), false, 1, 1).
                          getPreferredSize().width;
            editor.stopCellEditing();
        }
        if (ROW_HEIGHT == -1) {
            ROW_HEIGHT  = renderer.getTableCellRendererComponent(table,
                          new ConnectionDescriptor(1, 1), false, false,
                          1, 1).getPreferredSize().height;
        }

        JScrollPane impl = new JScrollPane();
        JScrollPane scroll = new ScrollableContainer(table);
        scroll.setBorder(impl.getBorder());
        scroll.setViewportBorder(impl.getViewportBorder());
        scroll.setPreferredSize(new Dimension(TABLE_WIDTH + 20, 1));

        JLabel noConnection = new JLabel(NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_NoConnection"), JLabel.CENTER); // NOI18N
        noConnection.setEnabled(false);
        noConnection.setOpaque(true);
        noConnection.setBackground(table.getBackground());
        noConnection.setMinimumSize(new Dimension());
        JScrollPane emptyScroll = new JScrollPane(noConnection,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        emptyScroll.getViewport().setOpaque(false);
        emptyScroll.setOpaque(false);
        viewPanel = new JPanel(new CardLayout());
        viewPanel.setOpaque(false);
        viewPanel.add(emptyScroll, NO_DATA_VIEW);
        viewPanel.add(scroll, DATA_VIEW);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 10);
        add(viewPanel, c);

        addDefault = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                addDefault();
            }
        };
        Mnemonics.setLocalizedText(addDefault, NbBundle.getMessage(
                ConnectionsCustomizer.class, "BTN_AddDefault")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 3, 0);
        add(addDefault, c);

        JButton addCustom = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                addCustom();
            }
        };
        Mnemonics.setLocalizedText(addCustom, NbBundle.getMessage(
                ConnectionsCustomizer.class, "BTN_AddCustom")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 3, 0);
        add(addCustom, c);

        remove = new JButton() {
            protected void fireActionPerformed(ActionEvent e) {
                removeSelected();
            }
        };
        Mnemonics.setLocalizedText(remove, NbBundle.getMessage(
                ConnectionsCustomizer.class, "BTN_Remove")); // NOI18N
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 0, 0, 0);
        add(remove, c);

    }

    private JPanel viewPanel;
    private ConnectionsTable table;
    private JButton addDefault;
    private JButton remove;


    private static class CellRenderer extends JPanel implements TableCellRenderer {

        private static final int BORDER_HEIGHT = 4;

        private JLabel portLabel;
        private JLabel portValueLabel;
        private JLabel refreshLabel;
        private JLabel refreshValueLabel;
        private JLabel refreshUnitsLabel;

        private Color color;
        private Color darkerColor;

        private final NumberFormat format = NumberFormat.getInstance();

        public CellRenderer() {
            initComponents();
        }


        private void initComponents() {
            portLabel = new JLabel(NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_Port1")); // NOI18N
            portLabel.setFont(portLabel.getFont().deriveFont(Font.BOLD));
            final int w = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 0)).
                    getPreferredSize().width;
            portValueLabel = new JLabel() {
                public Dimension getPreferredSize() {
                    Dimension ps = super.getPreferredSize();
                    ps.width = w;
                    return ps;
                }
            };

            refreshLabel = new JLabel(NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_Refresh1")); // NOI18N
            refreshLabel.setFont(refreshLabel.getFont().deriveFont(Font.BOLD));
            refreshValueLabel = new JLabel();
            refreshUnitsLabel = new JLabel(NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_RefreshUnits")); // NOI18N

            setLayout(new GridBagLayout());
            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(portLabel, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(portValueLabel, c);

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 15, BORDER_HEIGHT, 0);
            add(refreshLabel, c);

            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(refreshValueLabel, c);

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(refreshUnitsLabel, c);

            JPanel filler = new JPanel(null);
            filler.setOpaque(false);
            c = new GridBagConstraints();
            c.gridx = 5;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, 5, 0, 0);
            add(filler, c);

            setOpaque(true);
            setBorder(EMPTY_BORDER);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            ConnectionDescriptor cd = (ConnectionDescriptor)value;
            portValueLabel.setText(format.format(cd.getPort()));
            refreshValueLabel.setText(format.format(cd.getRefreshRate()));

            if (color == null) {
                color = table.getBackground();
                darkerColor = darker(color);
            }

            if (!isSelected) {
                boolean oddRow = row % 2 == 0;
                setBackground(oddRow ? darkerColor : color);
            }

            return this;
        }

        private static Color darker(Color c) {
            if (c == null) return null;
            int r = Math.abs(c.getRed() - 11);
            int g = Math.abs(c.getGreen() - 11);
            int b = Math.abs(c.getBlue() - 11);
            int a = c.getAlpha();
            return new Color(r, g, b, a);
        }

    }

    private static class CellEditor extends JPanel implements TableCellEditor {

        private static final int BORDER_HEIGHT = 6;
        private static final Color BACKGROUND_COLOR =
                UIManager.getColor("Tree.selectionBackground"); // NOI18N
        private static final Color FOREGROUND_COLOR =
                UIManager.getColor("Tree.selectionForeground"); // NOI18N

        private Runnable updater;

        private JTable table;
        private ConnectionDescriptor cd;

        private JLabel portLabel;
        private JSpinner portSpinner;
        private JLabel refreshLabel;
        private JSpinner refreshSpinner;
        private JLabel refreshUnitsLabel;


        public CellEditor(Runnable updater) {
            this.updater = updater;
            initComponents();
        }

        private PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateBorder();
                    }
                });
            }
        };

        private void updateBorder() {
            if (displayFocus()) setBorder(SELECTED_BORDER);
            else setBorder(EMPTY_BORDER);
        }

        private boolean displayFocus() {
            if (table == null) return false;
            Component focusOwner = KeyboardFocusManager.
                    getCurrentKeyboardFocusManager().getFocusOwner();
            return focusOwner != null ? focusOwner == table || SwingUtilities.
                    isDescendingFrom(focusOwner, this) : false;
        }

        private void initComponents() {
            portLabel = new JLabel();
            Mnemonics.setLocalizedText(portLabel, NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_Port2")); // NOI18N
            portLabel.setFont(portLabel.getFont().deriveFont(Font.BOLD));
            portSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
            portLabel.setLabelFor(portSpinner);

            refreshLabel = new JLabel();
            Mnemonics.setLocalizedText(refreshLabel, NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_Refresh2")); // NOI18N
            refreshLabel.setFont(refreshLabel.getFont().deriveFont(Font.BOLD));
            refreshSpinner = new JSpinner(new SpinnerNumberModel(3.0, 1.0, 9999.0, 1.0));
            refreshLabel.setLabelFor(refreshSpinner);
            refreshUnitsLabel = new JLabel(NbBundle.getMessage(
                ConnectionsCustomizer.class, "LBL_RefreshUnits")); // NOI18N

            setLayout(new GridBagLayout());
            GridBagConstraints c;

            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(portLabel, c);

            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(portSpinner, c);

            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 15, BORDER_HEIGHT, 0);
            add(refreshLabel, c);

            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 0);
            add(refreshSpinner, c);

            c = new GridBagConstraints();
            c.gridx = 4;
            c.gridy = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(BORDER_HEIGHT, 5, BORDER_HEIGHT, 5);
            add(refreshUnitsLabel, c);

            JPanel filler = new JPanel(null);
            filler.setOpaque(false);
            c = new GridBagConstraints();
            c.gridx = 5;
            c.gridy = 0;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.gridheight = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(0, 5, 0, 0);
            add(filler, c);

            setOpaque(true);
            setBackground(BACKGROUND_COLOR);
            portLabel.setForeground(FOREGROUND_COLOR);
            refreshLabel.setForeground(FOREGROUND_COLOR);
            refreshUnitsLabel.setForeground(FOREGROUND_COLOR);

            JComponent portEditor = portSpinner.getEditor();
            if (portEditor instanceof JSpinner.DefaultEditor) {
                final JFormattedTextField tf = ((JSpinner.DefaultEditor)portEditor).getTextField();
                tf.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { commitEdit(); }
                    public void removeUpdate(DocumentEvent e) { commitEdit(); }
                    public void changedUpdate(DocumentEvent e) { commitEdit(); }

                    private void commitEdit() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    int val = (Integer)tf.getFormatter().
                                            stringToValue(tf.getText().trim());
                                    if (cd != null) {
                                        cd.setPort(val);
                                        updater.run();
                                    }
                                } catch (Exception ex) {}
                            }
                        });
                    }
                });
            } else {
                portSpinner.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        if (cd != null) cd.setPort((Integer)portSpinner.getValue());
                    }
                });
            }

            JComponent refreshEditor = refreshSpinner.getEditor();
            if (refreshEditor instanceof JSpinner.DefaultEditor) {
                final JFormattedTextField tf = ((JSpinner.DefaultEditor)refreshEditor).getTextField();
                tf.getDocument().addDocumentListener(new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { commitEdit(); }
                    public void removeUpdate(DocumentEvent e) { commitEdit(); }
                    public void changedUpdate(DocumentEvent e) { commitEdit(); }

                    private void commitEdit() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                try {
                                    int val = (Integer)tf.getFormatter().
                                            stringToValue(tf.getText().trim());
                                    if (cd != null) cd.setRefreshRate(val);
                                } catch (Exception ex) {}
                            }
                        });
                    }
                });
            } else {
                refreshSpinner.addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        if (cd != null) cd.setRefreshRate((Integer)refreshSpinner.getValue());
                    }
                });
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.table = table;
            cd = (ConnectionDescriptor)value;

            portSpinner.setValue(cd.getPort());
            refreshSpinner.setValue(cd.getRefreshRate());

            updateBorder();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    addPropertyChangeListener("focusOwner", listener); // NOI18N

            return this;
        }

        public Object getCellEditorValue() { return cd; }

        public boolean stopCellEditing() { cleanup(); return true; }

        public boolean isCellEditable(EventObject anEvent) { return true; }

        public boolean shouldSelectCell(EventObject anEvent) { return true; }

        public void cancelCellEditing() { cleanup(); }

        public void addCellEditorListener(CellEditorListener l) {}

        public void removeCellEditorListener(CellEditorListener l) {}


        private void cleanup() {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                    removePropertyChangeListener("focusOwner", listener); // NOI18N
            table = null;
            cd = null;
        }

    }

}
