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
package org.netbeans.modules.profiler.heapwalk.details.jdk.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.netbeans.modules.profiler.heapwalk.model.BrowserUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "Utils_NoDetails=<No details>"
})
final class Utils {
    
    static String getFontName(Instance instance, Heap heap) {
        String name = getFieldString(instance, "name");                         // NOI18N
        if (name == null) {
            Instance font2DHandle = (Instance)instance.getValueOfField(
                    "font2DHandle");                                            // NOI18N
            if (font2DHandle != null) {
                Instance font2D = (Instance)font2DHandle.getValueOfField(
                        "font2D");                                              // NOI18N
                if (font2D != null) {
                    name = getFieldString(instance, "fullName");                // NOI18N
                    if (name == null)
                        name = getFieldString(instance, "nativeFontName");      // NOI18N
                }
            }
        }
        return name;
    }
    
    static String getFieldString(Instance instance, String field) {
        Object _s = instance.getValueOfField(field);
        if (_s instanceof Instance) {
            Instance s = (Instance)_s;
            int offset = DetailsUtils.getIntFieldValue(s, "offset", 0);         // NOI18N
            int count = DetailsUtils.getIntFieldValue(s, "count", -1);          // NOI18N
            List<String> ch = DetailsUtils.getPrimitiveArrayFieldValues(s, "value"); // NOI18N
            if (ch != null) {
                int valuesCount = count < 0 ? ch.size() - offset :
                                  Math.min(count, ch.size() - offset);            
                StringBuilder value = new StringBuilder(valuesCount * 2);
                int lastValue = offset + valuesCount - 1;
                for (int i = offset; i <= lastValue; i++)
                    value.append(ch.get(i));
                return value.toString();
            }
        }
        return null;
    }
    
    static final class PlaceholderIcon implements Icon {
        
        private final int width;
        private final int height;
        
        PlaceholderIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public int getIconWidth() {
            return width;
        }

