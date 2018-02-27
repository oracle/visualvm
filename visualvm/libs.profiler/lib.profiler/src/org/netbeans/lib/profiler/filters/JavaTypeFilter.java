/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.lib.profiler.filters;

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
