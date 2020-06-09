/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources.truffle;

import org.graalvm.visualvm.sources.SourceHandle;
import org.graalvm.visualvm.sources.SourceHandleUtils;
import org.graalvm.visualvm.sources.SourcePathHandle;

/**
 *
 * @author Jiri Sedlacek
 */
final class TruffleSourceHandle extends SourceHandle {
    
    private final String language;

    private final String className;
    private final String methodName;
    private final String methodSignature;
    
    private final int line;
    
    private String text;
    
    private int column;
    private int offset;
    
    private final SourcePathHandle pathHandle;
    
    
    TruffleSourceHandle(String language, String className, String methodName, String methodSignature, int line, SourcePathHandle pathHandle) {
        this.language = language;
        
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        
        this.line = line;
        
        this.offset = -1;
        this.column = -1;
        
        this.pathHandle = pathHandle;
    }
    
    
    @Override
    public String getLanguage() {
        return language;
    }
    

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getMethodSignature() {
        return methodSignature;
    }

    
    @Override
    public int getLine() {
        return line;
    }
    
    @Override
    public int getColumn() {
        if (column == -1) {
            column = 1;
        }
        return column;
    }

    @Override
    public int getOffset() {
        if (offset == -1) offset = SourceHandleUtils.lineToOffset(getText(), getLine());
        return offset;
    }

    
    @Override
    public String getText() {
        if (text == null) text = pathHandle.readText();
        return text;
    }

    
    @Override
    public String getSourceUri() {
        return pathHandle.getPath().toUri().toString();
    }

    @Override
    public String getSourceFile() {
        return pathHandle.getRegularPath().toAbsolutePath().toString();
    }

    
    @Override
    protected void close() {
        pathHandle.close();
    }
    
}
