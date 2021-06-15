/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results;

import java.util.ResourceBundle;
import org.graalvm.visualvm.lib.jfluid.global.CommonConstants;

/**
 *
 * @author Jiri Sedlacek
 */
public final class FilterSortSupport implements CommonConstants {

    public static final String FILTERED_OUT_LBL;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.results.Bundle"); // NOI18N
        FILTERED_OUT_LBL = messages.getString("FilteringSupport_FilteredOutLbl"); //NOI18N
    }

    public static boolean passesFilter(Configuration info, String nodeName) {
        return passesFilter(info.getFilterString(), info.getFilterType(), nodeName);
    }

    public static boolean passesFilter(String filter, int filterType, String nodeName) {
        switch (filterType) {
            case FILTER_NONE:
                return true;
            case FILTER_CONTAINS:
                return nodeName.toLowerCase().contains(filter);
            case FILTER_NOT_CONTAINS:
                return !nodeName.toLowerCase().contains(filter);
            case FILTER_REGEXP:
                try {
                    return nodeName.matches(filter); // case sensitive!
                } catch (java.util.regex.PatternSyntaxException e) {
                    return false;
                }
        }
        return false;
    }


    public static final class Configuration {

        private int sortBy;
        private boolean sortOrder;
        private String filterString = ""; // NOI18N
        private int filterType = CommonConstants.FILTER_CONTAINS;


        public int getSortBy() {
            return sortBy;
        }

        public boolean getSortOrder() {
            return sortOrder;
        }

        public String getFilterString() {
            return filterString;
        }

        public int getFilterType() {
           return filterType;
        }
        
        
        public void setSortInfo(int sortBy, boolean sortOrder) {
            this.sortBy = sortBy;
            this.sortOrder = sortOrder;
        }
        
        public void setFilterInfo(String filterString, int filterType) {
            this.filterString = filterString;
            this.filterType = filterType;
        }
        
    }
   
}
