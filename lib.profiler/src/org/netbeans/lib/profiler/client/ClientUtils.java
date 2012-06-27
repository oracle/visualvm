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

package org.netbeans.lib.profiler.client;

import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.utils.formatting.DefaultMethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;


/**
 * A collection of utility classes and methods used exclusively by the client.
 *
 * @author Tomas Hurka
 * @author Misha Dmitriev
 * @author Adrian Mos
 * @author Ian Formanek
 */
public class ClientUtils implements CommonConstants {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final MethodNameFormatter classNameFormatter = new DefaultMethodNameFormatter(DefaultMethodNameFormatter.VERBOSITY_CLASS);
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class SourceCodeSelection implements Cloneable {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        String className;
        String methodName;
        String methodSignature;
        boolean isMarkerMethod;
        int endLine;
        int startLine;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public SourceCodeSelection(String className, int startLine, int endLine) {
            this.className = (className != null) ? className : ""; // NULL might cause problems in JFluid - see eg. #95961
            this.startLine = startLine;
            this.endLine = endLine;

            // methodName and methodSignature are null in this case
        }

        public SourceCodeSelection(String className, String methodName, String methodSignature) {
            this.className = (className != null) ? className : ""; // NULL might cause problems in JFluid - see eg. #95961
            this.methodName = (methodName != null) ? methodName : ""; // NULL might cause problems in JFluid - see eg. #95961
            this.methodSignature = (methodSignature != null) ? methodSignature : ""; // NULL might cause problems in JFluid - see eg. #95961
            startLine = endLine = -1;
        }

        // Used for various special instrumentation kinds where no real method name/line are used
        public SourceCodeSelection(int specialCode) {
            if (specialCode == 1) {
                // "Instrument all spawned threads" profiling mode. The "run()" method of all threads started after
                // the "instrument" command is issued, is instrumented - but NOT the main() method. The intended usage
                // of this mode is when JFluid is attached to an already running app. Otherwise, one should set the
                // main method as a root, and call GlobalSettins.setInstrumentSpawnedThreads(true) explicitly.
                className = NO_CLASS_NAME;
                methodName = NO_METHOD_NAME;
                methodSignature = NO_METHOD_SIGNATURE;
                startLine = endLine = 0; // So that it's defined neither via source line nor via method name
            }
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getClassName() {
            return className;
        }

        public int getEndLine() {
            return endLine;
        }

        public void setMarkerMethod(boolean value) {
            isMarkerMethod = value;
        }

        public boolean isMarkerMethod() {
            return isMarkerMethod;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodSignature() {
            return methodSignature;
        }

        public int getStartLine() {
            return startLine;
        }

        /** Return if the selection represents whole default package. */
        public boolean isDefaultPackage() {
            return className.isEmpty() && methodName.isEmpty() && methodSignature.isEmpty();
        }
        
        /** Return if the selection represents subset of default package. */
        public boolean isInDefaultPackage() {
            return isDefaultPackage() || !className.contains(".");
        }

        public Object clone() throws CloneNotSupportedException {
            SourceCodeSelection clone = (SourceCodeSelection) super.clone();
            clone.className = className;
            clone.methodName = methodName;
            clone.methodSignature = methodSignature;

            return clone;
        }

        public boolean contains(ClientUtils.SourceCodeSelection anotherSelection) {
            if (definedViaSourceLines()) {
                if (className.equals(anotherSelection.className)) {
                    return (startLine >= anotherSelection.startLine) && (endLine <= anotherSelection.endLine);
                }
            } else {
                if (isDefaultPackage()) {
                    return isInDefaultPackage();
                } else {
                    String thisFlattened = toFlattened().replace('.', '\\').replace('$', '\\') + "\\"; //NOI18N
                    String anotherFlattened = anotherSelection.toFlattened().replace('.', '\\').replace('$', '\\'); //NOI18N

                    return anotherFlattened.startsWith(thisFlattened);
                }
            }

            return false;
        }

        public boolean definedViaMethodName() {
            return startLine == -1;
        }

        public boolean definedViaSourceLines() {
            return startLine > 0;
        }

        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (!(obj instanceof SourceCodeSelection)) {
                return false;
            }

            SourceCodeSelection other = (SourceCodeSelection) obj;

            //      if (this.isMarkerMethod != other.isMarkerMethod) return false;
            // check start/end lines - they should be the same even in case they are not used
            if ((this.startLine != other.startLine) || (this.endLine != other.endLine)) {
                return false;
            }

            // length of classNames needs to be the same
            // normalizing the class name; result of #203446
            String cn1 = this.className.replace("$**", "").replace(".**", "").replace(".*", ""); // NOI18N
            // normalizing the class name; result of #203446
            String cn2 = other.className.replace("$**", "").replace(".**", "").replace(".*", ""); // NOI18N
            if (!cn1.equals(cn2)) {
                return false;
            }

            if (this.methodName != null) {
                if (!this.methodName.equals(other.methodName)) {
                    return false;
                }
            } else {
                if (other.methodName != null) {
                    return false;
                }
            }

            if (this.methodSignature != null) {
                if (!this.methodSignature.equals(other.methodSignature)) {
                    return false;
                }
            } else {
                if (other.methodSignature != null) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            int hashcode = 0;
            hashcode += (startLine + endLine);
            hashcode += className.hashCode();
            hashcode += ((methodName != null) ? methodName.hashCode() : 0);
            hashcode += ((methodSignature != null) ? methodSignature.hashCode() : 0);

            return hashcode;
        }

        public String toFlattened() {
            if ((className == null) || (className.length() == 0)) {
                return ""; // NOI18N
            }

            boolean wildcard = className.endsWith("*"); // NOI18N
            StringBuilder flattenedBuf = new StringBuilder(className.replace("$**", "").replace(".**", "").replace(".*", "")); // NOI18N

            if (!wildcard && methodName != null && methodName.length() > 0 && !methodName.endsWith("*")) { //NOI18N
                flattenedBuf.append('.').append(methodName);
            }

            if (!wildcard && methodSignature != null && methodSignature.length() > 0 && !methodSignature.endsWith("*")) { //NOI18N
                flattenedBuf.append(methodSignature);
            }

            return flattenedBuf.toString(); //NOI18N
        }

        public String toString() {
            if (definedViaSourceLines()) {
                return "Source Code defined via lines: [class: " // NOI18N
                       + className + ", start line: " // NOI18N
                       + startLine + ", end line: " // NOI18N
                       + endLine + "]"; // NOI18N
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Source Code defined via method: "); // NOI18N
                sb.append("\n"); // NOI18N
                sb.append("  class: "); // NOI18N
                sb.append(className);
                sb.append("\n  "); // NOI18N

                if (methodName != null) {
                    sb.append("  method: "); // NOI18N
                    sb.append(methodName);
                    sb.append("  "); // NOI18N
                    sb.append(methodSignature);
                    sb.append("\n  "); // NOI18N
                }

                if (isMarkerMethod) {
                    sb.append("[Marker Method]\n"); //NOI18N
                }

                return sb.toString();
            }
        }        
    }

    public static class TargetAppFailedToStart extends Exception {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private String origCause;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TargetAppFailedToStart(String errorMessage) {
            this.origCause = errorMessage;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public String getOrigCause() {
            return origCause;
        }
    }

    public static class TargetAppOrVMTerminated extends Exception {
        //~ Static fields/initializers -------------------------------------------------------------------------------------------

        public static final int VM = 1;
        public static final int APP = 2;

        //~ Instance fields ------------------------------------------------------------------------------------------------------

        int code;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public TargetAppOrVMTerminated(int code) {
            this.code = code;
        }

        public TargetAppOrVMTerminated(int code, String message) {
            super(message);
            this.code = code;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isAppTerminated() {
            return (code == APP);
        }

        public String getMessage() {
            if (super.getMessage() == null) {
                return (code == VM) ? "Target JVM inactive" : "Target JVM inactive"; // NOI18N
            } else {
                return super.getMessage();
            }
        }

        public boolean isVMTerminated() {
            return (code == VM);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String LINES_PREFIX = "[lines]"; //NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static String selectionToString(ClientUtils.SourceCodeSelection selection) {
        if (selection == null) {
            return ""; //NOI18N
        }

        if (selection.definedViaSourceLines()) {
            return LINES_PREFIX + selection.getClassName() + "," + selection.getStartLine() + "," + selection.getEndLine(); //NOI18N
        } else {
            if (selection.getMethodName() == null) {
                return selection.getClassName();
            } else if (selection.getMethodSignature() == null) {
                return selection.getClassName() + "," + selection.getMethodName(); //NOI18N
            } else {
                return selection.getClassName() + "," + selection.getMethodName() + "," + selection.getMethodSignature(); //NOI18N
            }
        }
    }

    public static ClientUtils.SourceCodeSelection stringToSelection(String str) {
        if ((str == null) || (str.length() == 0)) {

            return null;
        }

        boolean viaLines = false;

        if (str.startsWith(LINES_PREFIX)) {
            viaLines = true;
            str = str.substring(LINES_PREFIX.length());
        }

        String[] parts = str.split(","); //NOI18N

        if (viaLines) {
            if (parts.length != 3) {
                return null; // invalid
            }

            try {
                return new ClientUtils.SourceCodeSelection(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            } catch (NumberFormatException e) {
                return null; // error in encoding of lines
            }
        } else {
            String className = ""; // NOI18N

            if (parts.length > 0) {
                className = parts[0];
            }

            String methodName = ""; // NOI18N

            if (parts.length > 1) {
                methodName = parts[1];
            }

            String methodSig = ""; // NOI18N

            if (parts.length > 2) {
                methodSig = parts[2];
            }

            return new ClientUtils.SourceCodeSelection(className, methodName, methodSig);
        }
    }

    /** Format class name for use in root editor dialogs.
     *  @param className class name obtained from {@link SourceCodeSelection#getClassName() }
     *  @return class name formated to human readable form
     */
    public static String formatClassName(String className) {
        return classNameFormatter.formatMethodName(className, "", "").toFormatted();
    }

    /** Parse user input text to the class name. Inversion of {@link #formatClassName(String)}.
     *  @param text user input
     *  @param allowWildcards enable wildcards in the class name
     *  @return class name for use in {@link SourceCodeSelection} or <code>null</code> in case of malformed input.
     */
    public static String parseClassName(String text, boolean allowWildcards) {
        if(text.isEmpty()) {
            return null;
        }
        //irregularities for default package
        if(text.equals(".*")) {
            return allowWildcards ? "" : null;
        }
        if(text.equals(".**")) {
            return null;
        }

        String[] components = text.split("\\.", -1);//NOI18N
        int len = components.length;
        String last = components[len-1];
        
        if(len == 1) {
            //class in default package
            if(text.startsWith("*")) {
                //only $** suffix allowed
                return null;
            }
            return checkWildcards(text, allowWildcards, false) ? text: null;
        }
        for(int i = 0; i < len; i++) {
            if(components[i].isEmpty()) {
                //missing component name
                return null;
            }
            if(i < len-1 && components[i].contains("*")) {//NOI18N
                //wildcards are allowed only in the last component
                return null;
            }
        }
        if(checkWildcards(last, allowWildcards, true)) {
            return text;
        }
        return null;
    }

    /** Check wildcards in the last component of the class name */
    private static boolean checkWildcards(String last, boolean allowWildcards, boolean allowSubPackages) {
        int wildcard = last.indexOf('*');//NOI18N
        if(wildcard == -1) {
            return true;
        }
        if(!allowWildcards) {
            return false;
        }
        if(wildcard == last.length()-1) {
            return true;
        }
        if(!allowSubPackages) {
            return false;
        }
        if(wildcard != last.length()-2 || !last.endsWith("*")) {//NOI18N
            // ** must be at the end of class name
            return false;
        }        
        return wildcard == 0 || last.charAt(wildcard-1) == '$';//NOI18N
    }
}
