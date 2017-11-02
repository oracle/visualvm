/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.visualvm.truffle.heapwalker;

import java.io.IOException;
import java.util.List;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Jiri Sedlacek
 */
public class TruffleLanguageSupport {
    
    private static final String LANGUAGE_INFO_FQN = "com.oracle.truffle.api.nodes.LanguageInfo"; // NOI18N
    private static final String LANGUAGE_CACHE_FQN = "com.oracle.truffle.api.vm.LanguageCache"; // NOI18N
    private static final String NAME_FIELD = "name";    // NOI18N

    public static Instance getLanguageInfo(Heap heap, String languageID) throws IOException {
        // check for DynamicObject
        if (!DynamicObject.hasDynamicObject(heap)) {
            return null;
        }
        
        // check for LanguageInfo
        JavaClass langInfoClass = heap.getJavaClassByName(LANGUAGE_INFO_FQN);
        if (!checkLangClass(langInfoClass)) {
            langInfoClass = heap.getJavaClassByName(LANGUAGE_CACHE_FQN);
            if (!checkLangClass(langInfoClass)) {
                return null;
            }
        }
        
        // search the language
        List<Instance> langInfos = langInfoClass.getInstances();
        for (Instance langInfo : langInfos) {
            Instance name = (Instance) langInfo.getValueOfField("name");   // NOI18N
            String langName = DetailsUtils.getInstanceString(name, heap);

            if (languageID.equals(langName)) {
                return langInfo;
            }
        }
        
        return null;
    }
    
    static boolean checkLangClass(JavaClass infoClass) {
        if (infoClass != null) {
            for (Object f : infoClass.getFields()) {
                Field field = (Field) f;

                if (NAME_FIELD.equals(field.getName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