        public int getIconHeight() {
            return height;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, width, height);
            g.setColor(Color.BLACK);
            g.drawLine(x, y, x + width - 1, y + height - 1);
            g.drawLine(x, y + height - 1, x + width - 1, y);
        }
        
    }
    
    static final class PlaceholderPanel extends JPanel {
        
        private static final Color LINE =
                          UIManager.getLookAndFeel().getID().equals("Metal") ?  // NOI18N
                          UIManager.getColor("Button.darkShadow") :             // NOI18N
                          UIManager.getColor("Button.shadow");                  // NOI18N
        
        private final JLabel label;
        
        PlaceholderPanel(String className) {
            super(null);
            
            putClientProperty("className", className);
            
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(LINE));
            
            label = new JLabel(BrowserUtils.getSimpleType(className), JLabel.CENTER);
            label.setOpaque(true);
        }
        
        public void doLayout() {
            Dimension s = getSize();
            Dimension p = label.getPreferredSize();
            
            int x = (s.width - p.width) / 2;
            int y = (s.height - p.height) / 2;
            
            label.setBounds(x, y, p.width, p.height);
        }
        
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(LINE);
            g.drawLine(0, 0, getWidth() - 1, getHeight() - 1);
            g.drawLine(0, getHeight() - 1, getWidth() - 1, 0);
            
            Point p = label.getLocation();
            g.translate(p.x, p.y);
            label.paint(g);
            g.translate(-p.x, -p.y);
        }
        
    }
    
    static final class JPopupMenuImpl extends JPopupMenu {
        
        public void setVisible(boolean visible) {}
        public boolean isVisible() { return true; }

        // Workarounds for best apperance of JPopupMenu preview
        public Component add(Component comp) {
            if (comp instanceof JComponent)
                ((JComponent)comp).setOpaque(false);
            return super.add(comp);
        }
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { repaint(); }
            });
        }
        
    }
    
    static abstract class InstanceBuilder<T> {

        InstanceBuilder(Instance instance, Heap heap) {}

        protected void setupInstance(T instance) {}

        protected T createInstanceImpl() { return null; }
        
        final T createInstance() {
            T instance = createInstanceImpl();
            if (instance != null) setupInstance(instance);
            return instance;
        }

    }
    
    static abstract class View<T extends InstanceBuilder> extends DetailsProvider.View implements Scrollable {
        
        private static final int DASH_SIZE = 20;
        
        private final int margin;
        private final boolean pattern;
        private final boolean stretch;
        
        private Component component;
        private JPanel glassPane;
        
        View(Instance instance, Heap heap) {
            this(10, true, false, instance, heap);
        }
        
        View(int margin, boolean pattern, boolean stretch, Instance instance, Heap heap) {
            super(instance, heap);
            this.margin = margin;
            this.pattern = pattern;
            this.stretch = stretch;
        }
        
        protected T getBuilder(Instance instance, Heap heap) {
            return null;
        }
        
        protected Component getComponent(T builder) {
            return null;
        }
        
        protected void setupGlassPane(JPanel glassPane) {}
        
        protected final void computeView(Instance instance, Heap heap) {
            final T builder = getBuilder(instance, heap);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeAll();
                    component = builder == null ? null : getComponent(builder);
                    if (component != null) {
                        component.setVisible(true);
                        if (stretch) {
                            add(component, BorderLayout.CENTER);
                        } else {
                            glassPane = new JPanel(null) {
                                public Point getToolTipLocation(MouseEvent event) {
                                    Point p = event.getPoint();
                                    p.translate(15, 15);
                                    return p;
                                }
                            };
                            glassPane.setOpaque(false);
                            glassPane.addMouseListener(new MouseAdapter() {});
                            glassPane.addMouseMotionListener(new MouseMotionAdapter() {});
                            glassPane.addKeyListener(new KeyAdapter() {});
                            setupGlassPane(glassPane);
                            add(glassPane);
                            
                            setLayout(null);
                            add(component);
                        }
                        if (component.getWidth() == 0 || component.getHeight() == 0)
                            component.setSize(component.getPreferredSize());
                        Dimension d = component.getSize();
                        d.width += margin;
                        d.height += margin;
                        setPreferredSize(d);
                        setBackground(UIUtils.getProfilerResultsBackground());
                        setForeground(UIUtils.getDarker(getBackground()));
                    } else {
                        component = new JLabel(Bundle.Utils_NoDetails(), JLabel.CENTER);
                        component.setEnabled(false);
                        add(component, BorderLayout.CENTER);
                    }
                    revalidate();
                    doLayout();
                    repaint();
                }
            });
        }
        
        public void doLayout() {
            if (getLayout() != null) {
                super.doLayout();
            } else {
                Dimension size = getSize();
                Dimension comp = component.getSize();
                
                int x = comp.width >= size.width ? 0 :
                        (size.width - comp.width) / 2;
                int y = comp.height >= size.height ? 0 :
                        (size.height - comp.height) / 2;
                
                component.move(x, y); // required to correctly setup JPopupMenu
                
                glassPane.setBounds(component.getBounds());
            }
        }
        
        protected void paintComponent(Graphics g) {
            if (!pattern || component == null) {
                super.paintComponent(g);
            } else {
                int x = 0;
                int y = 0;
                int w = getWidth();
                int h = getHeight();
                
                while (y <= h) {
                    boolean flag = (y / DASH_SIZE) % 2 == 0;
                    while (x <= w) {
                        g.setColor(flag ? getBackground() : getForeground());
                        g.fillRect(x, y, DASH_SIZE, DASH_SIZE);
                        x += DASH_SIZE;
                        flag = !flag;
                    }
                    x = 0;
                    y += DASH_SIZE;
                }
            }
        }
        
        public Dimension getPreferredScrollableViewportSize() {
            return null;
        }

        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            // Scroll almost one screen
            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return 50;
            return (int)(((JViewport)parent).getHeight() * 0.95f);
        }

        public boolean getScrollableTracksViewportHeight() {
            // Allow dynamic vertical enlarging of the panel but request the vertical scrollbar when needed
            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getPreferredSize().height < ((JViewport)parent).getHeight();
        }

        public boolean getScrollableTracksViewportWidth() {
            // Allow dynamic horizontal enlarging of the panel but request the vertical scrollbar when needed
            Container parent = getParent();
            if ((parent == null) || !(parent instanceof JViewport)) return false;
            return getPreferredSize().width < ((JViewport)parent).getWidth();
        }

        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }
        
    }
    
}
