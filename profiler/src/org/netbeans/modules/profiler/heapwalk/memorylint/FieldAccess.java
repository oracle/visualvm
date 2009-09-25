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

package org.netbeans.modules.profiler.heapwalk.memorylint;

import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;
import java.util.List;


/**
 *
 * @author nenik
 */
public class FieldAccess {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    Field fld;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Field */
    public FieldAccess(JavaClass jc, String name) {
        @SuppressWarnings("unchecked")
        List<Field> fields = jc.getFields();

        for (Field f : fields) {
            if (f.getName().equals(name)) {
                fld = f;

                break;
            }
        }
        assert (fld != null);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getIntValue(Instance in) {
        @SuppressWarnings("unchecked")
        List<FieldValue> values = in.getFieldValues();

        for (FieldValue fv : values) {
            if (fv.getField().equals(fld)) {
                try {
                    return Integer.parseInt(fv.getValue());
                } catch (NumberFormatException nfe) {
                }
            }
        }
        assert false; // shouldn't reach

        return -1;
    }

    public Instance getRefValue(Instance in) {
        assert fld.getType().getName().equals("object");

        @SuppressWarnings("unchecked")
        List<FieldValue> values = in.getFieldValues();

        for (FieldValue fv : values) {
            if (fv.getField().equals(fld)) {
                return ((ObjectFieldValue) fv).getInstance();
            }
        }
        assert false; // shouldn't reach

        return null;
    }
}
