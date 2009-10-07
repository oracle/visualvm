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

import org.netbeans.lib.profiler.ui.components.ComponentMorpher;
import org.netbeans.lib.profiler.ui.components.VerticalLayout;
import org.netbeans.lib.profiler.ui.components.XPStyleBorder;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class TaskChooser extends JPanel {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    // --- Required interfaces definition ----------------------------------------
    public static interface Item {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public JComponent getLargeComponent();

        public JComponent getSmallComponent();
    }

    public static interface Listener {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void itemCollapsed(Item item);

        public void itemExpanded(Item item);

        public void itemWillCollapse(Item item);

        public void itemWillExpand(Item item);
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String MORPHER_ACCESS_NAME = NbBundle.getMessage(MonitorSettingsBasicPanel.class,
                                                                          "TaskChooser_MorpherAccessName"); // NOI18N
    private static final String MORPHER_ACCESS_DESCR = NbBundle.getMessage(MonitorSettingsBasicPanel.class,
                                                                           "TaskChooser_MorpherAccessDescr"); // NOI18N
                                                                                                              // -----

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ArrayList items = new ArrayList();
    private Color parentColor;
    private ComponentMorpher expandedMorpher = null;
    private Item expandedItem = null;
    private Vector listeners = new Vector();
    private boolean isSwitching = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TaskChooser() {
        this(UIManager.getColor("Panel.background")); // NOI18N
    }

    public TaskChooser(Color parentColor) {
        this.parentColor = parentColor;
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void add(Item item) {
        items.add(item);
        super.add(createItemMorpher(item));
    }

    // --- Disabling original java.awt.Container component adding stuff
    public Component add(Component comp) {
        throw new RuntimeException("Not implemented for this component"); // NOI18N
    }

    public Component add(Component comp, int index) {
        throw new RuntimeException("Not implemented for this component"); // NOI18N
    }

    public void add(Component comp, Object constraints) {
        throw new RuntimeException("Not implemented for this component"); // NOI18N
    }

    public void add(Component comp, Object constraints, int index) {
        throw new RuntimeException("Not implemented for this component"); // NOI18N
    }

    public Component add(String name, Component comp) {
        throw new RuntimeException("Not implemented for this component"); // NOI18N
    }

    public void addItemListener(Listener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void expand(Item item) {
        expandItem(item);
    }

    // Does not fire any events!
    public void expandImmediately(Item item) {
        reset();

        if (item != null) {
            ComponentMorpher morpherToExpand = (ComponentMorpher) getComponent(items.indexOf(item));
            morpherToExpand.expand();
            morpherToExpand.setFocusable(false);
            ((XPStyleBorder) morpherToExpand.getBorder()).setSelected();
            refreshLayout();
            expandedMorpher = morpherToExpand;
            expandedItem = item;
        }
    }

    public void refreshLayout() {
        Component[] components = getComponents();

        for (Component component : components) {
            if (component instanceof ComponentMorpher) {
                ((ComponentMorpher) component).refreshLayout();
            }
        }
    }

    public void removeItemListener(Listener listener) {
        listeners.remove(listener);
    }

    // Does not fire any events!
    public void reset() {
        if (expandedMorpher != null) {
            expandedMorpher.reset();
            ((XPStyleBorder) expandedMorpher.getBorder()).setDefault();
            refreshLayout();
            expandedMorpher = null;
            expandedItem = null;
        }
    }

    private ComponentMorpher createItemMorpher(final Item item) {
        JComponent itemSmallComponent = setupItemSmallComponent(item);
        JComponent itemLargeComponent = setupItemLargeComponent(item);

        final ComponentMorpher morpher = new ComponentMorpher(itemSmallComponent, itemLargeComponent, 6, 10);
        morpher.getAccessibleContext().setAccessibleName(MORPHER_ACCESS_NAME); // NOI18N
        morpher.getAccessibleContext().setAccessibleDescription(MORPHER_ACCESS_DESCR); // NOI18N

        final XPStyleBorder morpherBorder = XPStyleBorder.getDefaultInstance(SelectProfilingTask.BACKGROUND_COLOR_INACTIVE,
                                                                             parentColor);
        morpher.setBorder(morpherBorder);
        morpher.setFocusable(true);

        FocusListener focusListener = new FocusListener() {
            public void focusGained(FocusEvent e) {
                if (morpherBorder.isSelected()) {
                    return;
                }

                morpherBorder.setFocused();
                morpher.repaint();
            }

            public void focusLost(FocusEvent e) {
                if (morpherBorder.isSelected()) {
                    return;
                }

                morpherBorder.setDefault();
                morpher.repaint();
            }
        };

        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (!TaskChooser.this.isEnabled()) {
                    return;
                }

                if ((e.getButton() == MouseEvent.BUTTON1) && !isSwitching && (morpher != expandedMorpher)) {
                    expandItem(item);
                }
            }

            public void mouseEntered(MouseEvent e) {
                if (!TaskChooser.this.isEnabled()) {
                    return;
                }

                if (morpherBorder.isSelected() || morpher.isFocusOwner()) {
                    return;
                }

                morpherBorder.setFocused();

                if (morpher != expandedMorpher) {
                    morpher.repaint();
                }
            }

            public void mouseExited(MouseEvent e) {
                if (!TaskChooser.this.isEnabled()) {
                    return;
                }

                if (morpherBorder.isSelected() || morpher.isFocusOwner()) {
                    return;
                }

                morpherBorder.setDefault();

                if (morpher != expandedMorpher) {
                    morpher.repaint();
                }
            }
        };

        KeyListener keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_SPACE) && !isSwitching && (morpher != expandedMorpher)) {
                    expandItem(item);
                }
            }
        };

        morpher.addFocusListener(focusListener);
        morpher.addMouseListener(mouseListener);
        morpher.addKeyListener(keyListener);
        itemSmallComponent.getComponent(0).addMouseListener(mouseListener); // itemSmallComponent's glassPane

        return morpher;
    }

    private void expandItem(final Item item) {
        final ComponentMorpher morpherToExpand = (ComponentMorpher) getComponent(items.indexOf(item));
        final ComponentMorpher morpherToCollapse = expandedMorpher;

        ((XPStyleBorder) morpherToExpand.getBorder()).setSelected();

        if (morpherToCollapse != null) {
            ((XPStyleBorder) morpherToCollapse.getBorder()).setDefault();
        }

        RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    isSwitching = true;

                    morpherToExpand.setupMorphing();

                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    fireItemWillExpand(item);
                                }
                            });
                    } catch (Exception e) {
                    }

                    if (morpherToCollapse != null) {
                        morpherToCollapse.setupMorphing();
                    }

                    if (expandedItem != null) {
                        fireItemWillCollapse(expandedItem);
                    }

                    while (morpherToExpand.isMorphing() || ((morpherToCollapse != null) && morpherToCollapse.isMorphing())) {
                        try {
                            SwingUtilities.invokeLater(new Runnable() { // originally was invokeAndWait but this could behave better on slower machines
                                    public void run() {
                                        morpherToExpand.morphingStep();

                                        if (morpherToCollapse != null) {
                                            morpherToCollapse.morphingStep();
                                        }
                                    }
                                });
                            Thread.sleep(morpherToExpand.getMorphingDelay());
                        } catch (Exception e) {
                        }
                    }

                    final Item collapsedItem = expandedItem;
                    expandedItem = item;
                    expandedMorpher = morpherToExpand;

                    isSwitching = false;

                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    if (morpherToCollapse != null) {
                                        morpherToCollapse.setFocusable(true);
                                    }

                                    fireItemExpanded(item);
                                    fireItemCollapsed(collapsedItem);
                                    morpherToExpand.setFocusable(false);
                                }
                            });
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }, 0, Thread.MAX_PRIORITY - 1);
    }

    private void fireItemCollapsed(Item item) {
        for (int i = 0; i < listeners.size(); i++) {
            ((Listener) listeners.get(i)).itemCollapsed(item);
        }
    }

    private void fireItemExpanded(Item item) {
        for (int i = 0; i < listeners.size(); i++) {
            ((Listener) listeners.get(i)).itemExpanded(item);
        }
    }

    private void fireItemWillCollapse(Item item) {
        for (int i = 0; i < listeners.size(); i++) {
            ((Listener) listeners.get(i)).itemWillCollapse(item);
        }
    }

    private void fireItemWillExpand(Item item) {
        for (int i = 0; i < listeners.size(); i++) {
            ((Listener) listeners.get(i)).itemWillExpand(item);
        }
    }

    private void initComponents() {
        setOpaque(false);
        setLayout(new VerticalLayout());
        setFocusable(false);
    }

    private JComponent setupItemLargeComponent(final Item item) {
        return item.getLargeComponent();
    }

    private JComponent setupItemSmallComponent(final Item item) {
        final JComponent itemSmallComponent = item.getSmallComponent();

        JPanel itemSmallComponentContainer = new JPanel();
        itemSmallComponentContainer.setLayout(new OverlayLayout(itemSmallComponentContainer));

        JPanel itemSmallComponentGlassPane = new JPanel();
        itemSmallComponentGlassPane.setOpaque(false);
        itemSmallComponentContainer.add(itemSmallComponentGlassPane);
        itemSmallComponentContainer.add(itemSmallComponent);
        itemSmallComponentContainer.setOpaque(true);
        itemSmallComponentContainer.setBackground(itemSmallComponent.getBackground());

        return itemSmallComponentContainer;
    }
}
