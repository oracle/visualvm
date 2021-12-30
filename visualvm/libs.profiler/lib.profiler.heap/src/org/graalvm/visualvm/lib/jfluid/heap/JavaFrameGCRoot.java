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
 * This represents one Java Frame GC root. It has kind ({@link GCRoot#JAVA_FRAME}) and also corresponding
 * {@link Instance}, which is actual GC root and represent a local variable held on the stack.
 * @author Tomas Hurka
 */
public interface JavaFrameGCRoot extends GCRoot {

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * returns Thread root GC object for the thread where this local variable is held.
     * <br>
     * speed:normal
     * @return {@link ThreadObjectGCRoot} for the corresponding thread.
     */
    ThreadObjectGCRoot getThreadGCRoot();

    /**
     * frame number in stack trace.
     * <br>
     * Speed:fast
     * @return frame number in stack trace (-1 for empty)
     */
    int getFrameNumber();
}
