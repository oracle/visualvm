/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.filters;

import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author Jiri Sedlacek
 */
public class JavaTypeFilter extends GenericFilter {

    protected static final int MODE_STARTS_WITH_EX = 1025;

    private transient Boolean isAll;
    private transient Boolean isExact;
    private transient Boolean hasArray;


    public JavaTypeFilter() {
        super();
    }

    public JavaTypeFilter(GenericFilter other) {
        super(other);
    }

    public JavaTypeFilter(String value, int type) {
        super(null, value, type);
    }

    public JavaTypeFilter(Properties properties, String id) {
        super(properties, id);
    }


    public void copyFrom(JavaTypeFilter other) {
        super.copyFrom(other);

        isAll = other.isAll;
        isExact = other.isExact;
        hasArray = other.hasArray;
    }


    public final boolean isAll() {
        if (super.isAll()) return true;
        if (isAll == null) computeFlags();
        return isAll;
    }

    public final boolean isExact() {
        if (isExact == null) computeFlags();
        return isExact;
    }

    private void computeFlags() {
        for (String value : getValues())
            if ("*".equals(value) || "**".equals(value)) // NOI18N
                { isAll = true; isExact = false; break; }
            else if (value.charAt(value.length() - 1) == '*') // NOI18N
                { isExact = false; }

        if (isAll == null) isAll = false;
        if (isExact == null) isExact = true;
    }

    public final boolean hasArray() {
        if (hasArray == null) hasArray = getValue().indexOf('[') > -1; // NOI18N
        return hasArray;
    }


    protected void valueChanged() {
        super.valueChanged();
        isAll = null;
        isExact = null;
        hasArray = null;
    }

    protected String[] computeValues(String value) {
        return super.computeValues(value.replace('.', '/')); // NOI18N
    }
    
    
    protected int[] computeModes(String[] values) {
        int length = values.length;
        int[] modes = new int[length];
        
        Arrays.fill(modes, MODE_EQUALS);
        
        for (int i = 0; i < length; i++) {
            String value = values[i];
            int vlength = value == null ? 0 : value.length();
            
            if (vlength > 0 && '*' == value.charAt(vlength - 1)) { // NOI18N
                if (vlength > 1 && '*' == value.charAt(vlength - 2)) {
                    value = value.substring(0, vlength - 2);
                    modes[i] = MODE_STARTS_WITH;
                } else {
                    value = value.substring(0, vlength - 1);
                    modes[i] = MODE_STARTS_WITH_EX;
                }
                values[i] = value;
            }
        }
        
        return modes;
    }
    
    
    protected boolean matches(String string, String filter, int mode) {
        if (filter.isEmpty()) return true;
        
        if (mode == MODE_STARTS_WITH_EX) {
            if (!string.startsWith(filter)) return false;
            for (int i = filter.length(); i < string.length(); i++)
                if ('/' == string.charAt(i)) return false; // NOI18N
            return true;
        }
        
        return super.matches(string, filter, mode);
    }
    
}
