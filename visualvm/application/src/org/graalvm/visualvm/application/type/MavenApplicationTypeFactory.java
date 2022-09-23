/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.type;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;

/**
 * Factory which recognizes Apache Maven.
 * @author Tomas Hurka
 */
public class MavenApplicationTypeFactory extends MainClassApplicationTypeFactory {
    
    private static final String MAIN_CLASS = "org.codehaus.classworlds.Launcher"; // NOI18N
    private static final String PROP_1 = "-Dclassworlds.conf="; // NOI18N
    private static final String PROP_2 = "-Dmaven.home="; // NOI18N
    
    /**
     * Detects Apache Maven. It returns
     * {@link MavenApplicationType} for Maven.
     *
     * @return {@link ApplicationType} subclass or <code>null</code> if
     * this application is not Apache Maven
     */
    public ApplicationType createApplicationTypeFor(Application app, Jvm jvm, String mainClass) {
        if (MAIN_CLASS.equals(mainClass)) {
            String args = jvm.getJvmArgs();
            if (args != null) {
                if (args.contains(PROP_1) && args.contains(PROP_2)) {
                    return new MavenApplicationType(app);
                }
            }
        }
        return null;
    }
    
}
