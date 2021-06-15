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
import org.graalvm.visualvm.lib.common.integration.IntegrationUtils;
import java.util.Map;


/**
 * Storage of all settings that affect the method of attaching.
 *
 * @author Tomas Hurka
 * @author Ian Formanek
 * @author Misha Dmitriev
 */
public final class AttachSettings {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final String PROP_ATTACH_DIRECT = "profiler.attach.direct"; //NOI18N
    public static final String PROP_ATTACH_REMOTE = "profiler.attach.remote"; //NOI18N
    public static final String PROP_ATTACH_DYNAMIC_JDK16 = "profiler.attach.dynamic.jdk16"; //NOI18N
    public static final String PROP_ATTACH_HOST = "profiler.attach.host"; //NOI18N
    public static final String PROP_ATTACH_PORT = "profiler.attach.port"; //NOI18N
    public static final String PROP_ATTACH_DYNAMIC_PID = "profiler.attach.dynamic.pid"; //NOI18N
    public static final String PROP_ATTACH_DYNAMIC_PROCESS_NAME = "profiler.attach.dynamic.processName"; //NOI18N
    public static final String PROP_ATTACH_DYNAMIC_AUTO = "profiler.attach.dynamic.auto"; //NOI18N

    // following items are for settings persistency only, they don't affect attaching at all
    public static final String PROP_ATTACH_TARGET_TYPE = "profiler.attach.target.type"; //NOI18N
    public static final String PROP_ATTACH_SERVER_TYPE = "profiler.attach.server.type"; //NOI18N
    public static final String PROP_ATTACH_HOST_OS = "profiler.attach.host.os"; //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // for remote:
    private String host = ""; // NOI18N
    private String hostOS;
    private String serverType = ""; // NOI18N

    // for persistence only:
    private String targetType = ""; // NOI18N

    // Direct is true means what we also call "attach on startup" - when the target VM is started with all necessary options
    // and waits for us to connect. It can be used both for local and remote profiling. In fact, currently remote profiling
    // can only be done in this way, but later we can implement an equivalent of "remote Ctrl+Break" or something. In that case,
    // the constructor of the AttachSettings for remote profiling will have to be modified.
    private boolean direct = true;

