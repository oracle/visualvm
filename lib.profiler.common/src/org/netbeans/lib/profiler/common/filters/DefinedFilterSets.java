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
 *
 * @author Tomas Hurka
 * @author  Jiri Sedlacek
 */
public final class DefinedFilterSets {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Vector definedFilterSets;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of DefinedFilterSets */
    public DefinedFilterSets() {
        definedFilterSets = new Vector();
    }

    public DefinedFilterSets(final DefinedFilterSets filterSets) {
        this();
        setValuesFrom(filterSets);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public FilterSet getFilterSet(final String filterSetName) {
        final int filterSetIndex = getFilterSetIndex(filterSetName);

        if (filterSetIndex == -1) {
            return null;
        }

        return getFilterSetAt(filterSetIndex);
    }

    public FilterSet getFilterSetAt(final int index) {
        return (FilterSet) definedFilterSets.get(index);
    }

    public int getFilterSetIndex(final String filterSetName) {
        for (int i = 0; i < definedFilterSets.size(); i++) {
            if (getFilterSetAt(i).getFilterSetName().equals(filterSetName)) {
                return i;
            }
        }

        return -1;
    }

    public int getFilterSetsCount() {
        return definedFilterSets.size();
    }

    public void setValuesFrom(final DefinedFilterSets filterSets) {
        definedFilterSets.clear();

        for (int i = 0; i < filterSets.getFilterSetsCount(); i++) {
            definedFilterSets.add(new FilterSet(filterSets.getFilterSetAt(i)));
        }
    }

    public void addFilterSet(final FilterSet filterSet) {
        if (filterSet == null) {
            return;
        }

        definedFilterSets.add(filterSet);
    }

    public void clear() {
        definedFilterSets.clear();
    }

    public String debug() {
        final StringBuffer sb = new StringBuffer();

        sb.append("DefinedFilterSets:"); //NOI18N
        sb.append("\n"); //NOI18N

        for (int i = 0; i < definedFilterSets.size(); i++) {
            sb.append(((FilterSet) definedFilterSets.get(i)).debug());
            sb.append("\n"); //NOI18N
        }

        return sb.toString();
    }

    public DefinedFilterSets load(final Map props) {
        clear();

        int index = 0;

        while (props.get("FilterSet-" + index + "-name") != null) { //NOI18N
            addFilterSet(new FilterSet().load(props, index));
            index++;
        }

        return this;
    }

    public void moveFilterSetDown(final int index) {
        if (index > (definedFilterSets.size() - 2)) {
            return;
        }

        final FilterSet filterSet = (FilterSet) definedFilterSets.remove(index);
        definedFilterSets.add(index + 1, filterSet);
    }

    public void moveFilterSetUp(final int index) {
        if (index < 1) {
            return;
        }

        final FilterSet filterSet = (FilterSet) definedFilterSets.remove(index);
        definedFilterSets.add(index - 1, filterSet);
    }

    public void removeFilterSet(final FilterSet filterSet) {
        if (filterSet == null) {
            return;
        }

        definedFilterSets.remove(filterSet);
    }

    public void removeFilterSet(final int index) {
        definedFilterSets.remove(index);
    }

    public void store(final Map props) {
        for (int i = 0; i < definedFilterSets.size(); i++) {
            ((FilterSet) definedFilterSets.get(i)).store(props, i);
        }
    }
}
