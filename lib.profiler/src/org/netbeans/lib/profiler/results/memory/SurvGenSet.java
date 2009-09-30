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

package org.netbeans.lib.profiler.results.memory;


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
