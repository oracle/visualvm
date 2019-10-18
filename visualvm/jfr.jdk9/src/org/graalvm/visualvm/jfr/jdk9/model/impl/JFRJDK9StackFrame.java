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
package org.graalvm.visualvm.jfr.jdk9.model.impl;

import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import org.graalvm.visualvm.jfr.model.JFRMethod;
import org.graalvm.visualvm.jfr.model.JFRStackFrame;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRJDK9StackFrame extends JFRStackFrame {
    
    private final RecordedFrame stackFrame;
    
    
    JFRJDK9StackFrame(RecordedFrame stackFrame) {
        this.stackFrame = stackFrame;
    }
    

    @Override
    public JFRMethod getMethod() {
        RecordedMethod method = stackFrame.getMethod();
        return method == null ? null : new JFRJDK9Method(method);
    }

    @Override
    public int getLine() {
        return stackFrame.getLineNumber();
    }

    @Override
    public String getType() {
        return stackFrame.getType();
    }
    
    
    @Override
    public int hashCode() {
        return stackFrame.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK9StackFrame ? stackFrame.equals(((JFRJDK9StackFrame)o).stackFrame) : false;
    }
    
}
