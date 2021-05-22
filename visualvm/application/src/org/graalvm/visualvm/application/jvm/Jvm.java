/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.application.jvm;

import org.graalvm.visualvm.core.model.Model;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * This class encapsulates functionality of the target Java application. Use
 * {@link JvmFactory#getJVMFor(Application )} to get instance of Jvm class.
 * @author Tomas Hurka
 */
public abstract class Jvm extends Model {
    protected static final Logger LOGGER = Logger.getLogger(Jvm.class.getName());
    
    /**
     * Property name for {@link #isDumpOnOOMEnabled()}. 
     * If property dumpOnOOMEnabled changes its state, property change is fired
     * with this property name.
     */
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
     * Tests if target JVM is JRE 1.8.
     * @return <CODE>true</CODE> if JVM is JRE 1.8, <CODE>false</CODE> otherwise
     */
    public boolean is18() {
        // default implementation for backward compatibility
        return false;
    }

    /**
     * Tests if target JVM is JRE 1.9.
     * @return <CODE>true</CODE> if JVM is JRE 1.9, <CODE>false</CODE> otherwise
     */
    public boolean is19() {
        // default implementation for backward compatibility
        return false;
    }

    /**
     * Tests if target JVM is JRE 10.
     * @return <CODE>true</CODE> if JVM is JRE 10, <CODE>false</CODE> otherwise
     */
    public boolean is100() {
        // default implementation for backward compatibility
        return false;
    }

    /**
     * Tests if target JVM is JRE 11.
     * @return <CODE>true</CODE> if JVM is JRE 11, <CODE>false</CODE> otherwise
     */
    public boolean is110() {
        // default implementation for backward compatibility
        return false;
    }

    /**
     * Tests if VisualVM can attach to target JVM via
     * <a href=http://download.oracle.com/javase/6/docs/technotes/guides/attach/index.html>Attach API</a>.
     * @return <CODE>true</CODE> if VisualVM can attach to target JVMvia Attach API, <CODE>false</CODE> otherwise
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
     * Returns the VM version for the target Java application.
     * @return Returns the version of the target Java application
     */
    public abstract String getVmVersion();

    /**
     * Returns the Java version for the target Java application.
     * @return Returns the Java version of the target Java application
     */
    public abstract String getJavaVersion();
    
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
     * {@link MonitoredDataListener#monitoredDataEvent(MonitoredData )}
     * @return <CODE>true</CODE> if JVM is supports monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isMonitoringSupported();

    /**
     * Tests if target JVM supports class monitoring. If true,
     * methods getLoadedClasses(), getSharedLoadedClasses(),
     * getSharedUnloadedClasses(), getUnloadedClasses()
     * from {@link MonitoredData} returns meaningful data.
     * @return <CODE>true</CODE> if JVM supports class monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isClassMonitoringSupported();

    /**
     * Tests if target JVM supports thread monitoring. If true,
     * methods getThreadsDaemon(), getThreadsLive(),
     * getThreadsLivePeak(), getThreadsStarted()
     * from {@link MonitoredData} returns meaningful data.
     * @return <CODE>true</CODE> if JVM supports thread monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isThreadMonitoringSupported();

    /**
     * Tests if target JVM supports thread monitoring. If true,
     * methods getGenCapacity(), getGenUsed(), getGenMaxCapacity()
     * from {@link MonitoredData} returns meaningful data.
     * @return <CODE>true</CODE> if JVM supports memory monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isMemoryMonitoringSupported();

    /**
     * Tests if target JVM supports process CPU time monitoring. If true,
     * methods getProcessCpuTime()
     * from {@link MonitoredData} returns meaningful data.
     * @return <CODE>true</CODE> if JVM supports process CPU time monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isCpuMonitoringSupported();
    
    /**
     * Tests if target JVM supports Garbage collection time monitoring. If true,
     * methods getCollectionTime()
     * from {@link MonitoredData} returns meaningful data.
     * @return <CODE>true</CODE> if JVM supports Garbage collection time monitoring,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isCollectionTimeSupported();

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
     * Returns the names of Java virtual machine spaces.
     *
     * @return Index 0 is the display name for heap,
     * index 1 is display name for Permanent Generation (PermGen)
     */
    public abstract String[] getGenName();

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
     * @param enabled new value of HeapDumpOnOutOfMemoryError flag
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
    
    /**
     * Takes heap histogram of target Application.
     * @return Returns {@link HeapHistogram} of the heap from target Application.
     */
    public HeapHistogram takeHeapHistogram() {
        // default implementation for backward compatibility
        return null;        
    }
     
    /**
     * Returns the number of processors available to the Java virtual machine.
     *
     * <p> This value may change during a particular invocation of the virtual
     * machine.  Applications that are sensitive to the number of available
     * processors should therefore occasionally poll this property and adjust
     * their resource usage appropriately. </p>
     *
     * @return  the maximum number of processors available to the virtual
     *          machine; never smaller than one
     */
    public int getAvailableProcessors() {
        // default implementation
        return 1;
    }

    /**
     * Tests if it is possible to use JFR in target JVM.
     *
     * @return <CODE>true</CODE> if JVM supports JFR,
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
     * @return true if recording was successfully started.
     */
    public abstract boolean startJfrRecording();

    /**
     * Stops JFR recording.
     * @return true if recording was successfully stopped.
     */
    public abstract boolean stopJfrRecording();

    /**
     * Provides access to current values of monitored data in instance of {@link MonitoredData}. 
     * The methods may return <CODE>null</CODE> if the {@link MonitoredData} are not available
     * or are not supported by particular {@link Jvm} instance.
     * @return instance of {@link MonitoredData} with current values of monitored data or
     * <CODE>null</CODE> if the monitored data cannot be retrieved.
     */
    public abstract MonitoredData getMonitoredData();
    
    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If <code>listener</code> is null, no exception is thrown and no action
     * is taken.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *		that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        changeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
    
}
