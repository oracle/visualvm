/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package net.java.visualvm.btrace.ui.components.graph;

import java.awt.Color;

/**
 *
 * @author Jaroslav Bachorik
 */
public class LegendItem {
    private Color itemColor;
    private ValueProvider valueProvider;
    private boolean visible;
    
    public LegendItem(Color itemColor, ValueProvider provider) {
        this.itemColor = itemColor;
        this.valueProvider = provider;
        this.visible = false;
    }
    
    public LegendItem(ValueProvider provider) {
        this(new Color(provider.hashCode() % 0xffffff), provider);
    }

    public Color getItemColor() {
        return itemColor;
    }

    public void setItemColor(Color itemColor) {
        this.itemColor = itemColor;
    }

    public String getItemDescription() {
        String desc = valueProvider.description;
        if (valueProvider.unit.length() > 0)  desc = desc.concat(" [" + valueProvider.unit + "]");
        return desc;
    }

    public String getItemName() {
        return valueProvider.name;
    }

    public ValueProvider getValueProvider() {
        return valueProvider;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
