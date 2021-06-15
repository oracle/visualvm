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

package org.graalvm.visualvm.lib.jfluid.results.memory;


/**
 * This class is used to calculate the cardinality of the set of all object ages for the given class,
 * which is actually the definition of the number of surviving generations.
 *
 * @author Misha Dmitriev
 */
public class SurvGenSet {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int[] age;
    private int limit;
    private int nEls;
    private int nSlots;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SurvGenSet() {
        nSlots = 11;
        age = new int[nSlots];

        for (int i = 0; i < nSlots; i++) {
            age[i] = -1;
        }

        nEls = 0;
        limit = (nSlots * 3) / 4;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /* Get the total number of different ages */
    public int getTotalNoOfAges() {
        return nEls;
    }

    /**
     * Add the given age to the existing set of ages
     */
    public void addAge(int objAge) {
        int pos = objAge % nSlots;

        while ((age[pos] != objAge) && (age[pos] != -1)) {
            pos = (pos + 1) % nSlots;
        }

        if (age[pos] == -1) {
            age[pos] = objAge;
            nEls++;

            if (nEls >= limit) {
                rehash();
            }
        }
    }

    public void mergeWith(SurvGenSet other) {
        int[] otherAge = other.age;
        int otherLen = otherAge.length;

        for (int i = 0; i < otherLen; i++) {
            if (otherAge[i] != -1) {
                addAge(otherAge[i]);
            }
        }
    }

    private void rehash() {
        int[] oldAge = age;
        int oldNSlots = nSlots;
        nSlots = (oldNSlots * 2) + 1;
        age = new int[nSlots];

        for (int i = 0; i < nSlots; i++) {
            age[i] = -1;
        }

        nEls = 0;
        limit = (nSlots * 3) / 4;

        for (int i = 0; i < oldNSlots; i++) {
            if (oldAge[i] != -1) {
                addAge(oldAge[i]);
            }
        }
    }
}
