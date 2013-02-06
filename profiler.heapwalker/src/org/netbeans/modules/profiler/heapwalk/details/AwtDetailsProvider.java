/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.heapwalk.details;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Instance;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=InstanceDetailsProvider.class)
public class AwtDetailsProvider extends InstanceDetailsProvider {
    
    public String getDetailsString(Instance instance) {
        if (isSubclassOf(instance, "java.awt.Font")) { // NOI18N                // Font+
            return getStringFieldValue(instance, "name"); // NOI18N
        } else if (isSubclassOf(instance, "java.awt.Color")) { // NOI18N        // Color+
            Color color = createColor(instance);
            if (color != null) return "[" + color.getRed() + ", " + color.getGreen() + // NOI18N
                                      ", " + color.getBlue() + ", " + color.getAlpha() + "]"; // NOI18N
        } else if (isSubclassOf(instance, "java.awt.Point")) { // NOI18N        // Point+
            Point point = createPoint(instance);
            if (point != null) return "[" + point.x + ", " + point.y + "]"; // NOI18N
        } else if (isSubclassOf(instance, "java.awt.Dimension")) { // NOI18N    // Dimension+
            Dimension dimension = createDimension(instance);
            if (dimension != null) return "[" + dimension.width + ", " + dimension.height + "]"; // NOI18N
        } else if (isSubclassOf(instance, "java.awt.Rectangle")) { // NOI18N    // Rectangle+
            Rectangle rectangle = createRectangle(instance);
            if (rectangle != null) return "[" + rectangle.x + ", " + rectangle.y + // NOI18N
                                      ", " + rectangle.width + ", " + rectangle.height + "]"; // NOI18N
        } else if (isSubclassOf(instance, "java.awt.Insets")) { // NOI18N       // Insets+
            Insets insets = createInsets(instance);
            if (insets != null) return "[" + insets.top + ", " + insets.left + // NOI18N
                                      ", " + insets.bottom + ", " + insets.right + "]"; // NOI18N
        }
        return null;
    }
    
    public View getDetailsView(Instance instance) {
        if (isSubclassOf(instance, "java.awt.Font")) { // NOI18N                // Font+
            return new FontView(instance);
        } else if (isSubclassOf(instance, "java.awt.Color")) { // NOI18N                // Font+
            return new ColorView(instance);
        }
        return null;
    }
    
    static Font createFont(Instance instance) {
        String name = getStringFieldValue(instance, "name"); // NOI18N
        if (name != null && name.trim().isEmpty()) name = null;
                
        int style = getIntFieldValue(instance, "style"); // NOI18N
        if (style == Integer.MIN_VALUE) style = 0;
        
        int size = getIntFieldValue(instance, "size"); // NOI18N
        if (size == Integer.MIN_VALUE) size = 10; // TODO: should use default font size
        
        return new Font(name, style, size);
    }
    
    static Color createColor(Instance instance) {
        int rgba = getIntFieldValue(instance, "value"); // NOI18N
        return rgba != Integer.MIN_VALUE ? new Color(rgba) : null;
    }
    
    static Point createPoint(Instance instance) {
        int x = getIntFieldValue(instance, "x"); // NOI18N
//        if (x == Integer.MIN_VALUE) x = 0;
        
        int y = getIntFieldValue(instance, "y"); // NOI18N
//        if (y == Integer.MIN_VALUE) y = 0;
        
        return new Point(x, y);
    }
    
    static Dimension createDimension(Instance instance) {
        int width = getIntFieldValue(instance, "width"); // NOI18N
//        if (width == Integer.MIN_VALUE) width = 0;
        
        int height = getIntFieldValue(instance, "height"); // NOI18N
//        if (height == Integer.MIN_VALUE) width = 0;
        
        return new Dimension(width, height);
    }
    
    static Rectangle createRectangle(Instance instance) {
        int x = getIntFieldValue(instance, "x"); // NOI18N
//        if (x == Integer.MIN_VALUE) x = 0;
        
        int y = getIntFieldValue(instance, "y"); // NOI18N
//        if (y == Integer.MIN_VALUE) y = 0;
        
        int width = getIntFieldValue(instance, "width"); // NOI18N
//        if (width == Integer.MIN_VALUE) width = 0;
        
        int height = getIntFieldValue(instance, "height"); // NOI18N
//        if (height == Integer.MIN_VALUE) width = 0;
        
        return new Rectangle(x, y, width, height);
    }
    
    static Insets createInsets(Instance instance) {
        int top = getIntFieldValue(instance, "top"); // NOI18N
//        if (top == Integer.MIN_VALUE) top = 0;
        
        int left = getIntFieldValue(instance, "left"); // NOI18N
//        if (left == Integer.MIN_VALUE) left = 0;
        
        int bottom = getIntFieldValue(instance, "bottom"); // NOI18N
//        if (bottom == Integer.MIN_VALUE) bottom = 0;
        
        int right = getIntFieldValue(instance, "right"); // NOI18N
//        if (right == Integer.MIN_VALUE) right = 0;
        
        return new Insets(top, left, bottom, right);
    }
    
    private static class FontView extends View {
        
        FontView(Instance instance) {
            super(instance);
        }
        
        protected void computeView(Instance instance) {
            final Font font = createFont(instance);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeAll();
                    JLabel label = new JLabel();
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setFont(font);
                    label.setText("ABCabc123");
                    add(label, BorderLayout.CENTER);
                    invalidate();
                    revalidate();
                    doLayout();
                }
            });
        }
        
    }
    
    private static class ColorView extends View {
        
        ColorView(Instance instance) {
            super(instance);
        }
        
        protected void computeView(Instance instance) {
            final Color color = createColor(instance);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    removeAll();
                    setOpaque(true);
                    setBackground(color);
                }
            });
        }
        
    }
    
}
