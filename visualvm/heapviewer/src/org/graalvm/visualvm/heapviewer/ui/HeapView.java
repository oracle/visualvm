/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.visualvm.heapviewer.ui;

import javax.swing.Icon;
import javax.swing.JComponent;
import org.netbeans.lib.profiler.ui.components.ProfilerToolbar;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class HeapView {
    
    public static final String PROP_KEY = "HeapView"; // NOI18N
    
    
    private final String name;
    private final String description;
    private final Icon icon;


    public HeapView(String name, String description) {
        this(name, description, null);
    }

    public HeapView(String name, String description, Icon icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }


    public String getName() { return name; }

    public String getDescription() { return description; }

    public Icon getIcon() { return icon; }
    
    
    public abstract JComponent getComponent();
    
    public abstract ProfilerToolbar getToolbar();
    
    
    protected void showing() {}
    
    protected void hidden() {}
    
}
