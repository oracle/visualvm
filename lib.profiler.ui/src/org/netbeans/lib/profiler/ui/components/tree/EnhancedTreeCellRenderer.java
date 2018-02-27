/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.lib.profiler.ui.components.tree;

import org.netbeans.lib.profiler.ui.UIConstants;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import org.netbeans.lib.profiler.ui.UIUtils;


public class EnhancedTreeCellRenderer extends JPanel implements TreeCellRendererPersistent {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected Color backgroundNonSelectionColor;
    protected Color backgroundSelectionColor = UIConstants.TABLE_SELECTION_BACKGROUND_COLOR;
    protected Color borderSelectionColor;
    protected Color textNonSelectionColor;

    // Colors
    protected Color textSelectionColor = UIConstants.TABLE_SELECTION_FOREGROUND_COLOR;
    protected boolean hasFocus;
    protected boolean selected;

    // Icons
    private transient Icon closedIcon = UIManager.getIcon("Tree.closedIcon"); // NOI18N
    private transient Icon leafIcon = UIManager.getIcon("Tree.leafIcon"); // NOI18N
    private transient Icon openIcon = UIManager.getIcon("Tree.openIcon"); // NOI18N

    // subcomponents
    private JLabel label1;
    private JLabel label2;
    private JLabel label3;
    private JTree tree;

    private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Returns a new instance of DefaultTreeCellRenderer.  Alignment is
     * set to left aligned. Icons and text color are determined from the
     * UIManager.
     */
    public EnhancedTreeCellRenderer() {
        super(null);
        setOpaque(false);

        label1 = new InternalLabel();
        label2 = new InternalLabel();
        label3 = new InternalLabel();

        label2.setFont(label1.getFont().deriveFont(Font.BOLD));

        add(label1);
        add(label2);
        add(label3);

        label1.setHorizontalAlignment(JLabel.LEFT);

        setLeafIcon(UIManager.getIcon("Tree.leafIcon")); // NOI18N
        setClosedIcon(UIManager.getIcon("Tree.closedIcon")); // NOI18N
        setOpenIcon(UIManager.getIcon("Tree.openIcon")); // NOI18N

        setTextSelectionColor(UIManager.getColor("Tree.selectionForeground")); // NOI18N
        setTextNonSelectionColor(UIManager.getColor("Tree.textForeground")); // NOI18N
        setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground")); // NOI18N
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground")); // NOI18N
        setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor")); // NOI18N
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void doLayout() {
        Dimension size  = getSize();
	Dimension size1 = label1.getPreferredSize();
        Dimension size2 = label2.getPreferredSize();
        Dimension size3 = label3.getPreferredSize();

        size.height = Math.max(size.height, size1.height);
        size.height = Math.max(size.height, size2.height);
        size.height = Math.max(size.height, size3.height);

        int x = 0;

        if ("".equals(label1.getText())) size1.width += label1.getIconTextGap(); // NOI18N
        label1.setBounds(x, 0, size1.width, size.height);
        x += size1.width;

        label2.setBounds(x, 0, size2.width, size.height);
        x += size2.width;

        label3.setBounds(x, 0, size3.width, size.height);
    }

    public Dimension getPreferredSize() {
	Dimension size = label1.getPreferredSize();
        if ("".equals(label1.getText())) size.width += label1.getIconTextGap(); // NOI18N
        size.width += label2.getPreferredSize().width;
        size.width += label3.getPreferredSize().width;
        return size;
    }

    public Dimension getMaximumSize() {
	return getPreferredSize();
    }

    public Dimension getMinimumSize() {
	return getPreferredSize();
    }

    /**
     * Subclassed to map <code>ColorUIResource</code>s to null. If
     * <code>color</code> is null, or a <code>ColorUIResource</code>, this
     * has the effect of letting the background color of the JTree show
     * through. On the other hand, if <code>color</code> is non-null, and not
     * a <code>ColorUIResource</code>, the background becomes
     * <code>color</code>.
     */
    public void setBackground(Color color) {
        if (color instanceof ColorUIResource) {
            color = null;
        }

        super.setBackground(color);
    }

    /**
     * Sets the background color to be used for non selected nodes.
     */
    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    /**
     * Returns the background color to be used for non selected nodes.
     */
    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    /**
     * Sets the color to use for the background if node is selected.
     */
    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }

