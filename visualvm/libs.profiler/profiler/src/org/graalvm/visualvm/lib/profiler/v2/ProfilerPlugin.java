/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.v2;

import javax.swing.JMenu;
import org.openide.util.Lookup;

/**
 * Common superclass for simple addons integrating into the Profile menu and
 * getting notifications about state of the profiler session.
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerPlugin {

    private final String name;


    public ProfilerPlugin(String name) {
        if (name == null) throw new IllegalArgumentException("Plugin name cannot be null"); // NOI18N
        this.name = name;
    }


    public final String getName() {
        return name;
    }


    public abstract void createMenu(JMenu menu);


    protected void sessionStarting() {}

    protected void sessionStarted()  {}

    protected void sessionStopping() {}

    protected void sessionStopped()  {}


    // --- Provider ------------------------------------------------------------

    public static abstract class Provider {

        public abstract ProfilerPlugin createPlugin(Lookup.Provider project, SessionStorage storage);

    }

}
