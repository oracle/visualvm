/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.sampler;

import java.util.List;
import org.graalvm.visualvm.jfr.model.JFRMethod;
import org.graalvm.visualvm.jfr.model.JFRStackFrame;
import org.graalvm.visualvm.jfr.model.JFRStackTrace;
import org.graalvm.visualvm.jfr.model.JFRThread;
import org.graalvm.visualvm.lib.jfluid.filters.InstrumentationFilter;
import org.graalvm.visualvm.lib.jfluid.results.cpu.StackTraceSnapshotBuilder;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRThreadInfoSupport {
    
    static StackTraceSnapshotBuilder.SampledThreadInfo getInfo(JFRThread thread, JFRStackTrace stack, String state, InstrumentationFilter filter) {
        return getInfo(thread, stack, state(state), filter);
    }
    
    static StackTraceSnapshotBuilder.SampledThreadInfo getInfo(JFRThread thread, JFRStackTrace stack, Thread.State state, InstrumentationFilter filter) {
        String name = thread.getName();
        long id = thread.getId();
        StackTraceElement[] stackTrace = stackTrace(stack);
        
        return new StackTraceSnapshotBuilder.SampledThreadInfo(name, id, state, stackTrace, filter);
    }
    
    
    private static StackTraceElement[] stackTrace(JFRStackTrace stack) {
        List<JFRStackFrame> frames = stack.getFrames();
        StackTraceElement[] elements = new StackTraceElement[frames.size()];
        
        for (int i = 0; i < frames.size(); i++)
            elements[i] = stackTraceElement(frames.get(i));
        
        return elements;
    }
    
    private static StackTraceElement stackTraceElement(JFRStackFrame frame) {
        JFRMethod method = frame.getMethod();
        String className = method == null ? "<unknown class>" : method.getType().getName();
        if (className == null) className = "<unknown class>";
        String methodName = method == null ? "<unknown method>" : method.getName(); // TODO: add signature!
        if (methodName == null) methodName = "<unknown method>";
//        String methodName = method.getName() + method.getDescriptor(); // TODO: add signature!
        int lineNumber = "Native".equals(frame.getType()) ? -2 : frame.getLine(); // NOI18N
        
        return new StackTraceElement(className, methodName, null, lineNumber);
    }
    
    private static Thread.State state(String state) {
        if ("STATE_RUNNABLE".equals(state)) return Thread.State.RUNNABLE;
        return Thread.State.RUNNABLE; // TODO: add other states!
    }
    
}
