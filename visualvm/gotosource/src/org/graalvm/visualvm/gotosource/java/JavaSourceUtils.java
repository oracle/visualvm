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

import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author Jiri Sedlacek
 */
final class JavaSourceUtils {
    
    private static final char COMMENT_MASK_CHAR = ' ';                          // NOI18N
    private static final char STRING_MASK_CHAR = '+';                           // NOI18N
    private static final char NONBLOCK_MASK_CHAR = '=';                         // NOI18N
    private static final char CLASS_MASK_CHAR = '_';                            // NOI18N
    
    private static final String LAMBDA_CLASS_PREFIX = "$Lambda$";               // NOI18N
    static final String LAMBDA_CLASS_PREFIX_MASK = "-Lambda-";                  // NOI18N
    
    static final String IDENTIFIER_REGEX = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*"; // NOI18N
    static final String FULLY_QUALIFIED_IDENTIFIER_REGEX = "(" + IDENTIFIER_REGEX + "\\.)*" + IDENTIFIER_REGEX; // NOI18N
    private static final String THROWS_REGEX = "throws\\s+(" + FULLY_QUALIFIED_IDENTIFIER_REGEX + "\\s*,\\s*)*" + FULLY_QUALIFIED_IDENTIFIER_REGEX; // NOI18N
    
    
    static final String REGEX_PARAMETER_0 = "{#$0#}";                           // NOI18N
    
    static final String REGEX_GROUP_NAME = "name";                              // NOI18N
    static final String REGEX_GROUP_INDEX = "index";                            // NOI18N
    static final String REGEX_GROUP_PREFIX = "prefix";                          // NOI18N
    
    
    static final String ANONYMOUS_LOCAL_CLASSNAME_REGEX = "(?<" + REGEX_GROUP_INDEX + ">[\\d]+)(?<" + REGEX_GROUP_NAME + ">[\\D]*)"; // NOI18N
    static final String CLASS_REGEX = "(^|[\\W&&[^.]])(class|interface|enum)\\s+(?<" + REGEX_GROUP_NAME + ">" + IDENTIFIER_REGEX + ")[^\\p{javaJavaIdentifierPart}]"; // NOI18N
    static final String ANONYMOUS_CLASS_START_REGEX = "\\Wnew\\s*(\\s|\\<)"; // NOI18N
    static final String ANONYMOUS_CLASS_END_REGEX = "\\G\\s*(?<" + REGEX_GROUP_NAME + ">" + FULLY_QUALIFIED_IDENTIFIER_REGEX + ")\\s*"; // NOI18N
    static final String ANONYMOUS_CLASS_METHOD_REGEX = "\\G\\s*.s*\\(";         // NOI18N
    
    static final int SHORTEST_ANNONYMOUS_LENGTH = "new X(){}".length();
    
    static final String CLASS_INITIALIZER_REGEX = "\\Wstatic\\s*\\{";           // NOI18N
    static final String INSTANCE_INITIALIZER_REGEX = "[\\{\\};]\\s*\\{";        // NOI18N
    
    static final String DEFINED_METHOD_WITHBODY_START_REGEX = "(?<" + REGEX_GROUP_PREFIX + ">[\\s\\>])" + REGEX_PARAMETER_0 + "\\s*\\("; // NOI18N
    static final String DEFINED_METHOD_WITHBODY_END_REGEX = "\\G\\s*(" + THROWS_REGEX + ")??\\s*\\{"; // NOI18N
    /* TODO: review */ static final String DEFINED_METHOD_WITHOUTBODY_START_REGEX = "(?<" + REGEX_GROUP_PREFIX + ">\\Wnative[\\s\\S&&[^;]&&[^\\(]]*?[\\s\\>])" + REGEX_PARAMETER_0 + "\\s*\\("; // NOI18N
    static final String DEFINED_METHOD_WITHOUTBODY_END_REGEX = "\\G\\s*(" + THROWS_REGEX + ")??\\s*;"; // NOI18N
    
    
    private JavaSourceUtils() {}
    
    
    static String toplevelClassName(String className) {
        className = className.replace("[]", "");                                // NOI18N
        int innerIndex = className.indexOf('$');                                // NOI18N
        return innerIndex == -1 ? className : className.substring(0, innerIndex);
    }
    
    static String toplevelClassFile(String toplevelClassName) {
        return toplevelClassName.replace(".", "/") + ".java";                   // NOI18N
    }
    
    
    static String plainClassName(String className) {
        int index = className.lastIndexOf('.');                                 // NOI18N
        return index == -1 ? className : className.substring(index + 1);
    }
    
    private static String[] classNameComponents(String className) {
        String pureClassName = className.replace("[]", "");                     // NOI18N
        String plainClassName = plainClassName(pureClassName);
        
        plainClassName = plainClassName.replace(LAMBDA_CLASS_PREFIX, LAMBDA_CLASS_PREFIX_MASK);
        
        return plainClassName.split(Pattern.quote("$"));                        // NOI18N
    }
    
    
    static int classDefinitionOffset(String text, String className, boolean exactOnly) {
        text = maskNonCode(text);
        
        String[] classNameComponents = classNameComponents(className);
        
        JavaClass cls = getJavaClass(text, classNameComponents, exactOnly);
        return cls == null ? -1 : cls.getNameStart();
    }
    
    
    private static JavaClass getJavaClass(String text, String[] plainClassNames, boolean exactOnly) {
        JavaClass cls = null;
        JavaClass foundClass = null;
        
        for (String classNameComponent : plainClassNames) {
            if (cls == null) cls = JavaClass.fromSource(text, classNameComponent);
            else cls = cls.getClass(classNameComponent);
            
            if (cls != null) foundClass = cls;
            else return exactOnly ? null : foundClass;
        }
        
        return foundClass;
    }
    
