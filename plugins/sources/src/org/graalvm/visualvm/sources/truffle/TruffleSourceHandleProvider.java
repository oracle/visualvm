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

import java.util.HashMap;
import java.util.Map;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.sources.SourceHandle;
import org.graalvm.visualvm.sources.SourceHandleProvider;
import org.graalvm.visualvm.sources.SourcePathHandle;
import org.graalvm.visualvm.sources.SourcesRoot;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "TruffleSourceHandleProvider_ObjectsNotSupported=Opening {0} objects source not supported yet." // NOI18N
})
@ServiceProvider(service=SourceHandleProvider.class, position = 50)
public final class TruffleSourceHandleProvider extends SourceHandleProvider {
    
    // {<truffle language prefix>, <language id>}
    private static final String[] JS_LANG_ID = new String[]     {"js", "javascript"};   // NOI18N
    private static final String[] RUBY_LANG_ID = new String[]   {"ruby", "ruby"};       // NOI18N
    private static final String[] R_LANG_ID = new String[]      {"R", "r"};             // NOI18N
    private static final String[] PYTHON_LANG_ID = new String[] {"python", "python"};   // NOI18N
    
    private static final Map<String, String> SUPPORTED_LANGUAGES = new HashMap();
    static {
        SUPPORTED_LANGUAGES.put(JS_LANG_ID[0],      JS_LANG_ID[1]);
        SUPPORTED_LANGUAGES.put(RUBY_LANG_ID[0],    RUBY_LANG_ID[1]);
        SUPPORTED_LANGUAGES.put(R_LANG_ID[0],       R_LANG_ID[1]);
        SUPPORTED_LANGUAGES.put(PYTHON_LANG_ID[0],  PYTHON_LANG_ID[1]);
    }
    
    
    @Override
    public SourceHandle createHandle(String className, String methodName, String methodSignature, int line) {        
        int langIdIdx = className.indexOf(".");                                 // NOI18N
        String langId = langIdIdx == -1 ? className : className.substring(0, langIdIdx);
        String language = SUPPORTED_LANGUAGES.get(langId);
        
        if (language != null) {
//            if (langIdIdx != -1) className = className.substring(langIdIdx + 1);
            if (langIdIdx != -1) {
                ProfilerDialogs.displayError(Bundle.TruffleSourceHandleProvider_ObjectsNotSupported(language));
                return SourceHandle.EMPTY;
            }
            
            String fileLine = methodSignature.substring(2, methodSignature.length() - 4);
            String[] fileAndLine = fileLine.split(":");                         // NOI18N
            
            SourcePathHandle pathHandle = SourcesRoot.getPathHandle(fileAndLine[0]);
            return pathHandle == null ? null : new TruffleSourceHandle(language, className, methodName, methodSignature, Integer.parseInt(fileAndLine[1]), pathHandle);
        }
        
        return null;
    }
    
}
