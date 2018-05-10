/*
 * Copyright (c) 2007, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.application.type;

import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.Application;
import java.util.Scanner;

/**
 * Factory which recognizes application based IntelliJ Platform.
 * @author Tomas Hurka
 * 
 * @since VisualVM 1.3.4
 */
public class IntellijApplicationTypeFactory extends MainClassApplicationTypeFactory {
    
    private static final String MAIN_CLASS = "com.intellij.idea.Main"; // NOI18N
    private static final String PLATFORM_ID = "-Didea.paths.selector="; // NOI18N
    private static final String PLATFORM_PREFIX = "-Didea.platform.prefix="; // NOI18N
    private static final String IDEA_ID = "Idea";  // NOI18N
    private static final String IDEA_NAME = "IntelliJ IDEA"; // NOI18N
    private static final String PLATFORM_NAME = "IntelliJ Platform"; // NOI18N
    
    /**
     * Detects IntelliJ Plaform application. It returns
     * {@link IntellijApplicationType} for IntelliJ Platform application.
     *
     * @return {@link ApplicationType} subclass or <code>null</code> if
     * this application is not IntelliJ Platform application
     */
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if (MAIN_CLASS.equals(mainClass)) {
            return new IntellijApplicationType(app, getName(jvm));
        }
        if (mainClass == null || mainClass.length() == 0) {    // there is no main class - detect native Windows launcher
            String args = jvm.getJvmArgs();
            if (args != null && args.contains(PLATFORM_ID)) {
                return new IntellijApplicationType(app, getName(jvm));
            }
        }
        return null;
    }
    
    private String getName(Jvm jvm) {
        String jvmArgs = jvm.getJvmArgs();
        String name = getValue(jvmArgs, PLATFORM_PREFIX);
        String selector;
        
        if (name != null) {
            return name;
        }
        selector = getValue(jvmArgs, PLATFORM_ID);
        if (selector != null && selector.startsWith(IDEA_ID)) {
            return IDEA_NAME;
        }
        return PLATFORM_NAME;
    }
    
    private String getValue(String args, String key) {
        if (args == null) return null;
        int index = args.indexOf(key);
        
        if (index >= 0) {
            Scanner sc = new Scanner(args.substring(index+key.length()));
            if (sc.hasNext()) {
                return sc.next();
            }
        }
        return null;
    }
}
