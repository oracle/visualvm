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

package org.netbeans.lib.profiler.utils;

import static org.netbeans.lib.profiler.utils.VMUtils.*;

/**
 * A class that can be used to obtain Java class or method name formatted in various ways.
 *
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public class MethodNameFormatter {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private String methodName;
    private String params;
    private String returnType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new methodNameFormatter for given class/method/signature.
     * The method name and signature can be null, in which case the formatter works for class only.
     *
     * @param cname  A fully qualified name of the class, or null if only formatting methods
     *                   - e.g. "java/lang/String"
     * @param mname A name of method, empty string or "<init>" for constructors or null if method is not specified
     *                   - e.g. "concat"
     * @param sig  VM signature of method or null if method is not specified or <clinit> is the method
     *                   - e.g. "(Ljava/lang/String;)Ljava/lang/String;"
     */
    public MethodNameFormatter(String cname, String mname, String sig) {
        int curPos;
        int idx1;
        int idx2;
        char nextChar;

        StringBuilder arrayIndicator = new StringBuilder();

        if (cname != null) {
            this.className = cname.replace('/', '.'); // NOI18N
        } else {
            // can be null in case formatter is only interested in method formatting
            this.className = "<unknown class>"; // NOI18N
        }

        if (mname == null /*|| mname.isEmpty() || mname.equals("*")*/) { // NOI18N // breaks tests, not sure why added
            // methodName can be null when class- or package-level view is used
            params = ""; // NOI18N
            returnType = ""; // NOI18N
        } else {
            if ("".equals(mname)) { // NOI18N
                this.methodName = "<init>"; // NOI18N
            } else {
                this.methodName = mname;
            }

            if ("<clinit>".equals(methodName) || Wildcards.isMethodWildcard(methodName)) { // NOI18N
                params = ""; // NOI18N
                returnType = ""; // NOI18N
            } else {
                idx1 = sig.lastIndexOf(')') + 1; // NOI18N

                if ((idx1 > 0) && !"<init>".equals(methodName)) { // NOI18N
                                                                  // For e.g. the "Thread" root node it may be zero; there was a bug with it when this method
                                                                  // was hit for "Thread" in results export
                    returnType = sig.substring(idx1);
                    curPos = 0;

                    while (returnType.charAt(curPos) == '[') { // NOI18N
                        arrayIndicator.append("[]"); // NOI18N
                        curPos++;
                    }

                    nextChar = returnType.charAt(curPos++);

                    if (nextChar == BOOLEAN) {
                        returnType = BOOLEAN_STRING + arrayIndicator.toString();
                    } else if (nextChar == CHAR) {
                        returnType = CHAR_STRING + arrayIndicator.toString();
                    } else if (nextChar == BYTE) {
                        returnType = BYTE_STRING + arrayIndicator.toString();
                    } else if (nextChar == SHORT) {
                        returnType = SHORT_STRING + arrayIndicator.toString();
                    } else if (nextChar == INT) {
                        returnType = INT_STRING + arrayIndicator.toString();
                    } else if (nextChar == LONG) {
                        returnType = LONG_STRING + arrayIndicator.toString();
                    } else if (nextChar == FLOAT) {
                        returnType = FLOAT_STRING + arrayIndicator.toString();
                    } else if (nextChar == DOUBLE) {
                        returnType = DOUBLE_STRING + arrayIndicator.toString();
                    } else if (nextChar == VOID) {
                        returnType = VOID_STRING + arrayIndicator.toString();
                    } else { // return type is a class
                             // Check if the class belongs to the java.lang.* package, and replace it with simple name if so.
                             // However, avoid doing so if it's from say java.lang.ref.* package - otherwise we'll get confusing
                             // names like ref.Reference
                        returnType = returnType.substring(curPos, returnType.length() - 1); //strip "L" at the beginning
                                                                                            // and ";" at end

                        if (returnType.startsWith("java/lang/") && (returnType.indexOf('/', 10) == -1)) { // NOI18N
                            returnType = returnType.substring(10);
                        }

                        returnType = returnType.replace('$', '.'); // NOI18N
                        returnType = returnType.replace('/', '.') + arrayIndicator.toString(); // NOI18N
                    }
                } else {
                    // constructor or no end parenthesis
                    returnType = ""; // NOI18N
                }

                idx1 = sig.indexOf('(') + 1; // NOI18N
                idx2 = sig.lastIndexOf(')'); // NOI18N

                if (idx2 > 0) {
                    String paramsString = sig.substring(idx1, idx2);
                    StringBuilder paramsBuf = new StringBuilder();
                    arrayIndicator.setLength(0);
                    curPos = 0;

                    while (curPos < paramsString.length()) {
                        while (paramsString.charAt(curPos) == '[') { // NOI18N
                            arrayIndicator.append("[]"); // NOI18N
                            curPos++;
                        }

                        nextChar = paramsString.charAt(curPos++);

                        if (nextChar == BOOLEAN) {
                            paramsBuf.append(BOOLEAN_STRING);
                        } else if (nextChar == CHAR) {
                            paramsBuf.append(CHAR);
                        } else if (nextChar == BYTE) {
                            paramsBuf.append(BYTE_STRING);
                        } else if (nextChar == SHORT) {
                            paramsBuf.append(SHORT_STRING);
                        } else if (nextChar == INT) {
                            paramsBuf.append(INT_STRING);
                        } else if (nextChar == LONG) {
                            paramsBuf.append(LONG_STRING);
                        } else if (nextChar == FLOAT) {
                            paramsBuf.append(FLOAT_STRING);
                        } else if (nextChar == DOUBLE) {
                            paramsBuf.append(DOUBLE_STRING);
                        } else { // it's a class

                            int startPos = curPos;

                            while (paramsString.charAt(curPos) != ';') { // NOI18N
                                curPos++;
                            }

                            String typeName = paramsString.substring(startPos, curPos); //strip "L" at the beginning and ";" at end

                            if (typeName.startsWith("java/lang/") && (typeName.indexOf('/', 10) == -1)) { // NOI18N
                                typeName = typeName.substring(10);
                            }

                            typeName = typeName.replace('$', '.'); // NOI18N
                            typeName = typeName.replace('/', '.'); // NOI18N
                            paramsBuf.append(typeName);
                            curPos++;
                        }

                        if (arrayIndicator.length() > 0) {
                            paramsBuf.append(arrayIndicator.toString());
                        }

                        arrayIndicator.setLength(0);

                        if (curPos < paramsString.length()) {
                            paramsBuf.append(", "); // NOI18N
                        }
                    }

                    params = paramsBuf.toString();
                } else {
                    params = ""; // NOI18N
                }
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * @return formatted class name, using '.' to separate packages = e.g. "java.lang.String"
     */
    public String getFormattedClass() {
        return className;
    }

    /**
     * @return formatted class and method name (with parameter types, but without its return type).
     *         Will return the same as getFormattedClass if method was not specified.
     */
    public String getFormattedClassAndMethod() {
        if (methodName == null) {
            return getFormattedClass();
        } else {
            return className + "." + getFormattedMethod(); // NOI18N
        }
    }

    /**
     * @return formatted method name, with parameter types, but without the return type. Will return empty string
     *         if method was not specified.
     */
    public String getFormattedMethod() {
        if (methodName == null) {
            return ""; // NOI18N  // Understanding is that class-level view is used
        } else if ("<clinit>".equals(methodName)) { // NOI18N
            return methodName + "()"; // NOI18N
        } else {
            return methodName + "(" + getParamsString() + ")"; // NOI18N
        }
    }

    /**
     * @return formatted class and method name (with parameter types and return type)
     *         Will return the same as getFormattedClass if method was not specified.
     */
    public String getFullFormattedClassAndMethod() {
        if (methodName == null) {
            return getFormattedClass();
        } else {
            return className + "." + getFullFormattedMethod(); // NOI18N
        }
    }

    /**
     * @return formatted method name with parameter types and return type (return type not used if constructor).
     *         Will return empty string if method was not specified.
     */
    public String getFullFormattedMethod() {
        if (methodName == null) {
            return ""; // NOI18N  // Understanding is that class-level view is used
        } else if ("<clinit>".equals(methodName)) { // NOI18N
            return methodName + "()"; // NOI18N
        } else if ("<init>".equals(methodName)) { // NOI18N
            return methodName + "(" + getParamsString() + ")"; // NOI18N
        } else {
            return methodName + "(" + getParamsString() + ") : " + getReturnType(); // NOI18N
        }
    }

    /**
     * @return parameters of the given method - formatted string -, empty string if the method has no parameters or
     *         no method was specified
     */
    public String getParamsString() {
        return params;
    }

    /**
     * @return return type of the given method, empty String if void or method was not specified
     */
    public String getReturnType() {
        return returnType;
    }

    public String toString() {
        return getFullFormattedClassAndMethod();
    }

    private boolean isAllWildCard(String methodName) {
        return methodName.equals("<all>") || methodName.equals("*"); // NOI18N
    }
}
