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

package org.graalvm.visualvm.tools.jmx;

import org.graalvm.visualvm.application.jvm.HeapHistogram;
import org.graalvm.visualvm.core.datasupport.AsyncPropertyChangeSupport;
import org.graalvm.visualvm.core.model.Model;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServiceURL;

/**
 * <p>This class encapsulates the JMX functionality of the target Java application.</p>
 *
 * <p>Call {@link JmxModelFactory#getJmxModelFor(Application)} to get an instance of the
 * {@link JmxModel} class.</p>
 *
 * <p>Usually this class will be used as follows:</p>
 *
 * <pre>
 * JmxModel jmx = JmxModelFactory.getJmxModelFor(application);
 * if (jmx == null || jmx.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
 *     // JMX connection not available...
 * } else {
 *     MBeanServerConnection mbsc = jmx.getMBeanServerConnection();
 *     if (mbsc != null) {
 *         // Invoke JMX operations...
 *     }
 * }
 * </pre>
 *
 * <p>Any of the {@link CachedMBeanServerConnectionFactory CachedMBeanServerConnectionFactory.getCachedMBeanServerConnection}
 * methods can be used to work with a {@link CachedMBeanServerConnection} instead of a plain {@link MBeanServerConnection}.</p>
 *
 * <p>In case the JMX connection is not established yet, you could register
 * a listener on the {@code JmxModel} for ConnectionState property changes.
 * The JmxModel notifies any PropertyChangeListeners about the ConnectionState
 * property change to CONNECTED and DISCONNECTED. The JmxModel instance will
 * be the source for any generated events.</p>
 *
 * <p>Polling for the ConnectionState is also possible by calling
 * {@link JmxModel#getConnectionState()}.</p>
 *
 * @author Luis-Miguel Alventosa
 * @author Tomas Hurka
 */
public abstract class JmxModel extends Model {

    private static final String JAR_SUFFIX = ".jar";  // NOI18N
    private static final Pattern MODULE_MAIN_CLASS_PATTERN = Pattern.compile("^(\\w+\\.)*\\w+/(\\w+\\.)+\\w+$");

    protected PropertyChangeSupport propertyChangeSupport =
            new AsyncPropertyChangeSupport(this);
    /**
     * The {@link ConnectionState ConnectionState} bound property name.
     */
    public static final String CONNECTION_STATE_PROPERTY = "connectionState"; // NOI18N

    /**
     * Values for the {@linkplain #CONNECTION_STATE_PROPERTY
     * <i>ConnectionState</i>} bound property.
     */
    public enum ConnectionState {
        /**
         * The connection has been successfully established.
         */
        CONNECTED,
        /**
         * No connection present.
         */
        DISCONNECTED,
        /**
         * The connection is being attempted.
         */
        CONNECTING
    }

    /**
     * Add a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * to the listener list.
     * The listener is registered for all properties.
     * The same listener object may be added more than once, and will be called
     * as many times as it is added.
     * If {@code listener} is {@code null}, no exception is thrown and
     * no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Removes a {@link java.beans.PropertyChangeListener PropertyChangeListener}
     * from the listener list. This
     * removes a {@code PropertyChangeListener} that was registered for all
     * properties. If {@code listener} was added more than once to the same
     * event source, it will be notified one less time after being removed. If
     * {@code listener} is {@code null}, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param listener the {@code PropertyChangeListener} to be removed.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Returns the current connection state.
     *
     * @return the current connection state.
     */
    public abstract ConnectionState getConnectionState();

    /**
     * Returns the {@link MBeanServerConnection MBeanServerConnection} for the
     * connection to an application. The returned {@code MBeanServerConnection}
     * object becomes invalid when the connection state is changed to the
     * {@link ConnectionState#DISCONNECTED DISCONNECTED} state.
     *
     * @return the {@code MBeanServerConnection} for the
     * connection to an application. It returns {@code null}
     * if the JMX connection couldn't be established.
     */
    public abstract MBeanServerConnection getMBeanServerConnection();

    /**
     * Returns the {@link JMXServiceURL} associated to this (@code JmxModel}.
     *
     * @return the {@link JMXServiceURL} associated to this (@code JmxModel}.
     */
    public abstract JMXServiceURL getJMXServiceURL();

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
     * Tests if it is possible to obtain heap dump from target JVM via JMX.
     * @return <CODE>true</CODE> if JMX supports heap dump,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isTakeHeapDumpSupported();

    /**
     * Takes heap dump of target JVM via JMX.
     * The heap is written to the <tt>fileName</tt> file in the same
     * format as the hprof heap dump.
     * @param fileName {@link String} where heap dump will be stored.
     * @return returns <CODE>true</CODE> if operation was successful.
     */
    public abstract boolean takeHeapDump(String fileName);

