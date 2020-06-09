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
package org.graalvm.visualvm.sources;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourceHandleUtils {
    
    private static final char CH_CR     = '\r';                                 // NOI18N
    private static final char CH_LF     = '\n';                                 // NOI18N
    
    private static final String CR      = new String(new char[] { CH_CR });
    private static final String LF      = new String(new char[] { CH_LF });
    private static final String CRLF    = new String(new char[] { CH_CR, CH_LF });
    
    
    private SourceHandleUtils() {}
    
    
    public static int lineToOffset(String text, int line) {
        int offset = 0;
            
        if (line > 1) {
            String breakString = CRLF;
            int breakIndex = text.indexOf(breakString);
            
            if (breakIndex == -1) {
                breakString = LF;
                breakIndex = text.indexOf(breakString);
                
                if (breakIndex == -1) {
                    breakString = CR;
                    breakIndex = text.indexOf(breakString);
                }
            }
            
            int _line = 1;
            while (breakIndex > -1 && _line < line) {
                offset = breakIndex + breakString.length();
                breakIndex = text.indexOf(breakString, offset);
                _line++;
            }
        }
        
        return offset;
    }
    
    public static int[] offsetToLineColumn(String text, int offset) {
        int line = 1;
        int column = 1;
        
        boolean crlf = text.contains(CRLF);
        boolean newlinePending = false;
        
        offset = Math.min(offset, text.length());
        
        for (int pos = 0; pos < offset; pos++) {
            if (newlinePending) {
                line++;
                column = 1;
                newlinePending = false;
            }
            
            char ch = text.charAt(pos);
            
            switch (ch) {
                case CH_CR:
                    if (crlf) pos++;
                    newlinePending = true;
                    break;
                case CH_LF:
                    newlinePending = true;
                    break;
                default:
                    column++;
                    break;
            }
        }
        
        return new int[] { line, column };
    }
    
}
