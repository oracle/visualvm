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

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.descriptor.DataSourceDescriptorFactory;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.java.visualvm.btrace.config.ProbeConfig;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ScriptDataSourceProvider {

    private static final ScriptDataSourceProvider INSTANCE = new ScriptDataSourceProvider();
    private static final ScriptDataSourceDescriptorProvider DESC_FACTORY = new ScriptDataSourceDescriptorProvider();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");
    

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getDefault());
    }
    private final ExecutorService deployer = Executors.newCachedThreadPool();

    volatile private boolean providerReady = true;
    
    private ScriptDataSourceProvider() {
    }

    public static void initialize() {
        DataSourceDescriptorFactory.getDefault().registerFactory(DESC_FACTORY);
    }

    public static void shutdown() {
        DataSourceDescriptorFactory.getDefault().unregisterFactory(DESC_FACTORY);
    }

    public static ScriptDataSourceProvider sharedInstance() {
        return INSTANCE;
    }

    public void dataChanged(DataChangeEvent<Application> event) {
        Set<Application> removed = event.getRemoved();
        if (removed != null && !removed.isEmpty()) {
            for (Application app : removed) {
                for (ScriptDataSource pds : (Set<ScriptDataSource>) app.getRepository().getDataSources(ScriptDataSource.class)) {
                    // undeploy probe
                }
            }
        }
    }

    public void startProbe(final ProbeConfig config, final Application app) throws IOException {
        byte[] code = loadProbeCode(config);
        if (code != null) {
            deployer.submit(new DeployTask(BTraceClientCache.sharedInstance().getClient(app), code) {

                protected ScriptDataSource prepareProbe(DeployTask deployer) {
                    providerReady = false;
                    ScriptDataSource pds = new ScriptDataSource(ScriptDataSourceProvider.this, config, app, deployer);
                    app.getRepository().addDataSource(pds);
                    openProbeWindow(pds);
                    return pds;
                }

                @Override
                protected void removeProbe(ScriptDataSource pds) {
                    pds.getApplication().getRepository().removeDataSource(pds);
                    providerReady = true;
                }
            });
        }
    }

    public void stopProbe(ScriptDataSource pds) {
        pds.stop();
    }
    
    public boolean isReady() {
        return providerReady;
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

    private static void openProbeWindow(ScriptDataSource pds) {
        DataSourceWindowManager.sharedInstance().openDataSource(pds);
    }
}
