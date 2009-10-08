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

package org.netbeans.lib.profiler.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.MenuComponent;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;


/**
 *
 * @author Jiri Sedlacek
 */
public class JTitledPanel extends JPanel {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class ThinBevelBorder extends BevelBorder {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ThinBevelBorder(int bevelType, Color highlight, Color shadow) {
            super(bevelType, highlight.brighter(), highlight, shadow, shadow.brighter());
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 1;

            return insets;
        }

        protected void paintLoweredBevel(Component c, Graphics g, int x, int y, int width, int height) {
            if (!c.isEnabled()) {
                return;
            }

            Color oldColor = g.getColor();
            int h = height;
            int w = width;

            g.translate(x, y);

            g.setColor(getShadowOuterColor(c));
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 1, 0);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(1, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, 1, w - 1, h - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }

        protected void paintRaisedBevel(Component c, Graphics g, int x, int y, int width, int height) {
            if (!c.isEnabled()) {
                return;
            }

            Color oldColor = g.getColor();
            int h = height;
            int w = width;

            g.translate(x, y);

            g.setColor(getHighlightInnerColor(c));
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 1, 0);

            g.setColor(getShadowOuterColor(c));
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, 0, w - 1, h - 2);

            g.translate(-x, -y);
            g.setColor(oldColor);
        }
    }

    private class DoubleClickListener extends MouseAdapter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseClicked(MouseEvent e) {
            if ((e.getModifiers() == InputEvent.BUTTON1_MASK) && (e.getClickCount() == 2)) {
                if (isMaximized()) {
                    restore();
                } else {
                    maximize();
                }
            }

            ;
        }
    }

    private class ImageIconButton extends JButton implements MouseListener {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Border emptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        private Border loweredBorder = new ThinBevelBorder(BevelBorder.LOWERED, Color.WHITE, Color.GRAY);
        private Border raisedBorder = new ThinBevelBorder(BevelBorder.RAISED, Color.WHITE, Color.GRAY);
        private boolean focused = false;
        private boolean pressed = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public ImageIconButton(ImageIcon icon) {
            super();

            GrayFilter enabledFilter = new GrayFilter(true, 35);
            ImageProducer prod = new FilteredImageSource(icon.getImage().getSource(), enabledFilter);
            Icon grayIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));
            GrayFilter disabledFilter = new GrayFilter(true, 60);
            prod = new FilteredImageSource(icon.getImage().getSource(), disabledFilter);

            Icon disabledIcon = new ImageIcon(Toolkit.getDefaultToolkit().createImage(prod));

            setIcon(grayIcon);
            setRolloverIcon(icon);
            setPressedIcon(icon);
            setDisabledIcon(disabledIcon);
            setIconTextGap(0);
            setBorder(emptyBorder);
            setFocusable(false);
            setContentAreaFilled(false);

            setPreferredSize(new Dimension(icon.getIconWidth() + 8, icon.getIconHeight() + 8));

            addMouseListener(this);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            focused = true;

            if (pressed) {
                setBorder(loweredBorder);
            } else {
                setBorder(raisedBorder);
            }
        }

        public void mouseExited(MouseEvent e) {
            focused = false;
            setBorder(emptyBorder);
        }

        public void mousePressed(MouseEvent e) {
            pressed = true;
            setBorder(loweredBorder);
        }

        public void mouseReleased(MouseEvent e) {
            pressed = false;

            if (focused) {
                setBorder(raisedBorder);
            } else {
                setBorder(emptyBorder);
            }
        }
    }

    // --- Presenter -------------------------------------------------------------
    private class Presenter extends JToggleButton {
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public Presenter() {
            super();

            if (JTitledPanel.this.getIcon() == null) {
                setText(JTitledPanel.this.getTitle());
                setToolTipText(JTitledPanel.this.getTitle());
            } else {
                setIcon(JTitledPanel.this.getIcon());
                setToolTipText(JTitledPanel.this.getTitle());
            }

            setSelected(JTitledPanel.this.isVisible());
            addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JTitledPanel.this.setVisible(isSelected());
                    }
                });
            JTitledPanel.this.addComponentListener(new ComponentAdapter() {
                    public void componentShown(ComponentEvent e) {
                        setSelected(true);
                    }

                    public void componentHidden(ComponentEvent e) {
                        setSelected(false);
                    }
                });
            addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if ("enabled".equals(evt.getPropertyName())) {
                            JTitledPanel.this.setButtonsEnabled(isEnabled()); // NOI18N
                        }
                    }
                });
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final ImageIcon closePanelIcon = new ImageIcon(JTitledPanel.class.getResource("/org/netbeans/lib/profiler/ui/resources/closePanel.png")); // NOI18N
    private static final ImageIcon maximizePanelIcon = new ImageIcon(JTitledPanel.class.getResource("/org/netbeans/lib/profiler/ui/resources/maximizePanel.png")); // NOI18N
    private static final ImageIcon restorePanelIcon = new ImageIcon(JTitledPanel.class.getResource("/org/netbeans/lib/profiler/ui/resources/restorePanel.png")); // NOI18N
    private static final ImageIcon minimizePanelIcon = new ImageIcon(JTitledPanel.class.getResource("/org/netbeans/lib/profiler/ui/resources/minimizePanel.png")); // NOI18N
    public static final int STATE_CLOSED = 1000;
    public static final int STATE_RESTORED = 1001;
    public static final int STATE_MAXIMIZED = 1002;
    public static final int STATE_MINIMIZED = 1003;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private AbstractButton presenter;
    private Icon icon;
    private JButton closePanelButton;
    private JButton maximizePanelButton;
    private JButton minimizePanelButton;
    private JButton restorePanelButton;
    private JPanel contentPanel;
    private JPanel titlePanel;
    private String title;
    private Vector actionListeners;
    private boolean showButtons;
    private int state;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JTitledPanel(String title) {
        this(title, null);
    }

    public JTitledPanel(String title, Icon icon) {
        this(title, icon, false);
    }

    public JTitledPanel(String title, Icon icon, boolean showButtons) {
        super();
        actionListeners = new Vector();
        this.title = title;
        this.icon = icon;
        this.showButtons = showButtons;
        initComponents();
        restore();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setButtonsEnabled(boolean enabled) {
        closePanelButton.setEnabled(enabled);
        maximizePanelButton.setEnabled(enabled);
        restorePanelButton.setEnabled(enabled);
        minimizePanelButton.setEnabled(enabled);
    }

    public boolean isClosed() {
        return getState() == STATE_CLOSED;
    }

    public JPanel getContentPanel() {
        if (contentPanel == null) {
            contentPanel = new JPanel();
        }

        return contentPanel;
    }

    public Icon getIcon() {
        return icon;
    }

    public void setLayout(LayoutManager mgr) {
        getContentPanel().setLayout(mgr);
    }

    public LayoutManager getLayout() {
        return getContentPanel().getLayout();
    }

    public boolean isMaximized() {
        return getState() == STATE_MAXIMIZED;
    }

    public boolean isMinimized() {
        return getState() == STATE_MINIMIZED;
    }

    public Dimension getMinimumSize() {
        return new Dimension(0, titlePanel.getPreferredSize().height);
    }

    public AbstractButton getPresenter() {
        if (presenter == null) {
            presenter = new Presenter();
        }

        return presenter;
    }

    public boolean isRestored() {
        return getState() == STATE_RESTORED;
    }

    public int getState() {
        if (!isVisible()) {
            state = STATE_CLOSED;
        }

        if (isVisible() && (state == STATE_CLOSED)) {
            state = STATE_RESTORED;
        }

        return state;
    }

    public String getTitle() {
        return title;
    }

    public Component add(Component comp) {
        return getContentPanel().add(comp);
    }

    public Component add(Component comp, int index) {
        return getContentPanel().add(comp, index);
    }

    public void add(Component comp, Object constraints) {
        getContentPanel().add(comp, constraints);
    }

    public void add(Component comp, Object constraints, int index) {
        getContentPanel().add(comp, constraints, index);
    }

    public Component add(String name, Component comp) {
        return getContentPanel().add(name, comp);
    }

    public void add(PopupMenu popup) {
        getContentPanel().add(popup);
    }

    public void addActionListener(ActionListener listener) {
        if (!actionListeners.contains(listener)) {
            actionListeners.add(listener);
        }
    }

    public boolean areButtonsEnabled() {
        return closePanelButton.isEnabled();
    }

    public void close() {
        if (isClosed()) {
            return;
        }

        setVisible(false);
        state = STATE_CLOSED;
        fireActionPerformed();
    }

    public void maximize() {
        if (isMaximized()) {
            return;
        }

        maximizePanelButton.setVisible(false);
        restorePanelButton.setVisible(true);
        minimizePanelButton.setVisible(true);
        contentPanel.setVisible(true);
        state = STATE_MAXIMIZED;
        fireActionPerformed();
    }

    public void minimize() {
        if (isMinimized()) {
            return;
        }

        maximizePanelButton.setVisible(true);
        restorePanelButton.setVisible(true);
        minimizePanelButton.setVisible(false);
        contentPanel.setVisible(false);
        state = STATE_MINIMIZED;
        fireActionPerformed();
    }

    public void remove(Component component) {
        getContentPanel().remove(component);
    }

    public void remove(MenuComponent component) {
        getContentPanel().remove(component);
    }

    public void remove(int index) {
        getContentPanel().remove(index);
    }

    public void removeActionListener(ActionListener listener) {
        actionListeners.remove(listener);
    }

    public void removeAll() {
        getContentPanel().removeAll();
    }

    public void restore() {
        if (isRestored()) {
            return;
        }

        maximizePanelButton.setVisible(true);
        restorePanelButton.setVisible(false);
        minimizePanelButton.setVisible(true);
        contentPanel.setVisible(true);
        state = STATE_RESTORED;
        fireActionPerformed();
    }

    protected Color getTitleBorderColor() {
        return UIManager.getLookAndFeel().getID().equals("Metal") ? // NOI18N
                          UIManager.getColor("Button.darkShadow") : // NOI18N
                          UIManager.getColor("Button.shadow"); // NOI18N
    }

    private void fireActionPerformed() {
        for (int i = 0; i < actionListeners.size(); i++) {
            ((ActionListener) actionListeners.get(i)).actionPerformed(new ActionEvent(this, getState(), ""));
        }
    }

    private void initComponents() {
        DoubleClickListener dblClickListener = new DoubleClickListener();
        titlePanel = new JPanel();
        titlePanel.addMouseListener(dblClickListener);

        super.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel(title) {
            public Dimension getMinimumSize() {
                return new Dimension(0, super.getMinimumSize().height);
            }
        };

        if (icon != null) {
            titleLabel.setIcon(icon);
        }

        titleLabel.setForeground(UIManager.getColor("ToolTip.foreground")); // NOI18N
        titleLabel.setFont(UIManager.getFont("ToolTip.font")); // NOI18N
        titleLabel.setOpaque(false);
        titleLabel.addMouseListener(dblClickListener);

        closePanelButton = new ImageIconButton(closePanelIcon);
        closePanelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    close();
                }
                ;
            });

        maximizePanelButton = new ImageIconButton(maximizePanelIcon);
        maximizePanelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    maximize();
                }
                ;
            });

        restorePanelButton = new ImageIconButton(restorePanelIcon);
        restorePanelButton.setVisible(false);
        restorePanelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    restore();
                }
                ;
            });

        minimizePanelButton = new ImageIconButton(minimizePanelIcon);
        minimizePanelButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    minimize();
                }
                ;
            });

        JPanel buttonsContainer = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        buttonsContainer.setOpaque(false);
        //    buttonsContainer.add(minimizePanelButton);
        //    buttonsContainer.add(restorePanelButton);
        //    buttonsContainer.add(maximizePanelButton);
        buttonsContainer.add(closePanelButton);

        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.setOpaque(false);
        buttonsPanel.add(buttonsContainer, new GridBagConstraints());

        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.WEST);

        if (showButtons) {
            titlePanel.add(buttonsPanel, BorderLayout.EAST);
        }

        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                             BorderFactory.createLineBorder(getTitleBorderColor()),
                             BorderFactory.createEmptyBorder(2, 5, 2, 2)));
        titlePanel.setOpaque(true);
        //    titlePanel.setBackground(UIManager.getColor("ToolTip.background"));
        titlePanel.setBackground(new Color(245, 245, 245));

        super.add(titlePanel, BorderLayout.NORTH);
        super.add(contentPanel, BorderLayout.CENTER);
    }
}
