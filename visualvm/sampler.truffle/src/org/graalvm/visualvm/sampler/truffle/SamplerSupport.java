/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler.truffle;

import java.util.Properties;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.application.type.ApplicationType;
import org.graalvm.visualvm.application.type.ApplicationTypeFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public final class SamplerSupport {

    private static final String GRAALVM_ID = "GraalVM";     // NOI18N
    private static SamplerSupport instance;
    
    private ApplicationSamplerViewProvider samplerViewProvider;


    public static synchronized SamplerSupport getInstance() {
        if (instance == null) instance = new SamplerSupport();
        return instance;
    }
    
    boolean supportsProfiling(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        if (!application.isLocalApplication()) return false;
        Jvm jvm = JvmFactory.getJVMFor(application);
        if (!jvm.isBasicInfoSupported() || !jvm.isAttachable()) return false;
        ApplicationType appType = ApplicationTypeFactory.getApplicationTypeFor(application);
        if (appType.getClass().getName().contains("GraalVMApplicationType")) {  // NOI18N
            return true;
        }
        return isRunningOnGraalVM(jvm);
    }
    
    void selectSamplerView(Application application) {
        if (application == null) return;
        DataSourceView activeView = samplerViewProvider.view(application);
        if (activeView == null) return;
        DataSourceWindowManager.sharedInstance().selectView(activeView);
    }
             
    private SamplerSupport() {
        samplerViewProvider = new ApplicationSamplerViewProvider();
        samplerViewProvider.initialize();
    }

    private boolean isRunningOnGraalVM(Jvm jvm) {
        if (jvm.getVmName().contains(GRAALVM_ID)) {
            return true;
        }
        if (jvm.isGetSystemPropertiesSupported()) {
            Properties props = jvm.getSystemProperties();
            if (props != null) {
                String vendorVersion = props.getProperty("java.vendor.version", "");    // NOI18N
                return vendorVersion.contains(GRAALVM_ID);
            }
        }
        return false;
    }
}
