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
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
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

    // following items are for settings persistency only, they don't affect attaching at all
    public static final String PROP_ATTACH_TARGET_TYPE = "profiler.attach.target.type"; //NOI18N
    public static final String PROP_ATTACH_SERVER_TYPE = "profiler.attach.server.type"; //NOI18N
    public static final String PROP_ATTACH_HOST_OS = "profiler.attach.host.os"; //NOI18N

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // for remote:
    private String host = ""; // NOI18N
    private String hostOS = ""; // NOI18N
    private String hostOS_dbl = new String(hostOS);
    private String host_dbl = new String(host);
    private String remoteHostOS = "";
    private String serverType = ""; // NOI18N
    private String serverType_dbl = new String(serverType);

    // for persistence only:
    private String targetType = ""; // NOI18N
    private String targetType_dbl = new String(targetType);

    // Direct is true means what we also call "attach on startup" - when the target VM is started with all necessary options
    // and waits for us to connect. It can be used both for local and remote profiling. In fact, currently remote profiling
    // can only be done in this way, but later we can implement an equivalent of "remote Ctrl+Break" or something. In that case,
    // the constructor of the AttachSettings for remote profiling will have to be modified.
    private boolean direct = true;
    private boolean direct_dbl = direct;

    // for local:
    private boolean dynamic16 = false;
    private boolean dynamic16_dbl = dynamic16;
    private boolean remote = false;
    private boolean remote_dbl = remote;
    private int pid = -1;
    private int pid_dbl = pid;
    private int transientPort = -1;
    private int transientPort_dbl = transientPort;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Create AttachSettings for direct local profiling */
    public AttachSettings() {
        remote = false;
        direct = true;

        hostOS = IntegrationUtils.getLocalPlatform(-1);
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
        if (hostOS != null) {
            this.hostOS = hostOS;
        } else {
            if (remote) {
                this.hostOS = remoteHostOS;
            } else {
                this.hostOS = IntegrationUtils.getLocalPlatform(-1);
            }
        }
    }

    public String getHostOS() {
        return hostOS;
    }

    public void setPid(final int pid) {
        this.pid = pid;
    }

    public int getPid() {
        return pid;
    }

    /** This is only intended to be used to handle AttachSettings defined via ant.
     *
     * @param port A port to use insteead of globally defined port. The value is transient and will not be persisted.
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
    }

    public boolean commit() {
        boolean dirty = false;

        if (direct != direct_dbl) {
            dirty = true;
            direct_dbl = direct;
        }

        if (remote != remote_dbl) {
            dirty = true;
            remote_dbl = remote;
        }

        if (dynamic16 != dynamic16_dbl) {
            dirty = true;
            dynamic16_dbl = dynamic16;
        }

        // changing dynamic attach method doesn't mean changing attach parameters
        //    if (pid != pid_dbl) {
        //      dirty = true;
        //      pid_dbl = pid;
        //    }
        if ((host_dbl == null) || !host.equalsIgnoreCase(host_dbl)) {
            dirty = true;
            host_dbl = new String(host);
        }

        if (transientPort_dbl != transientPort) {
            dirty = true;
            transientPort_dbl = transientPort;
        }

        if ((targetType_dbl == null) || !targetType.equalsIgnoreCase(targetType_dbl)) {
            dirty = true;
            targetType_dbl = new String(targetType);
        }

        if ((serverType_dbl == null) || !serverType.equalsIgnoreCase(serverType_dbl)) {
            dirty = true;
            serverType_dbl = new String(serverType);
        }

        if ((hostOS_dbl == null) || !hostOS.equalsIgnoreCase(hostOS_dbl)) {
            dirty = true;
            hostOS_dbl = new String(hostOS);
        }

        return dirty;
    }

    public void copyInto(AttachSettings as) {
        as.direct = direct;
        as.direct_dbl = direct_dbl;
        as.remote = remote;
        as.remote_dbl = remote_dbl;
        as.dynamic16 = dynamic16;
        as.dynamic16_dbl = dynamic16_dbl;
        as.pid = pid;
        as.pid_dbl = pid_dbl;
        as.host = host;
        as.host_dbl = host_dbl;
        as.targetType = targetType;
        as.targetType_dbl = targetType_dbl;
        as.serverType = serverType;
        as.serverType_dbl = serverType_dbl;
        as.hostOS = hostOS;
        as.hostOS_dbl = hostOS_dbl;
        as.remoteHostOS = remoteHostOS;
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
        remoteHostOS = ProfilingSettings.getProperty(props, PROP_ATTACH_HOST_OS, IntegrationUtils.getLocalPlatform(-1)); //NOI18N
    }

    public void store(final Map props) {
        props.put(PROP_ATTACH_DIRECT, Boolean.toString(direct));
        props.put(PROP_ATTACH_REMOTE, Boolean.toString(remote));
        props.put(PROP_ATTACH_DYNAMIC_JDK16, Boolean.toString(dynamic16));
        props.put(PROP_ATTACH_HOST, host);
        props.put(PROP_ATTACH_TARGET_TYPE, targetType);
        props.put(PROP_ATTACH_SERVER_TYPE, serverType);
        props.put(PROP_ATTACH_HOST_OS, hostOS);
    }

    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("target type =" + targetType); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("server type =" + serverType); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("remote =" + remote); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("direct =" + direct); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("dynamic JDK16 =" + dynamic16); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("pid =" + pid); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("host =" + host); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("host os =" + hostOS); //NOI18N
        sb.append("\n"); //NOI18N
        sb.append("transient port =" + transientPort); //NOI18N
        sb.append("\n"); //NOI18N

        return sb.toString();
    }
}