    /**
     * Returns the color to use for the background if node is selected.
     */
    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    /**
     * Sets the color to use for the border.
     */
    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    /**
     * Returns the color the border is drawn.
     */
    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are not expanded.
     */
    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are not
     * expanded.
     */
    public Icon getClosedIcon() {
        return closedIcon;
    }

    /**
     * Subclassed to map <code>FontUIResource</code>s to null. If
     * <code>font</code> is null, or a <code>FontUIResource</code>, this
     * has the effect of letting the font of the JTree show
     * through. On the other hand, if <code>font</code> is non-null, and not
     * a <code>FontUIResource</code>, the font becomes <code>font</code>.
     */
    public void setFont(Font font) {
        if (font instanceof FontUIResource) {
            font = null;
        }

        super.setFont(font);
    }

    /**
     * Gets the font of this component.
     *
     * @return this component's font; if a font has not been set
     *         for this component, the font of its parent is returned
     */
    public Font getFont() {
        Font font = super.getFont();

        if ((font == null) && (tree != null)) {
            // Strive to return a non-null value, otherwise the html support
            // will typically pick up the wrong font in certain situations.
            font = tree.getFont();
        }

        return font;
    }

    /**
     * Sets the icon used to represent leaf nodes.
     */
    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    /**
     * Returns the icon used to represent leaf nodes.
     */
    public Icon getLeafIcon() {
        return leafIcon;
    }

    /**
     * Sets the icon used to represent non-leaf nodes that are expanded.
     */
    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    /**
     * Returns the icon used to represent non-leaf nodes that are expanded.
     */
    public Icon getOpenIcon() {
        return openIcon;
    }

