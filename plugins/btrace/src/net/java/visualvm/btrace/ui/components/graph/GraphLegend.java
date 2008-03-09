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
package net.java.visualvm.btrace.ui.components.graph;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GraphLegend extends JPanel implements ListDataListener {

    public static final String PROPERTY_COLOR = GraphLegend.class.getName() + "#color"; // NOI18N
    public static final String PROPERTY_LEGEND = GraphLegend.class.getName() + "#legend"; // NOI18N
    
    private JList legendList;
    private DefaultListModel legendListModel;
    final private List<LegendItem> availableItems = new ArrayList<LegendItem>();
    private int itemSize;
    private Color hiliteColor = Color.GRAY;

    public GraphLegend() {
        super(new BorderLayout(), true);

        itemSize = 12;

        legendList = new JList();
        legendListModel = new DefaultListModel();
        legendListModel.addListDataListener(this);
        legendList.setLayout(new BoxLayout(legendList, BoxLayout.PAGE_AXIS));
        legendList.setModel(legendListModel);

        legendList.setCellRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                LegendItem item = (LegendItem) value;
                JLabel label = new JLabel(item.getItemDescription(), createIcon(item), JLabel.LEFT);
                if (isSelected) {
                    label.setOpaque(true);
                    label.setBackground(hiliteColor);
                }
                return label;
            }
        });

        legendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        legendList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(final MouseEvent e) {
                int index = legendList.locationToIndex(e.getPoint());
                if (e.getButton() == 1) {
                    if (e.getClickCount() == 2) {
                        if (index > -1 && index < legendListModel.getSize()) {
                            legendList.setSelectedIndex(index);
                            LegendItem item = (LegendItem) legendListModel.get(index);
                            Color newColor = JColorChooser.showDialog(legendList, "Customize color", item.getItemColor());
                            firePropertyChange(PROPERTY_COLOR, item.getItemColor(), newColor);
                            if (newColor != null) {
                                item.setItemColor(newColor);
                            }
                        }
                    }
                } else if (e.getButton() == 3) {
                    if (index > -1 && index < legendListModel.getSize()) {
                        legendList.setSelectedIndex(index);
                        final LegendItem item = (LegendItem) legendListModel.get(index);
                        JPopupMenu menu = new JPopupMenu();
                        JMenu addMenu = new JMenu("Add");
                        
                        boolean isAddAvailable = false;
                        for(LegendItem availItem : availableItems) {
                            if (availItem.isVisible()) continue;
                            isAddAvailable = true;
                            final LegendItem addingItem = availItem;
                            addMenu.add(new JMenuItem(new AbstractAction(addingItem.getItemDescription()) {
                                public void actionPerformed(ActionEvent e) {
                                    addingItem.setVisible(enabled);
                                    legendListModel.addElement(addingItem);
                                }
                            }));
                        }
                        if (isAddAvailable) {
                            menu.add(addMenu);
                        }

                        if (legendListModel.getSize() > 1) {

                            menu.add(new AbstractAction("Remove") {

                                public void actionPerformed(ActionEvent ev) {
                                    item.setVisible(false);
                                    legendListModel.removeElement(item);
                                    firePropertyChange(PROPERTY_COLOR, null, null);
                                }
                            });
                        }
                        menu.show(legendList, e.getX(), e.getY());
                    }
                }
            }
        });
        add(legendList, BorderLayout.CENTER);
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        if (legendList != null) {
            legendList.setBackground(bg);
        }
    }

    public void setItems(List<LegendItem> items) {
        availableItems.clear();
        availableItems.addAll(items);

        for (LegendItem item : items) {
            if (item.isVisible()) {
                legendListModel.addElement(item);
            }
        }
        refreshUI();
        firePropertyChange(PROPERTY_COLOR, null, null);
    }

    public List<LegendItem> getVisibleItems() {
        List<LegendItem> list = new ArrayList<LegendItem>();
        for (Enumeration enm = legendListModel.elements(); enm.hasMoreElements();) {
            list.add((LegendItem) enm.nextElement());
        }
        return list;
    }

    public int getItemSize() {
        return itemSize;
    }

    public void setItemSize(int itemSize) {
        this.itemSize = itemSize;
    }

    public Color getHiliteColor() {
        return hiliteColor;
    }

    public void setHiliteColor(Color hiliteColor) {
        this.hiliteColor = hiliteColor;
    }

    public void contentsChanged(ListDataEvent e) {
//        refreshUI();
//        firePropertyChange(PROPERTY_LEGEND, null, null);
    }

    public void intervalAdded(ListDataEvent e) {
        firePropertyChange(PROPERTY_LEGEND, null, null);
    }

    public void intervalRemoved(ListDataEvent e) {
        firePropertyChange(PROPERTY_LEGEND, null, null);
    }

    private void refreshUI() {
        validate();
        repaint();
    }

    private Icon createIcon(LegendItem item) {
        BufferedImage img = new BufferedImage(itemSize, itemSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = img.createGraphics();
        gr.setColor(item.getItemColor());
        gr.fillRect(0, 0, itemSize, itemSize);

        return new ImageIcon(img);
    }
}
