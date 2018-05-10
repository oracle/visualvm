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

package org.graalvm.visualvm.jvmstat;

import org.graalvm.visualvm.core.model.AbstractModelProvider;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModel;
import org.graalvm.visualvm.tools.jvmstat.JvmstatModelFactory;
import org.graalvm.visualvm.tools.jvmstat.JvmJvmstatModel;

/**
 *
 * @author Tomas Hurka
 */
public class OracleJRockitJvmJvmstatModelProvider extends JvmJvmstatModelProvider {
    
    public JvmJvmstatModel createModelFor(Application app) {
        JvmstatModel jvmstat = JvmstatModelFactory.getJvmstatFor(app);
        if (jvmstat != null) {
            String vmName = jvmstat.findByName("java.property.java.vm.name");   // NOI18N
            
            if (vmName != null && "Oracle JRockit(R)".equals(vmName)) {  // NOI18N
                JvmJvmstatModel jvm = null;
                String vmVersion = jvmstat.findByName("java.property.java.vm.version"); // NOI18N
                
                if (vmVersion != null) {
                    if (vmVersion.contains("1.6.0")) {  // NOI18N
                        jvm = new OracleJRockitJvmJvmstatModel(app,jvmstat);
                    } else {
                        jvm = new OracleJRockitJvmJvmstatModel(app,jvmstat);
                    }
                }
                return jvm;
            }
        }
        return null;
    }
    
}
