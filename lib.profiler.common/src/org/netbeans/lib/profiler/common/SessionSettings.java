/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.common;

import org.netbeans.lib.profiler.ProfilerEngineSettings;
import org.netbeans.lib.profiler.global.Platform;
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
    private static final ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.lib.profiler.common.Bundle"); // NOI18N
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
    }

    public String debug() {
        final StringBuffer sb = new StringBuffer();
        sb.append("mainClass: " + mainClass); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("mainClassPath: " + mainClassPath); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("mainArgs: " + mainArgs); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("jvmArgs =" + jvmArgs); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("workingDir =" + workingDir); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("javaExecutable =" + javaExecutable); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("javaVersionString =" + javaVersionString); //NOI18N
        sb.append('\n'); //NOI18N
        sb.append("architecture =" + architecture);
        sb.append('\n');
        sb.append("portNo =" + portNo); //NOI18N
        sb.append('\n');

        return sb.toString();
    }

    public void load(Map props) {
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
            Profiler.getDefault().displayWarning(INCORRECT_ARCH_MSG);
            architecture = 32;
        }

        String port = getProperty(props, PROP_PORT_NO, "5140"); //NOI18N

        try {
            setPortNo(Integer.parseInt(port));
        } catch (NumberFormatException e) {
            Profiler.getDefault().displayWarning(INCORRECT_PORT_MSG);
            portNo = 5140;
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
