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

package org.graalvm.visualvm.application.views.overview;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSource;
import org.graalvm.visualvm.core.datasource.Storage;
import org.graalvm.visualvm.core.snapshot.Snapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import org.openide.util.NbBundle;


/**
 *
 * @author Jiri Sedlacek
 */
final class ApplicationOverviewModel {
    
    private static final String PROP_PREFIX = "ApplicationOverviewModel_";  // NOI18N
    
    static final String SNAPSHOT_VERSION = PROP_PREFIX + "version"; // NOI18N
    private static final String SNAPSHOT_VERSION_DIVIDER = "."; // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MAJOR = "1";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION_MINOR = "0";   // NOI18N
    private static final String CURRENT_SNAPSHOT_VERSION = CURRENT_SNAPSHOT_VERSION_MAJOR + SNAPSHOT_VERSION_DIVIDER + CURRENT_SNAPSHOT_VERSION_MINOR;
    
    private static final String PROP_NOT_DEFINED = "<not defined>"; // NOI18N
    
    public static final String PROP_BASIC_INFO_SUPPORTED = PROP_PREFIX + "basic_info_supported";    // NOI18N
    public static final String PROP_SYSTEM_PROPERTIES_SUPPORTED = PROP_PREFIX + "system_properties_supported";  // NOI18N
    public static final String PROP_PID = PROP_PREFIX + "pid";  // NOI18N
    public static final String PROP_HOST_NAME = PROP_PREFIX + "host_name";  // NOI18N
    public static final String PROP_MAIN_CLASS = PROP_PREFIX + "main_class";    // NOI18N
    public static final String PROP_MAIN_ARGS = PROP_PREFIX + "main_args";  // NOI18N
    public static final String PROP_VM_ID = PROP_PREFIX + "vm_id";  // NOI18N
    public static final String PROP_JAVA_HOME = PROP_PREFIX + "java_home";  // NOI18N
    public static final String PROP_JAVA_VERSION = PROP_PREFIX + "java_version";  // NOI18N
    public static final String PROP_JAVA_VENDOR = PROP_PREFIX + "java_vendor";  // NOI18N
    public static final String PROP_JVM_FLAGS = PROP_PREFIX + "jvm_flags";  // NOI18N
    public static final String PROP_OOME_ENABLED = PROP_PREFIX + "oome_enabled";    // NOI18N
    public static final String PROP_JVM_ARGS = PROP_PREFIX + "jvm_args";    // NOI18N
    public static final String PROP_SYSTEM_PROPERTIES = PROP_PREFIX + "system.properties";  // NOI18N
    
    private boolean initialized;
    private DataSource source;

    private boolean basicInfoSupported;
    private boolean systemPropertiesSupported;
    private String pid;
    private String hostName;
    private String mainClass;
    private String mainArgs;
    private String vmId;
    private String javaHome;
    private String javaVersion;
    private String javaVendor;
    private String jvmFlags;
    private String oomeEnabled;
    private String jvmArgs;
    private String systemProperties;

    
    public static ApplicationOverviewModel create(Application application) {
        ApplicationOverviewModel model = new ApplicationOverviewModel();
        model.initialized = false;
        model.source = application;
        return model;
    }
    
