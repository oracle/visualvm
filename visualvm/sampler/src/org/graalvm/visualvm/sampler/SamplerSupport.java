/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.sampler;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.core.ui.DataSourceView;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.sampler.cpu.CPUSamplerParameters;
import org.graalvm.visualvm.sampler.memory.MemorySamplerParameters;

/**
 *
 * @author Jiri Sedlacek
 * @author Tomas Hurka
 */
public final class SamplerSupport {    
    private static SamplerSupport instance;
    
    private final ApplicationSamplerViewProvider samplerViewProvider;


    public static synchronized SamplerSupport getInstance() {
        if (instance == null) instance = new SamplerSupport();
        return instance;
    }
    
    boolean supportsProfiling(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) return false;
        return JvmFactory.getJVMFor(application).isBasicInfoSupported();
    }
    
    void selectSamplerView(Application application) {
        ApplicationSamplerView view = getSamplerView(application);
        if (view != null) DataSourceWindowManager.sharedInstance().selectView(view);
    }
    
    
    void startCPU(Application application, String settings) {
        ApplicationSamplerView view = getSamplerView(application);
        if (view != null) {
            DataSourceWindowManager.sharedInstance().selectView(view);
            view.startCPU(CPUSamplerParameters.parse(settings));
        }
    }
    
    void startMemory(Application application, String settings) {
        ApplicationSamplerView view = getSamplerView(application);
        if (view != null) {
            DataSourceWindowManager.sharedInstance().selectView(view);
            view.startMemory(MemorySamplerParameters.parse(settings));
        }
    }
    
    void takeSnapshot(Application application, boolean openView) {
        ApplicationSamplerView view = getSamplerView(application);
        if (view != null) {
            DataSourceWindowManager.sharedInstance().selectView(view);
            view.takeSnapshot(openView);
        }
    }
    
    void stop(Application application) {
        ApplicationSamplerView view = getSamplerView(application);
        if (view != null) {
            DataSourceWindowManager.sharedInstance().selectView(view);
            view.stop();
        }
    }
    
    
    private ApplicationSamplerView getSamplerView(Application application) {
        if (application == null) return null;
        DataSourceView activeView = samplerViewProvider.view(application);
        if (activeView == null) return null;
        return (ApplicationSamplerView)activeView;
    }
             
    private SamplerSupport() {
        samplerViewProvider = new ApplicationSamplerViewProvider();
        samplerViewProvider.initialize();
    }

}
