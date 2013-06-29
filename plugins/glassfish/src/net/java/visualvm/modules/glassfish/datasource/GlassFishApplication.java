/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package net.java.visualvm.modules.glassfish.datasource;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class GlassFishApplication extends GlassFishDataSource {    
    private GlassFishModel glassFishRoot;
    private String name;
    private String objectName;
    
    public GlassFishApplication(String name, String objName, GlassFishModel gfRoot) {
        super();
        this.name = name;
        this.glassFishRoot = gfRoot;
        this.objectName = objName;
    }

    public GlassFishModel getGlassFishRoot() {
        return glassFishRoot;
    }

    public String getName() {
        return name;
    }
    
    public String getObjectName() {
        return objectName;
    }
    
    abstract public void generateContents();

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GlassFishApplication other = (GlassFishApplication) obj;
        if (this.glassFishRoot != other.glassFishRoot && (this.glassFishRoot == null || !this.glassFishRoot.equals(other.glassFishRoot))) {
            return false;
        }
        if (this.objectName != other.objectName && (this.objectName == null || !this.objectName.equals(other.objectName))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 11 * hash + (this.glassFishRoot != null ? this.glassFishRoot.hashCode() : 0);
        hash = 11 * hash + (this.objectName != null ? this.objectName.hashCode() : 0);
        return hash;
    }
}