    public static ApplicationOverviewModel create(Snapshot snapshot) {
        ApplicationOverviewModel model = new ApplicationOverviewModel();
        model.initialized = false;
        model.source = snapshot;
        return model;
    }

    
    public DataSource getSource() { return source; }
    public boolean basicInfoSupported() { return basicInfoSupported; }
    public boolean systemPropertiesSupported() { return systemPropertiesSupported; }
    public String getPid() { return pid; }
    public String getHostName() { return hostName; }
    public String getMainClass() { return mainClass; }
    public String getMainArgs() { return mainArgs; }
    public String getVmId() { return vmId; }
    public String getJavaHome() { return javaHome; }
    public String getJavaVersion() { return javaVersion; }
    public String getJavaVendor() { return javaVendor; }
    public String getJvmFlags() { return jvmFlags; }
    public String oomeEnabled() {
        if (basicInfoSupported() && source instanceof Application) {
            Jvm jvm = JvmFactory.getJVMFor((Application)source);
            oomeEnabled = jvm.isDumpOnOOMEnabled() ? NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_enabled") : NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_disabled");  // NOI18N
        }
        return oomeEnabled;
    }
    public String getJvmArgs() { return jvmArgs; }
    public String getSystemProperties() { return systemProperties; }
    
    
    public synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        if (source instanceof Application) initialize((Application)source);
        else initialize((Snapshot)source);
    }
    
    public void save(Snapshot snapshot) {
        
        initialize();
        
        Storage storage = snapshot.getStorage();
        
        setProperty(storage, SNAPSHOT_VERSION, CURRENT_SNAPSHOT_VERSION);
        
        setProperty(storage, PROP_BASIC_INFO_SUPPORTED, Boolean.toString(basicInfoSupported));
        setProperty(storage, PROP_SYSTEM_PROPERTIES_SUPPORTED, Boolean.toString(systemPropertiesSupported));
        setProperty(storage, PROP_PID, pid);
        setProperty(storage, PROP_HOST_NAME, hostName);
        setProperty(storage, PROP_MAIN_CLASS, mainClass);
        setProperty(storage, PROP_MAIN_ARGS, mainArgs);
        setProperty(storage, PROP_VM_ID, vmId);
        setProperty(storage, PROP_JAVA_HOME, javaHome);
        setProperty(storage, PROP_JAVA_VERSION, javaVersion);
        setProperty(storage, PROP_JAVA_VENDOR, javaVendor);
        setProperty(storage, PROP_JVM_FLAGS, jvmFlags);
        setProperty(storage, PROP_OOME_ENABLED, oomeEnabled);
        setProperty(storage, PROP_JVM_ARGS, jvmArgs);
        setProperty(storage, PROP_SYSTEM_PROPERTIES, systemProperties);
        
    }

    private void initialize(Snapshot snapshot) {
        // TODO: if some property cannot be loaded for current snapshot version, FAIL initializing the snapshot!
        Storage storage = snapshot.getStorage();
        
        basicInfoSupported = Boolean.parseBoolean(getProperty(storage, PROP_BASIC_INFO_SUPPORTED));
        systemPropertiesSupported = Boolean.parseBoolean(getProperty(storage, PROP_SYSTEM_PROPERTIES_SUPPORTED));
        pid = getProperty(storage, PROP_PID);
        hostName = getProperty(storage, PROP_HOST_NAME);
        mainClass = getProperty(storage, PROP_MAIN_CLASS);
        mainArgs = getProperty(storage, PROP_MAIN_ARGS);
        vmId = getProperty(storage, PROP_VM_ID);
        javaHome = getProperty(storage, PROP_JAVA_HOME);
        javaVersion = getProperty(storage, PROP_JAVA_VERSION);
        javaVendor = getProperty(storage, PROP_JAVA_VENDOR);
        jvmFlags = getProperty(storage, PROP_JVM_FLAGS);
        oomeEnabled = getProperty(storage, PROP_OOME_ENABLED);
        jvmArgs = getProperty(storage, PROP_JVM_ARGS);
        systemProperties = getProperty(storage, PROP_SYSTEM_PROPERTIES);
        
    }
    
    private static void setProperty(Storage storage, String property, String value) {
        storage.setCustomProperty(property, value == null ? PROP_NOT_DEFINED : value);
    }
    
    private static String getProperty(Storage storage, String property) {
        String value = storage.getCustomProperty(property);
        return PROP_NOT_DEFINED.equals(value) ? null : value;
    }
    
    private void initialize(Application application) {
        Jvm jvm = JvmFactory.getJVMFor(application);
        
        source = application;

        basicInfoSupported = jvm.isBasicInfoSupported();
        systemPropertiesSupported = jvm.isGetSystemPropertiesSupported();

        int pidInt = application.getPid();
        pid = pidInt == Application.UNKNOWN_PID ? NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_unknown") : "" + pidInt; // NOI18N
        
        hostName = application.getHost().getHostName();
        
        if (basicInfoSupported) {
            mainClass = jvm.getMainClass();
            if (mainClass == null || "".equals(mainClass)) mainClass = NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_unknown");  // NOI18N

            mainArgs = jvm.getMainArgs();
            if (mainArgs == null) mainArgs = NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_none");   // NOI18N

            vmId = jvm.getVmName() + " (" + jvm.getVmVersion() + ", " + jvm.getVmInfo() + ")";  // NOI18N

            javaHome = jvm.getJavaHome();
            javaVersion = jvm.getJavaVersion();
            javaVendor = jvm.getVmVendor();

            jvmFlags = jvm.getJvmFlags();
            if (jvmFlags == null || jvmFlags.length() == 0) jvmFlags = NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_none"); // NOI18N

            oomeEnabled = jvm.isDumpOnOOMEnabled() ? NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_enabled") : NbBundle.getMessage(ApplicationOverviewModel.class, "LBL_disabled");  // NOI18N
            String jvmArgss = jvm.getJvmArgs();
            if (jvmArgss != null) jvmArgs = formatJVMArgs(jvmArgss);
        }
        
        if (systemPropertiesSupported) {
            Properties jvmProperties = jvm.getSystemProperties();
            if (jvmProperties != null) systemProperties = formatSystemProperties(jvmProperties);
        }
    }

    
    private static String formatJVMArgs(String jvmargs) {
        String mangledString = " ".concat(jvmargs).replace(" -", "\n"); // NOI18N
        StringTokenizer tok = new StringTokenizer(mangledString, "\n"); // NOI18N
        StringBuffer text = new StringBuffer(100);
        while (tok.hasMoreTokens()) {
            String arg = tok.nextToken().replace(" ", "&nbsp;");    // NOI18N
            int equalsSign = arg.indexOf('=');

            text.append("<b>"); // NOI18N
            text.append("-");   // NOI18N
            if (equalsSign != -1) {
                text.append(arg.substring(0, equalsSign));
                text.append("</b>");    // NOI18N
                text.append(arg.substring(equalsSign));
            } else {
                text.append(arg);
                text.append("</b>");    // NOI18N
            }
            text.append("<br>");    // NOI18N
        }
        return text.toString();
    }

    private static String formatSystemProperties(Properties properties) {
        StringBuilder text = new StringBuilder(200);
        List<String> keys = new ArrayList<>();
        Enumeration en = properties.propertyNames();

        while (en.hasMoreElements()) {
            keys.add((String) en.nextElement());
        }

        Collections.sort(keys);
        for (String key : keys) {
            String val = properties.getProperty(key);

            if ("line.separator".equals(key) && val != null) {  // NOI18N
                val = val.replace("\n", "\\n"); // NOI18N
                val = val.replace("\r", "\\r"); // NOI18N
            }

            text.append("<b>"); // NOI18N
            text.append(key);
            text.append("</b>=");   // NOI18N
            text.append(val);
            text.append("<br>");    // NOI18N
        }
        return expandInvalidXMLChars(text);
    }
    
    private static String expandInvalidXMLChars(CharSequence chars) {
        StringBuilder text = new StringBuilder(chars.length());
        char ch;
        
        for (int i = 0; i < chars.length(); i++) {
            ch = chars.charAt(i);
            text.append(isValidXMLChar(ch) ? ch :
                    "&lt;0x" + Integer.toHexString(0x10000 | ch).substring(1).toUpperCase() + "&gt;"); // NOI18N
        }
        
        return text.toString();
    }
    
    private static boolean isValidXMLChar(char ch) {
        return (ch == 0x9 || ch == 0xA || ch == 0xD ||
              ((ch >= 0x20) && (ch <= 0xD7FF)) ||
              ((ch >= 0xE000) && (ch <= 0xFFFD)) ||
              ((ch >= 0x10000) && (ch <= 0x10FFFF)));
    }
    
    private ApplicationOverviewModel() {}
}
