/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources;

import javax.swing.JComponent;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class SourcesViewer {
    
    private final String id;
    private final String name;
    private final String description;
    
    
    protected SourcesViewer(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
    
    
    public abstract boolean open(SourceHandle handle);
    
    
    public final String getID() { return id; }
    
    public final String getName() { return name; }
    
    public final String getDescription() { return description; };
    
    
    public void loadSettings() {}
    
    public void saveSettings() {}
    
    public boolean settingsDirty() { return false; }
    
    public JComponent getSettingsComponent() { return null; }
    
    
    public final boolean equals(Object o) { return o instanceof SourcesViewer ? id.equals(((SourcesViewer)o).id) : false; }
    
    public final int hashCode() { return id.hashCode(); }
    
    public final String toString() { return name; }
    
}
