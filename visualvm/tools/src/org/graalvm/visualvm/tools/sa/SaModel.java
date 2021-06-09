/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.tools.sa;

import org.graalvm.visualvm.core.model.Model;
import java.util.Properties;

/**
 * This class uses Serviceability Agent (SA)
 * (http://openjdk.java.net/groups/hotspot/docs/Serviceability.html)
 * to obtain various information from JVM. Note that
 * Serviceability Agent is available in JDK 5 and up and only for Linux and Solaris.
 * 
 * @author Tomas Hurka
 */
public abstract class SaModel extends Model {

    /**
     * Returns the current system properties in the target Application.
     * 
     * <p> This method returns the system properties in the target virtual
     * machine. Properties whose key or value is not a <tt>String</tt> are 
     * omitted. The method is approximately equivalent to the invocation of the
     * method {@link java.lang.System#getProperties System.getProperties}
     * in the target virtual machine except that properties with a key or
     * value that is not a <tt>String</tt> are not included.
     * @return The system properties of target Application
     * @see java.lang.System#getProperties
     */
    public abstract Properties getSystemProperties();
    
    /**
     * Takes heap dump of target Application.
     * The heap is written to the <tt>fileName</tt> file in the same
     * format as the hprof heap dump.
     * @return returns <CODE>true</CODE> if operation was successful.
     * @param fileName {@link String} where heap dump will be stored.
     */
    public abstract boolean takeHeapDump(String fileName);
    
    /**
     * Takes thread dump of target Application.
     * @return Returns {@link String} of the thread dump from target Application.
     */
    public abstract String takeThreadDump();
    
    /**
     * Returns the Java virtual machine flags.
     *
     * @return String - contains the flags of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  flags be determined.
     */
    public abstract String getJvmFlags();
    
    /**
     * Returns the Java virtual machine command line arguments.
     *
     * @return String - contains the command line arguments of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line arguments cannot be determined.
     */
    public abstract String getJvmArgs();
    
    /**
     * Returns the Java virtual machine command line.
     *
     * @return String - contains the command line of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line cannot be determined.
     */
    public abstract String getJavaCommand();
    
    /**
     * Returns the Java virtual machine implementation version. 
     * This method is equivalent to {@link System#getProperty 
     * System.getProperty("java.vm.version")}.
     *
     * @return the Java virtual machine implementation version.
     *
     * @see java.lang.System#getProperty
     */
    public String getVmVersion() {
        return findByName("java.vm.version");  //NOI18N
    }
    
   /**
     * Returns the Java virtual machine home directory. 
     * This method is equivalent to {@link System#getProperty 
     * System.getProperty("java.home")}.
     *
     * @return the Java virtual machine home directory.
     *
     * @see java.lang.System#getProperty
     */
    public String getJavaHome() {
        return findByName("java.home"); //NOI18N
    }
    
   /**
     * Returns the Java virtual machine VM info. 
     * This method is equivalent to {@link System#getProperty 
     * System.getProperty("java.vm.info")}.
     *
     * @return the Java virtual machine VM info.
     *
     * @see java.lang.System#getProperty
     */
    public String getVmInfo() {
        return findByName("java.vm.info"); //NOI18N
    }
    
    /**
     * Returns the Java virtual machine implementation name. 
     * This method is equivalent to {@link System#getProperty 
     * System.getProperty("java.vm.name")}.
     *
     * @return the Java virtual machine implementation name.
     *
     * @see java.lang.System#getProperty
     */
    public String getVmName() {
        return findByName("java.vm.name"); //NOI18N
    }
    
    private String findByName(String key) {
        Properties p = getSystemProperties();
        if (p == null)
            return null;
        return p.getProperty(key);
    }
    
}
