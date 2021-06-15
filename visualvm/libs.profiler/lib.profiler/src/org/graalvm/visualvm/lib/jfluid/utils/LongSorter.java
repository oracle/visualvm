/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.utils;


/**
 * An implementation of quick sort for long numbers.
 * The advantage of this class is that it provides a protected swap(idx1, idx2) method, that can be overridden by a
 * subclass. This allows one to easily create a subclass of LongSorter, that would sort, for example, a data structure
 * consisting of several arrays, whose elements at the same index are viewed as a single logical record, and the order
 * of these records is determined by the order of elements in one long[] array. A subclass to sort such records should
 * override swap(). The new implementation of swap() should call super.swap() and then take care of swapping the rest
 * of the "fields" of the two given logical records.
 *
 * @author Misha Dmitriev
 */
public class LongSorter {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private long[] x;
    private int len;
    private int off;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public LongSorter(long[] x, int off, int len) {
        this.x = x;
        this.off = off;
        this.len = len;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Performs sorting in ascending or descending order
     * @param asc Defines the order of sorting: <CODE>true</CODE> means ascending order, <CODE>false</CODE> means descending order.
     */
    public void sort(boolean asc) {
        if (asc) {
            sort1Asc(off, len);
        } else {
            sort1Desc(off, len);
        }
    }

    /** Swaps x[a] with x[b]. An subclass may override this method to e.g. swap other data associated with elements of the sorted array */
    protected void swap(int a, int b) {
        long t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    /** Returns the index of the median of the three indexed integers. */
    private int med3(int a, int b, int c) {
        return ((x[a] < x[b]) ? ((x[b] < x[c]) ? b : ((x[a] < x[c]) ? c : a)) : ((x[b] > x[c]) ? b : ((x[a] > x[c]) ? c : a)));
    }

    private void sort1Asc(int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < (len + off); i++) {
                for (int j = i; (j > off) && (x[j - 1] > x[j]); j--) {
                    swap(j, j - 1);
                }
            }

            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element

        if (len > 7) {
            int l = off;
            int n = (off + len) - 1;

            if (len > 40) { // Big arrays, pseudomedian of 9

                int s = len / 8;
                l = med3(l, l + s, l + (2 * s));
                m = med3(m - s, m, m + s);
                n = med3(n - (2 * s), n - s, n);
            }

            m = med3(l, m, n); // Mid-size, med of 3
        }

        long v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off;

        // Establish Invariant: v* (<v)* (>v)* v*
        int b = a;

        // Establish Invariant: v* (<v)* (>v)* v*
        int c = (off + len) - 1;

        // Establish Invariant: v* (<v)* (>v)* v*
        int d = c;

        while (true) {
            while ((b <= c) && (x[b] <= v)) {
                if (x[b] == v) {
                    swap(a++, b);
                }

                b++;
            }

            while ((c >= b) && (x[c] >= v)) {
                if (x[c] == v) {
                    swap(c, d--);
                }

                c--;
            }

            if (b > c) {
                break;
            }

            swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s;

        // Swap partition elements back to middle
        int n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sort1Asc(off, s);
        }

        if ((s = d - c) > 1) {
            sort1Asc(n - s, s);
        }
    }

    private void sort1Desc(int off, int len) {
        // Insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < (len + off); i++) {
                for (int j = i; (j > off) && (x[j - 1] < x[j]); j--) {
                    swap(j, j - 1);
                }
            }

            return;
        }

        // Choose a partition element, v
        int m = off + (len >> 1); // Small arrays, middle element

        if (len > 7) {
            int l = off;
            int n = (off + len) - 1;

            if (len > 40) { // Big arrays, pseudomedian of 9

                int s = len / 8;
                l = med3(l, l + s, l + (2 * s));
                m = med3(m - s, m, m + s);
                n = med3(n - (2 * s), n - s, n);
            }

            m = med3(l, m, n); // Mid-size, med of 3
        }

        long v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off;

        // Establish Invariant: v* (<v)* (>v)* v*
        int b = a;

        // Establish Invariant: v* (<v)* (>v)* v*
        int c = (off + len) - 1;

        // Establish Invariant: v* (<v)* (>v)* v*
        int d = c;

        while (true) {
            while ((b <= c) && (x[b] >= v)) {
                if (x[b] == v) {
                    swap(a++, b);
                }

                b++;
            }

            while ((c >= b) && (x[c] <= v)) {
                if (x[c] == v) {
                    swap(c, d--);
                }

                c--;
            }

            if (b > c) {
                break;
            }

            swap(b++, c--);
        }

        // Swap partition elements back to middle
        int s;

        // Swap partition elements back to middle
        int n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(b, n - s, s);

        // Recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            sort1Desc(off, s);
        }

        if ((s = d - c) > 1) {
            sort1Desc(n - s, s);
        }
    }

    /** Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)]. */
    private void vecswap(int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(a, b);
        }
    }
}
