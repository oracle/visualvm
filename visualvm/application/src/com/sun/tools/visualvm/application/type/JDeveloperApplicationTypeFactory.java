/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import java.util.regex.Pattern;

/**
 * Factory which recognizes JDeveloper 11g and 10g.
 * @author Tomas Hurka
 */
public class JDeveloperApplicationTypeFactory extends MainClassApplicationTypeFactory {
    
    private static final String MAIN_CLASS_12 = "oracle.ide.osgi.boot.JDeveloper"; // NOI18N
    private static final String MAIN_CLASS_11 = "oracle.ide.boot.Launcher"; // NOI18N
    private static final String MAIN_CLASS_10 = "oracle.ideimpl.Main"; // NOI18N
    private static final String IDE_CONF_12 = "\\-Dide.conf=\\S+jdev.conf"; // NOI18N
    private static final String IDE_CONF_11 = "-Dide.config_pathname="; // NOI18N
    private static final String IDE_PRODUCT_10 = "-Dide.product=oracle.jdeveloper"; // NOI18N

    private static final Pattern IDE_CONF_12_PATTERN = Pattern.compile(IDE_CONF_12);
    
    private String getJDeveloperVersion(Jvm jvm, String mainClass) {
        if (MAIN_CLASS_12.equals(mainClass)) {
            return "12c";  // NOI18N
        }
        if (MAIN_CLASS_11.equals(mainClass)) {
            return "11g";  // NOI18N
        }
        if (MAIN_CLASS_10.equals(mainClass)) {
            return "10g";  // NOI18N
        }
        if (mainClass == null || mainClass.length() == 0) {    // there is no main class - detect native Windows launcher
            String args = jvm.getJvmArgs();
            if (args != null) {
                if (args.contains(IDE_CONF_11)) {
                    return "11g";  // NOI18N
                }
                if (args.contains(IDE_PRODUCT_10)) {
                    return "10g";  // NOI18N
                }
                if (IDE_CONF_12_PATTERN.matcher(args).find()) {
                    return "12c";  // NOI18N
                }
            }
        }
        return null;
    }
    
    
    /**
     * Detects JDeveloper IDE. It returns
     * {@link JDeveloperApplicationType} for JDeveloper 12c, 11g and 10g.
     *
     * @return {@link ApplicationType} subclass or <code>null</code> if
     * this application is not JDeveloper IDE
     */
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        String version = getJDeveloperVersion(jvm,mainClass);
        
        if (version != null) {
            return new JDeveloperApplicationType(app,version);
        }
        return null;
    }
    
}
