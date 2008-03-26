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

package com.sun.tools.visualvm.modules.jconsole;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Logger;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 *
 * @author Luis-Miguel Alventosa
 */
public class Install extends ModuleInstall {
    private final static Logger LOGGER = Logger.getLogger(Install.class.getName());
    
    @Override
    public void restored() {
        try {
            String javahome = System.getProperty("jdk.home"); // NOI18N
            File jconsoleFile = new File(javahome, "lib/jconsole.jar"); // NOI18N
            URL thisJarUrl = Install.class.getProtectionDomain().getCodeSource().getLocation();
            URL jconsoleUrl = jconsoleFile.toURI().toURL();
            ClassLoader jconsoleLoader = new JConsoleClassLoader(thisJarUrl, jconsoleUrl);
            Class<JConsoleViewsSupport> jconsoleViewsSupport = (Class<JConsoleViewsSupport>) Class.forName("com.sun.tools.visualvm.modules.jconsole.JConsoleViewsSupport", true, jconsoleLoader);
            Method method = jconsoleViewsSupport.getDeclaredMethod("sharedInstance");
            method.setAccessible(true);
            method.invoke(null);
        } catch (Exception e) {
            LOGGER.throwing(Install.class.getName(), "restored", e);
        }
    }
}
