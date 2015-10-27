/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.lib.profiler.global;

import java.util.Arrays;


/**
 * Singleton instrumentation filter.
 * FilterType and FilterStrings should be set immediately after received from client side.
 *
 * TODO [ian] Not sure why this class is singleton, this is quite counter intuitive and can lead to errors.
 *
 * @author  Jiri Sedlacek
 */
public class InstrumentationFilter implements Cloneable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // Constants for instrumentation filter types
    public static final int INSTR_FILTER_NONE = 0;
    public static final int INSTR_FILTER_EXCLUSIVE = 10;
    public static final int INSTR_FILTER_INCLUSIVE = 20;
    public static final int INSTR_FILTER_EXCLUSIVE_EXACT = 30;
    public static final int INSTR_FILTER_INCLUSIVE_EXACT = 40;
    
    private static final int FILTER_MATCHES   = 1; // string exactly matches the filter (class name)
    private static final int FILTER_STARTS    = 2; // string starts by the filter (package and subpackages)
    private static final int FILTER_STARTS_EX = 3; // string starts by the filter and no '.' follows (package without subpackages)
    
    private static InstrumentationFilter defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String[] instrFilterUserStrings;
    private String[] instrFilterStrings;
    private int[] instrFilterTypes;
    private int instrFilterType;
    private boolean hasArray;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates empty instrumentation filter. */
    public InstrumentationFilter() {
        clearFilter();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Returns default instance of InstrumentationFilter.
     * filterType and filterStrings should be set immediately after received from client side.
     */
    public static InstrumentationFilter getDefault() {
        if (defaultInstance == null) {
            defaultInstance = new InstrumentationFilter();
        }

        return defaultInstance;
    }

    /**
     * Sets the current filter strings from a single flatFilterString.
     * flatFilterString is a string of filter strings separated by comma and/or space.
     *
     * This method splits single flatFilterString into slashed filterStrings and calls setSlashedFilterStrings(String flatFilterString).
     */
    public void setFilterStrings(String flatFilterString) {
        //if (flatFilterString == null) return; // don't be paranoid:o)
        flatFilterString = flatFilterString.replace(',', ' '); // NOI18N // filterStrings can be separated by comma and/or space
//        flatFilterString = flatFilterString.replace('.', '/'); // NOI18N // create slashed filterStrings
        
        hasArray = flatFilterString.indexOf('[') > -1; // NOI18N

        setSlashedFilterStrings(flatFilterString.trim().split(" +")); // NOI18N
    }

    /** Gets the current slashed classnames to be filtered. */
    public String[] getFilterStrings() {
        return instrFilterStrings;
    }
    
    public String[] getUserFilterStrings() {
        return instrFilterUserStrings;
    }
    
    /** Sets the current filter type as defined in CommonConstants. */
    public void setFilterType(int filterType) {
        instrFilterType = filterType;
    }

    /** Gets the current filter type as defined in CommonConstants. */
    public int getFilterType() {
        return instrFilterType;
    }

    /** Sets the current slashed classnames to be filtered. */
    public void setSlashedFilterStrings(String[] slashedFilterStrings) {
        //if (filterStrings == null) return; // don't be paranoid:o)  
        instrFilterStrings = slashedFilterStrings;
        instrFilterUserStrings = (String[])Arrays.copyOf(instrFilterStrings, instrFilterStrings.length);
        
        if (instrFilterType == INSTR_FILTER_INCLUSIVE_EXACT || instrFilterType == INSTR_FILTER_EXCLUSIVE_EXACT) {
            instrFilterTypes = new int[instrFilterStrings.length];
            Arrays.fill(instrFilterTypes, FILTER_MATCHES);
        } else {
            instrFilterTypes = null;
        }

        // remove trailing '*'
        for (int i = 0; i < instrFilterStrings.length; i++) {
            instrFilterStrings[i] = instrFilterStrings[i].replace('.', '/'); // NOI18N // create slashed filterStrings

            if (instrFilterStrings[i].equals("*") || instrFilterStrings[i].equals("**")) { // NOI18N
                if (instrFilterTypes != null) {
                    boolean _hasArray = hasArray;
                    clearFilter();
                    hasArray = _hasArray;
                    break;
                }
            } else if (instrFilterStrings[i].endsWith("**")) { // NOI18N
                instrFilterStrings[i] = instrFilterStrings[i].substring(0, instrFilterStrings[i].length() - 2);
                if (instrFilterTypes != null) instrFilterTypes[i] = FILTER_STARTS;
            } else if (instrFilterStrings[i].endsWith("*")) { // NOI18N
                instrFilterStrings[i] = instrFilterStrings[i].substring(0, instrFilterStrings[i].length() - 1);
                if (instrFilterTypes != null) instrFilterTypes[i] = FILTER_STARTS_EX;
            }
        }
    }

    /** Sets the filter type to INSTR_FILTER_NONE and clears filter strings. */
    public void clearFilter() {
        instrFilterType = INSTR_FILTER_NONE;
        instrFilterStrings = new String[0];
        instrFilterTypes = null;
        hasArray = false;
    }

    public Object clone() throws CloneNotSupportedException {
        InstrumentationFilter clone = (InstrumentationFilter) super.clone();
        
        clone.instrFilterStrings = (String[])Arrays.copyOf(instrFilterStrings, instrFilterStrings.length);
        clone.instrFilterUserStrings = (String[])Arrays.copyOf(instrFilterUserStrings, instrFilterUserStrings.length);
        clone.instrFilterTypes = instrFilterTypes == null ? null : Arrays.copyOf(instrFilterTypes, instrFilterTypes.length);
        clone.hasArray = hasArray;

        return clone;
    }

    public String debug() {
        StringBuffer filterStringsBuffer = new StringBuffer();

        switch (instrFilterType) {
            case INSTR_FILTER_NONE:
                filterStringsBuffer.append("  Filter type: None\n"); // NOI18N
                break;
            case INSTR_FILTER_EXCLUSIVE:
                filterStringsBuffer.append("  Filter type: Exclusive\n"); // NOI18N
                break;
            case INSTR_FILTER_INCLUSIVE:
                filterStringsBuffer.append("  Filter type: Inclusive\n"); // NOI18N
                break;
            case INSTR_FILTER_EXCLUSIVE_EXACT:
                filterStringsBuffer.append("  Filter type: Exclusive exact\n"); // NOI18N
                break;
            case INSTR_FILTER_INCLUSIVE_EXACT:
                filterStringsBuffer.append("  Filter type: Inclusive exact\n"); // NOI18N
                break;
        }

        filterStringsBuffer.append("  Filter value: "); // NOI18N

        for (int i = 0; i < instrFilterStrings.length; i++) {
            filterStringsBuffer.append(instrFilterStrings[i]);
            filterStringsBuffer.append(" "); // NOI18N
        }

        filterStringsBuffer.append("\n"); // NOI18N

        return filterStringsBuffer.toString();
    }

    public boolean acceptsArrays() {
        switch (instrFilterType) {
            case INSTR_FILTER_EXCLUSIVE:
            case INSTR_FILTER_INCLUSIVE:
                return true;
            case INSTR_FILTER_NONE:
//            case INSTR_FILTER_EXCLUSIVE_EXACT: // NOTE: not used by memory profiling
            case INSTR_FILTER_INCLUSIVE_EXACT:
                return hasArray;
        }
        return true;
    }
    
    public void debugDump() {
        System.err.println("----------------------------------"); // NOI18N
        System.err.println("Instrumentation filter debug dump:"); // NOI18N

        switch (instrFilterType) {
            case INSTR_FILTER_NONE:
                System.err.println("  Filter type: None"); // NOI18N
                break;
            case INSTR_FILTER_EXCLUSIVE:
                System.err.println("  Filter type: Exclusive"); // NOI18N
                break;
            case INSTR_FILTER_INCLUSIVE:
                System.err.println("  Filter type: Inclusive"); // NOI18N
                break;
            case INSTR_FILTER_EXCLUSIVE_EXACT:
                System.err.println("  Filter type: Exclusive exact"); // NOI18N
                break;
            case INSTR_FILTER_INCLUSIVE_EXACT:
                System.err.println("  Filter type: Inclusive exact"); // NOI18N
                break;
        }

        StringBuffer filterStringsBuffer = new StringBuffer();

        for (int i = 0; i < instrFilterStrings.length; i++) {
            filterStringsBuffer.append(instrFilterStrings[i]);
            filterStringsBuffer.append(" "); // NOI18N
        }

        System.err.println("  Filter strings: " + filterStringsBuffer.toString()); // NOI18N
    }

    /** Returns true if the given string passes the current filter */
    public boolean passesFilter(String string) {
        //if (string == null) return; // don't be paranoid:o)
        if (instrFilterType == INSTR_FILTER_NONE) {
            return true;
        }

        if (instrFilterStrings.length == 0) {
            return true;
        }

        boolean filterInclusive = (instrFilterType == INSTR_FILTER_INCLUSIVE || instrFilterType == INSTR_FILTER_INCLUSIVE_EXACT);

        for (int i = 0; i < instrFilterStrings.length; i++) {
            int filterType = instrFilterTypes == null ? instrFilterType : instrFilterTypes[i];
            if (matches(filterType, string, instrFilterStrings[i])) {
                return filterInclusive;
            }
        }
        return !filterInclusive;
    }

    private static boolean matches(int type, String string, String filter) {
        switch (type) {
            case INSTR_FILTER_EXCLUSIVE:
            case INSTR_FILTER_INCLUSIVE:
            case FILTER_STARTS:
                return string.startsWith(filter);
            case INSTR_FILTER_EXCLUSIVE_EXACT:
            case INSTR_FILTER_INCLUSIVE_EXACT:
            case FILTER_MATCHES:
                return string.equals(filter);
            case FILTER_STARTS_EX:
                if (!string.startsWith(filter)) return false;
                for (int i = filter.length(); i < string.length(); i++)
                    if (string.charAt(i) == '/') return false; // NOI18N
                return true;
            default:
                throw new IllegalArgumentException("Illegal filter type:"+type);
        }
    }
}
