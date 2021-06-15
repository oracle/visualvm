/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.common;

import org.graalvm.visualvm.lib.jfluid.ProfilerEngineSettings;
import org.graalvm.visualvm.lib.jfluid.global.Platform;
import java.util.Map;
import java.util.ResourceBundle;


/**
 * A Class holding transient data for a single profiling session. Typical usage is creating this class based on context
 * and then call applySettings () on ProfilingEngineSettings. Not used for Attach.
 *
 * The class travels through Ant, and thus can store/load itself to/from Properties.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 */
public final class SessionSettings {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.graalvm.visualvm.lib.common.Bundle"); // NOI18N
    private static final String INCORRECT_PORT_MSG = bundle.getString("SessionSettings_IncorrectPortMsg"); // NOI18N
    private static final String INCORRECT_ARCH_MSG = bundle.getString("SessionSettings_IncorrectArchMsg"); // NOI18N
                                                                                                           // -----
    public static final String PROP_CLASS_NAME = "profiler.session.class.name"; //NOI18N
    public static final String PROP_CLASS_PATH = "profiler.session.class.path"; //NOI18N
    public static final String PROP_ARGS = "profiler.session.args"; //NOI18N
    public static final String PROP_JVM_ARGS = "profiler.session.jvm.args"; //NOI18N
    public static final String PROP_WORKING_DIR = "profiler.session.working.dir"; //NOI18N
    public static final String PROP_JAVA_EXECUTABLE = "profiler.session.java.executable"; //NOI18N
    public static final String PROP_JAVA_VERSION = "profiler.session.java.version"; //NOI18N
    public static final String PROP_ARCHITECTURE = "profiler.session.java.architecture"; //NOI18N
    public static final String PROP_PORT_NO = "profiler.session.port.no"; //NOI18N
    public static final String PROP_REMOTE_HOST = "profiler.session.remote.host"; //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String javaExecutable = ""; //NOI18N
    private String javaVersionString = ""; //NOI18N
    private String jvmArgs = ""; //NOI18N  // Only used for Profile, not for Attach
    private String mainArgs = ""; //NOI18N // Only used for Profile, not for Attach
    private String mainClass = ""; //NOI18N
    private String mainClassPath = ""; //NOI18N
    private String workingDir = System.getProperty("user.dir"); //NOI18N // Only used for Profile, not for Attach
    private int architecture = Platform.ARCH_32;
    private int portNo = 5140;
    private String remoteHost = ""; //NOI18N

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SessionSettings() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setJVMArgs(final String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        jvmArgs = value;
    }

    public String getJVMArgs() {
        return jvmArgs;
    }

    public void setJavaExecutable(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        javaExecutable = value;
    }

    public String getJavaExecutable() {
        return javaExecutable;
    }

    public void setJavaVersionString(String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        javaVersionString = value;
    }

    public String getJavaVersionString() {
        return javaVersionString;
    }

    public void setMainArgs(final String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        mainArgs = value;
    }

    public String getMainArgs() {
        return mainArgs;
    }

    public void setMainClass(final String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        mainClass = value;
    }

    // -- Session-related settings ---------------------------------------------------------------------------------------
    public String getMainClass() {
        return mainClass;
    }

    public void setMainClassPath(final String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        mainClassPath = value;
    }

    public String getMainClassPath() {
        return mainClassPath;
    }

    public void setPortNo(int value) {
        portNo = value;
    }

    public int getPortNo() {
        return portNo;
    }
    
    public String getRemoteHost() {
        return remoteHost;
    }
    
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setSystemArchitecture(int value) {
        architecture = value;
    }

    public int getSystemArchitecture() {
        return architecture;
    }

    public void setWorkingDir(final String value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        workingDir = value;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void applySettings(final ProfilerEngineSettings settings) {
        settings.setMainClass(mainClass);
        settings.setMainClassPath(mainClassPath);
        settings.setMainArgs(mainArgs);
        settings.setJVMArgs(jvmArgs);
        settings.setWorkingDir(workingDir);
        settings.setTargetJVMExeFile(javaExecutable);
        settings.setTargetJDKVersionString(javaVersionString);
        settings.setSystemArchitecture(architecture);
        settings.setPortNo(portNo);
        settings.setRemoteHost(remoteHost);
    }

    public String debug() {
        final StringBuffer sb = new StringBuffer();
        sb.append("mainClass: ").append(mainClass); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("mainClassPath: ").append(mainClassPath); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("mainArgs: ").append(mainArgs); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("jvmArgs =").append(jvmArgs); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("workingDir =").append(workingDir); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("javaExecutable =").append(javaExecutable); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("javaVersionString =").append(javaVersionString); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("architecture =").append(architecture);
        sb.append('\n');
        sb.append("remoteHost =").append(remoteHost); //NOI18N
        sb.append('\n');
        sb.append("portNo =").append(portNo); //NOI18N
        sb.append('\n');

        return sb.toString();
    }

    public void load(Map props) throws IllegalArgumentException {
        setMainClass(getProperty(props, PROP_CLASS_NAME, "")); //NOI18N
        setMainClassPath(getProperty(props, PROP_CLASS_PATH, "")); //NOI18N
        setMainArgs(getProperty(props, PROP_ARGS, "")); //NOI18N
        setJVMArgs(getProperty(props, PROP_JVM_ARGS, "")); //NOI18N
        setWorkingDir(getProperty(props, PROP_WORKING_DIR, System.getProperty("user.home"))); //NOI18N
        setJavaExecutable(getProperty(props, PROP_JAVA_EXECUTABLE, "")); //NOI18N
        setJavaVersionString(getProperty(props, PROP_JAVA_VERSION, "")); //NOI18N

        String arch = getProperty(props, PROP_ARCHITECTURE, String.valueOf(Platform.ARCH_32)); //NOI18N

        try {
            setSystemArchitecture(Integer.parseInt(arch));
        } catch (NumberFormatException e) {
            architecture = 32;
            throw new IllegalArgumentException(INCORRECT_ARCH_MSG, e);
        }

        setRemoteHost(getProperty(props, PROP_REMOTE_HOST, ""));
        String port = getProperty(props, PROP_PORT_NO, "5140"); //NOI18N

        try {
            setPortNo(Integer.parseInt(port));
        } catch (NumberFormatException e) {
            portNo = 5140;
            throw new IllegalArgumentException(INCORRECT_PORT_MSG, e);
        }
    }

    public void store(Map props) {
        props.put(PROP_CLASS_NAME, mainClass);
        props.put(PROP_CLASS_PATH, mainClassPath);
        props.put(PROP_ARGS, mainArgs);
        props.put(PROP_JVM_ARGS, jvmArgs);
        props.put(PROP_WORKING_DIR, workingDir);
        props.put(PROP_JAVA_EXECUTABLE, javaExecutable);
        props.put(PROP_JAVA_VERSION, javaVersionString);
        props.put(PROP_ARCHITECTURE, Integer.toString(architecture));
        props.put(PROP_REMOTE_HOST, remoteHost);
        props.put(PROP_PORT_NO, Integer.toString(portNo));
    }

    private static String getProperty(Map props, String key, String defaultValue) {
        String ret = (String) props.get(key);

        if (ret == null) {
            ret = defaultValue;
        }

        return ret;
    }
}
