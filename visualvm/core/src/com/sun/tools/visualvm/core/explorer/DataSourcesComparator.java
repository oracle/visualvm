/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.core.explorer;

import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptor;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.Positionable;
import java.util.Comparator;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class DataSourcesComparator implements Comparator<DataSource> {

    public int compare(DataSource d1, DataSource d2) {
        DataSourceDescriptor dd1 = DataSourceDescriptorFactory.getDescriptor(d1);
        DataSourceDescriptor dd2 = DataSourceDescriptorFactory.getDescriptor(d2);

        int p1 = dd1.getPreferredPosition();
        int p2 = dd2.getPreferredPosition();

        if (p1 == Positionable.POSITION_AT_THE_END &&
            p2 == Positionable.POSITION_AT_THE_END) {
            p1 = getRelativePosition(d1, Positionable.POSITION_AT_THE_END);
            p2 = getRelativePosition(d2, Positionable.POSITION_AT_THE_END);
        } else if (p1 == Positionable.POSITION_LAST &&
                   p2 == Positionable.POSITION_LAST) {
            p1 = getRelativePosition(d1, Positionable.POSITION_LAST);
            p2 = getRelativePosition(d2, Positionable.POSITION_LAST);
        }

        int result = doCompare(p1, p2);
        if (result == 0) result = dd1.getName().compareTo(dd2.getName());
        return result;
    }

    protected abstract int getRelativePosition(DataSource d, int positionType);

    private int doCompare(int i1, int i2) {
        if (i1 == i2) return 0;
        if (i1 > i2) return 1;
        return -1;
    }

}
