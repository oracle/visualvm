/*
 * Copyright (c) 1997, 2022, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.cpu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import org.graalvm.visualvm.lib.ui.UIUtils;
import org.graalvm.visualvm.lib.ui.charts.ChartModelListener;
import org.graalvm.visualvm.lib.ui.charts.PieChart;
import org.graalvm.visualvm.lib.ui.charts.PieChartModel;
import org.graalvm.visualvm.lib.ui.components.HTMLTextArea;
import org.graalvm.visualvm.lib.ui.components.SnippetPanel;


/**
 *
 * @author Jiri Sedlacek
 */
public class StatisticsPanel extends JPanel {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Listener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void itemClicked(int itemIndex);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private class ChartItemPresenter {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        public JLabel valueLabel = new JLabel(""); // NOI18N
        public JPanel filler = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
        public KeyboardAwareLabel nameLabel; // NOI18N
        private ColorIcon colorIcon = new ColorIcon(UIUtils.getProfilerResultsBackground());
        private PieChartModel model;
        private int index;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ChartItemPresenter(PieChart pieChart, int index) {
            this.model = pieChart.getModel();
            this.index = index;

            initComponents();
            refresh();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void refresh() {
            double percentage = model.getItemValueRel(index);

            if (percentage == 0d) {
                nameLabel.setVisible(false);
                valueLabel.setVisible(false);
            } else {
                colorIcon.setColor(model.getItemColor(index));
                nameLabel.setIcon(colorIcon);
                nameLabel.setText(model.getItemName(index));
                valueLabel.setText(getRelValue(percentage));

                nameLabel.setVisible(true);
                valueLabel.setVisible(true);
            }
        }

        private String getRelValue(double value) {
            int percent = (int) Math.floor(value * 100);
            int permille = (int) Math.round(value * 1000) - (10 * percent);

            return percent + "." + permille + "%"; // NOI18N
        }

        private void initComponents() {
            filler.setOpaque(false);
            nameLabel = new KeyboardAwareLabel(model.isSelectable(index), 
                            new Runnable() {
                                public void run() {
                                    for (Listener l : listeners) {
                                    l.itemClicked(index);
                                }
                            }
                        });

            valueLabel.setOpaque(false);
            valueLabel.setHorizontalAlignment(SwingConstants.TRAILING);
        }
    }

    private class ChartPanel extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ChartPanel(PieChart pieChart) {
            initComponents(pieChart);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void initComponents(final PieChart pieChart) {
            pieChart.setBackground(UIUtils.getProfilerResultsBackground());

            pieChart.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int clickedItem = pieChart.getItemIndexAt(e.getX(), e.getY());

                        for (Listener l : listeners) {
                            l.itemClicked(clickedItem);
                        }
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        pieChart.resetFocusedItem();
                    }
                });
            pieChart.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int focusedItem = pieChart.getItemIndexAt(e.getX(), e.getY());

                        if ((focusedItem != -1) && pieChart.getModel().isSelectable(focusedItem)) {
                            pieChart.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        } else {
                            pieChart.setCursor(Cursor.getDefaultCursor());
                        }

