/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
    private static InstrumentationFilter defaultInstance;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String[] instrFilterStrings;
    private int instrFilterType;

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
        flatFilterString = flatFilterString.replace('.', '/'); // NOI18N // create slashed filterStrings

        setSlashedFilterStrings(flatFilterString.trim().split(" +")); // NOI18N
    }

    /** Gets the current slashed classnames to be filtered. */
    public String[] getFilterStrings() {
        return instrFilterStrings;
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

        // remove trailing '*'
        String instrFilterString;

        for (int i = 0; i < instrFilterStrings.length; i++) {
            instrFilterString = instrFilterStrings[i];

            if (instrFilterString.endsWith("*")) {
                instrFilterStrings[i] = instrFilterString.substring(0, instrFilterString.length() - 1); // NOI18N
            }
        }
    }

    /** Sets the filter type to INSTR_FILTER_NONE and clears filter strings. */
    public void clearFilter() {
        instrFilterType = INSTR_FILTER_NONE;
        instrFilterStrings = new String[0];
    }

    public Object clone() throws CloneNotSupportedException {
        InstrumentationFilter clone = (InstrumentationFilter) super.clone();
        clone.instrFilterStrings = new String[instrFilterStrings.length];

        for (int i = 0; i < instrFilterStrings.length; i++) {
            clone.instrFilterStrings[i] = instrFilterStrings[i];
        }

        return clone;
    }

    public String debug() {
        StringBuffer filterStringsBuffer = new StringBuffer();

        switch (instrFilterType) {
            case INSTR_FILTER_NONE:
                filterStringsBuffer.append("  Filter type: None\n");

                break; // NOI18N
            case INSTR_FILTER_EXCLUSIVE:
                filterStringsBuffer.append("  Filter type: Exclusive\n");

                break; // NOI18N
            case INSTR_FILTER_INCLUSIVE:
                filterStringsBuffer.append("  Filter type: Inclusive\n");

                break; // NOI18N
        }

        filterStringsBuffer.append("  Filter value: "); // NOI18N

        for (int i = 0; i < instrFilterStrings.length; i++) {
            filterStringsBuffer.append(instrFilterStrings[i]);
            filterStringsBuffer.append(" "); // NOI18N
        }

        filterStringsBuffer.append("\n"); // NOI18N

        return filterStringsBuffer.toString();
    }

    public void debugDump() {
        System.err.println("----------------------------------"); // NOI18N
        System.err.println("Instrumentation filter debug dump:"); // NOI18N

        switch (instrFilterType) {
            case INSTR_FILTER_NONE:
                System.err.println("  Filter type: None");

                break; // NOI18N
            case INSTR_FILTER_EXCLUSIVE:
                System.err.println("  Filter type: Exclusive");

                break; // NOI18N
            case INSTR_FILTER_INCLUSIVE:
                System.err.println("  Filter type: Inclusive");

                break; // NOI18N
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

        boolean filterInclusive = (instrFilterType == INSTR_FILTER_INCLUSIVE);

        if (filterInclusive) {
            for (int i = 0; i < instrFilterStrings.length; i++) {
                if (matches(string, instrFilterStrings[i])) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < instrFilterStrings.length; i++) {
                if (matches(string, instrFilterStrings[i])) {
                    return false;
                }
            }
        }

        if (filterInclusive) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean matches(String string, String filter) {
        return string.startsWith(filter);
    }
}
