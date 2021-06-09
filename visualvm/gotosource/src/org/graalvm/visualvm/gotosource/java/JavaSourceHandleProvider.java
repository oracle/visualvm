/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource.java;

import org.graalvm.visualvm.gotosource.SourceHandle;
import org.graalvm.visualvm.gotosource.SourceHandleProvider;
import org.graalvm.visualvm.gotosource.SourcePathHandle;
import org.graalvm.visualvm.gotosource.SourcesRoot;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service=SourceHandleProvider.class, position = 100)
public final class JavaSourceHandleProvider extends SourceHandleProvider {
    
    @Override
    public SourceHandle createHandle(String className, String methodName, String methodSignature, int line) {
        String toplevelClassName = JavaSourceUtils.toplevelClassName(className);
        String toplevelClassFile = JavaSourceUtils.toplevelClassFile(toplevelClassName);
        
        SourcePathHandle pathHandle = SourcesRoot.getPathHandle(toplevelClassFile);
        return pathHandle == null ? null : new JavaSourceHandle(className, methodName, methodSignature, line, pathHandle);
    }
    
}