    // for local:
    private boolean dynamic16 = false;
    private boolean remote = false;
    private int pid = -1;
    private String processName;
    private boolean autoSelect;
    private int transientPort = -1;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Create AttachSettings for direct local profiling */
    public AttachSettings() {
        remote = false;
        direct = true;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setDirect(final boolean direct) {
        this.direct = direct;
    }

    public boolean isDirect() {
        return direct;
    }

    public void setDynamic16(final boolean dynamic) {
        this.dynamic16 = dynamic;
    }

    public boolean isDynamic16() {
        return dynamic16;
    }

    public void setHost(final String host) {
        if (host == null) {
            throw new IllegalArgumentException();
        }

        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setHostOS(final String hostOS) {
        this.hostOS = hostOS;
    }

    public String getHostOS() {
        return hostOS == null ? IntegrationUtils.getLocalPlatform(-1) : hostOS;
    }

    public void setPid(final int pid) {
        this.pid = pid;
    }

    public int getPid() {
        return pid;
    }
    
    public void setProcessName(String processName) {
        this.processName = processName;
    }
    
    public String getProcessName() {
        return processName;
    }
    
    public void setAutoSelectProcess(boolean autoSelect) {
        this.autoSelect = autoSelect;
    }
    
    public boolean isAutoSelectProcess() {
        return autoSelect;
    }

    /** This is only intended to be used to handle AttachSettings defined via ant.
     *
     * @param port A port to use instead of globally defined port. The value is transient and will not be persisted.
     */
    public void setPort(final int port) {
        this.transientPort = port;
    }

    public int getPort() {
        if (transientPort != -1) {
            return transientPort;
        } else {
            return Profiler.getDefault().getGlobalProfilingSettings().getPortNo();
        }
    }

    public void setRemote(final boolean remote) {
        this.remote = remote;
    }

    public boolean isRemote() {
        return remote;
    }

    public void setServerType(final String serverType) {
        this.serverType = serverType;
    }

    public String getServerType() {
        return serverType;
    }

    public void setTargetType(final String targetType) {
        this.targetType = targetType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void applySettings(final ProfilerEngineSettings sharedSettings) {
        sharedSettings.setPortNo(getPort());

        if (remote) {
            sharedSettings.setRemoteHost(host);
        } else {
            sharedSettings.setRemoteHost(""); // NOI18N
        }
        if (isDynamic16()) {
            sharedSettings.setInstrumentObjectInit(true);
        } else {
            sharedSettings.setInstrumentObjectInit(false);            
        }
    }

    public void copyInto(AttachSettings as) {
        as.direct = direct;
        as.remote = remote;
        as.dynamic16 = dynamic16;
        as.pid = pid;
        as.processName = processName;
        as.autoSelect = autoSelect;
        as.host = host;
        as.targetType = targetType;
        as.serverType = serverType;
        as.hostOS = hostOS;
    }

    public String debug() {
        return toString();
    }

    public void load(final Map props) {
        direct = Boolean.valueOf(ProfilingSettings.getProperty(props, PROP_ATTACH_DIRECT, "true")).booleanValue(); //NOI18N
        remote = Boolean.valueOf(ProfilingSettings.getProperty(props, PROP_ATTACH_REMOTE, "false")).booleanValue(); //NOI18N
        dynamic16 = Boolean.valueOf(ProfilingSettings.getProperty(props, PROP_ATTACH_DYNAMIC_JDK16, "false")).booleanValue(); //NOI18N
        host = ProfilingSettings.getProperty(props, PROP_ATTACH_HOST, ""); //NOI18N
        targetType = ProfilingSettings.getProperty(props, PROP_ATTACH_TARGET_TYPE, ""); //NOI18N
        serverType = ProfilingSettings.getProperty(props, PROP_ATTACH_SERVER_TYPE, ""); //NOI18N
        hostOS = ProfilingSettings.getProperty(props, PROP_ATTACH_HOST_OS, null); //NOI18N
        pid = Integer.parseInt(ProfilingSettings.getProperty(props, PROP_ATTACH_DYNAMIC_PID, "-1")); //NOI18N
        processName = ProfilingSettings.getProperty(props, PROP_ATTACH_DYNAMIC_PROCESS_NAME, null); //NOI18
        autoSelect = Boolean.valueOf(ProfilingSettings.getProperty(props, PROP_ATTACH_DYNAMIC_AUTO, "false")).booleanValue(); //NOI18N
    }

    public void store(final Map props) {
        props.put(PROP_ATTACH_DIRECT, Boolean.toString(direct));
        props.put(PROP_ATTACH_REMOTE, Boolean.toString(remote));
        props.put(PROP_ATTACH_DYNAMIC_JDK16, Boolean.toString(dynamic16));
        props.put(PROP_ATTACH_HOST, host);
        props.put(PROP_ATTACH_TARGET_TYPE, targetType);
        props.put(PROP_ATTACH_SERVER_TYPE, serverType);
        if (hostOS == null)  props.remove(PROP_ATTACH_HOST_OS);
        else props.put(PROP_ATTACH_HOST_OS, hostOS);
        if (pid == -1) props.remove(PROP_ATTACH_DYNAMIC_PID);
        else props.put(PROP_ATTACH_DYNAMIC_PID, Integer.toString(pid));
        if (processName == null) props.remove(PROP_ATTACH_DYNAMIC_PROCESS_NAME);
        else props.put(PROP_ATTACH_DYNAMIC_PROCESS_NAME, processName);
        if (!autoSelect) props.remove(PROP_ATTACH_DYNAMIC_AUTO);
        else props.put(PROP_ATTACH_DYNAMIC_AUTO, Boolean.TRUE.toString());
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("target type =").append(targetType); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("server type =").append(serverType); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("remote =").append(remote); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("direct =").append(direct); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("dynamic JDK16 =").append(dynamic16); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("pid =").append(pid); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("process name =").append(processName); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("auto select =").append(autoSelect); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("host =").append(host); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("host os =").append(hostOS == null ? IntegrationUtils.getLocalPlatform(-1) : hostOS); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("transient port =").append(transientPort); //NOI18N
        sb.append("\n"); //NOI18N

        return sb.toString();
    }
}
