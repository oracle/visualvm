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

package org.netbeans.lib.profiler.results.cpu;

import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.utils.IntSorter;
import org.netbeans.lib.profiler.utils.LongSorter;
import org.netbeans.lib.profiler.utils.StringSorter;


/**
 * Container for CPU profiling results in the flat profile form. Supports sorting this
 * data by each column and filtering it as many times as needed (only the external representation
 * is changed in that case; internally data remains the same). This class is an abstract superclass
 * of concrete subclasses in which the data is either backed by CPUCCTContainer or not.
 *
 * @author Misha Dmitriev
 * @author Jiri Sedlacek
 */
public abstract class FlatProfileContainer {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_TIME = 2;
    public static final int SORT_BY_SECONDARY_TIME = 3;
    public static final int SORT_BY_INV_NUMBER = 4;
    public static final int SORT_BY_TOTAL_TIME = 5;
    public static final int SORT_BY_SECONDARY_TOTAL_TIME = 6;

    // This variable is used to remember the timestamp (absolute or thread-CPU) used to calculate percentage
    // numbers, between invocations of "Get results", i.e. creations of new objects of this class.
    protected static boolean staticUsePrimaryTime;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected int[] methodIds;
    protected final char[] methodMarks;
    protected int[] nInvocations;
    protected float[] percent;
    protected long[] timeInMcs0;
    protected long[] timeInMcs1;
    protected long[] totalTimeInMcs0;
    protected long[] totalTimeInMcs1;
    protected boolean collectingTwoTimeStamps;
    protected long nTotalInvocations;
    protected int nRows; // Number of methods currently displayed
                       // nRows may be < totalMethods due to user setting up a filter for flat profile data
    private int totalMethods; // Number of methods with non-zero number of invocations

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public FlatProfileContainer(long[] timeInMcs0, long[] timeInMcs1, long[] totalTimeInMcs0, long[] totalTimeInMcs1,
            int[] nInvocations, char[] marks, int nMethods) {
        this.timeInMcs0 = timeInMcs0;
        this.timeInMcs1 = timeInMcs1;
        this.totalTimeInMcs0 = totalTimeInMcs0;
        this.totalTimeInMcs1 = totalTimeInMcs1;
        this.nInvocations = nInvocations;
        this.methodMarks = marks;
        totalMethods = nMethods;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public boolean isCollectingTwoTimeStamps() {
        return collectingTwoTimeStamps;
    }

    public int getMethodIdAtRow(int row) {
        return methodIds[row];
    }

    public abstract String getMethodNameAtRow(int row);
    
    public abstract ClientUtils.SourceCodeSelection getSourceCodeSelectionAtRow(int row);

    public int getNInvocationsAtRow(int row) {
        return nInvocations[row];
    }

    public int getNRows() {
        return nRows;
    }

    public long getNTotalInvocations() {
        return nTotalInvocations;
    }

    public float getPercentAtRow(int row) {
        return percent[row];
    }

    public long getTimeInMcs0AtRow(int row) {
        return timeInMcs0[row];
    }

    public long getTimeInMcs1AtRow(int row) {
        return timeInMcs1[row];
    }

    public long getTotalTimeInMcs0AtRow(int row) {
        return totalTimeInMcs0[row];
    }

    public long getTotalTimeInMcs1AtRow(int row) {
        return totalTimeInMcs1[row];
    }

    public abstract double getWholeGraphNetTime0();

    public abstract double getWholeGraphNetTime1();

    public void filterOriginalData(String[] filters, int type, double valueFilter) {
//        //    percent = null;
//        if (((type == CommonConstants.FILTER_NONE) || (filters == null) || filters[0].isEmpty()) && (valueFilter == 0.0d)) { // NOI18N
//            nRows = totalMethods; // Effectively removes all filtering
//
//            return;
//        }
//
//        // Now go through all methods and move those that don't pass filter to the end of the array
//        nRows = totalMethods;
//
//        for (int i = 0; i < nRows; i++) {
//            if (!passedFilters(getMethodNameAtRow(i), filters, type)/*|| ! passedValueFilter (getPercentAtRow(i), valueFilter) */ ) {
//                int endIdx = --nRows;
//
//                if (i >= endIdx) {
//                    continue;
//                }
//
//                // Swap the current element and the one at (nRows - 1) index
//                swap(i, endIdx);
//                
//                swap(methodIds,i,endIdx);
//                swap(timeInMcs0,i,endIdx);
//                swap(totalTimeInMcs0,i,endIdx);
//
//                if (collectingTwoTimeStamps) {
//                    swap(timeInMcs1,i,endIdx);
//                    swap(totalTimeInMcs1,i,endIdx);
//                }
//
//                swap(nInvocations,i,endIdx);
//                i--; // Because we've just put an unchecked element at the current position
//            }
//        }
    }

    public void sortBy(int sortCrit, boolean order) {
//        switch (sortCrit) {
//            case SORT_BY_NAME:
//                sortDataByMethodName(order);
//
//                break;
//            case SORT_BY_TIME:
//                sortDataByTime(true, order);
//
//                break;
//            case SORT_BY_SECONDARY_TIME:
//                sortDataByTime(false, order);
//
//                break;
//            case SORT_BY_TOTAL_TIME:
//                sortDataByTotalTime(true, order);
//
//                break;
//            case SORT_BY_SECONDARY_TOTAL_TIME:
//                sortDataByTotalTime(false, order);
//
//                break;
//            case SORT_BY_INV_NUMBER:
//                sortDataByInvNumber(order);
//
//                break;
//        }
    }

    protected void removeZeroInvocationEntries() {
        nRows = 0;

        // Note that at index 0 we always have a "Thread" quazi-method, that we shouldn't take into account
        for (int i = 1; i < totalMethods; i++) {
            if (nInvocations[i] > 0) {
                nRows++;
            }
        }

        long[] oldTime0 = timeInMcs0;
        long[] oldTime1 = timeInMcs1;
        long[] oldTotalTime0 = totalTimeInMcs0;
        long[] oldTotalTime1 = totalTimeInMcs1;
        int[] oldNInvocations = nInvocations;

        timeInMcs0 = new long[nRows];
        totalTimeInMcs0 = new long[nRows];

        if (collectingTwoTimeStamps) {
            timeInMcs1 = new long[nRows];
            totalTimeInMcs1 = new long[nRows];
        }

        nInvocations = new int[nRows];
        methodIds = new int[nRows];

        int k = 0;

        for (int i = 1; i < totalMethods; i++) {
            if (oldNInvocations[i] > 0) {
                long time = oldTime0[i];
                if (time < 0) {
                    time = 0; // Replace possible negative time entries with 0
                }
                timeInMcs0[k] = time;

                time = oldTotalTime0[i];
                if (time < 0) {
                    time = 0;
                }
                totalTimeInMcs0[k] = time;

                if (collectingTwoTimeStamps) {
                    time = oldTime1[i];
                    if (time < 0) {
                        time = 0;
                    }
                    timeInMcs1[k] = time;

                    time = oldTotalTime1[i];
                    if (time < 0) {
                        time = 0;
                    }
                    totalTimeInMcs1[k] = time;
                }

                nInvocations[k] = oldNInvocations[i];
                nTotalInvocations += oldNInvocations[i];
                methodIds[k] = i;
                k++;
            }
        }

        totalMethods = nRows;
    }

    private void calculatePercent(boolean usePrimaryTime) {
        percent = new float[nRows];

        double wholeNetTime = getWholeGraphNetTime0();
        long[] tpm = timeInMcs0;

        if (collectingTwoTimeStamps && (!usePrimaryTime)) {
            wholeNetTime = getWholeGraphNetTime1();
            tpm = timeInMcs1;
        }

        for (int i = 0; i < nRows; i++) {
            percent[i] = (float) ((wholeNetTime > 0) ? ((double) tpm[i] / wholeNetTime * 100) : 0);
        }

        staticUsePrimaryTime = usePrimaryTime;
    }

    private boolean passedFilter(String value, String filter, int type) {
//        // Case sensitive comparison:
//        /*switch (type) {
//           case CommonConstants.FILTER_STARTS_WITH:
//             return value.startsWith(filter);
//           case CommonConstants.FILTER_CONTAINS:
//             return value.indexOf(filter) != -1;
//           case CommonConstants.FILTER_ENDS_WITH:
//             return value.endsWith(filter);
//           case CommonConstants.FILTER_EQUALS:
//             return value.equals(filter);
//           case CommonConstants.FILTER_REGEXP:
//             return value.matches(filter);
//           }*/
//
//        // Case insensitive comparison (except regexp):
//        switch (type) {
////            case CommonConstants.FILTER_STARTS_WITH:
////                return value.regionMatches(true, 0, filter, 0, filter.length()); // case insensitive startsWith, optimized
//            case CommonConstants.FILTER_CONTAINS:
//                return value.toLowerCase().contains(filter); // case insensitive indexOf, NOT OPTIMIZED
//            case CommonConstants.FILTER_NOT_CONTAINS:
//                return !value.toLowerCase().contains(filter);
////            case CommonConstants.FILTER_ENDS_WITH:
////                // case insensitive endsWith, optimized
////                return value.regionMatches(true, value.length() - filter.length(), filter, 0, filter.length());
////            case CommonConstants.FILTER_EQUALS:
////                return value.equalsIgnoreCase(filter); // case insensitive equals
//            case CommonConstants.FILTER_REGEXP:
//                try {
//                    return value.matches(filter); //  case sensitive!
//                } catch (java.util.regex.PatternSyntaxException e) {
//                    return false;
//                }
//        }

        return false;
    }

//    private boolean passedFilters(String value, String[] filters, int type) {
//        for (int i = 0; i < filters.length; i++) {
//            if (passedFilter(value, filters[i], type)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    private void sortDataByInvNumber(boolean sortOrder) {
//        if ((percent == null) || (percent.length != nRows)) {
//            calculatePercent(staticUsePrimaryTime);
//        }
//
//        (new IntSorter(nInvocations, 0, nRows) {
//                protected void swap(int a, int b) {
//                    super.swap(a, b);
//                    FlatProfileContainer.this.swap(a, b);
//
//                    FlatProfileContainer.swap(timeInMcs0,a,b);
//                    FlatProfileContainer.swap(totalTimeInMcs0,a,b);
//
//                    if (collectingTwoTimeStamps) {
//                        FlatProfileContainer.swap(timeInMcs1,a,b);
//                        FlatProfileContainer.swap(totalTimeInMcs1,a,b);
//                    }
//
//                    FlatProfileContainer.swap(methodIds,a,b);
//
//                    if (percent != null) {
//                        FlatProfileContainer.swap(percent,a,b);
//                    }
//                }
//            }).sort(sortOrder);
//    }
//
//    private void sortDataByMethodName(boolean sortOrder) {
//        String[] fullMethodNames = new String[nRows];
//
//        if ((percent == null) || (percent.length != nRows)) {
//            calculatePercent(staticUsePrimaryTime);
//        }
//
//        for (int i = 0; i < nRows; i++) {
//            fullMethodNames[i] = getMethodNameAtRow(i);
//        }
//
//        (new StringSorter(fullMethodNames, 0, nRows) {
//                protected void swap(int a, int b) {
//                    super.swap(a, b);
//                    FlatProfileContainer.this.swap(a, b);
//
//                    FlatProfileContainer.swap(timeInMcs0,a,b);
//                    FlatProfileContainer.swap(totalTimeInMcs0,a,b);
//
//                    if (collectingTwoTimeStamps) {
//                        FlatProfileContainer.swap(timeInMcs1,a,b);
//                        FlatProfileContainer.swap(totalTimeInMcs1,a,b);
//                    }
//
//                    FlatProfileContainer.swap(methodIds,a,b);
//                    FlatProfileContainer.swap(nInvocations,a,b);
//
//                    if (percent != null) {
//                        FlatProfileContainer.swap(percent,a,b);
//                    }
//                }
//            }).sort(sortOrder);
//    }
//
//    private void sortDataByTime(boolean usePrimaryTime, boolean sortOrder) {
//        long[] tpmA = null;
//        long[] tpmB = null;
//
//        // Percentage is recalculated every time, since it depends on whether primary/secondary time is used
//        if ((percent == null) || (usePrimaryTime != staticUsePrimaryTime) || (percent.length != nRows)) {
//            calculatePercent(usePrimaryTime);
//        }
//
//        if (collectingTwoTimeStamps) {
//            if (usePrimaryTime) {
//                tpmA = timeInMcs0;
//                tpmB = timeInMcs1;
//            } else {
//                tpmA = timeInMcs1;
//                tpmB = timeInMcs0;
//            }
//        } else {
//            tpmA = timeInMcs0;
//        }
//
//        final long[] tpmBF = tpmB;
//
//        (new LongSorter(tpmA, 0, nRows) {
//                protected void swap(int a, int b) {
//                    super.swap(a, b);
//                    FlatProfileContainer.this.swap(a, b);
//
//                    FlatProfileContainer.swap(totalTimeInMcs0,a,b);
//                    
//                    if (collectingTwoTimeStamps) {
//                        FlatProfileContainer.swap(tpmBF,a,b);
//                        FlatProfileContainer.swap(totalTimeInMcs1,a,b);
//                    }
//
//                    FlatProfileContainer.swap(methodIds,a,b);
//                    FlatProfileContainer.swap(nInvocations,a,b);
//
//                    if (percent != null) {
//                        FlatProfileContainer.swap(percent,a,b);
//                    }
//                }
//            }).sort(sortOrder);
//
//        // Next, sort the methods with zero time by the number of invocations
//        int len = nRows - 1;
//
//        while ((len >= 0) && (tpmA[len] == 0)) {
//            len--;
//        }
//
//        if (len < (nRows - 1)) {
//            (new IntSorter(nInvocations, len + 1, nRows - len - 1) {
//                    protected void swap(int a, int b) {
//                        super.swap(a, b);
//                        FlatProfileContainer.this.swap(a, b);
//
//                        FlatProfileContainer.swap(totalTimeInMcs0,a,b);
//                        
//                        if (collectingTwoTimeStamps) {
//                            FlatProfileContainer.swap(tpmBF,a,b);
//                            FlatProfileContainer.swap(totalTimeInMcs1,a,b);
//                        }
//
//                        FlatProfileContainer.swap(methodIds,a,b);
//                    }
//                }).sort(sortOrder);
//        }
//    }
//
//    private void sortDataByTotalTime(boolean usePrimaryTime, boolean sortOrder) {
//        long[] tpmA = null;
//        long[] tpmB = null;
//
//        // Percentage is recalculated every time, since it depends on whether primary/secondary time is used
//        if ((percent == null) || (usePrimaryTime != staticUsePrimaryTime) || (percent.length != nRows)) {
//            calculatePercent(usePrimaryTime);
//        }
//
//        if (collectingTwoTimeStamps) {
//            if (usePrimaryTime) {
//                tpmA = totalTimeInMcs0;
//                tpmB = totalTimeInMcs1;
//            } else {
//                tpmA = totalTimeInMcs1;
//                tpmB = totalTimeInMcs0;
//            }
//        } else {
//            tpmA = totalTimeInMcs0;
//        }
//
//        final long[] tpmBF = tpmB;
//
//        (new LongSorter(tpmA, 0, nRows) {
//                protected void swap(int a, int b) {
//                    super.swap(a, b);
//                    FlatProfileContainer.this.swap(a, b);
//
//                    FlatProfileContainer.swap(timeInMcs0,a,b);
//                    
//                    if (collectingTwoTimeStamps) {
//                        FlatProfileContainer.swap(tpmBF,a,b);
//                        FlatProfileContainer.swap(timeInMcs1,a,b);
//                    }
//
//                    FlatProfileContainer.swap(methodIds,a,b);
//                    FlatProfileContainer.swap(nInvocations,a,b);
//
//                    if (percent != null) {
//                        FlatProfileContainer.swap(percent,a,b);
//                    }
//                }
//            }).sort(sortOrder);
//
//        // Next, sort the methods with zero time by the number of invocations
//        int len = nRows - 1;
//
//        while ((len >= 0) && (tpmA[len] == 0)) {
//            len--;
//        }
//
//        if (len < (nRows - 1)) {
//            (new IntSorter(nInvocations, len + 1, nRows - len - 1) {
//                    protected void swap(int a, int b) {
//                        super.swap(a, b);
//                        FlatProfileContainer.this.swap(a, b);
//
//                        FlatProfileContainer.swap(timeInMcs0,a,b);
//
//                        if (collectingTwoTimeStamps) {
//                            FlatProfileContainer.swap(tpmBF,a,b);
//                            FlatProfileContainer.swap(timeInMcs1,a,b);
//                        }
//
//                        FlatProfileContainer.swap(methodIds,a,b);
//                    }
//                }).sort(sortOrder);
//        }
//    }
//    
//    private static void swap(float[] arr, int i1, int i2) {
//        float itmp = arr[i1];
//        arr[i1] = arr[i2];
//        arr[i2] = itmp;
//    }
//
//    private static void swap(long[] arr, int i1, int i2) {
//        long itmp = arr[i1];
//        arr[i1] = arr[i2];
//        arr[i2] = itmp;
//    }
//    
//    private static void swap(int[] arr, int i1, int i2) {
//        int itmp = arr[i1];
//        arr[i1] = arr[i2];
//        arr[i2] = itmp;
//    }
    
    protected void swap(int a, int b) {}
    
}
