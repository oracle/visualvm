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

package org.netbeans.lib.profiler.common.filters;

import java.util.Map;
import java.util.ResourceBundle;


/**
 * This class provides methods related to processing filters
 *
 * @author Tomas Hurka
 * @author Jiri Sedlacek
 */
public class FilterUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.filters.Bundle"); // NOI18N
    private static final String PROFILE_ALL_CLASSES_FILTER_NAME = bundle.getString("FilterUtils_ProfileAllClassesFilterName"); //NOI18N
    private static final String QUICK_FILTER_FILTER_NAME = bundle.getString("FilterUtils_QuickFilterFilterName"); //NOI18N
                                                                                                                  // -----
    public static final String PROP_FILTER_TYPE = "profiler.filter.type"; //NOI18N
    public static final SimpleFilter NONE_FILTER = new SimpleFilter(PROFILE_ALL_CLASSES_FILTER_NAME,
                                                                    SimpleFilter.SIMPLE_FILTER_NONE, ""); // NOI18N
    public static final SimpleFilter QUICK_FILTER = new SimpleFilter(QUICK_FILTER_FILTER_NAME,
                                                                     SimpleFilter.SIMPLE_FILTER_EXCLUSIVE, ""); // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /** Breaks set of filters into an String array of separate filter values */
    public static String[] getSeparateFilters(final String complexFilter) {
        return complexFilter.replace(',', ' ').trim().split(" +"); //NOI18N // filterStrings can be separated by comma and/or space
    }

    /** Tests if given filter value is a valid Java identifier containing '.' and/or ending by '.', '*' or '.*' */
    public static boolean isValidProfilerFilter(final String filterPart) {
        final int filterPartLength = filterPart.length();

        if (filterPartLength < 1) {
            return true; // Zero-length filter value
        }

        if (filterPart.indexOf("..") != -1) {
            return false; //NOI18N // Multiple dots (..)
        }

        final char[] c = new char[filterPartLength];
        filterPart.getChars(0, filterPart.length(), c, 0);

        if (!Character.isJavaIdentifierStart(c[0])) {
            return false; // Incorrect first letter
        }

        for (int i = 1; i < (filterPartLength - 1); ++i) {
            if ((!Character.isJavaIdentifierPart(c[i])) && (!(c[i] == '.'))) {
                return false; //NOI18N // Incorrect other letter
            }
        }

        if ((!Character.isJavaIdentifierPart(c[filterPartLength - 1])) && (!(c[filterPartLength - 1] == '.'))
                && (!(c[filterPartLength - 1] == '*'))) {
            return false; //NOI18N // Incorrect last letter
        }

        return true;
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public static Object loadFilter(Map props) {
        return loadFilter(props, ""); //NOI18N
    }

    public static Object loadFilter(Map props, String prefix) {
        Object filterType = props.get(prefix + PROP_FILTER_TYPE);

        if ((props == null) || (filterType == null)) {
            return null;
        }

        // Load SimpleFilter
        if (filterType.equals(SimpleFilter.PROP_FILTER_TYPE_VALUE)) {
            SimpleFilter filter = new SimpleFilter();
            filter.load(props, prefix);

            return filter;
        }

        // Load FilterSet
        if (filterType.equals(FilterSet.PROP_FILTER_TYPE_VALUE)) {
            FilterSet filter = new FilterSet();
            filter.load(props, prefix);

            return filter;
        }

        // No supported filter found
        return null;
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public static void storeFilter(Map props, Object filter) {
        storeFilter(props, filter, ""); //NOI18N
    }

    public static void storeFilter(Map props, Object filter, String prefix) {
        // null parameters
        if (props == null) {
            throw new RuntimeException("Cannot store a filter to null properties"); //NOI18N
        }

        if (filter == null) {
            throw new RuntimeException("Cannot store null filter"); //NOI18N
        }

        // Store SimpleFilter
        if (filter instanceof SimpleFilter) {
            ((SimpleFilter) filter).store(props, prefix);

            return;
        }

        // Store FilterSet
        if (filter instanceof FilterSet) {
            ((FilterSet) filter).store(props, prefix);

            return;
        }

        // Unsupported filter
        throw new RuntimeException("Unsupported filter type: " + filter.getClass().getName()); //NOI18N
    }
}
