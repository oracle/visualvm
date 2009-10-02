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


//import org.openide.util.NbBundle;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;


/**
 * This class represents one defined filterset and the functionality around
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class FilterSet {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.filters.Bundle"); // NOI18N
    public static final String DEFAULT_FILTERSET_NAME = bundle.getString("FilterSet_DefaultFilterSetName"); // NOI18N
                                                                                                            // -----
    public static final String PROP_FILTER_TYPE_VALUE = "profiler.filter.set"; //NOI18N
    public static final boolean FILTER_SET_EXCLUSIVE = false;
    public static final boolean FILTER_SET_INCLUSIVE = true;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String filterSetName;
    private String[] activeGlobalFilters;
    private boolean filterSetType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of FilterSet */
    public FilterSet() {
        this(DEFAULT_FILTERSET_NAME);
    }

    public FilterSet(final String name) {
        this(name, FILTER_SET_EXCLUSIVE, new String[0]);
    }

    public FilterSet(final String name, final boolean type, final String[] activeFilters) {
        setFilterSetName(name);
        setFilterSetType(type);
        activeGlobalFilters = activeFilters;
    }

    public FilterSet(final FilterSet filterSet) {
        setValuesFrom(filterSet);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getActiveGlobalFilterIndex(final String globalFilterName) {
        for (int i = 0; i < activeGlobalFilters.length; i++) {
            if (activeGlobalFilters[i].equals(globalFilterName)) {
                return i;
            }
        }

        return -1;
    }

    public void setActiveGlobalFilters(final String[] globalFilters) {
        if (globalFilters == null) {
            return;
        }

        activeGlobalFilters = globalFilters;
    }

    public String[] getActiveGlobalFilters() {
        return activeGlobalFilters;
    }

    public void setExclusive() {
        filterSetType = FILTER_SET_EXCLUSIVE;
    }

    public boolean isExclusive() {
        return filterSetType == FILTER_SET_EXCLUSIVE;
    }

    public void setFilterSetName(final String name) {
        if (name == null) {
            return;
        }

        filterSetName = name;
    }

    public String getFilterSetName() {
        return filterSetName;
    }

    public void setFilterSetType(final boolean type) {
        filterSetType = type;
    }

    public boolean getFilterSetType() {
        return filterSetType;
    }

    public void setInclusive() {
        filterSetType = FILTER_SET_INCLUSIVE;
    }

    public boolean isInclusive() {
        return filterSetType == FILTER_SET_INCLUSIVE;
    }

    public void setValuesFrom(final FilterSet filterSet) {
        if (filterSet == null) {
            return;
        }

        setFilterSetName(filterSet.getFilterSetName());
        setFilterSetType(filterSet.getFilterSetType());

        final int nGlobalFilters = filterSet.getActiveGlobalFilters().length;
        activeGlobalFilters = new String[nGlobalFilters];

        for (int i = 0; i < nGlobalFilters; i++) {
            activeGlobalFilters[i] = filterSet.getActiveGlobalFilters()[i];
        }
    }

    public void addActiveGlobalFilter(final String globalFilterName) {
        final int nCurrentFilters = activeGlobalFilters.length;
        final String[] newFilters = new String[nCurrentFilters + 1];
        System.arraycopy(activeGlobalFilters, 0, newFilters, 0, nCurrentFilters);
        newFilters[nCurrentFilters] = globalFilterName;
        activeGlobalFilters = newFilters;
    }

    public void clear() {
        setFilterSetName(DEFAULT_FILTERSET_NAME);
        setFilterSetType(FILTER_SET_EXCLUSIVE);
        activeGlobalFilters = new String[0];
    }

    public boolean containsActiveGlobalFilter(final String globalFilterName) {
        return (getActiveGlobalFilterIndex(globalFilterName) != -1);
    }

    public String debug() {
        final StringBuffer sb = new StringBuffer();

        sb.append("FilterSet name: " + filterSetName); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("  Type: " + ((filterSetType == FILTER_SET_EXCLUSIVE) ? "Exclusive" : "Inclusive")); //NOI18N
        sb.append("\n"); //NOI18N

        for (int i = 0; i < activeGlobalFilters.length; i++) {
            sb.append("  GlobalFilter " + i + ": " + activeGlobalFilters[i]); //NOI18N
            sb.append("\n"); //NOI18N
        }

        return sb.toString();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof FilterSet)) {
            return false;
        }

        FilterSet filterSet = (FilterSet) o;

        if (!filterSetName.equals(filterSet.filterSetName)) {
            return false;
        }

        if (filterSetType != filterSet.filterSetType) {
            return false;
        }

        if (activeGlobalFilters.length != filterSet.activeGlobalFilters.length) {
            return false;
        }

        for (int i = 0; i < activeGlobalFilters.length; i++) {
            if (!activeGlobalFilters[i].equals(filterSet.activeGlobalFilters[i])) {
                return false;
            }
        }

        return true;
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public FilterSet load(final Map props, final int index) {
        return load(props, index, ""); //NOI18N
    }

    public FilterSet load(final Map props, final String prefix) {
        if (!getProperty(props, prefix + FilterUtils.PROP_FILTER_TYPE, "").equals(PROP_FILTER_TYPE_VALUE)) {
            throw new RuntimeException("Trying to load incompatible filter"); //NOI18N
        }

        return load(props, -1, prefix);
    }

    public FilterSet load(final Map props, final int index, final String prefix) {
        final String itemPrefix = "FilterSet-" + ((index == -1) ? "" : (new Integer(index).toString() + "-")); //NOI18N

        filterSetName = getProperty(props, prefix + itemPrefix + "name", DEFAULT_FILTERSET_NAME); //NOI18N
        filterSetType = Boolean.valueOf(getProperty(props, prefix + itemPrefix + "type", "false")).booleanValue(); //NOI18N

        final Vector activeFilters = new Vector();
        String activeFilterName;

        while ((activeFilterName = (String) props.get(prefix + itemPrefix + "active_filter-" + activeFilters.size())) != null) { //NOI18N
            activeFilters.add(activeFilterName);
        }

        activeGlobalFilters = new String[activeFilters.size()];

        for (int i = 0; i < activeFilters.size(); i++) {
            activeGlobalFilters[i] = (String) activeFilters.get(i);
        }

        return this;
    }

    public void removeActiveGlobalFilter(final String globalFilterName) {
        final int filterIndex = getActiveGlobalFilterIndex(globalFilterName);

        if (filterIndex == -1) {
            return;
        }

        final int nCurrentFilters = activeGlobalFilters.length;
        final String[] newFilters = new String[nCurrentFilters - 1];

        if (filterIndex > 0) {
            System.arraycopy(activeGlobalFilters, 0, newFilters, 0, filterIndex);
        }

        if (filterIndex < (nCurrentFilters - 1)) {
            System.arraycopy(activeGlobalFilters, filterIndex + 1, newFilters, filterIndex, nCurrentFilters - filterIndex - 1);
        }

        activeGlobalFilters = newFilters;
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void store(final Map props, final int index) {
        store(props, index, ""); //NOI18N
    }

    public void store(final Map props, final String prefix) {
        props.put(prefix + FilterUtils.PROP_FILTER_TYPE, PROP_FILTER_TYPE_VALUE);
        store(props, -1, prefix);
    }

    public void store(final Map props, final int index, final String prefix) {
        final String itemPrefix = "FilterSet-" + ((index == -1) ? "" : (new Integer(index).toString() + "-")); //NOI18N
        props.put(prefix + itemPrefix + "name", filterSetName); //NOI18N
        props.put(prefix + itemPrefix + "type", Boolean.toString(filterSetType)); //NOI18N

        for (int i = 0; i < activeGlobalFilters.length; i++) {
            props.put(prefix + itemPrefix + "active_filter-" + i, activeGlobalFilters[i]); //NOI18N
        }
    }

    public String toString() {
        return getFilterSetName();
    }

    private static String getProperty(final Map props, final Object key, final String defaultValue) {
        final Object ret = props.get(key);

        return (ret != null) ? (String) ret : defaultValue;
    }
}
