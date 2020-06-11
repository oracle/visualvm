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
package org.graalvm.visualvm.sources.java;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.visualvm.sources.SourceHandle;
import org.graalvm.visualvm.sources.SourceHandleUtils;
import org.graalvm.visualvm.sources.SourcePathHandle;

/**
 *
 * @author Jiri Sedlacek
 */
final class JavaSourceHandle extends SourceHandle {
    
    private static final String LANGUAGE_ID = "java";                           // NOI18N
    
    private final String className;
    private final String methodName;
    private final String methodSignature;
    
    private String text;
    private int line;
    private int column;
    private int offset;
    private int endOffset;
    
    private final SourcePathHandle pathHandle;
    
    
    JavaSourceHandle(String className, String methodName, String methodSignature, int line, SourcePathHandle pathHandle) {
        this.className = className;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        
        this.line = line;
        
        this.offset = -1;
        this.endOffset = -1;
        this.column = -1;
        
        this.pathHandle = pathHandle;
    }
    
    
    @Override
    public String getLanguage() {
        return LANGUAGE_ID;
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
        if (line == -1) {
            int[] line_column = SourceHandleUtils.offsetToLineColumn(getText(), getOffset());
            line = line_column[0];
            column = line_column[1];
        }
        return line;
    }
    
    @Override
    public int getColumn() {
        if (column == -1) {
            int[] line_column = SourceHandleUtils.offsetToLineColumn(getText(), getOffset());
            line = line_column[0];
            column = line_column[1];
        }
        return column;
    }

    @Override
    public int getOffset() {
        if (offset == -1) {
            if (methodName == null || methodName.isEmpty() || methodName.startsWith("*")) { // NOI18N
                offset = JavaSourceUtils.classDefinitionOffset(getText(), className, false);
            } else {
                offset = JavaSourceUtils.methodDefinitionOffset(getText(), className, methodName, methodSignature, false);
            }
            if (offset == -1) offset = 0;
        }
        return offset;
    }
    
    @Override
    public int getEndOffset() {
        if (endOffset == -1) {
            int _offset = getOffset();
            String _text = getText();

            if (_text.charAt(_offset) == '{') {                                 // NOI18N
                endOffset = _offset + 1;
            } else {
                Pattern pattern = Pattern.compile(JavaSourceUtils.FULLY_QUALIFIED_IDENTIFIER_REGEX);
                Matcher matcher = pattern.matcher(_text);
                endOffset = _offset + (matcher.find(_offset) ? matcher.group().length() : 0);
            }
        }
        return endOffset;
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
