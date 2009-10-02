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


/**
 * This class defines simple instrumentation filter without referencing
 * Global Filters, i.e. for QuickFilter manipulation
 *
 * @author Jiri Sedlacek
 */
public class SimpleFilter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int SIMPLE_FILTER_NONE = 0;
    public static final int SIMPLE_FILTER_EXCLUSIVE = 1;
    public static final int SIMPLE_FILTER_INCLUSIVE = 2;
    public static final SimpleFilter NO_FILTER = new SimpleFilter("", SIMPLE_FILTER_NONE, ""); //NOI18N
    public static final String PROP_FILTER_TYPE_VALUE = "profiler.simple.filter"; //NOI18N
    private static final String PROP_SIMPLEFILTER_NAME = "profiler.simple.filter.name"; //NOI18N
    private static final String PROP_SIMPLEFILTER_TYPE = "profiler.simple.filter.type"; //NOI18N
    private static final String PROP_SIMPLEFILTER_VALUE = "profiler.simple.filter.value"; //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String filterName;
    private String filterValue;
    private int filterType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SimpleFilter() {
        setFilterName(""); // NOI18N
        setFilterType(SIMPLE_FILTER_NONE);
        setFilterValue(""); // NOI18N
    }

    public SimpleFilter(String filterName, int filterType, String filterValue) {
        setFilterName(filterName);
        setFilterType(filterType);
        setFilterValue(filterValue);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public String getFilterName() {
        return filterName;
    }

    public void setFilterType(int filterType) {
        this.filterType = filterType;
    }

    public int getFilterType() {
        return filterType;
    }

    public void setFilterValue(String filterValue) {
        this.filterValue = filterValue;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof SimpleFilter)) {
            return false;
        }

        SimpleFilter simpleFilter = (SimpleFilter) o;

        if (!filterName.equals(simpleFilter.filterName)) {
            return false;
        }

        if (filterType != simpleFilter.filterType) {
            return false;
        }

        if (!filterValue.equals(simpleFilter.filterValue)) {
            return false;
        }

        return true;
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void load(final Map props) {
        load(props, ""); //NOI18N
    }

    public void load(final Map props, final String prefix) {
        if (!getProperty(props, prefix + FilterUtils.PROP_FILTER_TYPE, "").equals(PROP_FILTER_TYPE_VALUE)) {
            throw new RuntimeException("Trying to load incompatible filter"); //NOI18N
        }

        setFilterName(getProperty(props, prefix + PROP_SIMPLEFILTER_NAME, "")); //NOI18N
        setFilterType(Integer.parseInt(getProperty(props, prefix + PROP_SIMPLEFILTER_TYPE, Integer.toString(SIMPLE_FILTER_NONE))));
        setFilterValue(getProperty(props, prefix + PROP_SIMPLEFILTER_VALUE, "")); //NOI18N
    }

    // TODO: just to keep backward compatibility, should be removed after code cleanup!!!
    public void store(final Map props) {
        store(props, ""); //NOI18N
    }

    public void store(final Map props, final String prefix) {
        props.put(prefix + FilterUtils.PROP_FILTER_TYPE, PROP_FILTER_TYPE_VALUE);
        props.put(prefix + PROP_SIMPLEFILTER_NAME, getFilterName());
        props.put(prefix + PROP_SIMPLEFILTER_TYPE, Integer.toString(getFilterType()));
        props.put(prefix + PROP_SIMPLEFILTER_VALUE, getFilterValue());
    }

    public String toString() {
        return filterName;
    }

    private static String getProperty(final Map props, final Object key, final String defaultValue) {
        final Object ret = props.get(key);

        return (ret != null) ? (String) ret : defaultValue;
    }
}
