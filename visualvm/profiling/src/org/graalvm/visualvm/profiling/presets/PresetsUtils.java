/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.profiling.presets;

import java.util.regex.Pattern;
import org.graalvm.visualvm.lib.jfluid.filters.GenericFilter;

/**
 *
 * @author Jiri Sedlacek
 */
final class PresetsUtils {
    
    static String normalizeValue(String value) {
        String[] values = value.split("\\n"); // NOI18N
        StringBuilder normalizedValue = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            String filterValue = values[i].trim();
            if ((i != (values.length - 1)) && !filterValue.endsWith(",")) // NOI18N
                filterValue = filterValue + ","; // NOI18N
            normalizedValue.append(filterValue);
        }

        return normalizedValue.toString();
    }
    
    
    static boolean isValidJavaValue(String normalizedValue, boolean allowEmpty, boolean acceptArrays) {
        // check whether empty value is allowed
        if (normalizedValue.isEmpty()) return allowEmpty;
        
        String[] values = GenericFilter.values(normalizedValue);
        for (String value : values) {
            // remove up to two trailing wildcards
            boolean hadWildcard = value.endsWith("*"); // NOI18N
            if (hadWildcard) value = value.substring(0, value.length() - 1);
            if (value.endsWith("*")) value = value.substring(0, value.length() - 1); // NOI18N
            
            if (hadWildcard) {
                // wildcards can only be standalone or prefixed by dot
                if (!value.isEmpty() && !value.endsWith(".")) return false; // NOI18N
            } else if (acceptArrays) {
                int len = value.length();

                // remove trailing arrays if allowed and not followed by wildcards
                while (value.endsWith("[]")) value = value.substring(0, value.length() - 2); // NOI18N

                // multiple array marks only allowed when prefixed by Java identifier
                if (len - value.length() > 2 && value.isEmpty()) return false;
            }
            
            // empty line is allowed
            if (value.isEmpty()) continue;
            
            // trailing dot only allowed when followed by wildcards
            if (value.endsWith(".")) { // NOI18N
                if (!hadWildcard) return false;
                value = value.substring(0, value.length() - 1); // NOI18N
            }
            
            // check whether the result is a valid Java identifier
            if (!isValidJavaIdentifier(value)) return false;
        }
        
        return true;
    }
    
    
    private static Pattern JAVA_IDENTIFIER_PATTERN;
    private static boolean isValidJavaIdentifier(String identifier) {
        if (JAVA_IDENTIFIER_PATTERN == null) {
            String ID_PATTERN = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"; // NOI18N
            JAVA_IDENTIFIER_PATTERN = Pattern.compile(ID_PATTERN + "(\\." + ID_PATTERN + ")*"); // NOI18N
        }
        return JAVA_IDENTIFIER_PATTERN.matcher(identifier).matches();
    }
    
}
