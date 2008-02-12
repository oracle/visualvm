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
package net.java.visualvm.modules.glassfish.datasource;

import com.sun.appserv.management.DomainRoot;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.datasource.DataSourceRepository;
import com.sun.tools.visualvm.core.datasource.DefaultDataSourceProvider;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.datasupport.DataFinishedListener;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.jmx.JMXModel;
import com.sun.tools.visualvm.core.model.jmx.JMXModelFactory;
import com.sun.tools.visualvm.core.model.jvm.JVM;
import com.sun.tools.visualvm.core.model.jvm.JVMFactory;
import net.java.visualvm.modules.glassfish.GlassFishApplicationType;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.management.MBeanServerConnection;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishRootProvider extends DefaultDataSourceProvider<GlassFishRoot> implements DataChangeListener<Application> {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Pattern SERVER_NAME_PATTERN = Pattern.compile(".*?-Dcom\\.sun\\.aas\\.instanceName=(.*?)\\s");
    private static final String SERVER_NAME_PROPERTY_KEY = "com.sun.aas.instanceName";

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private ExecutorService modelBuilder = Executors.newCachedThreadPool();

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void dataChanged(DataChangeEvent<Application> event) {
        if (event.getAdded().isEmpty() && event.getRemoved().isEmpty()) {
            // Initial event to deliver DataSources already created by the provider before registering to it as a listener
            // NOTE: already existing hosts are treated as new for this provider
            Set<Application> newApplications = event.getCurrent();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        } else {
            // Real delta event
            Set<Application> newApplications = event.getAdded();

            for (Application app : newApplications) {
                processNewApplication(app);
            }
        }
    }
    
    public void initialize() {
        DataSourceRepository.sharedInstance().addDataSourceProvider(this);
        DataSourceRepository.sharedInstance().addDataChangeListener(this, Application.class);
    }

    private static String getServerName(JVM jvm) {
        Matcher matcher = SERVER_NAME_PATTERN.matcher(jvm.getJvmArgs());

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // no server name in jvm args; try system properties
        Properties props = jvm.getSystemProperties();

        if (props != null) {
            return props.getProperty(SERVER_NAME_PROPERTY_KEY);
        }

        return "server";

        //        return null;
    }

    private void processFinishedApplication(Application app) {
        // TODO: remove listener!!!
        Set<GlassFishRoot> roots = app.getRepository().getDataSources(GlassFishRoot.class);
        app.getRepository().removeDataSources(roots);
        unregisterDataSources(roots);
    }

    private void processNewApplication(final Application app) {
        if (ApplicationTypeFactory.getApplicationTypeFor(app) instanceof GlassFishApplicationType) {
            modelBuilder.submit(new Runnable() {
                    public void run() {
                        try {
                            JVM jvm = JVMFactory.getJVMFor(app);
                            String serverName = getServerName(jvm);

                            if (serverName == null) {
                                return; // no server name; fail early
                            }

                            JMXModel jmx = JMXModelFactory.getJmxModelFor(app);
                            MBeanServerConnection serverConnection = jmx.getMBeanServerConnection();
                            DomainRoot dr = AMXUtil.getDomainRoot(serverConnection);

                            while (dr == null) {
                                Thread.sleep(500);
                                dr = AMXUtil.getDomainRoot(serverConnection);
                            }

                            dr.waitAMXReady();

                            GlassFishRoot gfr = new GlassFishRoot(serverName, dr, app);
                            registerDataSource(gfr);
                            app.getRepository().addDataSource(gfr);

                            app.notifyWhenFinished(new DataFinishedListener() {
                                    public void dataFinished(Object dataSource) {
                                        processFinishedApplication(app);
                                    }
                                });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
        }
    }
}