    static int methodDefinitionOffset(String text, String className, String methodName, String methodSignature, boolean exactOnly) {
        text = maskNonCode(text);
        
        String[] classNameComponents = classNameComponents(className);
        
        JavaClass cls = getJavaClass(text, classNameComponents, true);
        if (cls == null) {
            if (!exactOnly) cls = getJavaClass(text, classNameComponents, false);
            return cls == null ? -1 : cls.getNameStart();
        }
        
        JavaMethod method = cls.getMethod(methodName, methodSignature);
        if (method == null) return exactOnly ? -1 : cls.getNameStart();
        
        return method.getNameStart();
    }
    
    
    // TODO: all leading & trailing non-code characters should be preserved: /* */ // ""
    static String maskNonCode(String text) {
        char[] newText = new char[text.length()];
        
        boolean lineComment = false;
        boolean blockComment = false;
        
        boolean chardef = false;
        boolean string = false;
        int escapes = 0;
        
        char lastChar = ' ';                                                    // NOI18N
        
        for (int position = 0; position < newText.length; position++) {
            char currentChar = text.charAt(position);
            
            if (!lineComment && !chardef && !string) {
                if (!blockComment && '/' == lastChar && '*' == currentChar) {   // NOI18N
                    newText[position - 1] = COMMENT_MASK_CHAR;
                    blockComment = true;
                }
            }
            
            if (!blockComment && !chardef && !string) {
                if (!lineComment && '/' == lastChar && '/' == currentChar) {    // NOI18N
                    newText[position - 1] = COMMENT_MASK_CHAR;
                    lineComment = true;
                } else if (lineComment && ('\r' == currentChar || '\n' == currentChar)) { // NOI18N
                    lineComment = false;
                }
            }
            
            if (!lineComment && !blockComment && !string) {
                if (!chardef && '\'' == currentChar) {                            // NOI18N
                    chardef = true;
                } else if (chardef) {                                            // NOI18N
                    if ('\\' == currentChar) {
                        escapes++;
                    } else if ('\'' == currentChar) {                            // NOI18N
                        chardef = escapes % 2 != 0;
                        escapes = 0;
                    } else {
                        escapes = 0;
                    }
                }
            }
            
            if (!lineComment && !blockComment && !chardef) {
                if (!string && '"' == currentChar) {                            // NOI18N
                    string = true;
                } else if (string) {                                            // NOI18N
                    if ('\\' == currentChar) {
                        escapes++;
                    } else if ('"' == currentChar) {                            // NOI18N
                        string = escapes % 2 != 0;
                        escapes = 0;
                    } else {
                        escapes = 0;
                    }
                }
            }
            
            if (string || chardef) newText[position] = STRING_MASK_CHAR;
            else if (Character.isWhitespace(currentChar)) newText[position] = currentChar; // NOI18N
            else if (lineComment || blockComment) newText[position] = COMMENT_MASK_CHAR;
            else newText[position] = currentChar;
            
            if (!lineComment && !chardef && !string) {
                if (blockComment && '*' == lastChar && '/' == currentChar) {    // NOI18N
                    blockComment = false;
                }
            }
            
            lastChar = currentChar;
        }
        
        return new String(newText);
    }
    
    
    static String maskNonBlock(String text, char startDelimiter, char endDelimiter, int startPosition, int endPosition) {
        char[] newText = new char[text.length()];
        
        int currentCount = 0;        
        
        while (startPosition <= endPosition) {
            char currentChar = text.charAt(startPosition);
            
            if (startDelimiter == currentChar) currentCount++;
            
            newText[startPosition] = currentCount == 1 || (currentCount == 2 && (currentChar == startDelimiter || currentChar == endDelimiter)) || Character.isWhitespace(currentChar) ? currentChar : NONBLOCK_MASK_CHAR;
            
            if (endDelimiter == currentChar) currentCount--;
            
            startPosition++;
        }
        
        return new String(newText);
    }
    
    static String maskClasses(String text, List<JavaClass> classes) {
        char[] newText = text.toCharArray();
        
        for (JavaClass cls : classes) {
            for (int position = cls.getBodyStart() + 1; position < cls.getBodyEnd(); position++) {
                char currentChar = text.charAt(position);
                newText[position] = Character.isWhitespace(currentChar) ? currentChar : CLASS_MASK_CHAR;
            }
        }
        
        return new String(newText);
    }
    
    static int skipBlock(String text, int position, char startDelimiter, char endDelimiter) {
        return skipBlockImpl(text, position, startDelimiter, endDelimiter, true)[1];
    }
    
    static int[] getBlockBounds(String text, int position, char startDelimiter, char endDelimiter) {
        return skipBlockImpl(text, position, startDelimiter, endDelimiter, false);
    }
    
    private static int[] skipBlockImpl(String text, int position, char startDelimiter, char endDelimiter, boolean moveToNext) {
        int start = -1;
        
        int currentCount = 0;        
        boolean found = false;

        while (position < text.length()) {
            char currentChar = text.charAt(position);
            
            if (currentChar == endDelimiter) {
                currentCount--;
            } else if (currentChar == startDelimiter) {
                if (start == -1) start = position;
                currentCount++;
                found = true;
            }
            
            if (found && currentCount == 0) break;
            
            position++;
        }
        
        return new int[] { start, moveToNext ? position + 1 : position };
    }
    
    static int skipWhiteSpaces(String text, int position) {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) position++;
        return position;
    }
    
}
