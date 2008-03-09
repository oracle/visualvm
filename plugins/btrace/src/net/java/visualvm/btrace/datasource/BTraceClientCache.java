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
package net.java.visualvm.btrace.datasource;

import com.sun.btrace.client.Client;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.openide.modules.InstalledFileLocator;

/**
 *
 * @author Jaroslav Bachorik
 */
public class BTraceClientCache implements DataChangeListener<Application> {

    private static final BTraceClientCache INSTANCE = new BTraceClientCache();
    private final String agentPath;
    
    private final Map<Integer, Client> clientMap = new HashMap<Integer, Client>();

    private BTraceClientCache() {
        File locatedFile = InstalledFileLocator.getDefault().locate("modules/ext/btrace-agent.jar", "com.sun.btrace", false);
        agentPath = locatedFile.getAbsolutePath();
        
        DataSourceRepository.sharedInstance().addDataChangeListener(this, Application.class);
    }

    public static BTraceClientCache sharedInstance() {
        return INSTANCE;
    }

    public Client getClient(Application app) throws IOException {
        Client client = null;
        synchronized (clientMap) {
            if (clientMap.containsKey(app.getPid())) {
                return clientMap.get(app.getPid());
            }
//            client = new Client(24321, ".", Boolean.getBoolean("jtrace.debug"), false, "/tmp");
            client = new Client(24321, ".", true, false, "/tmp");
            clientMap.put(app.getPid(), client);
        }
        if (client != null) {
            client.attach(String.valueOf(app.getPid()), agentPath, findToolsJarPath(app), null);
        }
        return client;
    }

    public void dataChanged(DataChangeEvent<Application> event) {
        if (event.getRemoved().isEmpty()) {
            return;
        }
        synchronized (clientMap) {
            for (Application app : event.getRemoved()) {
                clientMap.remove(app.getPid());
            }
        }
    }
    
    private String findToolsJarPath(Application app) {
        String toolsJarPath = null;
        Properties props = JVMFactory.getJVMFor(app).getSystemProperties();
        if (props != null && props.containsKey("java.home")) {
            String java_home = props.getProperty("java.home");
            java_home = java_home.replace(File.separator + "jre", "");
            toolsJarPath = java_home + "/lib/tools.jar";
        }
        return toolsJarPath;
    }
}
