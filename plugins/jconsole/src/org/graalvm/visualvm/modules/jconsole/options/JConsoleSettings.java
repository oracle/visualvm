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

package org.graalvm.visualvm.modules.jconsole.options;

import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.NbPreferences;

public class JConsoleSettings implements ChangeListener {

    private static final String PROP_POLLING = "POLLING"; // NOI18N
    private static final String PROP_PLUGINSPATH = "PLUGINS"; // NOI18N
    private Preferences pref;
    private static JConsoleSettings INSTANCE;

    JConsoleSettings() {
        pref = NbPreferences.forModule(JConsoleSettings.class);
    }

    public static synchronized JConsoleSettings getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new JConsoleSettings();
        }
        return INSTANCE;
    }

    public void stateChanged(ChangeEvent e) {
    }

    public String getPluginsPath() {
        return pref.get(PROP_PLUGINSPATH, null);
    }

    public void setPluginsPath(String value) {
        pref.put(PROP_PLUGINSPATH, value);
    }

    public Integer getPolling() {
        return pref.getInt(PROP_POLLING, 4);
    }

    public void setPolling(Integer polling) {
        pref.putInt(PROP_POLLING, polling);
    }
}
