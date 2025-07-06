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
package org.graalvm.visualvm.gotosource;

import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "SourceHandle_Language=Source Language",                                    // NOI18N
    "SourceHandle_ClassName=Class Name",                                        // NOI18N
    "SourceHandle_MethodName=Method Name",                                      // NOI18N
    "SourceHandle_MethodSignature=Method Signature",                            // NOI18N
    "SourceHandle_SourceLine=Source Line",                                      // NOI18N
    "SourceHandle_SourceColumn=Source Column",                                  // NOI18N
    "SourceHandle_SourceOffset=Source Offset",                                  // NOI18N
    "SourceHandle_SourceText=Source Text",                                      // NOI18N
    "SourceHandle_SourceUri=Source URI",                                        // NOI18N
    "SourceHandle_SourceFile=Source File"                                       
})
public abstract class SourceHandle {
    
    public static final SourceHandle EMPTY = new Empty();
    
    
    public static enum Feature {
        
        LANGUAGE("{lang}", Bundle.SourceHandle_Language()) {},                  // NOI18N

        CLASS("{class}", Bundle.SourceHandle_ClassName()) {},                   // NOI18N
        METHOD("{method}", Bundle.SourceHandle_MethodName()) {},                // NOI18N
        SIGNATURE("{sig}", Bundle.SourceHandle_MethodSignature()) {},           // NOI18N

        LINE("{line}", Bundle.SourceHandle_SourceLine()) {},                    // NOI18N
        COLUMN("{column}", Bundle.SourceHandle_SourceColumn()) {},              // NOI18N
        OFFSET("{offset}", Bundle.SourceHandle_SourceOffset()) {},              // NOI18N

        TEXT("{text}", Bundle.SourceHandle_SourceText()) {},                    // NOI18N

        URI("{uri}", Bundle.SourceHandle_SourceUri()) {},                       // NOI18N
        FILE("{file}", Bundle.SourceHandle_SourceFile()) {};                    // NOI18N
        
        
        private final String code;
        private final String name;
        
        Feature(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public String getCode() { return code; }
        public String getName() { return name; }
        
        @Override public String toString() { return getName(); }
        
    }


    public abstract String getLanguage();
    
    
    public abstract String getClassName();
    
    public abstract String getMethodName();
    
    public abstract String getMethodSignature();
    
    
    public abstract int getLine();
    
    public abstract int getColumn();
    
    public abstract int getOffset();
    
    public          int getEndOffset() { return getOffset(); }
    
    
    public abstract String getText();
    
    
    public abstract String getSourceUri();
    
    public abstract String getSourceFile();
    
    
    public final String expandFeatures(String command) {
        String lang = Feature.LANGUAGE.getCode();
        int index = command.indexOf(lang);
        if (index > -1) command = command.replace(lang, getLanguage());
        
        String code = Feature.CLASS.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getClassName());
        
        code = Feature.METHOD.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getMethodName());
        
        code = Feature.SIGNATURE.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getMethodSignature());
        
        
        code = Feature.LINE.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, Integer.toString(getLine()));
        
        code = Feature.COLUMN.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, Integer.toString(getColumn()));
        
        code = Feature.OFFSET.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, Integer.toString(getOffset()));
        
        
        code = Feature.TEXT.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getText());
        
        
        code = Feature.URI.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getSourceUri());
        
        code = Feature.FILE.getCode();
        index = command.indexOf(code);
        if (index > -1) command = command.replace(code, getSourceFile());
        
        
        return command;
    }


    protected abstract void close();
    
    
    private static final String FILE_PREFIX = "file:///";                       // NOI18N
    
    public static String simpleUri(String uri) {
        int fileIndex = uri.indexOf(FILE_PREFIX);
        return fileIndex == -1 ? uri : uri.substring(fileIndex + FILE_PREFIX.length());
    }
    
    
    private static final class Empty extends SourceHandle {

        @Override public String getLanguage() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getClassName() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getMethodName() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getMethodSignature() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public int getLine() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public int getColumn() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public int getOffset() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getText() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getSourceUri() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override public String getSourceFile() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        @Override protected void close() { throw new UnsupportedOperationException("Not supported."); } // NOI18N
        
    }
    
}
