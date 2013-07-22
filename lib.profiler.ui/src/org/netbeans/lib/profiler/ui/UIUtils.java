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

package org.netbeans.lib.profiler.ui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.plaf.basic.BasicButtonListener;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


/** Various UI utilities used in the JFluid UI
 *
 * @author Ian Formanek
 * @author Jiri Sedlacek
 */
public final class UIUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Used to mark explicit expand/collapse on JTree which shouldn't be handled by automatic expander
    public static final String PROP_EXPANSION_TRANSACTION = "expansion_transaction"; // NOI18N
    public static Dimension DIMENSION_SMALLEST = new Dimension(0, 0);

    private static final Logger LOGGER = Logger.getLogger(UIUtils.class.getName());
    public static final float ALTERNATE_ROW_DARKER_FACTOR = 0.96f;
    private static final int MAX_TREE_AUTOEXPAND_LINES = 50;
    private static boolean toolTipValuesInitialized = false;
    private static Color unfocusedSelBg;
    private static Color unfocusedSelFg;
    private static Color disabledLineColor;

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public static void decorateProfilerPanel(JPanel panel) {
        Color panelBackground = UIManager.getColor(UIConstants.PROFILER_PANELS_BACKGROUND);
        if (panelBackground != null) {
            panel.setOpaque(true);
            panel.setBackground(panelBackground);
        }
    }
    
    public static JPanel createFillerPanel() {
        JPanel fillerPanel = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING)) {
            public Dimension getPreferredSize() {
                return DIMENSION_SMALLEST;
            }
        };

        fillerPanel.setOpaque(false);

        return fillerPanel;
    }

    public static JSeparator createHorizontalSeparator() {
        JSeparator horizontalSeparator = new JSeparator() {
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        };

        return horizontalSeparator;
    }
    
    public static JSeparator createHorizontalLine(Color background) {
        final boolean customPaint = isNimbus();
        JSeparator separator = new JSeparator() {
            public Dimension getMaximumSize() {
                return new Dimension(super.getMaximumSize().width, 1);
            }
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, 1);
            }
            public void paint(Graphics g) {
                if (customPaint) {
                    g.setColor(getDisabledLineColor());
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    super.paint(g);
                }
            }
        };
        separator.setBackground(background);
        return separator;
    }
    
    /** Determines if current L&F is AquaLookAndFeel */
    public static boolean isAquaLookAndFeel() {
        // is current L&F some kind of AquaLookAndFeel?
        return UIManager.getLookAndFeel().getID().equals("Aqua"); //NOI18N
    }

    public static Color getDarker(Color c) {
        if (c.equals(Color.WHITE)) {
            return new Color(244, 244, 244);
        }

        return getSafeColor((int) (c.getRed() * ALTERNATE_ROW_DARKER_FACTOR), (int) (c.getGreen() * ALTERNATE_ROW_DARKER_FACTOR),
                            (int) (c.getBlue() * ALTERNATE_ROW_DARKER_FACTOR));
    }

    public static Color getDarkerLine(Color c, float alternateRowDarkerFactor) {
        return getSafeColor((int) (c.getRed() * alternateRowDarkerFactor), (int) (c.getGreen() * alternateRowDarkerFactor),
                            (int) (c.getBlue() * alternateRowDarkerFactor));
    }
    
    public static Color getDisabledForeground(Color c) {
        if (isNimbusLookAndFeel()) return UIManager.getColor("nimbusDisabledText").darker(); //NOI18N
        else if (isMetalLookAndFeel()) return UIManager.getColor("Label.disabledForeground"); //NOI18N
        else if (Color.BLACK.equals(c)) return Color.GRAY;
        else return c.brighter();
    }

    public static int getDefaultRowHeight() {
        return new JLabel("X").getPreferredSize().height + 2; //NOI18N
    }

    /** Determines if current L&F is GTKLookAndFeel */
    public static boolean isGTKLookAndFeel() {
        // is current L&F some kind of GTKLookAndFeel?
        return UIManager.getLookAndFeel().getID().equals("GTK"); //NOI18N
    }
    
    /** Determines if current L&F is Nimbus */
    public static boolean isNimbusLookAndFeel() {
        // is current L&F Nimbus?
        return UIManager.getLookAndFeel().getID().equals("Nimbus"); //NOI18N
    }
    
    /** Determines if current L&F is GTK using Nimbus theme */
    public static boolean isNimbusGTKTheme() {
        // is current L&F GTK using Nimbus theme?
        return isGTKLookAndFeel() && "nimbus".equals(Toolkit.getDefaultToolkit().getDesktopProperty("gnome.Net/ThemeName")); //NOI18N
    }
    
    /** Determines if current L&F is Nimbus or GTK with Nimbus theme*/
    public static boolean isNimbus() {
        // is current L&F Nimbus or GTK with Nimbus theme?
        return isNimbusLookAndFeel() || isNimbusGTKTheme();
    }

    /** Determines if current L&F is MetalLookAndFeel */
    public static boolean isMetalLookAndFeel() {
        // is current L&F some kind of MetalLookAndFeel?
        return UIManager.getLookAndFeel().getID().equals("Metal"); //NOI18N
    }

    // Returns next enabled tab of JTabbedPane
    public static int getNextSubTabIndex(JTabbedPane tabs, int tabIndex) {
        int nextTabIndex = tabIndex;

        for (int i = 0; i < tabs.getComponentCount(); i++) {
            nextTabIndex++;

            if (nextTabIndex == tabs.getComponentCount()) {
                nextTabIndex = 0;
            }

            if (tabs.isEnabledAt(nextTabIndex)) {
                break;
            }
        }

        return nextTabIndex;
    }

    public static Window getParentWindow(Component comp) {
        while ((comp != null) && !(comp instanceof Window)) {
            comp = comp.getParent();
        }

        return (Window) comp;
    }

    // Returns previous enabled tab of JTabbedPane
    public static int getPreviousSubTabIndex(JTabbedPane tabs, int tabIndex) {
        int previousTabIndex = tabIndex;

        for (int i = 0; i < tabs.getComponentCount(); i++) {
            previousTabIndex--;

            if (previousTabIndex < 0) {
                previousTabIndex = tabs.getComponentCount() - 1;
            }

            if (tabs.isEnabledAt(previousTabIndex)) {
                break;
            }
        }

        return previousTabIndex;
    }

    public static Color getSafeColor(int red, int green, int blue) {
        red = Math.max(red, 0);
        red = Math.min(red, 255);
        green = Math.max(green, 0);
        green = Math.min(green, 255);
        blue = Math.max(blue, 0);
        blue = Math.min(blue, 255);

        return new Color(red, green, blue);
    }

    // Copied from org.openide.awt.HtmlLabelUI
    /** Get the system-wide unfocused selection background color */
    public static Color getUnfocusedSelectionBackground() {
        if (unfocusedSelBg == null) {
            //allow theme/ui custom definition
            unfocusedSelBg = UIManager.getColor("nb.explorer.unfocusedSelBg"); //NOI18N

            if (unfocusedSelBg == null) {
                //try to get standard shadow color
                unfocusedSelBg = UIManager.getColor("controlShadow"); //NOI18N

                if (unfocusedSelBg == null) {
                    //Okay, the look and feel doesn't suport it, punt
                    unfocusedSelBg = Color.lightGray;
                }

                //Lighten it a bit because disabled text will use controlShadow/
                //gray
                if (!Color.WHITE.equals(unfocusedSelBg.brighter())) {
                    unfocusedSelBg = unfocusedSelBg.brighter();
                }
            }
        }

        return unfocusedSelBg;
    }

    // Copied from org.openide.awt.HtmlLabelUI
    /** Get the system-wide unfocused selection foreground color */
    public static Color getUnfocusedSelectionForeground() {
        if (unfocusedSelFg == null) {
            //allow theme/ui custom definition
            unfocusedSelFg = UIManager.getColor("nb.explorer.unfocusedSelFg"); //NOI18N

            if (unfocusedSelFg == null) {
                //try to get standard shadow color
                unfocusedSelFg = UIManager.getColor("textText"); //NOI18N

                if (unfocusedSelFg == null) {
                    //Okay, the look and feel doesn't suport it, punt
                    unfocusedSelFg = Color.BLACK;
                }
            }
        }

        return unfocusedSelFg;
    }

    
    private static Color profilerResultsBackground;
    
    private static Color getGTKProfilerResultsBackground() {
        int[] pixels = new int[1];
        pixels[0] = -1;
        
        // Prepare textarea to grab the color from
        JTextArea textArea = new JTextArea();
        textArea.setSize(new Dimension(10, 10));
        textArea.doLayout();
        
        // Print the textarea to an image
        Image image = new BufferedImage(textArea.getSize().width, textArea.getSize().height, BufferedImage.TYPE_INT_RGB);
        textArea.printAll(image.getGraphics());
        
        // Grab appropriate pixels to get the color
        PixelGrabber pixelGrabber = new PixelGrabber(image, 5, 5, 1, 1, pixels, 0, 1);
        try {
            pixelGrabber.grabPixels();
            if (pixels[0] == -1) return Color.WHITE; // System background not customized
        } catch (InterruptedException e) {
            return getNonGTKProfilerResultsBackground();
        }
        
        return pixels[0] != -1 ? new Color(pixels[0]) : getNonGTKProfilerResultsBackground();
    }
    
    private static Color getNonGTKProfilerResultsBackground() {
        return UIManager.getColor("Table.background"); // NOI18N
    }
    
    public static Color getProfilerResultsBackground() {
        if (profilerResultsBackground == null) {
            if (isGTKLookAndFeel() || isNimbusLookAndFeel()) {
                profilerResultsBackground = getGTKProfilerResultsBackground();
            } else {
                profilerResultsBackground = getNonGTKProfilerResultsBackground();
            }
            if (profilerResultsBackground == null) profilerResultsBackground = Color.WHITE;
        }
        
        return profilerResultsBackground;
    }

    /** Determines if current L&F is Windows Classic LookAndFeel */
    public static boolean isWindowsClassicLookAndFeel() {
        if (!isWindowsLookAndFeel()) {
            return false;
        }

        return (!isWindowsXPLookAndFeel());
    }

    /** Determines if current L&F is WindowsLookAndFeel */
    public static boolean isWindowsLookAndFeel() {
        // is current L&F some kind of WindowsLookAndFeel?
        return UIManager.getLookAndFeel().getID().equals("Windows"); //NOI18N
    }

    /** Determines if current L&F is Windows XP LookAndFeel */
    public static boolean isWindowsXPLookAndFeel() {
        if (!isWindowsLookAndFeel()) {
            return false;
        }

        // is XP theme active in the underlying OS?
        boolean xpThemeActiveOS = Boolean.TRUE.equals(Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive")); //NOI18N
                                                                                                                                  // is XP theme disabled by the application?

        boolean xpThemeDisabled = (System.getProperty("swing.noxp") != null); // NOI18N

        return ((xpThemeActiveOS) && (!xpThemeDisabled));
    }

    /** Checks give TreePath for the last node, and if it ends with a node with just one child,
     * it keeps expanding further.
     * Current implementation expands through the first child that is not leaf. To more correctly
     * fulfil expected semantics in case maxChildToExpand is > 1, it should expand all paths through
     * all children.
     *
     * @param tree
     * @param path
     * @param maxChildToExpand
     */
    public static void autoExpand(JTree tree, TreePath path, int maxLines, int maxChildToExpand, boolean dontExpandToLeafs) {
        TreeModel model = tree.getModel();
        Object node = path.getLastPathComponent();
        TreePath newPath = path;

        int currentLines = 0;

        while (currentLines++ < maxLines &&
                !model.isLeaf(node) &&
                (model.getChildCount(node) > 0) &&
                (model.getChildCount(node) <= maxChildToExpand)) {
            for (int i = 0; i < model.getChildCount(node); i++) {
                node = tree.getModel().getChild(node, i);

                if (!model.isLeaf(node)) {
                    if (dontExpandToLeafs && hasOnlyLeafs(tree, node)) {
                        break;
                    }

                    newPath = newPath.pathByAddingChild(node); // if the leaf is added the path will not expand

                    break; // from for
                }
            }
        }

        tree.expandPath(newPath);
    }

    /** Checks if the root of the provided tree has only one child, and if so,
     * it autoexpands it.
     *
     * @param tree The tree whose root should be autoexpanded
     */
    public static void autoExpandRoot(JTree tree) {
        autoExpandRoot(tree, 1);
    }

    /** Checks if the root of the provided tree has only one child, and if so,
     * it autoexpands it.
     *
     * @param tree The tree whose root should be autoexpanded
     */
    public static void autoExpandRoot(JTree tree, int maxChildToExpand) {
        Object root = tree.getModel().getRoot();

        if (root == null) {
            return;
        }

        TreePath rootPath = new TreePath(root);
        autoExpand(tree, rootPath, MAX_TREE_AUTOEXPAND_LINES, maxChildToExpand, false);
    }

    public static long[] copyArray(long[] array) {
        if (array == null) {
            return new long[0];
        }

        if (array.length == 0) {
            return new long[0];
        } else {
            long[] ret = new long[array.length];
            System.arraycopy(array, 0, ret, 0, array.length);

            return ret;
        }
    }

    public static int[] copyArray(int[] array) {
        if (array == null) {
            return new int[0];
        }

        if (array.length == 0) {
            return new int[0];
        } else {
            int[] ret = new int[array.length];
            System.arraycopy(array, 0, ret, 0, array.length);

            return ret;
        }
    }

    public static float[] copyArray(float[] array) {
        if (array == null) {
            return new float[0];
        }

        if (array.length == 0) {
            return new float[0];
        } else {
            float[] ret = new float[array.length];
            System.arraycopy(array, 0, ret, 0, array.length);

            return ret;
        }
    }

    public static BufferedImage createScreenshot(Component component) {
        if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;

            return createComponentScreenshot(scrollPane.getViewport());
        } else {
            return createComponentScreenshot(component);
        }
    }

    public static void ensureMinimumSize(Component comp) {
        comp = getParentWindow(comp);

        if (comp != null) {
            final Component top = comp;
            top.addComponentListener(new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        Dimension d = top.getSize();
                        Dimension min = top.getMinimumSize();

                        if ((d.width < min.width) || (d.height < min.height)) {
                            top.setSize(Math.max(d.width, min.width), Math.max(d.height, min.height));
                        }
                    }
                });
        }
    }

    // Classic Windows LaF doesn't draw dotted focus rectangle inside JButton if parent is JToolBar,
    // XP Windows LaF doesn't draw dotted focus rectangle inside JButton at all
    // This method installs customized Windows LaF that draws dotted focus rectangle inside JButton always

    // On JDK 1.5 the XP Windows LaF enforces special border to all buttons, overriding any custom border
    // set by setBorder(). Class responsible for this is WindowsButtonListener. See Issue 71546.
    // Also fixes buttons size in JToolbar.

    /** Ensures that focus will be really painted if button is focused
     * and fixes using custom border for JDK 1.5 & XP LaF
     */
    public static void fixButtonUI(AbstractButton button) {
        try { // Fix for Issue 175755 - WindowsButtonUI is private API, incompatible changes may occur
            final int dashedRectGapX_Local = ((Integer)UIManager.get("Button.dashedRectGapX")).intValue(); // NOI18N
            final int dashedRectGapY_Local = ((Integer)UIManager.get("Button.dashedRectGapY")).intValue(); // NOI18N
            final int dashedRectGapWidth_Local = ((Integer)UIManager.get("Button.dashedRectGapWidth")).intValue(); // NOI18N
            final int dashedRectGapHeight_Local = ((Integer)UIManager.get("Button.dashedRectGapHeight")).intValue(); // NOI18N

            // JButton
            if (button.getUI() instanceof com.sun.java.swing.plaf.windows.WindowsButtonUI) {
                button.setUI(new com.sun.java.swing.plaf.windows.WindowsButtonUI() {
                        protected BasicButtonListener createButtonListener(AbstractButton b) {
                            return new BasicButtonListener(b); // Fix for  Issue 71546
                        }

                        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect,
                                                  Rectangle iconRect) {
                            int width = b.getWidth();
                            int height = b.getHeight();
                            g.setColor(getFocusColor());
                            javax.swing.plaf.basic.BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX_Local,
                                                                                     dashedRectGapY_Local,
                                                                                     width - dashedRectGapWidth_Local,
                                                                                     height - dashedRectGapHeight_Local);
                        }
                    });
            }
            // JToggleButton
            else if (button.getUI() instanceof com.sun.java.swing.plaf.windows.WindowsToggleButtonUI) {
                button.setUI(new com.sun.java.swing.plaf.windows.WindowsToggleButtonUI() {
                        protected BasicButtonListener createButtonListener(AbstractButton b) {
                            return new BasicButtonListener(b); // Fix for  Issue 71546
                        }

                        protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect,
                                                  Rectangle iconRect) {
                            int width = b.getWidth();
                            int height = b.getHeight();
                            g.setColor(getFocusColor());
                            javax.swing.plaf.basic.BasicGraphicsUtils.drawDashedRect(g, dashedRectGapX_Local,
                                                                                     dashedRectGapY_Local,
                                                                                     width - dashedRectGapWidth_Local,
                                                                                     height - dashedRectGapHeight_Local);
                        }
                    });
            }
        } catch (Throwable t) {
            // Private API is used, just skip any incompatibility failure
        }
    }

    public static boolean hasOnlyLeafs(JTree tree, Object node) {
        TreeModel model = tree.getModel();

        for (int i = 0; i < model.getChildCount(node); i++) {
            if (!model.isLeaf(model.getChild(node, i))) {
                return false;
            }
        }

        return true;
    }

    /** By calling this method, the provided tree will become auto-expandable, i.e.
     * When a node is expanded, if it has only one child, that child gets expanded, and so on.
     * This is very useful for trees that have a deep node hierarchy with typical paths from
     * root to leaves containing only one node along the whole path.
     *
     * @param tree The tree to make auto-expandable
     */
    public static void makeTreeAutoExpandable(JTree tree) {
        makeTreeAutoExpandable(tree, 1, false);
    }

    public static void makeTreeAutoExpandable(JTree tree, final boolean dontExpandToLeafs) {
        makeTreeAutoExpandable(tree, 1, dontExpandToLeafs);
    }

    /** By calling this method, the provided tree will become auto-expandable, i.e.
     * When a node is expanded, if it has only one child, that child gets expanded, and so on.
     * This is very useful for trees that have a deep node hierarchy with typical paths from
     * root to leaves containing only one node along the whole path.
     *
     * @param tree The tree to make auto-expandable
     */
    public static void makeTreeAutoExpandable(JTree tree, final int maxChildToExpand) {
        makeTreeAutoExpandable(tree, maxChildToExpand, false);
    }

    public static void makeTreeAutoExpandable(final JTree tree, final int maxChildToExpand, final boolean dontExpandToLeafs) {
        tree.addTreeExpansionListener(new TreeExpansionListener() {
                boolean internalChange = false;

                public void treeCollapsed(TreeExpansionEvent event) {
                }

                public void treeExpanded(TreeExpansionEvent event) {
                    if (internalChange || Boolean.TRUE.equals(tree.getClientProperty(PROP_EXPANSION_TRANSACTION))) { // NOI18N
                        return;
                    }

                    // Auto expand more if the just expanded child has only one child
                    TreePath path = event.getPath();
                    JTree tree = (JTree) event.getSource();
                    internalChange = true;
                    autoExpand(tree, path, MAX_TREE_AUTOEXPAND_LINES, maxChildToExpand, dontExpandToLeafs);
                    internalChange = false;
                }
            });
    }

    public static void runInEventDispatchThread(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    public static void runInEventDispatchThreadAndWait(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InvocationTargetException e) {
                LOGGER.severe(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }
    
    public static void addBorder(JComponent c, Border b) {
        Border cb = c.getBorder();
        Border nb = cb == null ? b : new CompoundBorder(cb, b);
        c.setBorder(nb);
    }
    
    public static Color getDisabledLineColor() {
        if (disabledLineColor == null) {
            disabledLineColor = UIManager.getColor("Label.disabledForeground"); // NOI18N
            if (disabledLineColor == null)
                disabledLineColor = UIManager.getColor("Label.disabledText"); // NOI18N
            if (disabledLineColor == null) disabledLineColor = Color.GRAY;
        }
        return disabledLineColor;
    } 

    private static BufferedImage createComponentScreenshot(final Component component) {
        final BufferedImage[] result = new BufferedImage[1];

        final Runnable screenshotPerformer = new Runnable() {
            public void run() {
                if (component instanceof JTable
                        || (component instanceof JViewport && ((JViewport) component).getView() instanceof JTable)) {
                    result[0] = createTableScreenshot(component);
                } else {
                    result[0] = createGeneralComponentScreenshot(component);
                }
            }
        };
        
        try {
            if (SwingUtilities.isEventDispatchThread()) screenshotPerformer.run();
            else SwingUtilities.invokeAndWait(screenshotPerformer);
        } catch (Exception e) {
            return null;
        }
        
        return result[0];
    }

    private static BufferedImage createGeneralComponentScreenshot(Component component) {
        Component source;
        Dimension sourceSize;

        if (component instanceof JViewport) {
            JViewport viewport = (JViewport) component;
            Component contents = viewport.getView();

            if (contents.getSize().height > viewport.getSize().height) {
                source = component;
                sourceSize = component.getSize();
            } else {
                source = contents;
                sourceSize = contents.getSize();
            }
        } else {
            source = component;
            sourceSize = component.getSize();
        }

        BufferedImage componentScreenshot = new BufferedImage(sourceSize.width, sourceSize.height, BufferedImage.TYPE_INT_RGB);
        Graphics componentScreenshotGraphics = componentScreenshot.getGraphics();
        source.printAll(componentScreenshotGraphics);

        return componentScreenshot;
    }

    private static BufferedImage createTableScreenshot(Component component) {
        Component source;
        Dimension sourceSize;
        JTable table;

        if (component instanceof JTable) {
            table = (JTable) component;

            if ((table.getTableHeader() == null) || !table.getTableHeader().isVisible()) {
                return createGeneralComponentScreenshot(component);
            }

            source = table;
            sourceSize = table.getSize();
        } else if (component instanceof JViewport && ((JViewport) component).getView() instanceof JTable) {
            JViewport viewport = (JViewport) component;
            table = (JTable) viewport.getView();

            if ((table.getTableHeader() == null) || !table.getTableHeader().isVisible()) {
                return createGeneralComponentScreenshot(component);
            }

            if (table.getSize().height > viewport.getSize().height) {
                source = viewport;
                sourceSize = viewport.getSize();
            } else {
                source = table;
                sourceSize = table.getSize();
            }
        } else {
            throw new IllegalArgumentException("Component can only be JTable or JViewport holding JTable"); // NOI18N
        }

        final JTableHeader tableHeader = table.getTableHeader();
        Dimension tableHeaderSize = tableHeader.getSize();

        BufferedImage tableScreenshot = new BufferedImage(sourceSize.width, tableHeaderSize.height + sourceSize.height,
                                                          BufferedImage.TYPE_INT_RGB);
        final Graphics tableScreenshotGraphics = tableScreenshot.getGraphics();

        // Component.printAll has to run in AWT Thread to print component contents correctly
        if (SwingUtilities.isEventDispatchThread()) {
            tableHeader.printAll(tableScreenshotGraphics);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            tableHeader.printAll(tableScreenshotGraphics);
                        }
                    });
            } catch (Exception e) {
            }
        }

        tableScreenshotGraphics.translate(0, tableHeaderSize.height);

        final Component printSrc = source;

        // Component.printAll has to run in AWT Thread to print component contents correctly
        if (SwingUtilities.isEventDispatchThread()) {
            printSrc.printAll(tableScreenshotGraphics);
        } else {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            printSrc.printAll(tableScreenshotGraphics);
                        }
                    });
            } catch (Exception e) {
            }
        }

        return tableScreenshot;
    }
}
