/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.jdk11.model.impl;

import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedMethod;
import org.graalvm.visualvm.jfr.model.JFRClass;
import org.graalvm.visualvm.jfr.model.JFRMethod;

/**
 *
 * @author Jiri Sedlacek
 */
final class JFRJDK11Method extends JFRMethod {
    
    private final RecordedMethod method;
    
    
    JFRJDK11Method(RecordedMethod method) {
        this.method = method;
    }

    
    @Override
    public String getName() {
        return method.getName();
    }
    
    @Override
    public String getDescriptor() {
        return method.getDescriptor();
    }

    @Override
    public JFRClass getType() {
        RecordedClass rclass = method.getType();
        return rclass == null ? null : new JFRJDK11Class(rclass);
    }
    
    
    @Override
    public int hashCode() {
        return method.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof JFRJDK11Method ? method.equals(((JFRJDK11Method)o).method) : false;
    }
    
}
