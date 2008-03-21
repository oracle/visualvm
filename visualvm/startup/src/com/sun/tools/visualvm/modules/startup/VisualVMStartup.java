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
package com.sun.tools.visualvm.modules.startup;

import java.util.Properties;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.openide.LifecycleManager;
import org.openide.modules.ModuleInstall;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class VisualVMStartup extends ModuleInstall {
    private static final Logger LOGGER = Logger.getLogger(VisualVMStartup.class.getName());
    
    public void validate() {
        if (!isCurrentJavaSupported()) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}

            JOptionPane.showMessageDialog(null, "You are running VisualVM using an unsupported  \n"+
                                                "version of Java (" + System.getProperty("java.version", "unknown version") + "),\n"+
                                                "from (" + System.getProperty("java.home", "unknown version") + "), startup terminated.  \n\n"+
                                                "Please refer to https://visualvm.dev.java.net  \n"+
                                                "for information about requirements and setup.  \n\n",
                                                "Error Starting VisualVM", JOptionPane.ERROR_MESSAGE);
            LifecycleManager.getDefault().exit();
        }
    }
    
    private static boolean isCurrentJavaSupported() {
        
        String javaVersion = System.getProperty("java.version");

        Class desktopClass = null;
        try { desktopClass = Class.forName("java.awt.Desktop"); } catch (ClassNotFoundException ex) {}

        Class vmClass = null;
        try { vmClass = Class.forName("com.sun.tools.attach.VirtualMachine"); } catch (ClassNotFoundException ex) {}

        boolean isCurrentJavaSupported = (javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7")) && desktopClass != null && vmClass != null;
        if (!isCurrentJavaSupported) {
            LOGGER.warning("Error starting VisualVM, unsupported Java version: " + getJavaInfo());
        }
        return isCurrentJavaSupported;
    }
    
    private static String getJavaInfo() {
        Properties systemProperties = System.getProperties();
        String javaVersion = systemProperties.getProperty("java.version", "unknown version");
        String vmName = systemProperties.getProperty("java.vm.name", "unknown name");
        String vmVerison = systemProperties.getProperty("java.vm.version", "");
        String vmInfo = systemProperties.getProperty("java.vm.info", "");
        String vendorInfo = systemProperties.getProperty("java.vendor", "unknown vendor");
        return javaVersion + "; " + vmName + " (" + vmVerison + ", " + vmInfo + "); " + vendorInfo;
    }
    
}
