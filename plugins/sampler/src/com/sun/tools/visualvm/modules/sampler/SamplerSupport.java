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

package com.sun.tools.visualvm.modules.sampler;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasupport.Stateful;
import com.sun.tools.visualvm.core.ui.DataSourceView;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.tools.jmx.JmxModel;
import com.sun.tools.visualvm.tools.jmx.JmxModelFactory;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeans;
import com.sun.tools.visualvm.tools.jmx.JvmMXBeansFactory;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public final class SamplerSupport {    
    private static SamplerSupport instance;
    
    private ApplicationSamplerViewProvider profilerViewProvider;


    public static synchronized SamplerSupport getInstance() {
        if (instance == null) instance = new SamplerSupport();
        return instance;
    }
      
    boolean supportsProfiling(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;

        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != null && jmxModel.getConnectionState() == JmxModel.ConnectionState.CONNECTED) {
            JvmMXBeans mxbeans = JvmMXBeansFactory.getJvmMXBeans(jmxModel);
            if (mxbeans == null)
                return false;
            return mxbeans.getThreadMXBean() != null;
        }
        return false;
    }
    
    void selectProfilerView(Application application) {
        if (application == null) return;
        DataSourceView activeView = profilerViewProvider.view(application);
        if (activeView == null) return;
        DataSourceWindowManager.sharedInstance().selectView(activeView);
    }
             
    private SamplerSupport() {
        profilerViewProvider = new ApplicationSamplerViewProvider();
        profilerViewProvider.initialize();
    }

}
