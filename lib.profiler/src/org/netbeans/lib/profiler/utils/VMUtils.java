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
    
    public static final String BOOLEAN_STRING = "boolean"; // NOI18N
    public static final String CHAR_STRING = "char"; // NOI18N
    public static final String BYTE_STRING = "byte"; // NOI18N
    public static final String SHORT_STRING = "short"; // NOI18N
    public static final String INT_STRING = "int"; // NOI18N
    public static final String LONG_STRING = "long"; // NOI18N
    public static final String FLOAT_STRING = "float"; // NOI18N
    public static final String DOUBLE_STRING = "double"; // NOI18N
    public static final String VOID_STRING = "void"; // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String typeToVMSignature(final String type) {
        //    System.err.println("sig for: "+type);
        String ret = type.replace('.', '/'); // NOI18N

        // 1. replace primitive types or surround class name
        if (ret.startsWith(BOOLEAN_STRING)) {
            ret = ret.replace(BOOLEAN_STRING, BOOLEAN_CODE);
        } else if (ret.startsWith(CHAR_STRING)) {
            ret = ret.replace(CHAR_STRING, CHAR_CODE);
        } else if (ret.startsWith(BYTE_STRING)) {
            ret = ret.replace(BYTE_STRING, BYTE_CODE);
        } else if (ret.startsWith(SHORT_STRING)) {
            ret = ret.replace(SHORT_STRING, SHORT_CODE);
        } else if (ret.startsWith(INT_STRING)) {
            ret = ret.replace(INT_STRING, INT_CODE);
        } else if (ret.startsWith(LONG_STRING)) {
            ret = ret.replace(LONG_STRING, LONG_CODE);
        } else if (ret.startsWith(FLOAT_STRING)) {
            ret = ret.replace(FLOAT_STRING, FLOAT_CODE);
        } else if (ret.startsWith(DOUBLE_STRING)) {
            ret = ret.replace(DOUBLE_STRING, DOUBLE_CODE);
        } else if (ret.startsWith(VOID_STRING)) {
            ret = ret.replace(VOID_STRING, VOID_CODE);
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
    
    public static boolean isVMPrimitiveType(String className) {
        if (className == null || className.length() != 1) return false;
        if (VMUtils.BOOLEAN_CODE.equals(className) || VMUtils.CHAR_CODE.equals(className) || VMUtils.BYTE_CODE.equals(className) ||
            VMUtils.SHORT_CODE.equals(className) || VMUtils.INT_CODE.equals(className) || VMUtils.LONG_CODE.equals(className) ||
            VMUtils.FLOAT_CODE.equals(className) || VMUtils.DOUBLE_CODE.equals(className)) return true;
        return false;
    }
    
    public static boolean isPrimitiveType(String className) {
        if (className == null || className.length() < 1) return false;
        if (VMUtils.BOOLEAN_STRING.equals(className) || VMUtils.CHAR_STRING.equals(className) || VMUtils.BYTE_STRING.equals(className) ||
            VMUtils.SHORT_STRING.equals(className) || VMUtils.INT_STRING.equals(className) || VMUtils.LONG_STRING.equals(className) ||
            VMUtils.FLOAT_STRING.equals(className) || VMUtils.DOUBLE_STRING.equals(className)) return true;
        return false;
    }
}
