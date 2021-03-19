/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sampler.truffle;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MalformedObjectNameException;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.datasupport.Stateful;
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
public abstract class TruffleDataProvider {

    protected static final Logger LOGGER = Logger.getLogger(TruffleDataProvider.class.getName());
    private static String AGENT_PATH = "modules/ext/stagent.jar";   // NOI18N

    protected String status;
    protected ProxyTruffleMBean tbean;

    protected static String initJMXConn(Application application) {
        if (application.getState() != Stateful.STATE_AVAILABLE) {
            return NbBundle.getMessage(TruffleDataProvider.class, "MSG_unavailable"); // NOI18N
        }
        JmxModel jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel == null) {
            return NbBundle.getMessage(TruffleDataProvider.class, "MSG_unavailable_init_jmx"); // NOI18N
        }
        if (jmxModel.getConnectionState() != JmxModel.ConnectionState.CONNECTED) {
            return NbBundle.getMessage(TruffleDataProvider.class, "MSG_unavailable_create_jmx"); // NOI18N
        }
        return null;
    }

    public String getStatus() {
        return status;
    }

    protected boolean checkAndLoadJMX(Application app) throws MalformedObjectNameException, IOException, InterruptedException {
        synchronized (app) {
            JmxModel jmxModel = JmxModelFactory.getJmxModelFor(app);
            tbean = new ProxyTruffleMBean(jmxModel.getMBeanServerConnection());
            if (tbean.isRegistered()) {
                return true;
            }
            if (loadAgent(app)) {
                for (int i = 0; i < 10; i++) {
                    if (tbean.isRegistered()) {
                        return true;
                    }
                    Thread.sleep(300);
                }
            }
            return tbean.isRegistered();
        }
    }

    private boolean loadAgent(Application app) {
        String pid = String.valueOf(app.getPid());
        String agentPath = getAgentPath();

        LOGGER.warning("Agent " + agentPath);    // NOI18N
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            LOGGER.warning(vm.toString());
            loadAgentIntoTargetJVM(vm, agentPath, null);
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
            LOGGER.log(Level.INFO, "loadAgent()", ex);
        }
        return false;
    }

    private static void loadAgentIntoTargetJVM(final VirtualMachine virtualMachine, final String jar, final String options)
            throws IOException, AgentLoadException, AgentInitializationException {
        try {
            virtualMachine.loadAgent(jar, options);
        } catch (AgentLoadException ex) {
            if ("0".equals(ex.getMessage())) {
                // JDK 10 -> JDK 9 mismatch
                return;
            }
            throw ex;
        } catch (IOException ex) {
            if ("readInt".equals(ex.getStackTrace()[0].getMethodName())) {
                // JDK 9 -> JDK 10 mismatch
                return;
            }
            throw ex;
        }
    }

    private String getAgentPath() {
        InstalledFileLocator loc = InstalledFileLocator.getDefault();
        ModuleInfo info = Modules.getDefault().ownerOf(getClass());
        File jar = loc.locate(AGENT_PATH, info.getCodeNameBase(), false);

        return jar.getAbsolutePath();
    }
}
