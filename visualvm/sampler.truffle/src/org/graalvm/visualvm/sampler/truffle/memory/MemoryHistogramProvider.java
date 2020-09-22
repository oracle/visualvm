/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.truffle.memory;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.Stateful;
import org.graalvm.visualvm.sampler.truffle.cpu.ThreadInfoProvider;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.openide.modules.InstalledFileLocator;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Tomas Hurka
 */
public class MemoryHistogramProvider {

    private static final Logger LOGGER = Logger.getLogger(ThreadInfoProvider.class.getName());
    private static String AGENT_PATH = "modules/ext/stagent.jar";   // NOI18N

    final private String status;
    private ObjectName truffleObjectName;
    private MBeanServerConnection conn;

    public MemoryHistogramProvider(Application app) {
        status = initialize(app);
    }

    public String getStatus() {
        return status;
    }

    private String initialize(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) {
            return NbBundle.getMessage(MemoryHistogramProvider.class, "MSG_unavailable"); // NOI18N
        }
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null) {
            return NbBundle.getMessage(MemoryHistogramProvider.class, "MSG_unavailable_init_jmx"); // NOI18N
        }
        if (jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            return NbBundle.getMessage(MemoryHistogramProvider.class, "MSG_unavailable_create_jmx"); // NOI18N
        }
        conn = jmxModel.getMBeanServerConnection();

        try {
            if (!checkandLoadJMX(application)) {
                return NbBundle.getMessage(MemoryHistogramProvider.class, "MSG_unavailable_threads");
            }
            if (!isHeapHistogramEnabled()) {
                return NbBundle.getMessage(MemoryHistogramProvider.class, "MSG_unavailable_heaphisto");
            }
        } catch (SecurityException e) {
            LOGGER.log(Level.INFO, "MemoryHistogramProvider.initialize() throws SecurityException for " + application, e); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads"); // NOI18N
        } catch (Throwable t) {
            LOGGER.log(Level.INFO, "MemoryHistogramProvider.initialize() throws Throwable for " + application, t); // NOI18N
            return NbBundle.getMessage(ThreadInfoProvider.class, "MSG_unavailable_threads"); // NOI18N
        }
        return null;
    }

    Map<String, Object>[] heapHistogram() throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        return (Map[]) conn.invoke(truffleObjectName, "heapHistogram", null, null);
    }

    boolean isHeapHistogramEnabled() throws InstanceNotFoundException, MBeanException, IOException, ReflectionException, AttributeNotFoundException {
        return (boolean) conn.getAttribute(truffleObjectName, "HeapHistogramEnabled");
    }

    boolean checkandLoadJMX(Application app) throws MalformedObjectNameException, IOException, InterruptedException {
        synchronized (app) {
            truffleObjectName = new ObjectName("com.truffle:type=Threading");
            if (conn.isRegistered(truffleObjectName)) {
                return true;
            }
            if (loadAgent(app)) {
                for (int i = 0; i < 10; i++) {
                    if (conn.isRegistered(truffleObjectName)) {
                        return true;
                    }
                    Thread.sleep(300);
                }
            }
            return conn.isRegistered(truffleObjectName);
        }
    }

    boolean loadAgent(Application app) {
        String pid = String.valueOf(app.getPid());
        String agentPath = getAgentPath();

        LOGGER.warning("Agent " + agentPath);    // NOI18N
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            LOGGER.warning(vm.toString());
            vm.loadAgent(agentPath);
            vm.detach();
            LOGGER.warning("Agent loaded");    // NOI18N
            return true;
        } catch (AttachNotSupportedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AgentLoadException ex) {
            Exceptions.printStackTrace(ex);
        } catch (AgentInitializationException ex) {
            LOGGER.log(Level.INFO,"loadAgent()", ex);
        }
        return false;
    }

    private String getAgentPath() {
        InstalledFileLocator loc = InstalledFileLocator.getDefault();
        ModuleInfo info = Modules.getDefault().ownerOf(getClass());
        File jar = loc.locate(AGENT_PATH, info.getCodeNameBase(), false);

        return jar.getAbsolutePath();
    }

}
