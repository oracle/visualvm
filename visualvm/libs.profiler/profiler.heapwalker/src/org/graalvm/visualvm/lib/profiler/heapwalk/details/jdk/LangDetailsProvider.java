/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk;

import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsProvider;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=DetailsProvider.class)
public class LangDetailsProvider extends DetailsProvider.Basic {
    private static final String ENUM_MASK = "java.lang.Enum+";                    // NOI18N
    private static final String STACKTRACE_MASK = "java.lang.StackTraceElement";    // NOI18N

    public LangDetailsProvider() {
        super(ENUM_MASK, STACKTRACE_MASK);
    }

    public String getDetailsString(String className, Instance instance) {
        switch (className) {
            case ENUM_MASK: { // Enum+
                String name = DetailsUtils.getInstanceFieldString(instance, "name"); // NOI18N
                int ordinal = DetailsUtils.getIntFieldValue(instance, "ordinal", -1); // NOI18N
                if (name != null) {
                    if (ordinal != -1) {
                        return name+" ("+ordinal+")";       // NOI18N
                    }
                    return name;
                }
                break;
            }
            case STACKTRACE_MASK: { // StackTraceElement
                String declaringClass = DetailsUtils.getInstanceFieldString(instance, "declaringClass"); // NOI18N
                if (declaringClass != null) {
                    String methodName = DetailsUtils.getInstanceFieldString(instance, "methodName"); // NOI18N
                    String fileName = DetailsUtils.getInstanceFieldString(instance, "fileName"); // NOI18N
                    int lineNumber = DetailsUtils.getIntFieldValue(instance, "lineNumber", -1); // NOi18N
                    if (methodName == null) methodName = "Unknown method";   // NOI18N
                    StackTraceElement ste = new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
                    return ste.toString();
                }
                break;
            }
            default:
                break;
        }
        
        return null;
    }
    
}
