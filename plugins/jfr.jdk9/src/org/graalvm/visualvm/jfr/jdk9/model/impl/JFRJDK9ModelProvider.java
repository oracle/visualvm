/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.jdk9.model.impl;

import java.io.File;
import java.io.IOException;
import org.graalvm.visualvm.jfr.model.JFRModel;
import org.graalvm.visualvm.jfr.model.JFRModelFactory;
import org.graalvm.visualvm.jfr.model.JFRModelProvider;

/**
 *
 * @author Jiri Sedlacek
 */
public final class JFRJDK9ModelProvider extends JFRModelProvider {
    
    private JFRJDK9ModelProvider() {
        super("JDK9 loader", 100); // NOI18N
    }
    
    
    public static void register() {
        // Always register the provider to not break the loaders hints in Overview
        JFRModelFactory.getDefault().registerProvider(new JFRJDK9ModelProvider());
    }
    
    
    @Override
    protected JFRModel createModel(String id, File file) throws IOException {
        // Only provide the model if running on Java 9 or Java 10
        return isActive() ? new JFRJDK9Model(id, file) : null;
    }
    
    
    // --- Support for running only on Java 9 & Java 10 ------------------------
    
    private static Boolean IS_ACTIVE;
    
    private static boolean isActive() {
        if (IS_ACTIVE == null) {
            String javaVersion = System.getProperty("java.version"); // NOI18N
            if (javaVersion != null) {
                if (javaVersion.equals("9") || javaVersion.startsWith("9.") || // NOI18N
                    javaVersion.equals("10") || javaVersion.startsWith("10.")) // NOI18N
                    
                    IS_ACTIVE = Boolean.TRUE;
            }
            if (IS_ACTIVE == null) IS_ACTIVE = Boolean.FALSE;
        }
        return IS_ACTIVE;
    }
    
}
