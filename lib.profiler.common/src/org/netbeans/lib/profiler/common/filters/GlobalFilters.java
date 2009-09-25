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
import java.util.Vector;


/**
 * This class encapsulates globally defined filters (name-value pairs) and the functionality around
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class GlobalFilters {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String[] filterNames;
    private String[] filterValues;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of GlobalFilters */
    public GlobalFilters() {
        clear();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getFilterIndex(final String filterName) {
        for (int i = 0; i < filterNames.length; i++) {
            if (filterNames[i].equals(filterName)) {
                return i;
            }
        }

        return -1;
    }

    public void setFilterNames(final String[] names) {
        if (names == null) {
            return;
        }

        filterNames = names;
    }

    public String[] getFilterNames() {
        return filterNames;
    }

    public String getFilterValue(final String filterName) {
        final int filterIndex = getFilterIndex(filterName);

        if (filterIndex == -1) {
            return null;
        }

        return filterValues[filterIndex];
    }

    public void setFilterValues(final String[] values) {
        if (values == null) {
            return;
        }

        filterValues = values;
    }

    public String[] getFilterValues() {
        return filterValues;
    }

    public void clear() {
        initBuffers(0);
    }

    public void load(final Map props) {
        final Vector filterNamesVect = new Vector();
        final Vector filterValuesVect = new Vector();

        String filterName;

        while ((filterName = (String) props.get("Filter-" + filterNamesVect.size() + "-name")) != null) { //NOI18N
            filterNamesVect.add(filterName);
            filterValuesVect.add(getProperty(props, "Filter-" + filterValuesVect.size() + "-value", "")); //NOI18N
        }

        filterNames = new String[filterNamesVect.size()];
        filterValues = new String[filterValuesVect.size()];

        for (int i = 0; i < filterNamesVect.size(); i++) {
            filterNames[i] = (String) filterNamesVect.get(i);
            filterValues[i] = (String) filterValuesVect.get(i);
        }
    }

    public void store(final Map props) {
        String itemPrefix;

        for (int i = 0; i < filterNames.length; i++) {
            itemPrefix = "Filter-" + i + "-"; //NOI18N
            props.put(itemPrefix + "name", filterNames[i]); //NOI18N
            props.put(itemPrefix + "value", filterValues[i]); //NOI18N
        }
    }

    private static String getProperty(final Map props, final Object key, final String defaultValue) {
        final Object ret = props.get(key);

        return (ret != null) ? (String) ret : defaultValue;
    }

    private void initBuffers(final int capacity) {
        filterNames = new String[capacity];
        filterValues = new String[capacity];
    }
}
