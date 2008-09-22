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

package com.sun.tools.visualvm.application.jvm;

import com.sun.tools.visualvm.core.model.Model;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class encapsulates functionality of the target Java application. Use
 * {@link JVMFactory.getJVMFor()} to get instance of JVM class.
 * @author Tomas Hurka
 */
public abstract class Jvm extends Model {
    protected static final Logger LOGGER = Logger.getLogger(Jvm.class.getName());
    
    public static final String PROPERTY_DUMP_OOME_ENABLED = "prop_oome";    // NOI18N
    
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    
    /**
     * Tests if target JVM is JRE 1.4.
     * @return <CODE>true</CODE> if JVM is JRE 1.4, <CODE>false</CODE> otherwise
     */
    public abstract boolean is14();

    /**
     * Tests if target JVM is JRE 1.5.
     * @return <CODE>true</CODE> if JVM is JRE 1.5, <CODE>false</CODE> otherwise
     */
    public abstract boolean is15();

    /**
     * Tests if target JVM is JRE 1.6.
     * @return <CODE>true</CODE> if JVM is JRE 1.6, <CODE>false</CODE> otherwise
     */
    public abstract boolean is16();

    /**
     * Tests if target JVM is JRE 1.7.
     * @return <CODE>true</CODE> if JVM is JRE 1.7, <CODE>false</CODE> otherwise
     */
    public abstract boolean is17();

    /**
     * Tests if target JVM supports Attach API.
     * {@link http://java.sun.com/javase/6/docs/technotes/guides/attach/index.html}
     * @return <CODE>true</CODE> if JVM supports Attach API, <CODE>false</CODE> otherwise
     */
    public abstract boolean isAttachable();

    /**
     * Tests if target JVM supports getting basic JVM informations. This includes
     * getCommandLine(), getJvmArgs(), getJvmFlags(), getMainClass(),
     * getVmVersion(), getJavaHome(), getVMInfo(), getVMName() methods.
     * @return <CODE>true</CODE> if target JVM supports basic JVM information,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isBasicInfoSupported();

    /**
     * Return the command line for the target Java application.
     * @return Returns the command line of the target Java application.
     */
    public abstract String getCommandLine();

    /**
     * Return the JVM arguments for the target Java application.
     * @return Returns the arguments passed to the JVM for the target Java application
     */
    public abstract String getJvmArgs();

    /**
     * Return the JVM flags for the target Java application.
     * @return Returns the flags passed to the JVM for the target Java application
     */
    public abstract String getJvmFlags();

    /**
     * Return the arguments to the main class for the target Java application.
     * @return Returns the arguments to the main class.
     */
    public abstract String getMainArgs();

    /**
     * Return the main class for the target Java application or the name of the jar file if the application
     * was started with the <code>-jar</code> option.
     * @return Returns the main class of the target Java application
     * or the name of the jar file.
     */
    public abstract String getMainClass();

    /**
     * Return the VM version for the target Java application.
     * @return Returns the version of the target Java application
     */
    public abstract String getVmVersion();

    /**
     * Returns the java.home property for the target Java application.
     * "java.home" property specify the directory
     * into which the JRE was installed.
     * @return Returns java.home property.
     */
    public abstract String getJavaHome();

    /**
     * Returns the java.vm.info property for the target Java application.
     * the java.vm.info property (which is reflected in the text displayed by "java -version")
     * @return Returns java.vm.info property.
     */
    public abstract String getVmInfo();

    /**
     * Returns the java.vm.name property for the target Java application.
     * java.vm.name property is Java Virtual Machine implementation name.
     * @return Returns java.vm.name property.
     */
    public abstract String getVmName();
    
    /**
     * Returns the java.vm.vendor property for the target Java application.
     * java.vm.vendor property is Java Virtual Machine vendor name.
     * @return Returns java.vm.vendor property.
     */
    public abstract String getVmVendor();

    /**
     * Tests if target JVM monitoring is supported. If true, JVM fires
     * {@link MonitoredDataListener#monitoredDataEvent()}
     * @return <CODE>true</CODE> if JVM is supports monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isMonitoringSupported();

    /**
     * Tests if target JVM supports class monitoring. If true,
     * methods getLoadedClasses(), getSharedLoadedClasses(),
     * getSharedUnloadedClasses(), getUnloadedClasses()
     * from {@link MonitoredData} returns meaningfull data.
     * @return <CODE>true</CODE> if JVM supports class monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isClassMonitoringSupported();

    /**
     * Tests if target JVM supports thread monitoring. If true,
     * methods getThreadsDaemon(), getThreadsLive(),
     * getThreadsLivePeak(), getThreadsStarted()
     * from {@link MonitoredData} returns meaningfull data.
     * @return <CODE>true</CODE> if JVM supports thread monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isThreadMonitoringSupported();

    /**
     * Tests if target JVM supports thread monitoring. If true,
     * methods getGenCapacity(), getGenUsed(), getGenMaxCapacity()
     * from {@link MonitoredData} returns meaningfull data.
     * @return <CODE>true</CODE> if JVM supports memory monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isMemoryMonitoringSupported();

    /**
     * Tests if target JVM supports process CPU time monitoring. If true,
     * methods getProcessCpuTime()
     * from {@link MonitoredData} returns meaningfull data.
     * @return <CODE>true</CODE> if JVM supports process CPU time monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isCpuMonitoringSupported();
    
    /**
     * Adds a {@link MonitoredDataListener} to the listener list.
     * @param l the MonitoredDataListener to be added
     */
    public abstract void addMonitoredDataListener(MonitoredDataListener l);

    /**
     * Removes a {@link MonitoredDataListener} to the listener list.
     * @param l the MonitoredDataListener to be removed
     */
    public abstract void removeMonitoredDataListener(MonitoredDataListener l);

    /**
     * Tests if it is possible to obtain system properties from target JVM.
     * @return <CODE>true</CODE> if JVM supports system properties,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isGetSystemPropertiesSupported();

    /**
     * Returns a map of names and values of all system properties of target JVM.
     * @return a map of names and values of all system properties.
     */
    public abstract Properties getSystemProperties();

    /**
     * Tests if it is possible to set HeapDumpOnOutOfMemoryError flag in target JVM.
     * @return <CODE>true</CODE> if JVM supports setting of HeapDumpOnOutOfMemoryError flag,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isDumpOnOOMEnabledSupported();

    /**
     * Gets value of HeapDumpOnOutOfMemoryError flag from target JVM.
     * @return returns value of HeapDumpOnOutOfMemoryError flag.
     */
    public abstract boolean isDumpOnOOMEnabled();

    /**
     * sets HeapDumpOnOutOfMemoryError flag on target JVM.
     * @param enabled new vaule of HeapDumpOnOutOfMemoryError flag
     */
    public abstract void setDumpOnOOMEnabled(boolean enabled);

    /**
     * Tests if it is possible to obtain heap dump from target JVM.
     * @return <CODE>true</CODE> if JVM supports heap dump,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isTakeHeapDumpSupported();

    /**
     * Takes heap dump of target JVM.
     * @return returns {@link File} where heap dump is stored.
     * @throws java.io.IOException I/O error
     */
    public abstract File takeHeapDump() throws IOException;

    /**
     * Tests if it is possible to obtain thread dump from target JVM.
     * @return <CODE>true</CODE> if JVM supports thread dump,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isTakeThreadDumpSupported();

    /**
     * Takes thread dump of target JVM.
     * @throws java.io.IOException i/O error
     * @return Returns {@link String} of the thread dump from target JVM.
     */
    public abstract File takeThreadDump() throws IOException;
    
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
    
}
