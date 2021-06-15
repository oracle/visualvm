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

import java.util.Locale;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 *
 * @author Jiri Sedlacek
 */
public class TextFilter extends GenericFilter {

    public static final int TYPE_REGEXP = 30;


    private static final String[] NORMALIZED_NOT_READY = new String[0];

    private String[] normalizedValues = NORMALIZED_NOT_READY;
    private transient Pattern[] regexpPatterns;


    public TextFilter() {
        super(null, "", TYPE_INCLUSIVE); // NOI18N
    }

    public TextFilter(String value, int type, boolean caseSensitive) {
        super(null, value, type);
        setCaseSensitive(caseSensitive);
    }

    public TextFilter(Properties properties, String id) {
        super(properties, id);
    }


    public void copyFrom(TextFilter other) {
        super.copyFrom(other);

        normalizedValues = other.normalizedValues;
        regexpPatterns = other.regexpPatterns;
    }


    public boolean isAll() {
//        return getType() == TYPE_REGEXP ? isEmpty() : super.isAll();
        return isEmpty();
    }


    public final void setCaseSensitive(boolean caseSensitive) {
//        if (caseSensitive != isCaseSensitive()) setValue(getValue()); // resets precomputed values
        if (caseSensitive || getType() == TYPE_REGEXP) {
            normalizedValues = null;
        } else {
            normalizedValues = NORMALIZED_NOT_READY;
        }
    }

    public final boolean isCaseSensitive() {
        return normalizedValues == null;
    }


    protected void valueChanged() {
        super.valueChanged();

        if (!isCaseSensitive()) normalizedValues = NORMALIZED_NOT_READY;
        regexpPatterns = null;
    }

//    protected String[] computeValues(String value) {
//        return getType() == TYPE_REGEXP ? super.computeValues(value) :
//               super.computeValues(value.replace('*', ' ')); // NOI18N
//    }
    
    
    public boolean passes(String string) {
        if (getType() == TYPE_REGEXP) {
            String[] values = getValues();
            
            if (regexpPatterns == null) regexpPatterns = new Pattern[values.length];
            
            for (int i = 0; i < regexpPatterns.length; i++) {
                if (regexpPatterns[i] == null) 
                    try {
                        regexpPatterns[i] = Pattern.compile(values[i]);
                    } catch (RuntimeException e) {
                        handleInvalidFilter(values[i], e);
                        regexpPatterns[i] = Pattern.compile(".*"); // NOI18N
                    }
                if (regexpPatterns[i].matcher(string).matches()) return true;
            }
            
            return false;
        } else {
//            return super.passes(string);
            if (simplePasses(string)) return true;
            
            String[] values = getValues();
            
            boolean caseSensitive = isCaseSensitive();
            if (!caseSensitive) {
                string = normalizeString(string);
                if (normalizedValues == NORMALIZED_NOT_READY) normalizedValues = new String[values.length];
            }
            
            for (int i = 0; i < values.length; i++) {
                String value;
                if (!caseSensitive) {
                    if (normalizedValues[i] == null) normalizedValues[i] = normalizeString(values[i]);
                    value = normalizedValues[i];
                } else {
                    value = values[i];
                }
                if (string.contains(value)) return getType() == TYPE_INCLUSIVE;
            }

            return getType() != TYPE_INCLUSIVE;
        }
    }
    
    
    protected void handleInvalidFilter(String invalidValue, RuntimeException e) {}
    
    
    private static String normalizeString(String string) {
        // NOTE: comparing String.toLowerCase doesn't work correctly for all locales
        // but is much faster than using String.equalsIgnoreCase or an exact algorithm
        // for case-insensitive comparison
        return string.toLowerCase(Locale.ENGLISH);
    }
    
    
    protected boolean valuesEquals(Object obj) {
        if (!super.valuesEquals(obj)) return false;
        
        TextFilter other = (TextFilter)obj;
        if (normalizedValues == null) {
            if (other.normalizedValues != null) return false;
        } else {
            if (other.normalizedValues == null) return false;
        }
        
        return true;
    }
    
    protected int valuesHashCode(int hashBase) {
        hashBase = super.valuesHashCode(hashBase);
        
        if (normalizedValues == null) hashBase = 67 * hashBase;
        
        return hashBase;
    }
    
}
