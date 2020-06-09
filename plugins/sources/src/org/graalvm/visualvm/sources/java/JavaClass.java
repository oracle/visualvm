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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Jiri Sedlacek
 */
final class JavaClass {
    
    private final String name;
    
    private final String source;
    
    private final int nameStart;
    private final int bodyStart;
    private final int bodyEnd;
    
    private List<JavaClass> namedClasses;
    private List<JavaClass> anonymousClasses;
    
    
    private JavaClass(String name, String source, int nameStart, int bodyStart, int bodyEnd) {
        this.name = name;
        
        this.source = source;
        
        this.nameStart = nameStart;
        this.bodyStart = bodyStart;
        this.bodyEnd = bodyEnd;
    }
    
    
    String getName() { return name; }
    
    
    int getNameStart() { return nameStart; }
    
    int getBodyStart() { return bodyStart; }
    
    int getBodyEnd() { return bodyEnd; }
    
    
    String getSource() {
        return source;
    }
    
    
    JavaClass getClass(String className) {
        if (className == null || className.isEmpty()) return null;
        
        // TODO: lambdas not supported yet!
        if (className.startsWith(JavaSourceUtils.LAMBDA_CLASS_PREFIX_MASK)) return null;
        
        String cIndex = null;
        String cName  = null;
        
        if (Character.isDigit(className.charAt(0))) {
            Pattern p = Pattern.compile(JavaSourceUtils.ANONYMOUS_LOCAL_CLASSNAME_REGEX);
            Matcher m = p.matcher(className);
            if (m.matches()) {
                cIndex = m.group(JavaSourceUtils.REGEX_GROUP_INDEX);
                cName = m.group(JavaSourceUtils.REGEX_GROUP_NAME);
                if (cName.isEmpty()) cName = null;
            }
        }
        
        int searchCount = cName == null ? 1 : Integer.parseInt(cIndex);
        String searchClass = cName == null ? className : cName;
        
        List<JavaClass> classes = cIndex == null || cName != null ? getNamedClasses() : getAnonymousClasses();
        
        for (JavaClass cls : classes)
            if (searchClass.equals(cls.getName()))
                if (--searchCount == 0)
                    return cls;
        
        return null;
    }
    
    private List<JavaClass> getNamedClasses() {
        if (namedClasses == null) namedClasses = populateNamedClasses(source, bodyStart, bodyEnd);
        return namedClasses;
    }
    
    private List<JavaClass> getAnonymousClasses() {
        if (anonymousClasses == null) anonymousClasses = populateAnonymousClasses(source, getNamedClasses(), bodyStart, bodyEnd);
        return anonymousClasses;
    }
    
    
    JavaMethod getMethod(String methodName, String methodSignature) {
        return JavaMethod.findMethod(methodName, methodSignature, this);
    }
    
    
    @Override
    public int hashCode() {
        return nameStart;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JavaClass)) return false;
        if (o == this) return true;
        return ((JavaClass)o).nameStart == nameStart;
    }
    
    @Override
    public String toString() {
        return "class " + name /*+ " (nameStart=" + nameStart + ", bodyStart=" + bodyStart + ", bodyEnd=" + bodyEnd + ")"*/; // NOI18N
    }
    
    
    static JavaClass fromSource(String source, String className) {
        List<JavaClass> classes = fromSource(source);
        
        for (JavaClass cls : classes)
            if (className.equals(cls.getName()))
                return cls;
        
        return null;
    }
    
    private static List<JavaClass> fromSource(String source) {
        return populateNamedClasses(source, 0, source.length());
    }
    
    
    private static List<JavaClass> populateNamedClasses(String source, int startOffset, int endOffset) {
        List<JavaClass> classes = new ArrayList();
        
        Pattern pattern = Pattern.compile(JavaSourceUtils.CLASS_REGEX);
        Matcher matcher = pattern.matcher(source);
        
        while (startOffset <= endOffset && matcher.find(startOffset)) {
            int offset = matcher.end() - 1;
            
            String name = matcher.group(JavaSourceUtils.REGEX_GROUP_NAME);
            int nameStart = offset - name.length();
            
            int[] bodyOffsets = JavaSourceUtils.getBlockBounds(source, offset, '{', '}'); // NOI18N
            if (bodyOffsets[0] == -1 || bodyOffsets[1] == -1 || bodyOffsets[1] > endOffset) break;
            
            classes.add(new JavaClass(name, source, nameStart, bodyOffsets[0], bodyOffsets[1]));
            
            startOffset = bodyOffsets[1] + 1;
        }
        
        return classes;
    }
    
    private static List<JavaClass> populateAnonymousClasses(String source, List<JavaClass> innerClasses, int startOffset, int endOffset) {
        List<JavaClass> classes = new ArrayList();
        
        String _source = JavaSourceUtils.maskClasses(source, innerClasses);

        Pattern pattern = Pattern.compile(JavaSourceUtils.ANONYMOUS_CLASS_REGEX);
        Matcher matcher = pattern.matcher(_source);
        
        while (startOffset <= endOffset && matcher.find(startOffset)) {
            startOffset = matcher.end();
            
            // Generics
            if (_source.charAt(startOffset) == '<') {                           // NOI18N
                startOffset = JavaSourceUtils.skipBlock(_source, startOffset, '<', '>'); // NOI18N
                startOffset = JavaSourceUtils.skipWhiteSpaces(_source, startOffset);
            }
            
            // Array definition
            if (_source.charAt(startOffset) == '[') {                           // NOI18N
                while (_source.charAt(startOffset) == '[' && startOffset < endOffset) // NOI18N
                    startOffset = JavaSourceUtils.skipBlock(_source, startOffset, '[', ']'); // NOI18N
                startOffset = JavaSourceUtils.skipWhiteSpaces(_source, startOffset);
            // Anonymous class
            } else {
                // Not expected
                if (_source.charAt(startOffset) != '(') {                       // NOI18N
                    // nothing we can do here, just search again
                } else {
                    // TODO do not skipBlock to search the parameters as well
                    // but the numbering of anonymous classes becomes broken
                    startOffset = JavaSourceUtils.skipBlock(_source, startOffset, '(', ')'); // NOI18N
                    startOffset = JavaSourceUtils.skipWhiteSpaces(_source, startOffset);
                    // Object creation only
                    if (_source.charAt(startOffset) != '{') {                   // NOI18N
                        // nothing we can do here, just search again
                    // Anonymous class
                    } else {
                        int nameOffset = JavaSourceUtils.skipWhiteSpaces(_source, matcher.start() + "_new".length()); // NOI18N
                        
                        int[] bodyOffsets = JavaSourceUtils.getBlockBounds(_source, startOffset, '{', '}'); // NOI18N
                        if (bodyOffsets[0] == -1 || bodyOffsets[1] == -1 || bodyOffsets[1] > endOffset) break;
                        
                        classes.add(new JavaClass(Integer.toString(classes.size() + 1), source, nameOffset, bodyOffsets[0], bodyOffsets[1]));
                        
                        startOffset = bodyOffsets[1] + 1;
                    }
                }
            }
        }
        
        return classes;
    }
    
}
