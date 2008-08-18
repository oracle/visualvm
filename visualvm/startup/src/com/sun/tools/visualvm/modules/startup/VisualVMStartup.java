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

import java.text.MessageFormat;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.openide.LifecycleManager;
import org.openide.modules.ModuleInstall;
import org.openide.util.NbBundle;

/**
 * Manages a module's lifecycle. Remember that an installer is optional and
 * often not needed at all.
 */
public class VisualVMStartup extends ModuleInstall {
    
    private static final boolean DISABLE_STARTUP_CHECK = Boolean.getBoolean("com.sun.tools.visualvm.modules.startup.DisableStartupCheck"); // NOI18N
    
    private static final ResourceBundle bundle = NbBundle.getBundle(VisualVMStartup.class);
    private static final String ERROR_STARTUP_CAPTION = bundle.getString("VisualVMStartup_ErrorStartupCaption"); // NOI18N
    private static final String INCORRECT_VERSION_MSG = bundle.getString("VisualVMStartup_IncorrectVersionMsg"); // NOI18N
    private static final String JRE_MSG = bundle.getString("VisualVMStartup_JreMsg"); // NOI18N
    
    
    public void validate() {
        
        if (DISABLE_STARTUP_CHECK) {
            System.err.println("Starting with com.sun.tools.visualvm.modules.startup.DisableStartupCheck=true"); // NOI18N
        } else {
            if (!isJava6or7()) {
                displayError6or7();
                LifecycleManager.getDefault().exit();
            } else if (!isJDK()) {
                displayErrorJRE();
                LifecycleManager.getDefault().exit();
            }
        }
    }
    
    private static void setSystemLaF() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
    }
    
    private static void displayError6or7() {
        setSystemLaF();
        JOptionPane.showMessageDialog(null, MessageFormat.format(INCORRECT_VERSION_MSG,
                new Object[] { System.getProperty("java.specification.version"), getJavaInfo(), // NOI18N
                getJvmInfo(), System.getProperties().getProperty("java.home", "unknown location") }), // NOI18N
                ERROR_STARTUP_CAPTION, JOptionPane.ERROR_MESSAGE);
    }
    
    private static void displayErrorJRE() {
        setSystemLaF();
        JOptionPane.showMessageDialog(null, MessageFormat.format(JRE_MSG, new Object[] { getJavaInfo(),
                getJvmInfo(), System.getProperties().getProperty("java.home", "unknown location") }), // NOI18N
                ERROR_STARTUP_CAPTION, JOptionPane.ERROR_MESSAGE);
    }
    
    private static boolean isJava6or7() {
        String javaVersion = System.getProperty("java.specification.version"); // NOI18N
        if (javaVersion == null) return false;
        return javaVersion.startsWith("1.6") || javaVersion.startsWith("1.7"); // NOI18N
    }
    
    private static boolean isJDK() {
        Class vmClass = null;
        try { vmClass = Class.forName("com.sun.tools.attach.VirtualMachine"); } catch (ClassNotFoundException ex) {} // NOI18N
        return vmClass != null;
    }
    
    private static String getJavaInfo() {
        Properties systemProperties = System.getProperties();
        String javaVersion = systemProperties.getProperty("java.version", "unknown version"); // NOI18N
        String vendorInfo = systemProperties.getProperty("java.vendor", "unknown vendor"); // NOI18N
        return javaVersion + "; " + vendorInfo; // NOI18N
    }
    
    private static String getJvmInfo() {
        Properties systemProperties = System.getProperties();
        String vmName = systemProperties.getProperty("java.vm.name", "unknown name"); // NOI18N
        String vmVerison = systemProperties.getProperty("java.vm.version", ""); // NOI18N
        String vmInfo = systemProperties.getProperty("java.vm.info", ""); // NOI18N
        return vmName + " (" + vmVerison + ", " + vmInfo + ")"; // NOI18N
    }
    
}
