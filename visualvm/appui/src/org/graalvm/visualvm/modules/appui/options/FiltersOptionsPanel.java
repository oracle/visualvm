/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.core.options.UISupport;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import org.graalvm.visualvm.lib.ui.results.ColoredFilter;
import org.graalvm.visualvm.lib.ui.results.PackageColorer;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTable;
import org.graalvm.visualvm.lib.ui.swing.ProfilerTableContainer;
import org.graalvm.visualvm.lib.ui.swing.SmallButton;
import org.graalvm.visualvm.lib.ui.swing.renderer.JavaNameRenderer;
import org.graalvm.visualvm.lib.ui.swing.renderer.LabelRenderer;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.api.icons.ProfilerIcons;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "FiltersOptionsPanel_Name=Filters",
    "FiltersOptionsPanel_ColoringResults=&Use defined filters for coloring results (Sampler, Profiler, Heap Viewer, JFR Viewer, etc.)",
    "FiltersOptionsPanel_DefinedFilters=Defined &Filters:",
    "FiltersOptionsPanel_AddFilter=Add new filter",
    "FiltersOptionsPanel_EditFilter=Edit selected filter",
    "FiltersOptionsPanel_DeleteFilter=Delete selected filter",
    "FiltersOptionsPanel_MoveUp=Move selected filter up",
    "FiltersOptionsPanel_MoveDown=Move selected filter down",
    "FiltersOptionsPanel_ColumnFilter=Filter",
    "FiltersOptionsPanel_ColumnPackages=Packages",
    "FiltersOptionsPanel_ColumnColor=Color",
    "ColorCustomizer_DefaultColor=Default color",
    "ColorCustomizer_CustomColor=Custom color [{0},{1},{2}]",
    "ColorCustomizer_Name=Name:",
    "ColorCustomizer_Color=Color:",
    "ColorCustomizer_ColorHint=Select to define custom color, unselect to use the default color",
    "ColorCustomizer_Value=Value:",
    "ColorCustomizer_AddCaption=Add Filter",
    "ColorCustomizer_EditCaption=Edit Filter",
    "ColorCustomizer_ColorCaption=Choose Filter Color"
})
final class FiltersOptionsPanel extends JPanel {
    
    private final List<ColoredFilter> colors = new ArrayList();
    private final ColorsTableModel colorsModel = new ColorsTableModel();
    
    private JCheckBox coloringChoice;
    
    
    FiltersOptionsPanel() {
        initUI();
    }

    
    public String getDisplayName() {
        return Bundle.FiltersOptionsPanel_Name();
    }

    public void storeTo(ProfilerIDESettings settings) {
        settings.setSourcesColoringEnabled(coloringChoice.isSelected());
        PackageColorer.setRegisteredColors(colors);
        for (Window w : Window.getWindows()) w.repaint();
    }

    public void loadFrom(ProfilerIDESettings settings) {
        coloringChoice.setSelected(settings.isSourcesColoringEnabled());
        colors.clear();
        colors.addAll(PackageColorer.getRegisteredColors());
        colorsModel.fireTableDataChanged();
    }

    public boolean equalsTo(ProfilerIDESettings settings) {
        if (coloringChoice.isSelected() != settings.isSourcesColoringEnabled()) return false;
        return Objects.equals(PackageColorer.getRegisteredColors(), colors);
    }
    
    
    private void initUI() {
        setLayout(new GridBagLayout());
        
        GridBagConstraints c;
        int y = 0;
        int htab = 15;
        int vgap = 5;
        
        SectionSeparator filtersSection = UISupport.createSectionSeparator(Bundle.FiltersOptionsPanel_Name());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 7, 0);
        add(filtersSection, c);
        