    /**
     * Sets the color the text is drawn with when the node isn't selected.
     */
    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node isn't selected.
     */
    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    /**
     * Sets the color the text is drawn with when the node is selected.
     */
    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    /**
     * Returns the color the text is drawn with when the node is selected.
     */
    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    /**
     * Configures the renderer based on the passed in components.
     * The value is set from messaging the tree with
     * <code>convertValueToText</code>, which ultimately invokes
     * <code>toString</code> on <code>value</code>.
     * The foreground color is set based on the selection and the icon
     * is set based on on leaf and expanded.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {

        String stringValue = value != null ? value.toString() : ""; // NOI18N

        this.tree = tree;
        this.hasFocus = hasFocus;

        label1.setText(getLabel1Text(value, stringValue));
        label2.setText(getLabel2Text(value, stringValue));
        label3.setText(getLabel3Text(value, stringValue));

        if (sel) {
            label1.setForeground(getTextSelectionColor());
            label2.setForeground(getTextSelectionColor());

            Color c = getTextSelectionColor();
            label3.setForeground(UIUtils.getDisabledForeground(c));
        } else {
            label1.setForeground(getTextNonSelectionColor());
            label2.setForeground(getTextNonSelectionColor());

            Color c = getTextNonSelectionColor();
            label3.setForeground(UIUtils.getDisabledForeground(c));
        }

        if (!tree.isEnabled()) {
            label1.setEnabled(false);
            label2.setEnabled(false);
            label3.setEnabled(false);

            if (leaf) {
                label1.setDisabledIcon(getLeafIcon(value));
            } else if (expanded) {
                label1.setDisabledIcon(getOpenIcon(value));
            } else {
                label1.setDisabledIcon(getClosedIcon(value));
            }
        } else {
            label1.setEnabled(true);
            label2.setEnabled(true);
            label3.setEnabled(true);

            if (leaf) {
                label1.setIcon(getLeafIcon(value));
            } else if (expanded) {
                label1.setIcon(getOpenIcon(value));
            } else {
                label1.setIcon(getClosedIcon(value));
            }
        }

        label1.setComponentOrientation(tree.getComponentOrientation()); // TODO [ian]: what does this mean wrt label2, label3

        selected = sel;

        return this;
    }

    public Component getTreeCellRendererComponentPersistent(JTree tree, Object value, boolean sel, boolean expanded,
                                                            boolean leaf, int row, boolean hasFocus) {
        EnhancedTreeCellRenderer renderer = new EnhancedTreeCellRenderer();
        renderer.setLeafIcon(leafIcon);
        renderer.setClosedIcon(closedIcon);
        renderer.setOpenIcon(openIcon);

        return renderer.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }

    /**
     * Paints the value. The background is filled based on selected.
     */
    public void paint(Graphics g) {
        Color bColor;

        if (selected) {
            bColor = getBackgroundSelectionColor();
        } else {
            bColor = getBackgroundNonSelectionColor();

            if (bColor == null) {
                bColor = getBackground();
            }
        }

        if (bColor != null) {
            g.setColor(bColor);
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        if (hasFocus) {
            Color bsColor = getBorderSelectionColor();

            if (bsColor != null) {
                g.setColor(bsColor);
                g.drawRect(0, 0, getWidth(), getHeight());
            }
        }

        super.paint(g);

    }

    protected Icon getClosedIcon(Object value) {
        return getClosedIcon();
    }

    /**
     * @param   node  The node value
     * @param   value Entire tree node text
     * @return  First part to display in plain font
     */
    protected String getLabel1Text(Object node, String value) {
        return value;
    }

    /**
     * @param   node  The node value
     * @param   value Entire tree node text
     * @return  Middle part to display in bold font
     */
    protected String getLabel2Text(Object node, String value) {
        return ""; // NOI18N
    }

    /**
     * @param   node  The node value
     * @param   value Entire tree node text
     * @return  Lat part to display in gray font
     */
    protected String getLabel3Text(Object node, String value) {
        return ""; // NOI18N
    }

    protected Icon getLeafIcon(Object value) {
        return getLeafIcon();
    }

    protected Icon getOpenIcon(Object value) {
        return getOpenIcon();
    }

    // --- Performance tweaks

    // Overridden for performance reasons.
    public Insets getInsets() { return ZERO_INSETS; }
    
    // Overridden for performance reasons.
    public void validate() { if (!isValid()) doLayout(); }
    
    // Overridden for performance reasons.
    public void revalidate() {}

    // Overridden for performance reasons.
    public void repaint(long tm, int x, int y, int width, int height) {}

    // Overridden for performance reasons.
    public void repaint(Rectangle r) {}

    // Overridden for performance reasons.
    public void repaint() {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

    // Overridden for performance reasons.
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    /// Overridden for performance reasons.
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}

    // ---


    /**
     * Tweaked JLabel optimized for performance - use only as component renderer!
     */
    private static class InternalLabel extends JLabel {

        private FontMetrics fontMetrics;
        private String text;
        private Color foreground;
        private boolean enabled;


        // Overridden for performance reasons.
        public void setText(String text) {
            this.text = text;
        }

        // Overridden for performance reasons.
        public String getText() {
            return text;
        }

        // Overridden for performance reasons.
        public void setForeground(Color foreground) {
            this.foreground = foreground;
        }

        // Overridden for performance reasons.
        public Color getForeground() {
            return foreground;
        }

        // Overridden for performance reasons.
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        // Overridden for performance reasons.
        public boolean isEnabled() {
            return enabled;
        }

        // Overridden for performance reasons.
        public FontMetrics getFontMetrics(Font font) {
            if (fontMetrics == null) fontMetrics = super.getFontMetrics(font);
            return fontMetrics;
        }

        // Overridden for performance reasons.
        public void setFont(Font font) {
            fontMetrics = null;
            super.setFont(font);
        }

        // Overridden for performance reasons.
        public void validate() {}

        // Overridden for performance reasons.
        public void revalidate() {}

        // Overridden for performance reasons.
        public void repaint(long tm, int x, int y, int width, int height) {}

        // Overridden for performance reasons.
        public void repaint(Rectangle r) {}

        // Overridden for performance reasons.
        public void repaint() {}

        // Overridden for performance reasons.
        public void setDisplayedMnemonic(int key) {}

        // Overridden for performance reasons.
        public void setDisplayedMnemonic(char aChar) {}

        // Overridden for performance reasons.
        public void setDisplayedMnemonicIndex(int index) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, char oldValue, char newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, short oldValue, short newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, int oldValue, int newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, long oldValue, long newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, float oldValue, float newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, double oldValue, double newValue) {}

        // Overridden for performance reasons.
        public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

        // Overridden for performance reasons.
        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}

        // Overridden for performance reasons.
        public void paint(Graphics g) {
            Graphics componentGraphics = getComponentGraphics(g);
            Graphics co = (componentGraphics == null) ? null :
                          componentGraphics.create();
            try {
                paintComponent(co);
            } finally {
                co.dispose();
            }
        }

    }
}
