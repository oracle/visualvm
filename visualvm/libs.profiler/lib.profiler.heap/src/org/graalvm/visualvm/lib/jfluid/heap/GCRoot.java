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

package org.graalvm.visualvm.lib.jfluid.heap;


/**
 * This represents one GC root. It has kind ({@link GCRoot#JNI_GLOBAL}, etc.) and also corresponding
 * {@link Instance}, which is actual GC root.
 * @author Tomas Hurka
 */
public interface GCRoot {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    /**
     * JNI global GC root kind.
     */
    public static final String JNI_GLOBAL = "JNI global"; // NOI18N

    /**
     * JNI local GC root kind.
     */
    public static final String JNI_LOCAL = "JNI local"; // NOI18N

    /**
     * Java frame GC root kind.
     */
    public static final String JAVA_FRAME = "Java frame"; // NOI18N

    /**
     * Native stack GC root kind.
     */
    public static final String NATIVE_STACK = "native stack"; // NOI18N

    /**
     * Sticky class GC root kind.
     */
    public static final String STICKY_CLASS = "sticky class"; // NOI18N

    /**
     * Thread block GC root kind.
     */
    public static final String THREAD_BLOCK = "thread block"; // NOI18N

    /**
     * Monitor used GC root kind.
     */
    public static final String MONITOR_USED = "monitor used"; // NOI18N

    /**
     * Thread object GC root kind.
     */
    public static final String THREAD_OBJECT = "thread object"; // NOI18N

    /**
     * Unknown GC root kind.
     */
    public static final String UNKNOWN = "unknown"; // NOI18N

    /**
     * Interned string GC root kind.
     */
    public static final String INTERNED_STRING = "interned string"; // NOI18N

    /**
     * Finalizing GC root kind.
     */
    public static final String FINALIZING = "finalizing"; // NOI18N

    /**
     * Debugger GC root kind.
     */
    public static final String DEBUGGER = "debugger"; // NOI18N

    /**
     * Reference cleanup GC root kind.
     */
    public static final String REFERENCE_CLEANUP = "reference cleanup"; // NOI18N

    /**
     * VM internal GC root kind.
     */
    public static final String VM_INTERNAL = "VM internal"; // NOI18N

    /**
     * JNI monitor GC root kind.
     */
    public static final String JNI_MONITOR = "JNI monitor"; // NOI18N

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * returns corresponding {@link Instance}, which is GC root.
     * <br>
     * Speed:normal
     * @return GC root instance
     */
    Instance getInstance();

    /**
     * returns kind of this GC root.
     * <br>
     * Speed:fast
     * @return human readable GC root kind.
     */
    String getKind();
}
