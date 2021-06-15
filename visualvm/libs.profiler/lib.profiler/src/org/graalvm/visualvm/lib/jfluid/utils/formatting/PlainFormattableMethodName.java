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

package org.graalvm.visualvm.lib.jfluid.utils.formatting;

import org.graalvm.visualvm.lib.jfluid.utils.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.graalvm.visualvm.lib.jfluid.utils.VMUtils.*;

/**
 * A class that can be used to obtain Java class or method name formatted in various ways.
 *
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public class PlainFormattableMethodName implements Formattable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(PlainFormattableMethodName.class.getName());

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

        StringBuilder arrayIndicator = new StringBuilder();

        if (cname != null) {
            this.className = cname.replace('/', '.'); // NOI18N
        } else {
            // can be null in case formatter is only interested in method formatting
            this.className = "<unknown class>"; // NOI18N
        }

        if (this.className.length() == 0) { // deafult package wildcard
            this.className = ".*"; // NOI18N
        }

        if (this.className.endsWith(".")) { // slightly different wildcard notation; for the backward compatibility sake we keep it
            this.className = this.className + "*";  // NOI18N
        }

        if (mname == null || mname.isEmpty() || mname.equals("*")) { // NOI18N
            // methodName can be null when class- or package-level view is used
            params = ""; // NOI18N
            returnType = ""; // NOI18N
        } else {
            if ("".equals(mname) && !this.className.contains("*")) { // NOI18N
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

                    try {
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
                        } else {
                            // return type is a class
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
                    } catch (StringIndexOutOfBoundsException e) {
                        LOGGER.log(Level.INFO, "Can not format return type \"{0}\"", sig.substring(idx1));
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

                    try {
                        while (curPos < paramsString.length()) {
                            while (paramsString.charAt(curPos) == '[') { // NOI18N
                                arrayIndicator.append("[]"); // NOI18N
                                curPos++;
                            }
                            
                            nextChar = paramsString.charAt(curPos++);
                            
                            if (nextChar == BOOLEAN) {
                                paramsBuf.append(BOOLEAN_STRING);
                            } else if (nextChar == CHAR) {
                                paramsBuf.append(CHAR_STRING);
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
                            } else {
                                // it's a class
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
                    } catch (StringIndexOutOfBoundsException e) {
                        LOGGER.log(Level.INFO, "Can not format param list \"{0}\"", paramsString);
                    }

                    params = paramsBuf.toString();
                } else {
                    params = ""; // NOI18N
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("Formattable method name for:"); // NOI18N
            LOGGER.log(Level.FINEST, "Class: {0}", this.className); // NOI18N
            LOGGER.log(Level.FINEST, "Method: {0}", this.methodName); // NOI18N
            LOGGER.log(Level.FINEST, "Return type: {0}", this.returnType); // NOI18N
            LOGGER.log(Level.FINEST, "Parameters: {0}", this.params); // NOI18N
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
            LOGGER.log(Level.FINER, "Getting formatted method name for {0}", methodName); // NOI18N
        }

        if ((methodName == null) || (methodName.length() == 0)) {
            return ""; // NOI18N  // Understanding is that class-level view is used
//        } else if ("<clinit>".equals(methodName)) { // NOI18N
//            return methodName;
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
            LOGGER.log(Level.FINER, "Getting full formatted method name for {0}", methodName); // NOI18N
        }

        if ((methodName == null) || (methodName.length() == 0)) {
            return ""; // NOI18N  // Understanding is that class-level view is used
        } else if ("<clinit>".equals(methodName)) { // NOI18N
            return methodName + "()";
        } else if ("<init>".equals(methodName)) { // NOI18N
            return methodName + "(" + getParamsString() + ")"; // NOI18N
        } else if ("*".equals(methodName)) { // NOI18N
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

    @Override
    public String toFormatted() {
        switch (verbosity) {
            case DefaultMethodNameFormatter.VERBOSITY_CLASS:return getFormattedClass();
            case DefaultMethodNameFormatter.VERBOSITY_METHOD:return getFormattedMethod();
            case DefaultMethodNameFormatter.VERBOSITY_CLASSMETHOD:return getFormattedClassAndMethod();
            case DefaultMethodNameFormatter.VERBOSITY_FULLMETHOD:return getFullFormattedMethod();
            case DefaultMethodNameFormatter.VERBOSITY_FULLCLASSMETHOD:return getFullFormattedClassAndMethod();
            default:return getFullFormattedClassAndMethod();
        }
    }

    @Override
    public String toString() {
        return getFullFormattedClassAndMethod();
    }

    private boolean isAllWildCard(String methodName) {
        return methodName.equals("<all>") || methodName.equals("*"); // NOI18N
    }
}
