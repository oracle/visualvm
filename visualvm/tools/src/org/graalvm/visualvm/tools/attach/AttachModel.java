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

package org.graalvm.visualvm.tools.attach;

import java.util.List;
import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.core.model.Model;
import java.util.Properties;

/**
 * This class uses <a href=http://download.oracle.com/javase/6/docs/technotes/guides/attach/index.html>Attach API</a> 
 * to obtain various information from JVM. Note that
 * Attach API is available in JDK 6 and up and only for local processes running as the
 * same user. See Attach API documentation for mode details.
 * 
 * @author Tomas Hurka
 */
public abstract class AttachModel extends Model {

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
     * Takes heap histogram of target Application.
     * @return Returns {@link HeapHistogram} of the heap from target Application.
     */
    public abstract HeapHistogram takeHeapHistogram();
    
    /**
     * print VM option.
     * Note that VM option is the one which starts with <CODE>-XX:</CODE>.
     * @param name name of VM option. For example <CODE>HeapDumpOnOutOfMemoryError</CODE>
     * @return Full text of VM option. For example <CODE>-XX:+HeapDumpOnOutOfMemoryError</CODE>
     */
    public abstract String printFlag(String name);
    
    /**
     * Sets a VM option of the given name to the specified value. 
     *
     * @param name Name of a VM option 
     * @param value New value of the VM option to be set 
     */
    public abstract void setFlag(String name,String value);

    /**
     * Returns the Java virtual machine command line.
     *
     * @return String - contains the command line of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line cannot be determined.
     */
    public abstract String getCommandLine();

    /**
     * Returns the Java virtual machine command line arguments.
     *
     * @return String - contains the command line arguments of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line arguments cannot be determined.
     */
    public abstract String getJvmArgs();

    /**
     * Returns the Java virtual machine flags.
     *
     * @return String - contains the flags of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  flags cannot be determined.
     */
    public abstract String getJvmFlags();

    /**
     * Tests if it is possible to use JFR in target JVM via Attach API.
     *
     * @return <CODE>true</CODE> if Attach API supports JFR,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isJfrAvailable();

    /**
     * Checks running JFR recording(s) of target Application.
     *
     * @return returns List of recording id-s. If no recordings are in progress,
     * empty List is returned.
     */
    public abstract List<Long> jfrCheck();

    /**
     * Takes JFR dump of target Application.
     * The JFR snapshot is written to the <tt>fileName</tt> file.
     *
     * @param recording id of recording obtained using {@link #jfrCheck()}
     * @param fileName path to file, where JFR snapshot will be written
     * @return returns <CODE>null</CODE> if operation was successful.
     */
    public abstract String takeJfrDump(long recording, String fileName);

    /**
     * Starts a new JFR recording.
     *
     * @return true if recording was successfully started.
     */
    public abstract boolean startJfrRecording();

    /**
     * Stops JFR recording.
     *
     * @return true if recording was successfully stopped.
     */
    public abstract boolean stopJfrRecording();

}