                        pieChart.setFocusedItem(focusedItem);
                    }
                });

            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
            setPreferredSize(new Dimension(240, 220));
            setMinimumSize(new Dimension(50, 220));

            setLayout(new BorderLayout());
            add(pieChart, BorderLayout.CENTER);
        }
    }

    private static class ColorIcon implements Icon {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final int ICON_SIZE = 9;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        protected Color color;
        protected int height;
        protected int width;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        ColorIcon(Color color) {
            this(ICON_SIZE, ICON_SIZE, color);
        }

        ColorIcon(int width, int height, Color color) {
            this.width = width;
            this.height = height;
            setColor(color);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setColor(Color color) {
            this.color = color;
        }

        public Color getColor() {
            return color;
        }

        public int getIconHeight() {
            return height;
        }

        public int getIconWidth() {
            return width;
        }

        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width - 1, height - 1);
        }
    }

    private static class Container extends JPanel implements Scrollable {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        Container() {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(UIUtils.getProfilerResultsBackground());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(250, 500);
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 50;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 10;
        }
    }

    private class KeyboardAwareLabel extends JLabel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String originalText;
        private boolean isMouseOver;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        KeyboardAwareLabel(final boolean selectable, final Runnable actionPerformer) {
            super();

            setOpaque(false);
            setFocusable(true);

            addFocusListener(new FocusListener() {
                    public void focusGained(FocusEvent e) {
                        updateText();
                    }

                    public void focusLost(FocusEvent e) {
                        updateText();
                    }
                });

            addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (selectable) {
                            requestFocusInWindow();
                            actionPerformer.run();
                        }
                    }

                    public void mouseEntered(MouseEvent e) {
                        if (selectable) {
                            isMouseOver = true;
                            updateText();
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }

                    public void mouseExited(MouseEvent e) {
                        if (selectable) {
                            isMouseOver = false;
                            updateText();
                            setCursor(Cursor.getDefaultCursor());
                        }
                    }
                });

            addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (selectable) {
                            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                                actionPerformer.run();
                            }
                        }
                    }
                });
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setText(String value) {
            this.originalText = value;
            updateText();
        }

        private void updateText() {
            setForeground(isFocusOwner() ? Color.RED : UIManager.getColor("Label.foreground")); // NOI18N

            if (isMouseOver) {
                super.setText("<html><nobr><u>" + originalText + ":" + "</u></nobr></html>"); // NOI18N
            } else {
                super.setText("<html><nobr>" + originalText + ":" + "</nobr></html>"); // NOI18N
            }
        }
    }

    private class NavPanel extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Color lineColor;
        private Color backgroundColor;
        private Color focusedBackgroundColor;
        private final MouseListener focusGrabber = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        };


        //~ Constructors ---------------------------------------------------------------------------------------------------------

        NavPanel(HTMLTextArea navArea) {
            initColors();
            initComponents(navArea);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void initComponents(HTMLTextArea navArea) {
            setBackground(backgroundColor);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, lineColor),
                                                         BorderFactory.createEmptyBorder(4, 0, 4, 0)));

            navArea.setHighlighter(null);
            navArea.setShowPopup(false);
            navArea.setBorder(BorderFactory.createEmptyBorder());
            navArea.setOpaque(false);
            navArea.setFocusable(false);

            GridBagConstraints constraints;
            setLayout(new GridBagLayout());

            JLabel scopeLabel = new JLabel(SCOPE_LABEL_TEXT);
            scopeLabel.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD)); //NOI18N
            scopeLabel.setOpaque(false);

            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(1, 5, 1, 5);
            add(scopeLabel, constraints);

            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets(1, 0, 1, 5);
            add(navArea, constraints);

            setFocusable(true);
            addFocusListener(new FocusListener() {
                    public void focusGained(FocusEvent e) {
                        setBackground(focusedBackgroundColor);
                    }

                    public void focusLost(FocusEvent e) {
                        setBackground(backgroundColor);
                    }
                });

            addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            navigationBackPerformer.run();
                        }
                    }
                });

            addMouseListener(focusGrabber);
            scopeLabel.addMouseListener(focusGrabber);
            navArea.addMouseListener(focusGrabber);
        }
        
        private void initColors() {
            Color systemBackgroundColor = UIUtils.getProfilerResultsBackground();

            int backgroundRed = systemBackgroundColor.getRed(); 
            int backgroundGreen = systemBackgroundColor.getGreen();
            int backgroundBlue = systemBackgroundColor.getBlue();
            boolean inverseColors = backgroundRed < 41 || backgroundGreen < 32 || backgroundBlue < 25;

            if (inverseColors) {
                lineColor = UIUtils.getSafeColor(backgroundRed + 41, backgroundGreen + 32, backgroundBlue + 8);
                backgroundColor = UIUtils.getSafeColor(backgroundRed + 7, backgroundGreen + 7, backgroundBlue + 7);
                focusedBackgroundColor = UIUtils.getSafeColor(backgroundRed + 25, backgroundGreen + 25, backgroundBlue + 25);
            } else {
                lineColor = UIUtils.getSafeColor(backgroundRed - 41 /*214*/, backgroundGreen - 32 /*223*/, backgroundBlue - 8 /*247*/);
                backgroundColor = UIUtils.getSafeColor(backgroundRed - 7 /*248*/, backgroundGreen - 7 /*248*/, backgroundBlue - 7 /*248*/);
                focusedBackgroundColor = UIUtils.getSafeColor(backgroundRed - 25 /*230*/, backgroundGreen - 25 /*230*/, backgroundBlue - 25 /*230*/);
            }
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.ui.cpu.Bundle"); // NOI18N
    private static final String SCOPE_LABEL_TEXT = messages.getString("StatisticsPanel_ScopeLabelText"); // NOI18N
                                                                                                         // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SnippetPanel.Padding snippetsBottomFiller;
    private ArrayList itemPresenters = new ArrayList();
    private ArrayList snippets = new ArrayList();

    // --- Declarations ----------------------------------------------------------
    private JPanel container;
    private JPanel noSnippetsBottomFiller;
    private NavPanel navPanel;
    private PieChart pieChart;
    private Runnable navigationBackPerformer;
    private Collection<Listener> listeners = new CopyOnWriteArraySet<>();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public StatisticsPanel(HTMLTextArea navArea, PieChart pieChart, Runnable navigationBackPerformer) {
        this.pieChart = pieChart;
        this.navigationBackPerformer = navigationBackPerformer;

        initComponents(navArea, pieChart);

        pieChart.getModel().addChartModelListener(new ChartModelListener() {
                public void chartDataChanged() {
                    updateItemPresenters();
                }
            });
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    // --- Listener stuff --------------------------------------------------------
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    // --- Public interface ------------------------------------------------------
    public void addSnippet(JComponent component) {
        SnippetPanel snippet = new SnippetPanel(component.getName(), component);
        snippet.setOpaque(false);
        snippets.add(snippet);
        updateSnippets();
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void removeSnippet(JComponent component) {
        for (int i = 0; i < snippets.size(); i++) {
            if (((SnippetPanel) snippets.get(i)).getContent() == component) {
                snippets.remove(i);

                break;
            }
        }

        updateSnippets();
    }

    private void initComponents(HTMLTextArea navArea, PieChart pieChart) {
        noSnippetsBottomFiller = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
        noSnippetsBottomFiller.setOpaque(false);
        snippetsBottomFiller = new SnippetPanel.Padding();

        container = new Container();

        GridBagConstraints constraints;
        container.setLayout(new GridBagLayout());

        navPanel = new NavPanel(navArea);
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, 0);
        container.add(navPanel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(5, 0, 5, 0);
        container.add(new ChartPanel(pieChart), constraints);

        updateItemPresenters();

        JScrollPane contentsScrollPane = new JScrollPane(container, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        contentsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentsScrollPane.getViewport().setOpaque(true);
        contentsScrollPane.getViewport().setBackground(UIUtils.getProfilerResultsBackground());

        setLayout(new BorderLayout());
        add(contentsScrollPane, BorderLayout.CENTER);
    }

    private void refreshItemPresenters() {
        for (int i = 0; i < itemPresenters.size(); i++) {
            ((ChartItemPresenter) itemPresenters.get(i)).refresh();
        }
    }

    // --- Private implementation ------------------------------------------------
    private void repopulateItemPresenters() {
        for (int i = 0; i < itemPresenters.size(); i++) {
            ChartItemPresenter itemPresenter = (ChartItemPresenter) itemPresenters.get(i);
            container.remove(itemPresenter.nameLabel);
            container.remove(itemPresenter.valueLabel);
            container.remove(itemPresenter.filler);
        }

        itemPresenters.clear();

        GridBagConstraints constraints;
        ChartItemPresenter itemPresenter;

        //    JPanel filler;
        for (int i = 0; i < pieChart.getModel().getItemCount(); i++) {
            itemPresenter = new ChartItemPresenter(pieChart, i);
            itemPresenters.add(itemPresenter);

            int bottomInset = (i == (pieChart.getModel().getItemCount() - 1)) ? 16 : 6;

            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2 + i;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(0, 15, bottomInset, 8);
            container.add(itemPresenter.nameLabel, constraints);

            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 2 + i;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.NONE;
            constraints.insets = new Insets(0, 22, bottomInset, 0);
            container.add(itemPresenter.valueLabel, constraints);

            constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.gridy = 2 + i;
            constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, 0, bottomInset, 8);
            container.add(itemPresenter.filler, constraints);
        }

        container.revalidate();
    }

    private void updateItemPresenters() {
        if (pieChart.getModel().getItemCount() != itemPresenters.size()) {
            repopulateItemPresenters();
            updateSnippets();
        } else {
            refreshItemPresenters();
        }

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        if ((focusOwner != null) && !focusOwner.isShowing()) {
            navPanel.requestFocusInWindow();
        }
    }

    private void updateSnippets() {
        for (int i = 0; i < snippets.size(); i++) {
            container.remove((JComponent) snippets.get(i));
        }

        container.remove(snippetsBottomFiller);
        container.remove(noSnippetsBottomFiller);

        GridBagConstraints constraints;

        //    SnippetPanel snippet;
        for (int i = 0; i < snippets.size(); i++) {
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2 + itemPresenters.size() + i + 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, 0, 0, 0);
            container.add((JComponent) snippets.get(i), constraints);
        }

        if (snippets.isEmpty()) {
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2 + itemPresenters.size() + 1;
            constraints.weighty = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets(0, 0, 0, 0);
            container.add(noSnippetsBottomFiller, constraints);
        } else {
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 2 + itemPresenters.size() + snippets.size() + 1;
            constraints.weighty = 1;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.insets = new Insets(0, 0, 0, 0);
            container.add(snippetsBottomFiller, constraints);
        }

        container.revalidate();
    }
}
