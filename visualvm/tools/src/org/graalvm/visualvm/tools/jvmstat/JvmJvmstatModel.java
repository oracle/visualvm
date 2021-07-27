/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.tools.jvmstat;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.model.Model;
import org.graalvm.visualvm.host.Host;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.openide.util.NbBundle;

/**
 * 
 * This class encapsulates information available via Jvmstat counters.
 * It is preferred to use this class to get access to java home, 
 * total loaded classes, number of running threads etc. The advantage is 
 * that user code does not depend on particular counter name, which can be
 * different for different JVM.
 * @author Tomas Hurka
 */
public abstract class JvmJvmstatModel extends Model {
    private static final Logger LOGGER = Logger.getLogger(JvmJvmstatModel.class.getName());
    
    private static final String JAR_SUFFIX = ".jar";  // NOI18N
    
    private static final Pattern MODULE_MAIN_CLASS_PATTERN = Pattern.compile("^(\\w+\\.)*\\w+/(\\w+\\.)+\\w+$");

    protected Application application;
    protected JvmstatModel jvmstat;
    protected MonitoredValue loadedClasses;
    protected MonitoredValue sharedLoadedClasses;
    protected MonitoredValue sharedUnloadedClasses;
    protected MonitoredValue unloadedClasses;
    protected MonitoredValue threadsDaemon;
    protected MonitoredValue threadsLive;
    protected MonitoredValue threadsLivePeak;
    protected MonitoredValue threadsStarted;
    protected MonitoredValue applicationTime;
    protected MonitoredValue upTime;
    protected long osFrequency;
    protected String[] genName;
    protected List<MonitoredValue> genCapacity;
    protected List<MonitoredValue> genUsed;
    protected long[] genMaxCapacity;
    
    protected JvmJvmstatModel(Application app,JvmstatModel stat) {
        application = app;
        jvmstat = stat;
        genName = new String[2];
        genName[0] = NbBundle.getMessage(JvmJvmstatModel.class, "LBL_Heap");   // NOI18N
        genName[1] = NbBundle.getMessage(JvmJvmstatModel.class, "LBL_PermGen");   // NOI18N        
    }
    
    /**
     * Returns the Java virtual machine command line.
     *
     * @return String - contains the command line of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line cannot be determined.
     */
    public String getCommandLine() {
        return jvmstat.findByName("sun.rt.javaCommand"); // NOI18N
    }
    
    /**
     * Returns the Java virtual machine command line arguments.
     *
     * @return String - contains the command line arguments of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  command line arguments cannot be determined.
     */
    public String getJvmArgs() {
        return jvmstat.findByName("java.rt.vmArgs");    // NOI18N
    }
    
