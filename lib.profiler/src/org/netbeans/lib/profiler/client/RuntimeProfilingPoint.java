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

package org.netbeans.lib.profiler.client;

import org.netbeans.lib.profiler.classfile.ClassInfo;
import org.netbeans.lib.profiler.classfile.ClassRepository;


/**
 * Represents a Profiling point.
 * Its lifetime must not span across sessions.
 *
 * @author Tomas Hurka
 * @author Maros Sandor
 */
public class RuntimeProfilingPoint implements Comparable {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    /**
     * Encapsulates an event of hitting a profiling point.
     */
    public static class HitEvent {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final int id;
        private final int threadId;
        private final long timestamp;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public HitEvent(int id, long timestamp, int threadId) {
            this.id = id;
            this.timestamp = timestamp;
            this.threadId = threadId;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getId() {
            return id;
        }

        public int getThreadId() {
            return threadId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String toString() {
            return "HitEvent [id=" + getId() + ", thread id=" + getThreadId() + ", timestamp=" + getTimestamp() + "]"; //NOI18N
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    /**
     * Name of the class where the profiling point resides in the form: package.Class.Inner
     */
    private final String className;

    /**
     * Name of the method where this profiling point resides. This is mutually exclusive with the line field and means
     * "beginning of a method".
     */
    private final String methodName;

    /**
     * Signature of the method where this profiling point resides. This is mutually exclusive with the line field and
     * supplements the methodName field. May be null to indicate 'the first method with the given name'.
     */
    private final String methodSignature;

    /**
     * Handler for hit events.
     */
    private final String serverHandlerClass;

    /**
     * Additional arbitrary info send to server
     */
    private final String serverInfo;

    /**
     * ID identifies this profiling point in eventbuffer events.
     */
    private final int id;

    /**
     * Line number where this profiling point resides. This is mutually exclusive with the method field.
     */
    private final int line;

    /**
     * Line offset where this profiling point resides. This is mutually exclusive with the method field.
     */
    private final int offset;

    /**
     * Bytecode index; filled at runtime when the class loads and we inject profile point hit methods.
     */
    private int bci;

    /**
     * Method index; filled at runtime when the class loads and we inject profile point hit methods.
     */
    private int methodIdx = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new profiling point.
     *
     *
     *
     * @param id unique ID of this profiling point
     * @param className name of the class where this profiling point is placed
     * @param line profiling point location in the class. If line is positive, it is be inserted just before the line executes.
     *             If line is negative, it is inserted just after the line executes.
     * @param offset line offset
     * @param serverHandlerClass handles hits of this profiling point on server side (fully qualified class name)
     */
    public RuntimeProfilingPoint(int id, String className, int line, int offset, String serverHandlerClass, String serverInfo) {
        this(id, className, line, offset, null, null, serverHandlerClass, serverInfo);
    }

    /**
     * Creates a new profiling point.
     *
     *
     *
     * @param id unique ID of this profiling point
     * @param className name of the class where this profiling point is placed
     * @param methodName name of the method where this profiling point is placed
     * @param methodSignature signature of the method where this profiling point is placed
     * @param serverHandlerClass handles hits of this profiling point on server side (fully qualified class name)
     */
    public RuntimeProfilingPoint(int id, String className, String methodName, String methodSignature, String serverHandlerClass,
                                 String serverInfo) {
        this(id, className, -1, -1, methodName, methodSignature, serverHandlerClass, serverInfo);
    }

    private RuntimeProfilingPoint(int id, String className, int line, int offset, String methodName, String methodSignature,
                                  String serverHandlerClass, String serverInfo) {
        this.id = id;
        this.className = className;
        this.line = line;
        this.offset = offset;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.serverHandlerClass = serverHandlerClass;
        this.serverInfo = serverInfo;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getBci() {
        return bci;
    }

    public String getClassName() {
        return className;
    }

    public int getId() {
        return id;
    }

    public int getMethodIdx() {
        return methodIdx;
    }

    public String getServerHandlerClass() {
        return serverHandlerClass;
    }

    public String getServerInfo() {
        return serverInfo;
    }

    public boolean resolve(ClassInfo classInfo) {
        if (methodIdx != -1) {
            return true;
        }

        return (methodName != null) ? resolveMethodName(classInfo) : resolveLineNumber(classInfo);
    }

    public String toString() {
        return "RuntimeProfilingPoint [id=" + id + ", classname=" + className + ", line=" + line + ", offset=" + offset
               + ", server handler=" + serverHandlerClass + "]"; //NOI18N
    }

    public int compareTo(Object o) {
       return getId() - ((RuntimeProfilingPoint)o).getId();
    }

    private boolean resolveLineNumber(ClassInfo classInfo) {
        try {
            //      int ln = Math.abs(line);
            ClassRepository.CodeRegionBCI crbci = ClassRepository.getMethodForSourceRegion(classInfo, line, line);
            methodIdx = classInfo.getMethodIndex(crbci.methodName, crbci.methodSignature);
            bci = (offset == Integer.MAX_VALUE) ? crbci.bci1 : crbci.bci0;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean resolveMethodName(ClassInfo classInfo) {
        if (methodSignature != null) {
            methodIdx = classInfo.getMethodIndex(methodName, methodSignature);
        } else {
            String[] allNames = classInfo.getMethodNames();

            for (int i = 0; i < allNames.length; i++) {
                if (methodName.equals(allNames[i])) {
                    methodIdx = i;

                    break;
                }
            }
        }

        if (methodIdx == -1) {
            return false;
        }

        bci = classInfo.getLineNumberTables().getStartPCs()[methodIdx][0];

        return true;
    }
}
