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
package org.graalvm.visualvm.sources.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.graalvm.visualvm.sources.SourcesRoot;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourceRoots {
    
    private static final String PROP_SAVED_ROOTS = "prop_SourceRoots_saved";    // NOI18N
    
    private static final String ROOTS_DELIMITER = "|";                          // NOI18N
    
    
    private static String[] FORCED_ROOTS;
    
    
    private SourceRoots() {}
    
    
    public static String[] getRoots() {
        if (areForcedRoots()) return FORCED_ROOTS;
        
        String definedString = NbPreferences.forModule(SourcesRoot.class).get(PROP_SAVED_ROOTS, ""); // NOI18N
        return definedString.isEmpty() ? new String[0] : definedString.split(Pattern.quote(ROOTS_DELIMITER));
    }
    
    public static void saveRoots(String[] roots) {
        if (areForcedRoots()) return;
        
        String joinedString = String.join(ROOTS_DELIMITER, roots);
        NbPreferences.forModule(SourceRoots.class).put(PROP_SAVED_ROOTS, joinedString);
    }
    
    
    public static void forceRoots(String[] roots) {
        FORCED_ROOTS = roots == null || roots.length == 0 ? null : roots;
    }
    
    public static boolean areForcedRoots() {
        return FORCED_ROOTS != null;
    }
    
    
    public static String[] splitRoots(String rootsString) {
        List<String> roots = new ArrayList();
        
        int position = 0;
        int length = rootsString.length();
        
        boolean inBlock = false;
        StringBuilder sb = new StringBuilder();

        while (position < length) {
            char currentChar = rootsString.charAt(position);
            
            if (currentChar == '[') {                                           // NOI18N
                inBlock = true;
            } else if (currentChar == ']') {                                    // NOI18N
                inBlock = false;
            }
            
            if (!inBlock && currentChar == File.pathSeparatorChar) {
                roots.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(currentChar);
                if (position == length - 1) roots.add(sb.toString());
            }
            
            position++;
        }
        
        return roots.toArray(new String[0]);
    }
    
}
