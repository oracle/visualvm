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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.ui.UIUtils;
import org.netbeans.modules.profiler.heapwalk.details.jdk.ui.Builders.InstanceBuilder;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
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
        String name = DetailsUtils.getInstanceFieldString(
                    instance, "name", heap);                                    // NOI18N
        if (name == null) {
            Instance font2DHandle = (Instance)instance.getValueOfField(
                    "font2DHandle");                                            // NOI18N
            if (font2DHandle != null) {
                Instance font2D = (Instance)font2DHandle.getValueOfField(
                        "font2D");                                              // NOI18N
                if (font2D != null) {
                    name = DetailsUtils.getInstanceFieldString(
                            instance, "fullName", heap);                        // NOI18N
                    if (name == null) {
                        name = DetailsUtils.getInstanceFieldString(
                                instance, "nativeFontName", heap);              // NOI18N
                    }
                }
            }
        }
        return name;
    }
    
    
    static abstract class View<T extends InstanceBuilder> extends DetailsProvider.View {
        
        private static final int DASH_SIZE = 20;
        
        private final int margin;
        private final boolean pattern;
        private final boolean stretch;
        
        private boolean displayingComponent = false;
        
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
        
        protected final void computeView(Instance instance, Heap heap) {
            final T builder = getBuilder(instance, heap);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeAll();
                    Component component = builder == null ? null :
                                          getComponent(builder);
                    if (component != null) {
                        component.setVisible(true);
                        if (stretch) {
                            add(component, BorderLayout.CENTER);
                        } else {
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
                        displayingComponent = true;
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
                Dimension comp = getComponent(0).getSize();
                
                int x = comp.width >= size.width ? 0 :
                        (size.width - comp.width) / 2;
                int y = comp.height >= size.height ? 0 :
                        (size.height - comp.height) / 2;
                
                getComponent(0).setLocation(x, y);
            }
        }
        
        protected void paintComponent(Graphics g) {
            if (!displayingComponent || !pattern) {
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
        
    }
    
}
