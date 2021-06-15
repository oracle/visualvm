/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.client;

import org.graalvm.visualvm.lib.jfluid.classfile.ClassInfo;
import org.graalvm.visualvm.lib.jfluid.classfile.ClassRepository;


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
        if (!(o instanceof RuntimeProfilingPoint)) return -1;
        return getId() - ((RuntimeProfilingPoint)o).getId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RuntimeProfilingPoint other = (RuntimeProfilingPoint) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + this.id;
        return hash;
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
