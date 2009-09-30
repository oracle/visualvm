/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.profiler.ui.stp;

import org.netbeans.lib.profiler.common.ProfilingSettings;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.modules.profiler.ui.HyperlinkTextArea;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class TaskPresenter implements TaskChooser.Item {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface Context {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void refreshLayout();

        public void selectSettings(ProfilingSettings settings);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ConfigurationsContainer extends JPanel implements Scrollable {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            // Scroll almost one screen
            Container parent = getParent();

            if ((parent == null) || !(parent instanceof JViewport)) {
                return 50;
            }

            return (int) (((JViewport) parent).getHeight() * 0.95f);
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }
    }

    private static class LargeTaskPresenter extends JPanel {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        private static final int MAX_VISIBLE_ROWS = 4;
        private static final int CONFIGURATION_HEIGHT = new HTMLTextArea("H").getPreferredSize().height; // NOI18N

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private ArrayList<ProfilingSettings> profilingSettings = new ArrayList();
        private Context context;
        private JMenuItem deleteItem;
        private JMenuItem duplicateItem;
        private JMenuItem moveDownItem;
        private JMenuItem moveUpItem;
        private JMenuItem renameItem;
        private JPanel configurationsContainer;
        private JPopupMenu presetPopupMenu;
        private JScrollPane configurationsScrollPane;
        private TPHyperlinkTextArea createCustom;
        private int activeSettingsIndex = -1; // valid only when popup menu is opened!
        private int selectedSettingsIndex = 0;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public LargeTaskPresenter(String title, Icon icon, Context context) {
            this.context = context;
            initComponents(title, icon);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setProfilingSettings(ArrayList<ProfilingSettings> profilingSettings) {
            resetProfilingSettings();
            this.profilingSettings.addAll(profilingSettings);
            updateItems();
        }

        public ArrayList<ProfilingSettings> getProfilingSettings() {
            return profilingSettings;
        }

        public ProfilingSettings getSelectedProfilingSettings() {
            if (selectedSettingsIndex == -1) {
                return null;
            } else {
                return profilingSettings.get(selectedSettingsIndex);
            }
        }

        public void resetProfilingSettings() {
            selectedSettingsIndex = 0;
            profilingSettings.clear();
        }

        public void selectSettings(ProfilingSettings settings) {
            selectSettings(settings, true);
        }

        private TPHyperlinkTextArea getPresenter(ProfilingSettings settings) {
            if ((settings == null) || (profilingSettings.indexOf(settings) == -1)) {
                return null;
            }

            return (TPHyperlinkTextArea) configurationsContainer.getComponent((profilingSettings.indexOf(settings) * 2) + 1);
        }

        private void createCustomSettings() {
            SelectProfilingTask.getDefault().synchronizeCurrentSettings();

            ProfilingSettings newSettings = ProfilingSettingsManager.getDefault()
                                                                    .createNewSettings(profilingSettings.get(0).getProfilingType(),
                                                                                       profilingSettings.toArray(new ProfilingSettings[profilingSettings
                                                                                                                                       .size()]));

            if (newSettings != null) {
                ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                profilingSettings.add(newSettings);
                updateItems();
                selectedSettingsIndex = profilingSettings.indexOf(selectedSettings);
                context.selectSettings(newSettings);
            }
        }

        private void initComponents(String title, Icon icon) {
            GridBagConstraints gridBagConstraints;

            setLayout(new GridBagLayout());
            setOpaque(true);
            setBackground(SelectProfilingTask.BACKGROUND_COLOR);

            JLabel label = new JLabel();
            label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 3));
            label.setForeground(new Color(198, 129, 0));
            label.setIcon(icon);
            label.setText(title);
            label.setIconTextGap(10);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 12, 5, 12);
            add(label, gridBagConstraints);

            JSeparator separator = Utils.createHorizontalSeparator();
            separator.setBackground(getBackground());
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.weightx = 1;
            gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(0, 10, 7, 10);
            add(separator, gridBagConstraints);

            configurationsContainer = new ConfigurationsContainer();
            configurationsContainer.setLayout(new GridBagLayout());
            configurationsContainer.setOpaque(true);
            configurationsContainer.setBackground(SelectProfilingTask.BACKGROUND_COLOR);
            configurationsContainer.addHierarchyListener(new HierarchyListener() { // Workaround to focus selected settings when panel is displayed
                    public void hierarchyChanged(HierarchyEvent e) {
                        if (((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) && configurationsContainer.isShowing()) {
                            if (selectedSettingsIndex != -1) {
                                ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                                TPHyperlinkTextArea selectedPresenter = getPresenter(selectedSettings);
                                selectedPresenter.requestFocusInWindow();
                            }
                        }
                    }
                });

            configurationsScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                    public Dimension getPreferredSize() {
                        return new Dimension(super.getPreferredSize().width,
                                             Math.min(configurationsContainer.getPreferredSize().height,
                                                      CONFIGURATION_HEIGHT * MAX_VISIBLE_ROWS));
                    }
                };
            configurationsScrollPane.setBorder(BorderFactory.createEmptyBorder());
            configurationsScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
            configurationsScrollPane.setViewportView(configurationsContainer);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.weighty = 1;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 0, 10);
            add(configurationsScrollPane, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 10, 0);
            add(Utils.createFillerPanel(), gridBagConstraints);

            createCustom = new TPHyperlinkTextArea(CREATE_CUSTOM_STRING);
            createCustom.setToolTipText(CREATE_CUSTOM_TOOLTIP);
            createCustom.setName(CREATE_CUSTOM_STRING);
            createCustom.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            mouseExited(e);
                            createCustomSettings();
                        }
                    }
                });
            createCustom.addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                            createCustomSettings();
                        }
                    }
                });

            renameItem = new JMenuItem(RENAME_ITEM_TEXT);
            renameItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ProfilingSettings activeSettings = profilingSettings.get(activeSettingsIndex);
                        ProfilingSettings newSettings = ProfilingSettingsManager.getDefault()
                                                                                .renameSettings(activeSettings,
                                                                                                profilingSettings.toArray(new ProfilingSettings[profilingSettings
                                                                                                                                                .size()]));

                        if (newSettings != null) {
                            ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                            profilingSettings.add(profilingSettings.indexOf(activeSettings), newSettings);
                            profilingSettings.remove(activeSettings);
                            updateItems();

                            if (activeSettingsIndex != selectedSettingsIndex) {
                                selectSettings(selectedSettings, false);
                            } else {
                                context.selectSettings(newSettings);
                            }
                        }
                    }
                });

            duplicateItem = new JMenuItem(DUPLICATE_ITEM_TEXT);
            duplicateItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        SelectProfilingTask.getDefault().synchronizeCurrentSettings();

                        ProfilingSettings activeSettings = profilingSettings.get(activeSettingsIndex);
                        ProfilingSettings newSettings = ProfilingSettingsManager.getDefault()
                                                                                .createDuplicateSettings(activeSettings,
                                                                                                         profilingSettings.toArray(new ProfilingSettings[profilingSettings
                                                                                                                                                         .size()]));

                        if (newSettings != null) {
                            ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                            profilingSettings.add(newSettings);
                            updateItems();
                            selectedSettingsIndex = profilingSettings.indexOf(selectedSettings);
                            context.selectSettings(newSettings);
                        }
                    }
                });

            deleteItem = new JMenuItem(DELETE_ITEM_TEXT);
            deleteItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        // TODO: show confirmation?
                        ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                        profilingSettings.remove(activeSettingsIndex);
                        updateItems();
                        selectedSettingsIndex = profilingSettings.indexOf(selectedSettings);

                        if (selectedSettingsIndex == -1) {
                            context.selectSettings(profilingSettings.get(Math.max(activeSettingsIndex - 1, 0)));
                        } else {
                            selectSettings(selectedSettings, false);
                        }
                    }
                });

            moveUpItem = new JMenuItem(MOVE_UP_ITEM_TEXT);
            moveUpItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                        ProfilingSettings activeSettings = profilingSettings.get(activeSettingsIndex);
                        profilingSettings.remove(activeSettings);
                        profilingSettings.add(activeSettingsIndex - 1, activeSettings);
                        updateItems();
                        selectSettings(selectedSettings, false);
                    }
                });

            moveDownItem = new JMenuItem(MOVE_DOWN_ITEM_TEXT);
            moveDownItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                        ProfilingSettings activeSettings = profilingSettings.get(activeSettingsIndex);
                        profilingSettings.remove(activeSettings);
                        profilingSettings.add(activeSettingsIndex + 1, activeSettings);
                        updateItems();
                        selectSettings(selectedSettings, false);
                    }
                });

            presetPopupMenu = new JPopupMenu();
            presetPopupMenu.add(renameItem);
            presetPopupMenu.add(duplicateItem);
            presetPopupMenu.add(deleteItem);
            presetPopupMenu.addSeparator();
            presetPopupMenu.add(moveUpItem);
            presetPopupMenu.add(moveDownItem);
        }

        private void selectSettings(ProfilingSettings settings, final boolean scrollToVisible) {
            if ((selectedSettingsIndex != -1) && (profilingSettings.get(selectedSettingsIndex) != settings)) {
                ProfilingSettings selectedSettings = profilingSettings.get(selectedSettingsIndex);
                TPHyperlinkTextArea selectedPresenter = getPresenter(selectedSettings);
                selectedPresenter.setSelected(false);
            }

            selectedSettingsIndex = profilingSettings.indexOf(settings);
            setCursor(Cursor.getDefaultCursor());

            final TPHyperlinkTextArea presenter = getPresenter(settings);
            presenter.setSelected(true);
            SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        presenter.requestFocusInWindow();

                        if (scrollToVisible) {
                            configurationsContainer.scrollRectToVisible(presenter.getBounds());
                        }
                    }
                });
        }

        private void showPopupMenu(ProfilingSettings settings, int x, int y) {
            activeSettingsIndex = profilingSettings.indexOf(settings);
            renameItem.setEnabled(!settings.isPreset());
            deleteItem.setEnabled(!settings.isPreset());
            moveUpItem.setEnabled(activeSettingsIndex > 0);
            moveDownItem.setEnabled(activeSettingsIndex < (profilingSettings.size() - 1));
            presetPopupMenu.show(getPresenter(settings), x, y);
        }

        private void updateItems() {
            configurationsContainer.removeAll();

            GridBagConstraints gridBagConstraints;

            for (int i = 0; i < profilingSettings.size(); i++) {
                final ProfilingSettings settings = profilingSettings.get(i);
                final String settingsName = settings.getSettingsName();

                JLabel dot = new JLabel("\u00B7"); // NOI18N
                dot.setOpaque(false);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy = i;
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new Insets(3, 25, 0, 2);
                configurationsContainer.add(dot, gridBagConstraints);

                final TPHyperlinkTextArea presenter = new TPHyperlinkTextArea(settingsName);
                presenter.getAccessibleContext().setAccessibleName(settingsName);
                gridBagConstraints = new GridBagConstraints();
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = i;
                gridBagConstraints.fill = GridBagConstraints.BOTH;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                gridBagConstraints.insets = new Insets(0, 0, 0, 4);
                configurationsContainer.add(presenter, gridBagConstraints);

                presenter.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (e.getButton() == MouseEvent.BUTTON1) {
                                if ((selectedSettingsIndex != -1) && (settings != profilingSettings.get(selectedSettingsIndex))) {
                                    context.selectSettings(settings);
                                }
                            } else if (e.getButton() == MouseEvent.BUTTON3) {
                                showPopupMenu(settings, e.getX(), e.getY());
                            }
                        }
                    });
                presenter.addKeyListener(new KeyAdapter() {
                        public void keyPressed(KeyEvent e) {
                            if ((e.getKeyCode() == KeyEvent.VK_SPACE) && (selectedSettingsIndex != -1)
                                    && (settings != profilingSettings.get(selectedSettingsIndex))) {
                                context.selectSettings(settings);
                            } else if ((e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU)
                                           || ((e.getKeyCode() == KeyEvent.VK_F10) && (e.getModifiers() == InputEvent.SHIFT_MASK))) {
                                Dimension size = presenter.getSize();
                                showPopupMenu(settings, size.width / 2, size.height / 2);
                            }
                        }
                    });
            }

            int lastItemIndex = profilingSettings.size();

            JLabel dot = new JLabel("\u00B7"); // NOI18N
            dot.setOpaque(false);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = lastItemIndex;
            gridBagConstraints.weighty = 1;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(3, 25, 0, 2);
            configurationsContainer.add(dot, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = lastItemIndex;
            gridBagConstraints.weightx = 1;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 0, 4);
            configurationsContainer.add(createCustom, gridBagConstraints);

            context.refreshLayout();
        }
    }

    // --- Private implementation ------------------------------------------------
    private static class SmallTaskPresenter extends JPanel {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SmallTaskPresenter(String title, Icon icon) {
            initComponents(title, icon);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        private void initComponents(String title, Icon icon) {
            setLayout(new BorderLayout());
            setOpaque(true);
            setBackground(SelectProfilingTask.BACKGROUND_COLOR_INACTIVE);

            JLabel label = new JLabel();
            label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() + 3));
            label.setForeground(new Color(80, 80, 80));
            label.setIcon(icon);
            label.setText(title);
            label.setIconTextGap(10);
            label.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

            add(label, BorderLayout.NORTH);
        }
    }

    private static class TPHyperlinkTextArea extends HyperlinkTextArea {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TPHyperlinkTextArea(String text) {
            super(text);
            setForeground(SelectProfilingTask.DARKLINK_COLOR_INACTIVE);
            addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) {
                        JComponent parent = (JComponent) getParent();

                        if (parent != null) {
                            parent.scrollRectToVisible(getBounds());
                        }
                    }
                });

            // Allows the default dialog button to work correctly
            KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); // NOI18N
            getInputMap().put(ks, ""); // NOI18N

        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        protected Color getHighlightColor() {
            return SelectProfilingTask.DARKLINK_COLOR;
        }

        protected String getHighlightText(String originalText) {
            return "<u>" + originalText + "</u>";
        } // NOI18N

        protected Color getNormalColor() {
            return SelectProfilingTask.DARKLINK_COLOR_INACTIVE;
        }

        protected String getNormalText(String originalText) {
            return originalText;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CREATE_CUSTOM_STRING = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_CreateCustomString"); // NOI18N
    private static final String RENAME_ITEM_TEXT = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_RenameItemText"); // NOI18N
    private static final String DUPLICATE_ITEM_TEXT = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_DuplicateItemText"); // NOI18N
    private static final String DELETE_ITEM_TEXT = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_DeleteItemText"); // NOI18N
    private static final String MOVE_UP_ITEM_TEXT = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_MoveUpItemText"); // NOI18N
    private static final String MOVE_DOWN_ITEM_TEXT = NbBundle.getMessage(TaskPresenter.class, "TaskPresenter_MoveDownItemText"); // NOI18N
    private static final String CREATE_CUSTOM_TOOLTIP = NbBundle.getMessage(TaskPresenter.class,
                                                                            "TaskPresenter_CreateCustomToolTip"); // NOI18N
                                                                                                                  // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Context context;
    private Icon icon;
    private LargeTaskPresenter largeTaskPresenter;
    private SmallTaskPresenter smallTaskPresenter;

    // --- Instance variables declaration ----------------------------------------
    private String name;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Public interface ------------------------------------------------------
    public TaskPresenter(String name, Icon icon, Context context) {
        this.name = name;
        this.icon = icon;
        this.context = context;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public JComponent getLargeComponent() {
        if (largeTaskPresenter == null) {
            largeTaskPresenter = new LargeTaskPresenter(name, icon, context);
        }

        return largeTaskPresenter;
    }

    public void setProfilingSettings(ArrayList<ProfilingSettings> profilingSettingsArr) {
        largeTaskPresenter.setProfilingSettings(profilingSettingsArr);
    }

    public ArrayList<ProfilingSettings> getProfilingSettings() {
        return largeTaskPresenter.getProfilingSettings();
    }

    public ProfilingSettings getSelectedProfilingSettings() {
        return largeTaskPresenter.getSelectedProfilingSettings();
    }

    public JComponent getSmallComponent() {
        if (smallTaskPresenter == null) {
            smallTaskPresenter = new SmallTaskPresenter(name, icon);
        }

        return smallTaskPresenter;
    }

    public void resetProfilingSettings() {
        largeTaskPresenter.resetProfilingSettings();
    }

    public void selectProfilingSettings(ProfilingSettings profilingSettings) {
        largeTaskPresenter.selectSettings(profilingSettings);
    }
}
