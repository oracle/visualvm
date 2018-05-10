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

package org.graalvm.visualvm.modules.systray;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
final class SysTrayPreferences {
    
    private static SysTrayPreferences INSTANCE = new SysTrayPreferences();

    private static final String KEY_ALWAYS_ON_TOP = "AlwaysOnTop"; // NOI18N
    private static final String KEY_HIDE_WHEN_MINIMIZED = "HideWhenMinimized";   // NOI18N
    private static final String KEY_HIDE_TRAY_ICON = "HideTrayIcon"; // NOI18N
    
    private final static boolean ALWAYS_ON_TOP_DEFAULT = false;
    private final static boolean HIDE_WHEN_MINIMIZED_DEFAULT = true;
    private final static boolean HIDE_TRAY_ICON_DEFAULT = false;
    private final Preferences prefs;
    
    
    static synchronized SysTrayPreferences getInstance() {
        if (INSTANCE == null) INSTANCE = new SysTrayPreferences();
        return INSTANCE;
    }
    
    
    void setAlwaysOnTop(boolean alwaysOnTop) {
        synchronized(prefs) {
            prefs.putBoolean(KEY_ALWAYS_ON_TOP, alwaysOnTop);
        }
    }
    
    boolean getAlwaysOnTop() {
        synchronized(prefs) {
            return prefs.getBoolean(KEY_ALWAYS_ON_TOP, ALWAYS_ON_TOP_DEFAULT);
        }
    }
    
    void setHideWhenMinimized(boolean hideWhenMinimized) {
        synchronized(prefs) {
            prefs.putBoolean(KEY_HIDE_WHEN_MINIMIZED, hideWhenMinimized);
        }
    }
    
    boolean getHideWhenMinimized() {
        synchronized(prefs) {
            return prefs.getBoolean(KEY_HIDE_WHEN_MINIMIZED, HIDE_WHEN_MINIMIZED_DEFAULT);
        }
    }
    
    void setHideTrayIcon(boolean hideTrayIcon) {
        synchronized(prefs) {
            prefs.putBoolean(KEY_HIDE_TRAY_ICON, hideTrayIcon);
        }
    }
    
    boolean getHideTrayIcon() {
        synchronized(prefs) {
            return prefs.getBoolean(KEY_HIDE_TRAY_ICON, HIDE_TRAY_ICON_DEFAULT);
        }
    }
    
    
    private SysTrayPreferences() {
        prefs = NbPreferences.forModule(SysTrayPreferences.class);
    }
}