    /**
     * Returns the Java virtual machine flags.
     *
     * @return String - contains the flags of the target Java
     *                  application or <CODE>NULL</CODE> if the
     *                  flags be determined.
     */
    public String getJvmFlags() {
        return jvmstat.findByName("java.rt.vmFlags");   // NOI18N
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
        return jvmstat.findByName("java.property.java.home");   // NOI18N
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
        return jvmstat.findByName("java.property.java.vm.info");    // NOI18N
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
        return jvmstat.findByName("java.property.java.vm.name");    // NOI18N
    }
    
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
        return jvmstat.findByName("java.property.java.vm.version"); // NOI18N
    }
    
    /**
     * Returns the Java Runtime Environment version.
     * This method is equivalent to {@link System#getProperty
     * System.getProperty("java.version")}.
     *
     * @return the Java virtual machine implementation version.
     *
     * @see java.lang.System#getProperty
     */
    public String getJavaVersion() {
        return jvmstat.findByName("java.property.java.version"); // NOI18N
    }

    /**
     * Returns the Java virtual machine implementation version. 
     * This method is equivalent to {@link System#getProperty 
     * System.getProperty("java.vm.vendor")}.
     *
     * @return the Java virtual machine vendor.
     *
     * @see java.lang.System#getProperty
     */
    public String getVmVendor() {
        return jvmstat.findByName("java.property.java.vm.vendor");  // NOI18N
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
    public String getClassPath() {
        return jvmstat.findByName("java.property.java.class.path"); // NOI18N
    }
    
    /**
     * Tests if target JVM supports
     * <a href=http://download.oracle.com/javase/6/docs/technotes/guides/attach/index.html>Attach API</a>
     * and that support is enabled in target JVM.
     * @return <CODE>true</CODE> if JVM supports Attach API, <CODE>false</CODE> otherwise
     */
    public boolean isAttachable() {
        String jvmCapabilities = jvmstat.findByName("sun.rt.jvmCapabilities");  // NOI18N
        if (jvmCapabilities == null) {
             return false;
        }
        return jvmCapabilities.charAt(0) == '1';
    }
    
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
        String arg0 = getFirstArgument();

        int firstSpace = arg0.length();
        if (firstSpace < commandLine.length()) {
            return commandLine.substring(firstSpace);
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
        String mainClassName = getFirstArgument();
        // if we are on localhost try read main class from jar file
        if (application.isLocalApplication()) {
            File jarFile = new File(mainClassName);
            if (jarFile.exists()) {
                try {
                    JarFile jf = new JarFile(jarFile);
                    mainClassName = jf.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                    assert mainClassName!=null;
                    jf.close();
                } catch (IOException ex) {
                    LOGGER.throwing(JvmJvmstatModel.class.getName(), "getMainClass", ex);   // NOI18N
                }
            }
        }
        
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

    private String getFirstArgument() {
        String commandLine = getCommandLine();
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
     * Returns the total number of classes that have been loaded since
     * the Java virtual machine has started execution.
     *
     * @return the total number of classes loaded.
     *
     */
    public long getLoadedClasses() {
        return getLongValue(loadedClasses);
    }
    
    /** 
     * Returns the total number of shared classes that have been loaded since
     * the Java virtual machine has started execution.
     *
     * @return the total number of shared classes loaded.
     *
     */
    public long getSharedLoadedClasses() {
        return getLongValue(sharedLoadedClasses);
    }
    
    /** 
     * Returns the total number of shared classes unloaded since the Java virtual machine
     * has started execution.
     *
     * @return the total number of unloaded shared classes.
     */
    public long getSharedUnloadedClasses() {
        return getLongValue(sharedUnloadedClasses);
   }
    
    /** 
     * Returns the total number of classes unloaded since the Java virtual machine
     * has started execution.
     *
     * @return the total number of unloaded classes.
     */
    public long getUnloadedClasses() {
        return getLongValue(unloadedClasses);
    }
    
    /**
     * Returns the current number of live daemon threads.
     *
     * @return the current number of live daemon threads.
     */
    public long getThreadsDaemon() {
        return getLongValue(threadsDaemon);
    }
    
    /**
     * Returns the current number of live threads including both 
     * daemon and non-daemon threads.
     *
     * @return the current number of live threads.
     */
    public long getThreadsLive() {
        return getLongValue(threadsLive);
    }
    
    /**
     * Returns the peak live thread count since the Java virtual machine 
     * started or peak was reset.
     *
     * @return the peak live thread count.
     */
    public long getThreadsLivePeak() {
        return getLongValue(threadsLivePeak);
    }
    
    /**
     * Returns the total number of threads created and also started 
     * since the Java virtual machine started.
     *
     * @return the total number of threads started.
     */
    public long getThreadsStarted() {
        return getLongValue(threadsStarted);
    }
    
    /**
     * Returns the total time of when application is running in OS ticks.
     * Application time is the uptime minus time spent in safe points.
     * Note that this value is updated only at the beginning and e 
     * @return application time of the Java virtual machine in OS ticks.
     * @see JvmJvmstatModel#getOsFrequency()
     */
    public long getApplicationTime() {
        return getLongValue(applicationTime);
    }
    
    /**
     * Returns the uptime of the Java virtual machine in OS ticks.
     * @return uptime of the Java virtual machine in OS ticks.
     * @see JvmJvmstatModel#getOsFrequency()
     */
    public long getUpTime() {
        return getLongValue(upTime);
    }
    
    /**
     * Returns the names of Java virtual machine spaces.
     *
     * @return Index 0 is the display name for heap,
     * index 1 is display name for Permanent Generation (PermGen)
     */
    public String[] getGenName() {
        return genName.clone();
    }

    /** 
     * Returns the amount of memory in bytes that is committed for
     * the Java virtual machine to use.  This amount of memory is
     * guaranteed for the Java virtual machine to use. 
     *
     * @return long[0] - the amount of committed heap memory in bytes.
     *         long[1] - the amount of committed Perm Gen memory in bytes.
     *
     */
    public long[] getGenCapacity() {
        return getGenerationSum(genCapacity);
    }
    
    /** 
     * Returns the amount of used memory in bytes.
     *
     * @return long[0] - the amount of used heap memory in bytes.
     *         long[1] - the amount of used Perm Gen memory in bytes.
     *
     */
    public long[] getGenUsed() {
        return getGenerationSum(genUsed);
    }
    
    /** 
     * Returns the maximum amount of memory in bytes that can be 
     * used for memory management.  This method returns <tt>-1</tt> 
     * if the maximum memory size is undefined.
     * 
     * @return long[0] - the maximum amount of heap memory in bytes; 
     *         long[1] - the maximum amount of Perm Gen memory in bytes;
     */
    public long[] getGenMaxCapacity() {
        return genMaxCapacity.clone();
    }
    
    /**
     * Returns the number of OS ticks per second.
     *
     * @return number of OS ticks per second.
     */
    public long getOsFrequency() {
        return osFrequency;
    }
    
    protected abstract String getPermGenPrefix();

    protected long getLongValue(MonitoredValue val) {
        if (val != null) {
            return ((Long)val.getValue()).longValue();
        }
        return 0;
    }
    
    protected long[] getGenerationSum(List<MonitoredValue> values) {
        long[] results=new long[2];
        String prefix = getPermGenPrefix();
        
        for (MonitoredValue value : values) {
            if (value != null) {
                long val = getLongValue(value);
                if (value.getName().startsWith(prefix)) {
                    results[1]+= val;
                } else {
                    results[0]+= val;
                }
            }
        }
        return results;
    }
}