        coloringChoice = new JCheckBox();
        Mnemonics.setLocalizedText(coloringChoice, Bundle.FiltersOptionsPanel_ColoringResults());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, htab, vgap * 3, 0);
        add(coloringChoice, c);
        
        JLabel tableCaption = new JLabel();
        Mnemonics.setLocalizedText(tableCaption, Bundle.FiltersOptionsPanel_DefinedFilters());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y++;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, htab, vgap, 0);
        add(tableCaption, c);
        
        final String colorString = "ABCabc123"; // NOI18N
        final ProfilerTable colorsTable = new ProfilerTable(colorsModel, false, false, null);
        colorsTable.setMainColumn(1);
        colorsTable.setFitWidthColumn(1);
        LabelRenderer stringRenderer = new LabelRenderer();
        colorsTable.setColumnRenderer(0, stringRenderer);
        colorsTable.setColumnRenderer(1, stringRenderer);
        LabelRenderer colorRenderer = new LabelRenderer() {
            private final Color _fg = new JTable().getForeground();
            private Color fg;
            {
                setText(colorString); // NOI18N
                setHorizontalAlignment(TRAILING);
            }
            public void setValue(Object value, int row) {
                fg = (Color)value;
            }
            public void setForeground(Color color) {
                if (fg != null && Objects.equals(color, _fg)) super.setForeground(fg);
                else super.setForeground(color);
            }
        };
        colorsTable.setColumnRenderer(2, colorRenderer);
        stringRenderer.setValue("PLACEHOLDER FILTER NAME", -1); // NOI18N
        colorsTable.setDefaultColumnWidth(0, stringRenderer.getPreferredSize().width);
        stringRenderer.setValue(colorString, -1);
        colorsTable.setDefaultColumnWidth(2, stringRenderer.getPreferredSize().width + 10);
        ProfilerTableContainer colorsContainer = new ProfilerTableContainer(colorsTable, true, null);
        colorsContainer.setPreferredSize(new Dimension(1, 1));
        tableCaption.setLabelFor(colorsTable);
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = y;
        c.weightx = 1;
        c.weighty = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, htab, 0, 0);
        add(colorsContainer, c);
        
        JButton addButton = new SmallButton(Icons.getIcon(GeneralIcons.ADD)) {
            {
                setToolTipText(Bundle.FiltersOptionsPanel_AddFilter());
            }
            protected void fireActionPerformed(ActionEvent e) {
                ColoredFilter newColor = ColorCustomizer.customize(new ColoredFilter("", "", null), true); // NOI18N
                if (newColor != null) {
                    colors.add(newColor);
                    colorsModel.fireTableDataChanged();
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
        
        final Runnable editPerformer = new Runnable() {
            public void run() {
                int row = colorsTable.getSelectedRow();
                if (row == -1) return;
                ColoredFilter selected = colors.get(row);
                ColoredFilter edited = ColorCustomizer.customize(selected, false);
                if (edited != null) {
                    selected.setName(edited.getName());
                    selected.setValue(edited.getValue());
                    selected.setColor(edited.getColor());
                    colorsModel.fireTableDataChanged();
                }
            }
        };
        final JButton editButton = new SmallButton(Icons.getIcon(GeneralIcons.EDIT)) {
            {
                setToolTipText(Bundle.FiltersOptionsPanel_EditFilter());
            }
            protected void fireActionPerformed(ActionEvent e) {
                editPerformer.run();
            }
        };
        colorsTable.setDefaultAction(new AbstractAction() {
            public void actionPerformed(ActionEvent e) { editPerformer.run(); }
        });
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(editButton, c);
        
        final JButton removeButton = new SmallButton(Icons.getIcon(GeneralIcons.REMOVE)) {
            {
                setToolTipText(Bundle.FiltersOptionsPanel_DeleteFilter());
            }
            protected void fireActionPerformed(ActionEvent e) {
                int row = colorsTable.getSelectedRow();
                if (row == -1) return;
                colors.remove(row);
                colorsModel.fireTableDataChanged();
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, vgap * 2, 0);
        add(removeButton, c);
        
        final JButton upButton = new SmallButton(Icons.getIcon(GeneralIcons.UP)) {
            {
                setToolTipText(Bundle.FiltersOptionsPanel_MoveUp());
            }
            protected void fireActionPerformed(ActionEvent e) {
                int row = colorsTable.getSelectedRow();
                if (row < 1) return;
                ColoredFilter color = colors.remove(row);
                colors.add(row - 1, color);
                colorsModel.fireTableDataChanged();
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(upButton, c);
        
        final JButton downButton = new SmallButton(Icons.getIcon(GeneralIcons.DOWN)) {
            {
                setToolTipText(Bundle.FiltersOptionsPanel_MoveDown());
            }
            protected void fireActionPerformed(ActionEvent e) {
                int row = colorsTable.getSelectedRow();
                if (row == -1 || row > colorsTable.getRowCount() - 2) return;
                ColoredFilter color = colors.remove(row);
                colors.add(row + 1, color);
                colorsModel.fireTableDataChanged();
            }
        };
        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = y++;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, htab, 0, 0);
        add(downButton, c);
        
        ListSelectionListener selection = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                int row = colorsTable.getSelectedRow();
                if (row == -1) {
                    editButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    upButton.setEnabled(false);
                    downButton.setEnabled(false);
                } else {
                    editButton.setEnabled(true);
                    removeButton.setEnabled(true);
                    upButton.setEnabled(row > 0);
                    downButton.setEnabled(row < colorsTable.getRowCount() - 1);
                }
            }
        };
        colorsTable.getSelectionModel().addListSelectionListener(selection);
        selection.valueChanged(null);
    }
    
    
    private class ColorsTableModel extends AbstractTableModel {
        
        public String getColumnName(int column) {
            switch (column) {
                case 0: return Bundle.FiltersOptionsPanel_ColumnFilter();
                case 1: return Bundle.FiltersOptionsPanel_ColumnPackages();
                case 2: return Bundle.FiltersOptionsPanel_ColumnColor();
                default: return null;
            }
        }

        public int getRowCount() {
            return colors.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public Object getValueAt(int rowIndex, int column) {
            switch (column) {
                case 0: return colors.get(rowIndex).getName();
                case 1: return colors.get(rowIndex).getValue();
                case 2: return colors.get(rowIndex).getColor();
                default: return null;
            }
        }
        
    }
    
    
    private static class ColorCustomizer {
        
        static ColoredFilter customize(ColoredFilter color, boolean newFilter) {
            final ColoredFilter customized = new ColoredFilter(color);
            JTextField nameF = new JTextField(customized.getName());
            JTextArea valueA = new JTextArea(customized.getValue());
            valueA.setRows(8);
            valueA.setColumns(45);
            valueA.setLineWrap(true);
            valueA.setWrapStyleWord(true);
            final JButton colorB = new JButton() {
                {
                    setIcon(customized.getIcon(16, 12));
                    setToolTipText(""); // NOI18N // register with ToolTipManager
                }
                protected void fireActionPerformed(ActionEvent e) {
                    Color c = selectColor(this, customized.getColor());
                    if (c != null) {
                        customized.setColor(c);
                        repaint();
                    }
                }
                public String getToolTipText(MouseEvent e) {
                    Color col = customized.getColor();
                    return col == null ? Bundle.ColorCustomizer_DefaultColor() :
                           Bundle.ColorCustomizer_CustomColor(col.getRed(), col.getGreen(), col.getBlue());
                }
            };
            
            JPanel p = new JPanel(new GridBagLayout());
            GridBagConstraints c;
            int hgap = 10;
            int htab = 5;
            int vgap = 5;
            int y = 0;
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.insets = new Insets(vgap * 2, hgap, 0, 0);
            p.add(new JLabel(Bundle.ColorCustomizer_Name()), c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(vgap * 2, htab, 0, 0);
            p.add(nameF, c);
            
            JCheckBox colorC = new JCheckBox(Bundle.ColorCustomizer_Color(), customized.getColor() != null) {
                private Color bkpC;
                protected void fireActionPerformed(ActionEvent e) {
                    super.fireActionPerformed(e);
                    if (isSelected()) {
                        customized.setColor(bkpC);
                        colorB.setEnabled(true);
                    } else {
                        bkpC = customized.getColor();
                        customized.setColor(null);
                        colorB.setEnabled(false);
                    }
                }
            };
            colorC.setOpaque(false);
            colorC.setToolTipText(Bundle.ColorCustomizer_ColorHint());
            colorB.setEnabled(colorC.isSelected());
            c = new GridBagConstraints();
            c.gridx = 2;
            c.gridy = y;
            c.insets = new Insets(vgap * 2, hgap * 2, 0, 0);
            p.add(colorC, c);
            
            c = new GridBagConstraints();
            c.gridx = 3;
            c.gridy = y++;
            c.insets = new Insets(vgap * 2, 2, 0, hgap);
            p.add(colorB, c);
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(vgap * 2, hgap, 0, 0);
            p.add(new JLabel(Bundle.ColorCustomizer_Value()), c);
            
            c = new GridBagConstraints();
            c.gridx = 1;
            c.gridy = y++;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.weightx = 1;
            c.weighty = 1;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(vgap * 2, htab, vgap, hgap);
            p.add(new JScrollPane(valueA), c);
            
//            HelpCtx helpCtx = new HelpCtx("PackageColorCustomizer.HelpCtx"); // NOI18N
            String dialogCaption = newFilter ? Bundle.ColorCustomizer_AddCaption() :
                                               Bundle.ColorCustomizer_EditCaption();
            DialogDescriptor dd = new DialogDescriptor(p, dialogCaption, true,
                                  new Object[] { DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION }, 
                                  DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_ALIGN,
                                  null, null);
            if (DialogDisplayer.getDefault().notify(dd) != DialogDescriptor.OK_OPTION) return null;
            
            customized.setName(nameF.getText().trim());
            customized.setValue(valueA.getText().trim());
        
            return customized;
        }
        
        private static Color selectColor(Component comp, Color color) {
            JPanel previewPanel = new JPanel(new BorderLayout());
            previewPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
            
            String[][] previewData = new String[][] { { "org.mypackage", "100 ms" }, // NOI18N
                                                      { "org.mypackage.MyClass", "10 ms" }, // NOI18N
                                                      { "org.mypackage.MyClass.myMethod(boolean, int, String)", "1 ms" } }; // NOI18N
            TableModel previewModel = new DefaultTableModel(previewData, new String[] { "Very long column name", "xxx" }); // NOI18N
            ProfilerTable previewTable = new ProfilerTable(previewModel, false, false, null);
            
            final Color initial = color == null ? previewTable.getForeground() : null;
            final JColorChooser pane = new JColorChooser(color == null ? initial : color);
            
            previewTable.setColumnRenderer(0, new JavaNameRenderer(Icons.getIcon(ProfilerIcons.NODE_LEAF)) {
                protected void setNormalValue(String value) {
                    super.setNormalValue(value);
                    Color color = pane.getColor();
                    if (initial != color) setCustomForeground(color);
                }
            });
            previewTable.setColumnRenderer(1, new LabelRenderer() { { setHorizontalAlignment(TRAILING); } });
            previewTable.setTableHeader(null);
            previewTable.setVisibleRows(3);
            JScrollPane previewScroll = new JScrollPane(previewTable, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            
            previewPanel.add(previewScroll, BorderLayout.CENTER);
            pane.setPreviewPanel(previewPanel);
            
            class Ret implements ActionListener {
                private Color clr;
                public void actionPerformed(ActionEvent e) { clr = pane.getColor(); }
                Color getColor() { return clr; }
            }
            Ret ret = new Ret();

            JDialog dialog = JColorChooser.createDialog(comp, Bundle.ColorCustomizer_ColorCaption(), true, pane, ret, null);

            dialog.addComponentListener(new ComponentAdapter() {
                public void componentHidden(ComponentEvent e) {
                    Window w = (Window)e.getComponent();
                    w.dispose();
                }
            });

            dialog.setVisible(true);
            
            return ret.getColor();
        }
        
    }
    
}
