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

import com.sun.btrace.CommandListener;
import com.sun.btrace.client.Client;
import com.sun.btrace.comm.Command;
import com.sun.btrace.comm.MessageCommand;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.java.visualvm.btrace.config.ProbeConfig;
import org.openide.modules.InstalledFileLocator;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ProbeDataSourceProvider extends DefaultDataSourceProvider<ProbeDataSource> implements DataChangeListener<Application> {

    private static final ProbeDataSourceProvider INSTANCE = new ProbeDataSourceProvider();
    private static final ProbeDataSourceDescriptorProvider DESC_FACTORY = new ProbeDataSourceDescriptorProvider();
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
    }
    
    private final ExecutorService deployer = Executors.newCachedThreadPool();

    private final Map<Application, Client> clientMap = new HashMap<Application, Client>();
    
    private ProbeDataSourceProvider() {
    }

    public static void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(INSTANCE);

        DataSourceDescriptorFactory.getDefault().registerFactory(DESC_FACTORY);
    }
    
    public static void shutdown() {
        DataSourceRepository.sharedInstance().removeDataSourceProvider(INSTANCE);
        DataSourceDescriptorFactory.getDefault().unregisterFactory(DESC_FACTORY);
    }

    public static ProbeDataSourceProvider sharedInstance() {
        return INSTANCE;
    }

    public void dataChanged(DataChangeEvent<Application> event) {
        Set<Application> removed = event.getRemoved();
        if (removed != null && !removed.isEmpty()) {
            for (Application app : removed) {
                for (ProbeDataSource pds : (Set<ProbeDataSource>) app.getRepository().getDataSources(ProbeDataSource.class)) {
                // undeploy probe
                }
            }
        }
    }

    public ProbeDataSource deploy(final ProbeConfig probe, final Application app) {
        try {
            InputStream is = new URLClassLoader(new URL[]{probe.getBaseURL()}).getResourceAsStream(probe.getClazz());
            byte[] mainBuffer = new byte[0];
            byte[] tmpBuffer = new byte[512];
            int read = 0;
            do {
                read = is.read(tmpBuffer);
                if (read > 0) {
                    int newLen = mainBuffer.length + read;
                    byte[] newBuf = new byte[newLen];
                    System.arraycopy(mainBuffer, 0, newBuf, 0, mainBuffer.length);
                    System.arraycopy(tmpBuffer, 0, newBuf, mainBuffer.length, read);
                    mainBuffer = newBuf;
                }
            } while (read > 0);

            final byte[] bytecodes = mainBuffer;

            deployer.submit(new Runnable() {

                public void run() {
                    try {
                        final PipedOutputStream pos = new PipedOutputStream();
                        final PipedInputStream pis = new PipedInputStream(pos);
                        final PrintWriter probeWriter = new PrintWriter(pos);
                        
                        final Client client = getClient(app);
                        
                        client.submit(bytecodes, new String[0], new CommandListener() {
                            volatile private ProbeDataSource pds = null;
                            public void onCommand(Command cmd) throws IOException {
                                switch (cmd.getType()) {
                                    case Command.MESSAGE: {
                                        if (pds == null) break;
                                        probeWriter.println(DATE_FORMAT.format(new Date(((MessageCommand) cmd).getTime())) + " : " + ((MessageCommand) cmd).getMessage());
                                        probeWriter.flush();
                                        break;
                                    }
                                    case Command.EXIT: {
                                        probeWriter.println("Probe finished ...");
                                        probeWriter.flush();
                                        probeWriter.close();
                                        pos.close();
                                        client.close();
                                        
                                        if (pds != null) {
                                            unregisterDataSource(pds);
                                            app.getRepository().removeDataSource(pds);
                                            pds.shutdown();
                                        }
                                        break;
                                    }
                                    case Command.SUCCESS: {
                                        probeWriter.println("Probe initialized ...");
                                        probeWriter.flush();
                                        pds = new ProbeDataSource(probe, pis, app, client);

                                        registerDataSource(pds);
                                        app.getRepository().addDataSource(pds);
                                        openProbeWindow(pds);
                                    }
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            });

            return null;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void undeploy(ProbeDataSource probe) {
        probe.close();
        unregisterDataSource(probe);
        probe.getApplication().getRepository().removeDataSource(probe);
    }
    
    private static String agentPath = "";
    
    static {
        File agentFile = InstalledFileLocator.getDefault().locate("modules/ext/btrace-agent.jar", "com.sun.btrace", false);
        agentPath = agentFile.getAbsolutePath();
    }
    
    private Client getClient(Application app) throws IOException, InterruptedException {
        if (clientMap.containsKey(app)) {
            return clientMap.get(app);
        }
        
        Client client = new Client(24321, ".", Boolean.getBoolean("jtrace.debug"), false, "/tmp");
        client.attach(String.valueOf(app.getPid()), agentPath);
        Thread.sleep(500);
        clientMap.put(app, client);
        return client;
    }
    
    private static void openProbeWindow(ProbeDataSource pds) {
        DataSource viewMaster = pds.getMaster();
        if (viewMaster != null) {
            DataSourceWindowManager.sharedInstance().addViews(viewMaster, pds);
        } else {
            DataSourceWindowManager.sharedInstance().openWindow(pds);
        }
    }
}
