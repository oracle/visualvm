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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsProvider;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=DetailsProvider.class)
public final class AwtDetailsProvider extends DetailsProvider.Basic {
    
    private static final String FONT_MASK = "java.awt.Font+";                   // NOI18N
    private static final String COLOR_MASK = "java.awt.Color+";                 // NOI18N
    private static final String POINT_MASK = "java.awt.Point+";                 // NOI18N
    private static final String DIMENSION_MASK = "java.awt.Dimension+";         // NOI18N
    private static final String RECTANGLE_MASK = "java.awt.Rectangle+";         // NOI18N
    private static final String INSETS_MASK = "java.awt.Insets+";               // NOI18N
    
    public AwtDetailsProvider() {
        super(FONT_MASK, COLOR_MASK, POINT_MASK, DIMENSION_MASK,
              RECTANGLE_MASK, INSETS_MASK);
    }
    
    public String getDetailsString(String className, Instance instance, Heap heap) {
        if (FONT_MASK.equals(className)) {                                      // Font+
            String name = DetailsUtils.getInstanceFieldString(
                    instance, "name", heap);                                    // NOI18N
            if (name == null) {
                Instance font2DHandle = (Instance)instance.getValueOfField(
                        "font2DHandle");                                        // NOI18N
                if (font2DHandle != null) {
                    Instance font2D = (Instance)font2DHandle.getValueOfField(
                            "font2D");                                          // NOI18N
                    if (font2D != null) {
                        name = DetailsUtils.getInstanceFieldString(
                                instance, "fullName", heap);                    // NOI18N
                        if (name == null) {
                            name = DetailsUtils.getInstanceFieldString(
                                    instance, "nativeFontName", heap);          // NOI18N
                        }
                    }
                }
            }
            int size = DetailsUtils.getIntFieldValue(instance, "size", 10);     // NOI18N // TODO: should use default font size
            if (name == null) name = size + "pt";                               // NOI18N
            else name += ", " + size + "pt";                                    // NOI18N
            int style = DetailsUtils.getIntFieldValue(instance, "style", 0);    // NOI18N
            if ((style & 1) != 0) name += ", bold";                             // NOI18N
            if ((style & 2) != 0) name += ", italic";                           // NOI18N
            return name;
        } else if (COLOR_MASK.equals(className)) {                              // Color+
            Color color = createColor(instance, heap);
            if (color != null) return color.getRed() + ", " + color.getGreen() +// NOI18N
                               ", " + color.getBlue() + ", " + color.getAlpha();// NOI18N
        } else if (POINT_MASK.equals(className)) {                              // Point+
            Point point = createPoint(instance, heap);
            if (point != null) return point.x + ", " + point.y;                 // NOI18N
        } else if (DIMENSION_MASK.equals(className)) {                          // Dimension+
            Dimension dimension = createDimension(instance, heap);
            if (dimension != null) return dimension.width + ", " +              // NOI18N
                                   dimension.height;
        } else if (RECTANGLE_MASK.equals(className)) {                          // Rectangle+
            Rectangle rectangle = createRectangle(instance, heap);
            if (rectangle != null) return rectangle.x + ", " + rectangle.y +    // NOI18N
                                   ", " + rectangle.width + ", " + rectangle.height; // NOI18N
        } else if (INSETS_MASK.equals(className)) {                             // Insets+
            Insets insets = createInsets(instance, heap);
            if (insets != null) return insets.top + ", " + insets.left +        // NOI18N
                                 ", " + insets.bottom + ", " + insets.right;    // NOI18N
        }
        return null;
    }
    
    public View getDetailsView(String className, Instance instance, Heap heap) {
        if (FONT_MASK.equals(className)) {                                      // Font+
            return new FontView(instance, heap);
        } else if (COLOR_MASK.equals(className)) {                              // Color+
            return new ColorView(instance, heap);
        }
        return null;
    }
    
    static Font createFont(Instance instance, Heap heap) {
        String name = DetailsUtils.getInstanceFieldString(instance, "name", heap); // NOI18N
        int style = DetailsUtils.getIntFieldValue(instance, "style", 0);        // NOI18N       
        int size = DetailsUtils.getIntFieldValue(instance, "size", 10);         // NOI18N // TODO: should use default font size
        return new Font(name, style, size);
    }
    
    static Color createColor(Instance instance, Heap heap) {
        int rgba = DetailsUtils.getIntFieldValue(instance, "value", 0);         // NOI18N
        return new Color(rgba);
    }
    
    static Point createPoint(Instance instance, Heap heap) {
        int x = DetailsUtils.getIntFieldValue(instance, "x", 0);                // NOI18N
        int y = DetailsUtils.getIntFieldValue(instance, "y", 0);                // NOI18N
        return new Point(x, y);
    }
    
    static Dimension createDimension(Instance instance, Heap heap) {
        int width = DetailsUtils.getIntFieldValue(instance, "width", 0);        // NOI18N
        int height = DetailsUtils.getIntFieldValue(instance, "height", 0);      // NOI18N
        return new Dimension(width, height);
    }
    
    static Rectangle createRectangle(Instance instance, Heap heap) {
        int x = DetailsUtils.getIntFieldValue(instance, "x", 0);                // NOI18N
        int y = DetailsUtils.getIntFieldValue(instance, "y", 0);                // NOI18N
        int width = DetailsUtils.getIntFieldValue(instance, "width", 0);        // NOI18N
        int height = DetailsUtils.getIntFieldValue(instance, "height", 0);      // NOI18N
        return new Rectangle(x, y, width, height);
    }
    
    static Insets createInsets(Instance instance, Heap heap) {
        int top = DetailsUtils.getIntFieldValue(instance, "top", 0);            // NOI18N
        int left = DetailsUtils.getIntFieldValue(instance, "left", 0);          // NOI18N
        int bottom = DetailsUtils.getIntFieldValue(instance, "bottom", 0);      // NOI18N
        int right = DetailsUtils.getIntFieldValue(instance, "right", 0);        // NOI18N
        return new Insets(top, left, bottom, right);
    }
    
    private static class FontView extends View {
        
        FontView(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected void computeView(Instance instance, Heap heap) {
            final Font font = createFont(instance, heap);
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
        
        ColorView(Instance instance, Heap heap) {
            super(instance, heap);
        }
        
        protected void computeView(Instance instance, Heap heap) {
            final Color color = createColor(instance, heap);
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