    /**
     * Tests if it is possible to obtain thread dump from target JVM via JMX.
     * @return <CODE>true</CODE> if JMX supports thread dump,
     * <CODE>false</CODE> otherwise
     */
    public abstract boolean isTakeThreadDumpSupported();

    /**
     * Takes thread dump of target JVM via JMX.
     * @return Returns {@link String} of the thread dump from target JVM.
     */
    public abstract String takeThreadDump();

    /**
     * Returns the combined stack trace from each thread
     * whose ID is in the input array <tt>threadIds</tt>,
     * @param threadIds an array of thread IDs
     * @return Returns {@link String} representing combined stack traces
     * from all threads IDs
     */
    public abstract String takeThreadDump(long[] threadIds);

    /**
     * Takes heap histogram of target Application.
     * @return Returns {@link HeapHistogram} of the heap from target Application.
     */
    public abstract HeapHistogram takeHeapHistogram();
 
    /**
     * print VM option.
     * Note that VM option is the one which starts with
     * <CODE>-XX:</CODE>
     * @param name name of VM option. For example <CODE>HeapDumpOnOutOfMemoryError</CODE>
     * @return Text value of VM option. For example <CODE>true</CODE>
     */
    public abstract String getFlagValue(String name);

    /**
     * Sets a VM option of the given name to the specified value.
     *
     * @param name Name of a VM option
     * @param value New value of the VM option to be set
     */
    public abstract void setFlagValue(String name,String value);

    /**
     * Tests if it is possible to use JFR in target JVM via JMX API.
     *
     * @return <CODE>true</CODE> if JMX supports JFR,
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
     * Returns the Java virtual machine command line.
     *
     * @return String - contains the command line of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line cannot be determined.
     */
    public abstract String getCommandLine();

    /**
     * Return the arguments to the main class for the target Java application.
     * Returns the arguments to the main class. If the arguments can't be
     * found, <code>null</code> is returned.
     *
     * @return String - contains the arguments to the main class for the
     *                  target Java application or the <code>null</code>
     *                  if the command line cannot be determined.
     */

    public String getMainArgs() {
        String commandLine = getCommandLine();

        if (commandLine != null) {
            String arg0 = getFirstArgument(commandLine);

            int firstSpace = arg0.length();
            if (firstSpace < commandLine.length()) {
                return commandLine.substring(firstSpace);
            }
        }
        return null;
    }

    /**
     * Return the main class for the target Java application.
     * Returns the main class, if the application started with the <em>-jar</em> option,
     * it tries to determine main class from the jar file. If
     * the jar file is not accessible, the main class is simple
     * name of the jar file.
     * @return String - the main class of the target Java
     *                  application.
     */
    public String getMainClass() {
        String commandLine = getCommandLine();

        if (commandLine != null) {
            String mainClassName = getFirstArgument(commandLine);

            if (mainClassName.endsWith(JAR_SUFFIX)) {
                mainClassName = mainClassName.replace('\\', '/');
                int index = mainClassName.lastIndexOf('/');
                if (index != -1) {
                    mainClassName = mainClassName.substring(index + 1);
                }
            } else if (MODULE_MAIN_CLASS_PATTERN.matcher(mainClassName).find()) {
                return mainClassName.substring(mainClassName.indexOf('/')+1);
            }
            mainClassName = mainClassName.replace('\\', '/').replace('/', '.');
            return mainClassName;
        }
        return null;
    }

    private String getFirstArgument(String commandLine) {
        String mainClassName = null;

        // search for jar file
        int jarIndex = commandLine.indexOf(JAR_SUFFIX);
        if (jarIndex != -1) {
            String jarFile = commandLine.substring(0,jarIndex+JAR_SUFFIX.length());
            // if this is not end of commandLine check that jar file is separated by space from other arguments
            if (jarFile.length() == commandLine.length() || commandLine.charAt(jarFile.length()) == ' ') {
                // jarFile must be on classpath
                String classPath = getClassPath();
                if (classPath != null && classPath.contains(jarFile)) {
                    mainClassName = jarFile;
                }
            }
        }
        // it looks like ordinary commandline with main class
        if (mainClassName == null) {
            int firstSpace = commandLine.indexOf(' ');
            if (firstSpace > 0) {
                mainClassName = commandLine.substring(0, firstSpace);
            } else {
                mainClassName = commandLine;
            }
        }
        return mainClassName;
    }

    /**
     * Returns the Java virtual machine implementation version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.class.path")}.
     *
     * @return the Java virtual machine classpath.
     *
     * @see java.lang.System#getProperty
     */
    private String getClassPath() {
        return getSystemProperties().getProperty("java.class.path"); // NOI18N
    }
}
