/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.extensions;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModel;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatModelFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Support additional JVMs.
 *
 * @author Luis-Miguel Alventosa
 */
public class ExtendedJvmProvider extends AbstractModelProvider<Jvm, Application> {

    public Jvm createModelFor(Application app) {
        JvmstatModel jvmstat = JvmstatModelFactory.getJvmstatFor(app);
        ExtendedJVMImpl jvm = null;
        if (jvmstat != null) {
            String vmVersion = jvmstat.findByName("java.property.java.vm.version"); // NOI18N
            if (vmVersion != null && vmVersion.startsWith("13.0")) { // NOI18N
                jvm = new ExtendedJVMImpl(app, jvmstat);
            }
        } else {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
            if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
                JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
                if (mxbeans != null) {
                    RuntimeMXBean runtime = mxbeans.getRuntimeMXBean();
                    if (runtime != null && runtime.getVmVersion().startsWith("13.0")) { // NOI18N
                        jvm = new ExtendedJVMImpl(app);
                    }
                }
            }
        }
        return jvm;
    }

    @Override
    public int priority() {
        return 3; // one more than JvmProvider
    }
}
