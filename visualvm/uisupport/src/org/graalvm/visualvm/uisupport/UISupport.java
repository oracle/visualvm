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

package com.sun.tools.visualvm.uisupport;

import java.awt.Color;
import org.netbeans.lib.profiler.ui.UIUtils;

/**
 * Various UI methods published from org.netbeans.lib.profiler.ui.UIUtils to be
 * used in VisualVM tool and plugins.
 *
 * @author Jiri Sedlacek
 */
public class UISupport {

    /** Returns default background of tables & textcomponents */
    public static Color getDefaultBackground() {
        return UIUtils.getProfilerResultsBackground();
    }

    /** Determines if current L&F is AquaLookAndFeel */
    public static boolean isAquaLookAndFeel() {
        return UIUtils.isAquaLookAndFeel();
    }

    /** Determines if current L&F is GTKLookAndFeel */
    public static boolean isGTKLookAndFeel() {
        return UIUtils.isGTKLookAndFeel();
    }

    /** Determines if current L&F is Nimbus */
    public static boolean isNimbusLookAndFeel() {
        return UIUtils.isNimbusLookAndFeel();
    }

    /** Determines if current L&F is GTK using Nimbus theme */
    public static boolean isNimbusGTKTheme() {
        return UIUtils.isNimbusGTKTheme();
    }

    /** Determines if current L&F is Nimbus or GTK with Nimbus theme*/
    public static boolean isNimbus() {
        return UIUtils.isNimbus();
    }

    /** Determines if current L&F is MetalLookAndFeel */
    public static boolean isMetalLookAndFeel() {
        return UIUtils.isMetalLookAndFeel();
    }

    /** Determines if current L&F is Windows Classic LookAndFeel */
    public static boolean isWindowsClassicLookAndFeel() {
        return UIUtils.isWindowsClassicLookAndFeel();
    }

    /** Determines if current L&F is WindowsLookAndFeel */
    public static boolean isWindowsLookAndFeel() {
        return UIUtils.isWindowsLookAndFeel();
    }

    /** Determines if current L&F is Windows XP LookAndFeel */
    public static boolean isWindowsXPLookAndFeel() {
        return UIUtils.isWindowsXPLookAndFeel();
    }

    public static void runInEventDispatchThread(final Runnable r) {
        UIUtils.runInEventDispatchThread(r);
    }

    public static void runInEventDispatchThreadAndWait(final Runnable r) {
        UIUtils.runInEventDispatchThreadAndWait(r);
    }

}
