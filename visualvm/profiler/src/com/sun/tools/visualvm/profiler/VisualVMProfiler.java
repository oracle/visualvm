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

package com.sun.tools.visualvm.profiler;

import java.io.File;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.modules.profiler.NetBeansProfiler;
import org.netbeans.modules.profiler.ProfilerModule;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.lookup.ServiceProvider;


/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=Profiler.class)
public class VisualVMProfiler extends NetBeansProfiler {

    @Override
    public String getLibsDir() {
        final File dir = InstalledFileLocator.getDefault().locate(ProfilerModule.LIBS_DIR + "/jfluid-server.jar", //NOI18N
                                                     "org.netbeans.lib.profiler", false); //NOI18N
        if (dir == null) {
            return null;
        }
        return dir.getParentFile().getPath();
    }

    protected boolean shouldOpenWindowsOnProfilingStart() {
        return false;
    }

    @Override
    public boolean rerunAvailable() {
        return false;
    }

    @Override
    public boolean modifyAvailable() {
        return false;
    }

    @Override
    public void rerunLastProfiling() {
        throw new UnsupportedOperationException("Not supported yet.");  // NOI18N
    }
 
}
