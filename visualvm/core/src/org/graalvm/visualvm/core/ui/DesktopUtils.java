/*
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

package org.graalvm.visualvm.core.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Support for java.awt.Desktop
 *
 * @author Jiri Sedlacek
 */
public final class DesktopUtils {

    private static final Object allowDesktopOpen = System.getProperty("org.graalvm.visualvm.core.ui.DesktopUtils.allowDesktopOpen");  // NOI18N

    private static boolean desktopSupported;
    private static boolean desktopSupportedInitialized = false;
    private static boolean browseAvailable;
    private static boolean browseAvailableInitialized = false;
    private static boolean openAvailable;
    private static boolean openAvailableInitialized = false;


    /**
     * Returns true if java.awt.Desktop is supported on host platform.
     * 
     * @return true if java.awt.Desktop is supported on host platform, false otherwise.
     */
    public synchronized static boolean isDesktopSupported() {
        if (!desktopSupportedInitialized) {
            desktopSupported = Desktop.isDesktopSupported();
            desktopSupportedInitialized = true;
        }
        return desktopSupported;
    }
    
    // NOTE: should always return true if in NetBeans IDE (uses its own user-customizable mechanism)
    /**
     * Returns true if opening a www address in a web browser is supported on host platform.
     * 
     * @return true if opening a www address in a web browser is supported on host platform, false otherwise.
     */
    public synchronized static boolean isBrowseAvailable() {
        if (!browseAvailableInitialized) {
            if (!isDesktopSupported()) browseAvailable = false;
            else browseAvailable = Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
            browseAvailableInitialized = true;
        }
        return browseAvailable;
    }
    
    /**
     * Returns true if opening a file in native displayer is supported on host platform.
     * 
     * @return true if opening a file in native displayer is supported on host platform, false otherwise.
     */
    public synchronized static boolean isOpenAvailable() {
        if (!openAvailableInitialized) {
             // NOTE: Open is disabled by default, crashes the VM at least on Windows
            if ("true".equals(allowDesktopOpen) && isDesktopSupported()) openAvailable = Desktop.getDesktop().isSupported(Desktop.Action.OPEN); // NOI18N
            else openAvailable = false;
            openAvailableInitialized = true;
        }
        return openAvailable;
    }
    
    // NOTE: If in NetBeans IDE use HtmlBrowser.URLDisplayer.showURL(URL u)
    /**
     * Performs Desktop.getDesktop().browse(URI).
     * 
     * @param uri URI to open.
     * @throws java.io.IOException
     */
    public static void browse(URI uri) throws IOException {
        Desktop.getDesktop().browse(uri);
    }
    
    /**
     * Performs Desktop.getDesktop().open(File).
     * 
     * @param file File to open.
     * @throws java.io.IOException
     */
    public static void open(File file) throws IOException {
        Desktop.getDesktop().open(file);
    }

}
