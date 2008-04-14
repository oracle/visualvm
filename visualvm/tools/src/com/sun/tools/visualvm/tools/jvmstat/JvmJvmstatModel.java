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

package com.sun.tools.visualvm.tools.jvmstat;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.model.Model;
import com.sun.tools.visualvm.host.Host;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 *
 * @author Tomas Hurka
 */
public abstract class JvmJvmstatModel extends Model {
    private static final Logger LOGGER = Logger.getLogger(JvmJvmstatModel.class.getName());
    
    private static final String JAR_SUFFIX = ".jar";  // NOI18N
    
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
    protected List<MonitoredValue> genCapacity;
    protected List<MonitoredValue> genUsed;
    protected long[] genMaxCapacity;
    
    protected JvmJvmstatModel(Application app,JvmstatModel stat) {
        application = app;
        jvmstat = stat;
    }
    
    public String getCommandLine() {
        return jvmstat.findByName("sun.rt.javaCommand"); // NOI18N
    }
    
    public String getJvmArgs() {
        return jvmstat.findByName("java.rt.vmArgs");    // NOI18N
    }
    
    public String getJvmFlags() {
        return jvmstat.findByName("java.rt.vmFlags");   // NOI18N
    }   
    
    public String getJavaHome() {
        return jvmstat.findByName("java.property.java.home");   // NOI18N
    }
    
    public String getVmInfo() {
        return jvmstat.findByName("java.property.java.vm.info");    // NOI18N
    }
    
    public String getVmName() {
        return jvmstat.findByName("java.property.java.vm.name");    // NOI18N
    }
    
    public String getVmVersion() {
        return jvmstat.findByName("java.property.java.vm.version"); // NOI18N
    }
    
    public String getVmVendor() {
        return jvmstat.findByName("java.property.java.vm.vendor");  // NOI18N
    }
    
    public String getClassPath() {
        return jvmstat.findByName("java.property.java.class.path"); // NOI18N
    }
    
    public boolean isAttachable() {
        String jvmCapabilities = jvmstat.findByName("sun.rt.jvmCapabilities");  // NOI18N
        if (jvmCapabilities == null) {
             return false;
        }
        return jvmCapabilities.charAt(0) == '1';
    }
    
    public String getMainArgs() {
        String commandLine = getCommandLine();
        String arg0 = getFirstArgument();

        int firstSpace = arg0.length();
        if (firstSpace < commandLine.length()) {
            return commandLine.substring(firstSpace);
        }
        return null;
    }
    
    public String getMainClass() {
        String mainClassName = getFirstArgument();
        // if we are on localhost try read main class from jar file
        if (application.getHost().equals(Host.LOCALHOST)) {
            File jarFile = new File(mainClassName);
            if (jarFile.exists()) {
                try {
                    JarFile jf = new JarFile(jarFile);
                    mainClassName = jf.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                    assert mainClassName!=null;
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
                if (classPath != null && classPath.indexOf(jarFile) != -1) {
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

    public long getLoadedClasses() {
        return getLongValue(loadedClasses);
    }
    
    public long getSharedLoadedClasses() {
        return getLongValue(sharedLoadedClasses);
    }
    
    public long getSharedUnloadedClasses() {
        return getLongValue(sharedUnloadedClasses);
   }
    
    public long getUnloadedClasses() {
        return getLongValue(unloadedClasses);
    }
    
    public long getThreadsDaemon() {
        return getLongValue(threadsDaemon);
    }
    
    public long getThreadsLive() {
        return getLongValue(threadsLive);
    }
    
    public long getThreadsLivePeak() {
        return getLongValue(threadsStarted);
    }
    
    public long getThreadsStarted() {
        return getLongValue(threadsStarted);
    }
    
    public long getApplicationTime() {
        return getLongValue(applicationTime);
    }
    
    public long getUpTime() {
        return getLongValue(upTime);
    }
    
    public long[] getGenCapacity() {
        return getGenerationSum(genCapacity);
    }
    
    public long[] getGenUsed() {
        return getGenerationSum(genUsed);
    }
    
    public long[] getGenMaxCapacity() {
        return genMaxCapacity;
    }
    
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
                long val = ((Long)value.getValue()).longValue();
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