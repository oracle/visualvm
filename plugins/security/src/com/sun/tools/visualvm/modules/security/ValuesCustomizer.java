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

package com.sun.tools.visualvm.modules.security;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class ValuesCustomizer extends JPanel {

    // --- Public customizer types ---------------------------------------------
    
    static final ValuesCustomizer PROTOCOLS = new Protocols();
    static final ValuesCustomizer CIPHER_SUITES = new CipherSuites();


    // --- Public entrypoint ---------------------------------------------------

    static String customize(final ValuesCustomizer customizer, String selectedValues) {
        customizer.init(selectedValues);

        final DialogDescriptor dd = new DialogDescriptor(customizer,
                                    customizer.dialogTitle(), true, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
        d.pack();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { customizer.onShown(); }
        });
        d.setVisible(true);

        String result = dd.getValue() != DialogDescriptor.OK_OPTION ? null :
                                         customizer.getSelectedCipherSuites();
        customizer.cleanup();
        return result;
    }


    // --- Predefined customizers ----------------------------------------------

    private static class Protocols extends ValuesCustomizer {

        private String[] allValues;

        String dialogTitle() {
            return NbBundle.getMessage(ValuesCustomizer.class, "CAP_SelectProtocols"); // NOI18N
        }

        String hintText() {
            return NbBundle.getMessage(ValuesCustomizer.class, "HINT_SelectProtocols"); // NOI18N
        }

        String valueName() {
            return NbBundle.getMessage(ValuesCustomizer.class, "COL_Protocols"); // NOI18N
        }

        synchronized String[] allValues() {
            if (allValues == null) {
                SocketFactory f = SSLSocketFactory.getDefault();
                if (!(f instanceof SSLSocketFactory)) allValues = new String[0];
                try {
                    Socket s = ((SSLSocketFactory)f).createSocket();
                    if (!(s instanceof SSLSocket)) allValues = new String[0];
                    allValues = ((SSLSocket)s).getSupportedProtocols();
                } catch (Exception e) {
                    allValues = new String[0];
                }
            }
            return allValues;
        }

    }


    private static class CipherSuites extends ValuesCustomizer {

        private String[] allValues;

        String dialogTitle() {
            return NbBundle.getMessage(ValuesCustomizer.class, "CAP_SelectCipherSuites"); // NOI18N
        }

        String hintText() {
            return NbBundle.getMessage(ValuesCustomizer.class, "HINT_SelectCipherSuites"); // NOI18N
        }

        String valueName() {
            return NbBundle.getMessage(ValuesCustomizer.class, "COL_CipherSuites"); // NOI18N
        }

        synchronized String[] allValues() {
            if (allValues == null) {
            SocketFactory f = SSLSocketFactory.getDefault();
                if (!(f instanceof SSLSocketFactory)) allValues = new String[0];
                allValues = ((SSLSocketFactory)f).getSupportedCipherSuites();
            }
            return allValues;
        }

    }


    // --- Abstract interface --------------------------------------------------

    abstract String dialogTitle();

    abstract String hintText();

    abstract String valueName();

    abstract String[] allValues();


    // --- Private implementation ----------------------------------------------

    private void init(String selectedValues) {
        initModels(selectedValues);
        initComponents();
    }

    private void onShown() {
        table.requestFocusInWindow();
    }

    private String getSelectedCipherSuites() {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < model.getRowCount(); i++)
            if (Boolean.TRUE.equals(model.getValueAt(i, 1)))
                b.append(model.getValueAt(i, 0).toString() + ","); // NOI18N

        int length = b.length();
        if (length > 0) b.deleteCharAt(length - 1);
        return b.toString();
    }

    private void cleanup() {
        removeAll();
        table = null;
        model = null;
    }


    private void initModels(String selectedValues) {
        String[] allValuesArr = allValues();
        String[] selectedValuesArr = selectedValues(selectedValues);

        final String[] cipherSuites =
                mergedValues(allValuesArr, selectedValuesArr);
        final boolean[] selectedMask =
                selectedValuesMask(cipherSuites, selectedValuesArr);
        final int rowsCount = cipherSuites.length;

        model = new DefaultTableModel() {
            public int getRowCount() { return rowsCount; }

            public int getColumnCount() { return 2; }

            public String getColumnName(int columnIndex) {
                if (columnIndex == 0) return valueName();
                else return NbBundle.getMessage(ValuesCustomizer.class, "COL_Enabled"); // NOI18N
            }

            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return String.class;
                else return Boolean.class;
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                if (columnIndex == 0) return false;
                else return true;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) return cipherSuites[rowIndex];
                else return selectedMask[rowIndex];
            }

            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == 1) selectedMask[rowIndex] = (Boolean)aValue;
            }
        };
    }


    private static String[] selectedValues(String selectedValues) {
        StringTokenizer st = new StringTokenizer(selectedValues, ","); // NOI18N
        String[] cipherSuites = new String[st.countTokens()];
        for (int i = 0; i < cipherSuites.length; i++)
            cipherSuites[i] = st.nextToken();
        return cipherSuites;
    }

    private static String[] mergedValues(String[] supported, String[] selected) {
        List<String> supportedList = Arrays.asList(supported);
        List<String> selectedList = Arrays.asList(selected);
        Set<String> mergedSet = new HashSet(supportedList);
        mergedSet.addAll(selectedList);
        List<String> merged = new ArrayList(mergedSet);
        Collections.sort(merged);
        return merged.toArray(new String[merged.size()]);
    }

    private static boolean[] selectedValuesMask(String[] allValues,
                                                String[] selectedValues) {
        boolean[] mask = new boolean[allValues.length];
        List<String> selectedValuesList = Arrays.asList(selectedValues);
        for (int i = 0; i < mask.length; i++)
            if (selectedValuesList.contains(allValues[i]))
                mask[i] = true;
        return mask;
    }


    private void setAllSelected(boolean selected) {
        for (int i = 0; i < model.getRowCount(); i++)
            model.setValueAt(selected, i, 1);
        model.fireTableDataChanged();
    }


    private void initComponents() {
        // hintLabel
        JLabel hintLabel = new JLabel();
        Mnemonics.setLocalizedText(hintLabel, hintText());

        // table
        table = new JTable(model) {
            protected void processMouseEvent(MouseEvent e) {
                MouseEvent eventToDispatch = e;
                Point p = e.getPoint();
                int column = columnAtPoint(p);
                if (column != 1) {
                    int row = rowAtPoint(p);
                    Rectangle cellRect = getCellRect(row, 1, false);
                    p.x = cellRect.x + 1;
                    eventToDispatch = new MouseEvent((Component)e.getSource(),
                            e.getID(), e.getWhen(), e.getModifiers(), p.x, p.y,
                            e.getClickCount(), e.isPopupTrigger(), e.getButton());
                }
                super.processMouseEvent(eventToDispatch);
            }
            protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
					int condition, boolean pressed) {
                getColumnModel().getSelectionModel().setSelectionInterval(1, 1);
                return super.processKeyBinding(ks, e, condition, pressed);
            }
            protected void initializeLocalVars() {
                super.initializeLocalVars();
                setPreferredScrollableViewportSize(new Dimension(450, 300));
            }
        };
        hintLabel.setLabelFor(table);
        table.setRowHeight(table.getRowHeight() + 4);
        Dimension d = table.getIntercellSpacing();
        table.setIntercellSpacing(new Dimension(d.width, 0));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setShowHorizontalLines(false);
        table.setGridColor(new Color(214, 223, 247));
        table.setDefaultRenderer(String.class, new Renderer(
                                 table.getDefaultRenderer(String.class)));
        table.setDefaultRenderer(Boolean.class, new Renderer(
                                 table.getDefaultRenderer(Boolean.class)));
        TableColumn c = table.getColumnModel().getColumn(1);
        c.setMaxWidth(c.getPreferredWidth());
        c.setResizable(false);

        // tableScroll
        JScrollPane tableScroll = new JScrollPane(table,
                                      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        final JScrollBar vScrollBar = tableScroll.getVerticalScrollBar();
        final BoundedRangeModel vScrollBarModel = vScrollBar.getModel();
        vScrollBarModel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                vScrollBar.setEnabled(vScrollBarModel.getExtent() !=
                                      vScrollBarModel.getMaximum());
            }
        });

        // cornerButton
        final JButton cornerButton = new JButton();
        cornerButton.setDefaultCapable(false);
        cornerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JMenuItem selectAll = new JMenuItem (new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        setAllSelected(true);
                    }
                });
                Mnemonics.setLocalizedText(selectAll, NbBundle.getMessage(
                        ValuesCustomizer.class, "ACT_SelectAll")); // NOI18N
                JMenuItem deselectAll = new JMenuItem(new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        setAllSelected(false);
                    }
                });
                Mnemonics.setLocalizedText(deselectAll, NbBundle.getMessage(
                        ValuesCustomizer.class, "ACT_DeselectAll")); // NOI18N
                JPopupMenu popup = new JPopupMenu();
                popup.add(selectAll);
                popup.add(deselectAll);
                Dimension s = popup.getPreferredSize();
                popup.show(cornerButton, cornerButton.getWidth() / 2 - s.width,
                                         cornerButton.getHeight() / 2);
            }
        });
        tableScroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, cornerButton);

        // this
        setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        setLayout(new BorderLayout(5, 5));
        add(hintLabel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }


    private static class Renderer implements TableCellRenderer {

        private TableCellRenderer impl;
        private Color color;
        private Color darkerColor;

        public Renderer(TableCellRenderer impl) { this.impl = impl; }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            if (value instanceof Boolean)
                // Workaround strange selection behavior for newly selected checkbox
                isSelected = isSelected || hasFocus;
            else if (value instanceof String)
                // Improve spacing of the text
                value = " " + value + " "; // NOI18N

            Component c = impl.getTableCellRendererComponent(
                    table, value, isSelected, false, row, column);

            if (color == null) {
                color = c.getBackground();
                darkerColor = darker(color);
            }

            if (!isSelected) {
                boolean oddRow = row % 2 == 0;
                c.setBackground(oddRow ? darkerColor : color);
            }

            return c;
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

    
    private ValuesCustomizer() {}


    private DefaultTableModel model;
    private JTable table;

}
