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
import com.sun.tools.visualvm.core.datasource.DataSource;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.model.dsdescr.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.java.visualvm.btrace.config.ProbeConfig;

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

    public void startProbe(final ProbeConfig config, final Application app) throws IOException {
        byte[] code = loadProbeCode(config);
        if (code != null) {
            deployer.submit(new DeployTask(BTraceClientCache.sharedInstance().getClient(app), code) {

                protected ProbeDataSource prepareProbe(DeployTask deployer) {
                    ProbeDataSource pds = new ProbeDataSource(ProbeDataSourceProvider.this, config, app, deployer);
                    registerDataSource(pds);
                    app.getRepository().addDataSource(pds);
                    openProbeWindow(pds);
                    return pds;
                }

                @Override
                protected void removeProbe(ProbeDataSource pds) {
                    pds.getApplication().getRepository().removeDataSource(pds);
                    unregisterDataSource(pds);
                }
            });
        }
    }

    public void stopProbe(ProbeDataSource pds) {
        pds.stop();
    }

    private byte[] loadProbeCode(ProbeConfig config) {
        try {
            InputStream is = new URLClassLoader(new URL[]{config.getBaseURL()}).getResourceAsStream(config.getClazz());
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

            return mainBuffer;
        } catch (IOException e) {
        }
        return null;
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
