/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.ui.results;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;
import java.util.Properties;
import javax.swing.Icon;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;

/**
 *
 * @author Jiri Sedlacek
 */
public class ColoredFilter extends GenericFilter {
    
    private static final String PROP_COLOR = "COLOR"; // NOI18N
    
    private Color color;
    private transient Icon icon;
    
    
    public ColoredFilter(ColoredFilter other) {
        super(other);
        
        this.color = other.color;
    }
    
    public ColoredFilter(String name, String value, Color color) {
        super(name, value, TYPE_INCLUSIVE);
        
        this.color = color;
    }
    
    public ColoredFilter(Properties properties, String id) {
        super(properties, id);
        
        color = loadColor(properties, id);
    }
    
    
    public void copyFrom(ColoredFilter other) {
        super.copyFrom(other);
        
        color = other.color;
    }
    
    
    public final void setColor(Color color) {
        this.color = color;
    }
    
    public final Color getColor() {
        return color;
    }
    
    
    public final Icon getIcon(int width, int height) {
        if (icon == null || icon.getIconWidth() != width || icon.getIconHeight() != height) {
            final int w = Math.max(16, width);
            final int h = Math.max(16, height);
            final int ww = width;
            final int hh = height;
            final int wo = ww >= 16 ? 0 : (16 - ww) / 2;
            final int ho = hh >= 16 ? 0 : (16 - hh) / 2;
            icon = new Icon() {
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    if (color == null) {
                        g.setColor(Color.BLACK);
                        g.drawLine(x + wo, y + ho, x + wo + ww, y + ho + hh);
                        g.drawLine(x + wo + ww, y + ho, x + wo, y + ho + hh);
                        g.drawRect(x + wo, y + ho, ww, hh);
                    } else {
                        g.setColor(color);
                        g.fillRect(x + wo, y + ho, ww, hh);
                        g.setColor(Color.BLACK);
                        g.drawRect(x + wo, y + ho, ww, hh);
                    }
                }
                public int getIconWidth() {
                    return w;
                }
                public int getIconHeight() {
                    return h;
                }
            };
        }
        return icon;
    }
    
    
    protected String[] computeValues(String value) {
        return super.computeValues(value.replace('*', ' ')); // NOI18N
    }
    
    
    public boolean passes(String string) {
        if (simplePasses(string)) return true;

        String[] values = getValues();
        for (int i = 0; i < values.length; i++)
            if (string.startsWith(values[i]))
                return true;
        
        return false;
    }
    
    
    protected boolean valuesEquals(Object obj) {
        if (!super.valuesEquals(obj)) return false;
        
        ColoredFilter other = (ColoredFilter)obj;
        if (!Objects.equals(color, other.color)) return false;
        
        return true;
    }
    
    protected int valuesHashCode(int hashBase) {
        hashBase = super.valuesHashCode(hashBase);
        
        if (color != null) hashBase = 67 * hashBase + color.hashCode();
        
        return hashBase;
    }
    
    
    public void store(Properties properties, String id) {
        super.store(properties, id);
        if (color != null) properties.put(id + PROP_COLOR, Integer.toString(color.getRGB()));
    }
    
    
    private static Color loadColor(Properties properties, String id) {
        String _color = properties.getProperty(id + PROP_COLOR);
        if (_color == null) return null;
        
        try {
            int _colorI = Integer.parseInt(_color);
            return new Color(_colorI);
        } catch (NumberFormatException e) {
            throw new InvalidFilterIdException("Bad color code specified", id); // NOI18N
        }
    }
    
}
