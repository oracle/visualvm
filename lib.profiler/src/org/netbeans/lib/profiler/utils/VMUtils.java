/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.utils;


/**
 * Utilities for dealing with VM representation of stuff.
 *
 * @author Ian Formanek
 */
public class VMUtils {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String BOOLEAN_CODE = "Z"; // NOI18N
    public static final String CHAR_CODE = "C"; // NOI18N
    public static final String BYTE_CODE = "B"; // NOI18N
    public static final String SHORT_CODE = "S"; // NOI18N
    public static final String INT_CODE = "I"; // NOI18N
    public static final String LONG_CODE = "J"; // NOI18N
    public static final String FLOAT_CODE = "F"; // NOI18N
    public static final String DOUBLE_CODE = "D"; // NOI18N
    public static final String VOID_CODE = "V"; // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String typeToVMSignature(final String type) {
        //    System.err.println("sig for: "+type);
        String ret = type.replaceAll("\\.", "/"); // NOI18N

        // 1. replace primitive types or surround class name
        if (ret.startsWith("boolean")) {
            ret = ret.replaceAll("boolean", BOOLEAN_CODE); // NOI18N
        } else if (ret.startsWith("char")) {
            ret = ret.replaceAll("char", CHAR_CODE); // NOI18N
        } else if (ret.startsWith("byte")) {
            ret = ret.replaceAll("byte", BYTE_CODE); // NOI18N
        } else if (ret.startsWith("short")) {
            ret = ret.replaceAll("short", SHORT_CODE); // NOI18N
        } else if (ret.startsWith("int")) {
            ret = ret.replaceAll("int", INT_CODE); // NOI18N
        } else if (ret.startsWith("long")) {
            ret = ret.replaceAll("long", LONG_CODE); // NOI18N
        } else if (ret.startsWith("float")) {
            ret = ret.replaceAll("float", FLOAT_CODE); // NOI18N
        } else if (ret.startsWith("double")) {
            ret = ret.replaceAll("double", DOUBLE_CODE); // NOI18N
        } else if (ret.startsWith("void")) {
            ret = ret.replaceAll("void", VOID_CODE); // NOI18N
        } else {
            // if the remainder is a class, surround it with "L...;"
            final int arIdx = ret.indexOf('['); // NOI18N

            if (arIdx == -1) {
                ret = "L" + ret + ";"; // NOI18N
            } else {
                ret = "L" + ret.substring(0, arIdx) + ";" + ret.substring(arIdx); // NOI18N
            }
        }

        // 2. put all array marks to the beginning in the VM-signature style
        while (ret.endsWith("[]")) { // NOI18N
            ret = "[" + ret.substring(0, ret.length() - 2); // NOI18N
        }

        //    System.err.println("is: "+ret);
        return ret;
    }
}
