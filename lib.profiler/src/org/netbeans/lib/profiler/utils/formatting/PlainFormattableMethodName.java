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

package org.netbeans.lib.profiler.utils.formatting;

import org.netbeans.lib.profiler.utils.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class that can be used to obtain Java class or method name formatted in various ways.
 *
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public class PlainFormattableMethodName implements Formattable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(PlainFormattableMethodName.class.getName());
    private static final String BOOLEAN_TEXT = "boolean"; // NOI18N
    private static final String CHAR_TEXT = "char"; // NOI18N
    private static final String BYTE_TEXT = "byte"; // NOI18N
    private static final String SHORT_TEXT = "short"; // NOI18N
    private static final String INT_TEXT = "int"; // NOI18N
    private static final String LONG_TEXT = "long"; // NOI18N
    private static final String FLOAT_TEXT = "float"; // NOI18N
    private static final String DOUBLE_TEXT = "double"; // NOI18N
    private static final String VOID_TEXT = "void"; // NOI18N
    private static final char BOOLEAN_CODE = 'Z'; // NOI18N
    private static final char CHAR_CODE = 'C'; // NOI18N
    private static final char BYTE_CODE = 'B'; // NOI18N
    private static final char SHORT_CODE = 'S'; // NOI18N
    private static final char INT_CODE = 'I'; // NOI18N
    private static final char LONG_CODE = 'J'; // NOI18N
    private static final char FLOAT_CODE = 'F'; // NOI18N
    private static final char DOUBLE_CODE = 'D'; // NOI18N
    private static final char VOID_CODE = 'V'; // NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String className;
    private String methodName;
    private String params;
    private String returnType;
    private int verbosity;

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
     * @param verbosity verbosity level: 1 = getFormattedClass(), 2 = getFormattedMethod(), 3 = getFormattedClassAndMethod(),
     *                                   4 = getFullFormattedMethod(), 5 = getFullFormatedClassAndMethod()
     */
    PlainFormattableMethodName(String cname, String mname, String sig, int verbosity) {
        int curPos;
        int idx1;
        int idx2;
        char nextChar;

        this.verbosity = verbosity;

        StringBuffer arrayIndicator = new StringBuffer();

        if (cname != null) {
            this.className = cname.replace('/', '.'); // NOI18N
        } else {
            // can be null in case formatter is only interested in method formatting // NOI18N
            this.className = "<unknown class>"; // NOI18N
        }

        if (this.className.length() == 0) { // deafult package wildcard
            this.className = ".*"; // NOI18N
        }

        if (this.className.endsWith(".")) { // slightly different wildcard notation; for the backward compatibility sake we keep it
            this.className = this.className + "*";
        }

        if (mname == null) {
            // methodName can be null when class- or package-level view is used
            params = ""; // NOI18N
            returnType = ""; // NOI18N
        } else {
            if ("".equals(mname) && !this.className.contains("*")) {
                // NOI18N
                this.methodName = "<init>"; // NOI18N
            } else {
                this.methodName = mname;
            }

            if ("<clinit>".equals(methodName) || Wildcards.isMethodWildcard(methodName)) {
                // NOI18N
                params = ""; // NOI18N
                returnType = ""; // NOI18N
            } else {
                idx1 = sig.lastIndexOf(')') + 1; // NOI18N

                if ((idx1 > 0) && !"<init>".equals(methodName)) {
                    // NOI18N
                    // For e.g. the "Thread" root node it may be zero; there was a bug with it when this method
                    // was hit for "Thread" in results export
                    returnType = sig.substring(idx1);
                    curPos = 0;

                    while (returnType.charAt(curPos) == '[') {
                        // NOI18N
                        arrayIndicator.append("[]"); // NOI18N
                        curPos++;
                    }

                    nextChar = returnType.charAt(curPos++);

                    if (nextChar == BOOLEAN_CODE) {
                        returnType = BOOLEAN_TEXT + arrayIndicator.toString();
                    } else if (nextChar == CHAR_CODE) {
                        returnType = CHAR_TEXT + arrayIndicator.toString();
                    } else if (nextChar == BYTE_CODE) {
                        returnType = BYTE_TEXT + arrayIndicator.toString();
                    } else if (nextChar == SHORT_CODE) {
                        returnType = SHORT_TEXT + arrayIndicator.toString();
                    } else if (nextChar == INT_CODE) {
                        returnType = INT_TEXT + arrayIndicator.toString();
                    } else if (nextChar == LONG_CODE) {
                        returnType = LONG_TEXT + arrayIndicator.toString();
                    } else if (nextChar == FLOAT_CODE) {
                        returnType = FLOAT_TEXT + arrayIndicator.toString();
                    } else if (nextChar == DOUBLE_CODE) {
                        returnType = DOUBLE_TEXT + arrayIndicator.toString();
                    } else if (nextChar == VOID_CODE) {
                        returnType = VOID_TEXT + arrayIndicator.toString();
                    } else {
                        // return type is a class
                        // Check if the class belongs to the java.lang.* package, and replace it with simple name if so.
                        // However, avoid doing so if it's from say java.lang.ref.* package - otherwise we'll get confusing
                        // names like ref.Reference
                        returnType = returnType.substring(curPos, returnType.length() - 1); //strip "L" at the beginning
                                                                                            // and ";" at end

                        if (returnType.startsWith("java/lang/") && (returnType.indexOf('/', 10) == -1)) {
                            // NOI18N
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
                    StringBuffer paramsBuf = new StringBuffer();
                    arrayIndicator.setLength(0);
                    curPos = 0;

                    while (curPos < paramsString.length()) {
                        while (paramsString.charAt(curPos) == '[') {
                            // NOI18N
                            arrayIndicator.append("[]"); // NOI18N
                            curPos++;
                        }

                        nextChar = paramsString.charAt(curPos++);

                        if (nextChar == BOOLEAN_CODE) {
                            paramsBuf.append(BOOLEAN_TEXT);
                        } else if (nextChar == CHAR_CODE) {
                            paramsBuf.append(CHAR_TEXT);
                        } else if (nextChar == BYTE_CODE) {
                            paramsBuf.append(BYTE_TEXT);
                        } else if (nextChar == SHORT_CODE) {
                            paramsBuf.append(SHORT_TEXT);
                        } else if (nextChar == INT_CODE) {
                            paramsBuf.append(INT_TEXT);
                        } else if (nextChar == LONG_CODE) {
                            paramsBuf.append(LONG_TEXT);
                        } else if (nextChar == FLOAT_CODE) {
                            paramsBuf.append(FLOAT_TEXT);
                        } else if (nextChar == DOUBLE_CODE) {
                            paramsBuf.append(DOUBLE_TEXT);
                        } else {
                            // it's a class
                            int startPos = curPos;

                            while (paramsString.charAt(curPos) != ';') {
                                // NOI18N
                                curPos++;
                            }

                            String typeName = paramsString.substring(startPos, curPos); //strip "L" at the beginning and ";" at end

                            if (typeName.startsWith("java/lang/") && (typeName.indexOf('/', 10) == -1)) {
                                // NOI18N
                                typeName = typeName.substring(10); // NOI18N
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

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Formattable method name for:");
            LOGGER.finest("Class: " + this.className);
            LOGGER.finest("Method: " + this.methodName);
            LOGGER.finest("Return type: " + this.returnType);
            LOGGER.finest("Parameters: " + this.params);
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
        if ((methodName == null) || (methodName.length() == 0)) {
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
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Getting formatted method name for " + methodName);
        }

        if ((methodName == null) || (methodName.length() == 0)) {
            return ""; // NOI18N  // Understanding is that class-level view is used
        } else if ("<clinit>".equals(methodName)) {
            // NOI18N
            return methodName;
        } else {
            return methodName + "(" + getParamsString() + ")"; // NOI18N
        }
    }

    /**
     * @return formatted class and method name (with parameter types and return type)
     *         Will return the same as getFormattedClass if method was not specified.
     */
    public String getFullFormattedClassAndMethod() {
        if ((methodName == null) || (methodName.length() == 0)) {
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
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Getting full formatted method name for " + methodName);
        }

        if ((methodName == null) || (methodName.length() == 0)) {
            return ""; // NOI18N  // Understanding is that class-level view is used
        } else if ("<clinit>".equals(methodName)) {
            // NOI18N
            return methodName;
        } else if ("<init>".equals(methodName)) {
            // NOI18N
            return methodName + "(" + getParamsString() + ")"; // NOI18N
        } else if ("*".equals(methodName)) {
            // NOI18N
            return methodName;
        } else {
            return methodName + "(" + getParamsString() + ") : " + getReturnTypeX(); // NOI18N
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
    public String getReturnTypeX() {
        return returnType;
    }

    public String toFormatted() {
        switch (verbosity) {
            case 1:return getFormattedClass();
            case 2:return getFormattedMethod();
            case 3:return getFormattedClassAndMethod();
            case 4:return getFullFormattedMethod();
            case 5:return getFullFormattedClassAndMethod();
            default:return getFullFormattedClassAndMethod();
        }
    }

    public String toString() {
        return getFullFormattedClassAndMethod();
    }

    private boolean isAllWildCard(String methodName) {
        return methodName.equals("<all>") || methodName.equals("*"); // NOI18N
    }
}
