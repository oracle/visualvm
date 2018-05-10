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
