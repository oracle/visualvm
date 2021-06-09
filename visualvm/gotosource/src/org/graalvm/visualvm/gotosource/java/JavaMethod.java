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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jiri Sedlacek
 */
final class JavaMethod {
    
    private final String name;
    private final String signature;
    
//    private final String source;
    
    private final int nameStart;
    private final int bodyStart;
    private final int bodyEnd;
    
    
    private JavaMethod(String name, String signature, String source, int nameStart, int bodyStart, int bodyEnd) {
        this.name = name;
        this.signature = signature;
        
//        this.source = source;
        
        this.nameStart = nameStart;
        this.bodyStart = bodyStart;
        this.bodyEnd = bodyEnd;
    }
    
    
    String getName() { return name; }
    
    String getSignature() { return signature; }
    
    
    int getNameStart() { return nameStart; }
    
    int getBodyStart() { return bodyStart; }
    
    int getBodyEnd() { return bodyEnd; }
    
    
    @Override
    public int hashCode() {
        return nameStart;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaMethod)) return false;
        if (o == this) return true;
        return ((JavaMethod)o).nameStart == nameStart;
    }
    
    @Override
    public String toString() {
        return "method " + name + " (nameStart=" + nameStart + ", bodyStart=" + bodyStart + ", bodyEnd=" + bodyEnd + ")"; // NOI18N
    }
    
    
    static JavaMethod findMethod(String methodName, String methodSignature, JavaClass cls) {
        // TODO: lambdas not supported yet!
        if (methodName.contains("lambda$")) return null;                        // NOI18N
        
        String source = JavaSourceUtils.maskNonBlock(cls.getSource(), '{', '}', cls.getBodyStart(), cls.getBodyEnd()); // NOI18N
        
        if ("<clinit>".equals(methodName)) return findClassInitializer(cls, source); // NOI18N
        else if ("<init>".equals(methodName)) return findInstanceInitializer(cls, methodSignature, source); // NOI18N
        
        // Regular method with body
        JavaMethod method = findMethodWithBody(methodName, methodName, methodSignature, cls, source);
        if (method != null) return method;
        
        // Native method without body (abstract & interface methods not displayed in results)
        return findMethodWithoutBody(methodName, methodSignature, cls, source);
    }
    
    private static JavaMethod findClassInitializer(JavaClass cls, String source) {
        int offset = cls.getBodyStart() + 1;
        
        String patternS = JavaSourceUtils.CLASS_INITIALIZER_REGEX;
        Pattern pattern = Pattern.compile(patternS);
        Matcher matcher = pattern.matcher(source);
        
        if (!matcher.find(offset)) return null;
        
        int bodyEnd = cls.getBodyEnd();
        offset = matcher.end();
        if (offset > bodyEnd) return null;
        
        offset--; // CLASS_INITIALIZER_REGEX matched the opening '{'
        int[] bodyOffsets = JavaSourceUtils.getBlockBounds(source, offset, '{', '}'); // NOI18N
        if (bodyOffsets[0] == -1 || bodyOffsets[1] == -1 || bodyOffsets[1] > bodyEnd) return null;
        
        return new JavaMethod("<clinit>", null, source, offset, bodyOffsets[0], bodyOffsets[1]); // NOI18N
    }
    
    private static JavaMethod findInstanceInitializer(JavaClass cls, String methodSignature, String source) {
        JavaMethod constructor = findMethod(cls.getName(), "<init>", methodSignature, cls, source, // NOI18N
                                            JavaSourceUtils.DEFINED_METHOD_WITHBODY_START_REGEX,
                                            JavaSourceUtils.DEFINED_METHOD_WITHBODY_END_REGEX, false);
        if (constructor != null) return constructor;
        
        int offset = cls.getBodyStart();
        
        String patternS = JavaSourceUtils.INSTANCE_INITIALIZER_REGEX;
        Pattern pattern = Pattern.compile(patternS);
        Matcher matcher = pattern.matcher(source);
        
        if (!matcher.find(offset)) return null;
        
        int bodyEnd = cls.getBodyEnd();
        offset = matcher.end();
        if (offset > bodyEnd) return null;
        
        offset--; // INSTANCE_INITIALIZER_REGEX matched the opening '{'
        int[] bodyOffsets = JavaSourceUtils.getBlockBounds(source, offset, '{', '}'); // NOI18N
        if (bodyOffsets[0] == -1 || bodyOffsets[1] == -1 || bodyOffsets[1] > bodyEnd) return null;
        
        return new JavaMethod("<init>", null, source, offset, bodyOffsets[0], bodyOffsets[1]); // NOI18N
    }
    
    private static JavaMethod findMethodWithBody(String methodName, String modelName, String methodSignature, JavaClass cls, String source) {
        return findMethod(methodName, modelName, methodSignature, cls, source,
                          JavaSourceUtils.DEFINED_METHOD_WITHBODY_START_REGEX,
                          JavaSourceUtils.DEFINED_METHOD_WITHBODY_END_REGEX, false);
    }
    
    private static JavaMethod findMethodWithoutBody(String methodName, String methodSignature, JavaClass cls, String source) {
        return findMethod(methodName, methodName, methodSignature, cls, source,
                          JavaSourceUtils.DEFINED_METHOD_WITHOUTBODY_START_REGEX,
                          JavaSourceUtils.DEFINED_METHOD_WITHOUTBODY_END_REGEX, true);
    }
    
    private static JavaMethod findMethod(String methodName, String modelName, String methodSignature, JavaClass cls, String source, String startRegEx, String endRegEx, boolean withoutBody) {
        int offset = cls.getBodyStart() + 1;
        int bodyEnd = cls.getBodyEnd();
        
        String patternS = startRegEx.replace(JavaSourceUtils.REGEX_PARAMETER_0, methodName);
        Pattern pattern = Pattern.compile(patternS);
        Matcher startMatcher = pattern.matcher(source);
        Matcher endMatcher = null;
        
        while (startMatcher.find(offset) && offset < bodyEnd) {
            int nameStart = startMatcher.start() + startMatcher.group(JavaSourceUtils.REGEX_GROUP_PREFIX).length();
             
            offset = startMatcher.end() - 1;
            if (offset > bodyEnd) return null;
            
            // Skip method parameters
            offset = JavaSourceUtils.skipBlock(source, offset, '(', ')');       // NOI18N
            if (offset > bodyEnd) return null;
            
            if (endMatcher == null) {
                pattern = Pattern.compile(endRegEx);
                endMatcher = pattern.matcher(source);
            }
            
            // Search for method declaration
            if (endMatcher.find(offset)) {
                offset = endMatcher.end() - 1;
                if (offset > bodyEnd) return null;

                if (withoutBody && ';' == source.charAt(offset)) return new JavaMethod(methodName, methodSignature, source, nameStart, -1, -1); // NOI18N

                // Search for method body
                int[] bodyOffsets = JavaSourceUtils.getBlockBounds(source, offset, '{', '}'); // NOI18N
                if (bodyOffsets[0] == -1 || bodyOffsets[1] == -1 || bodyOffsets[1] > bodyEnd) return null;

                return new JavaMethod(modelName, methodSignature, source, nameStart, bodyOffsets[0], bodyOffsets[1]); // NOI18N
            }
        }
        
        return null;
    }
    
}
