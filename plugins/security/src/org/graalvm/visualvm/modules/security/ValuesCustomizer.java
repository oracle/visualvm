/*
 * Copyright (c) 2007, 2023, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.security;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.graalvm.visualvm.uisupport.UISupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class ValuesCustomizer extends JPanel {

    // --- Private UI constants ------------------------------------------------

    private static final Color DEFAULT_GRID_COLOR = new Color(240, 240, 240);

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
        table.setOpaque(true);
        table.setBackground(UISupport.getDefaultBackground());
        table.setRowHeight(defaultRowHeight() + 4);
        table.setRowMargin(0);
        table.setAutoCreateRowSorter(true);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(true);
        table.setGridColor(DEFAULT_GRID_COLOR);
        table.setDefaultRenderer(String.class, new Renderer(
                                 table.getDefaultRenderer(String.class)));
        table.setDefaultRenderer(Boolean.class, new BooleanRenderer(
                                 table.getDefaultRenderer(Boolean.class)));
        table.getColumnModel().setColumnMargin(1);
        TableColumn c = table.getColumnModel().getColumn(1);
        c.setMaxWidth(c.getPreferredWidth());
        c.setResizable(false);

        // viewport
        JViewport viewport = new Viewport(table);

        // tableScroll
        JScrollPane tableScroll = new JScrollPane(
                                      JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                      JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setViewport(viewport);
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
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(15, 10, 5, 10));
        setLayout(new BorderLayout(5, 5));
        add(hintLabel, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
    }

    private static int defaultRowHeight() {
        return new JLabel("X").getPreferredSize().height + 4; // NOI18N
    }


    private ValuesCustomizer() {}


    private DefaultTableModel model;
    private JTable table;


    private static class Renderer implements TableCellRenderer {

        private static final Color BACKGROUND;
        private static final Color DARKER_BACKGROUND;

        static {
            BACKGROUND = UISupport.getDefaultBackground();

            int darkerR = BACKGROUND.getRed() - 11;
            if (darkerR < 0) darkerR += 26;
            int darkerG = BACKGROUND.getGreen() - 11;
            if (darkerG < 0) darkerG += 26;
            int darkerB = BACKGROUND.getBlue() - 11;
            if (darkerB < 0) darkerB += 26;
            DARKER_BACKGROUND = new Color(darkerR, darkerG, darkerB);
        }

        private TableCellRenderer impl;


        Renderer(TableCellRenderer impl) {
            this.impl = impl;
        }


        protected Object formatValue(JTable table, Object value, boolean isSelected,
                                     boolean hasFocus, int row, int column) {
            return value;
        }

        protected void updateRenderer(Component c, JTable table, Object value,
                                      boolean isSelected, boolean hasFocus, int row,
                                      int column) {
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? DARKER_BACKGROUND : BACKGROUND);
                // Make sure the renderer paints its background (Nimbus)
                if (c instanceof JComponent) ((JComponent)c).setOpaque(true);
            }
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            if (impl == null) impl = table.getDefaultRenderer(table.getColumnClass(column));

            value = formatValue(table, value, isSelected, false, row, column);
            Component c = impl.getTableCellRendererComponent(table, value, isSelected,
                                                             false, row, column);
            updateRenderer(c, table, value, isSelected, false, row, column);

            return c;
        }

    }

    private static class BooleanRenderer extends Renderer {

        BooleanRenderer(TableCellRenderer renderer) {
            super(renderer);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {

            // Workaround strange selection behavior for newly selected checkbox
            isSelected = isSelected || hasFocus;

            return super.getTableCellRendererComponent(table, value, isSelected,
                                                       hasFocus, row, column);
        }

    }

    private static class Viewport extends JViewport {

        private final JTable view;
        private final Color background;

        Viewport(JTable view) {
            super();

            setView(view);
            this.view = view;

            setOpaque(true);
            background = view.getBackground();
            setBackground(background);

            view.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
                public void columnAdded(TableColumnModelEvent e) { repaint(); }
                public void columnMoved(TableColumnModelEvent e) { repaint(); }
                public void columnRemoved(TableColumnModelEvent e) { repaint(); }
                public void columnMarginChanged(ChangeEvent e) { repaint(); }
                public void columnSelectionChanged(ListSelectionEvent e) {}
            });
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintVerticalLines(g);
        }

        private void paintVerticalLines(Graphics g) {
            int height = getHeight();
            int viewHeight = view.getHeight();
            if (viewHeight >= height) return;

            g.setColor(background);
            g.fillRect(0, viewHeight, getWidth(), getHeight() - viewHeight);

            int cellX = 0;
            int cellWidth;
            TableColumnModel model = view.getColumnModel();
            int columnCount = model.getColumnCount();

            g.setColor(DEFAULT_GRID_COLOR);
            for (int i = 0; i < columnCount; i++) {
                cellWidth = model.getColumn(i).getWidth();
                cellX += cellWidth;
                g.drawLine(cellX - 1, viewHeight, cellX - 1, height);
            }
        }

    }

}
