/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
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

package com.sun.tools.visualvm.jvm;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.JVM;
import com.sun.tools.visualvm.core.model.AbstractModelProvider;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmJmxModelFactory;
import com.sun.tools.visualvm.tools.jvmstat.Jvmstat;
import com.sun.tools.visualvm.tools.jvmstat.JvmstatFactory;


/**
 *
 * @author Tomas Hurka
 */
public class JvmProvider extends AbstractModelProvider<JVM,Application> {
    
    public JVM createModelFor(Application app) {
        Jvmstat jvmstat = JvmstatFactory.getJvmstatFor(app);
        JVMImpl jvm = null;
        
        if (jvmstat != null) {
            jvm = new JVMImpl(app,jvmstat);
        } else {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
            if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
               jvm = new JVMImpl(app,JvmJmxModelFactory.getJvmJmxModelFor(app));
            }
        }
        return jvm;
    }
}
