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
package net.java.visualvm.modules.glassfish;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.HTTPListenerConfig;
import com.sun.appserv.management.config.HTTPServiceConfig;
import com.sun.appserv.management.config.IIOPListenerConfig;
import com.sun.appserv.management.config.IIOPServiceConfig;
import com.sun.appserv.management.config.SystemPropertiesAccess;
import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.dataview.overview.OverviewViewSupport;
import com.sun.tools.visualvm.core.model.apptype.ApplicationTypeFactory;
import com.sun.tools.visualvm.core.model.jmx.JMXModel;
import com.sun.tools.visualvm.core.model.jmx.JMXModelFactory;
import com.sun.tools.visualvm.core.ui.ViewPlugin;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent;
import com.sun.tools.visualvm.core.ui.components.DataViewComponent.DetailsView;
import net.java.visualvm.modules.glassfish.datasource.GlassFishRoot;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.swing.JPanel;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;


/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishOverview implements ViewPlugin<Application> {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class GlassfishViewDescriptor implements ViewDescriptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private DomainRoot domainRoot;
        private String serverName, configName;
        private JMXModel jmxModel;
        
        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public GlassfishViewDescriptor(DomainRoot root, JMXModel jmx) {
            domainRoot = root;
            jmxModel = jmx;
            assert domainRoot != null && jmxModel != null;
            serverName = JMXUtil.getServerName(jmx);
            configName = JMXUtil.getServerConfig(jmx);
            assert serverName != null && configName != null;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public int getLocation() {
            return DataViewComponent.TOP_RIGHT;
        }

        public int getPreferredPosition() {
            return 0;
        }

        public DetailsView getView() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setOpaque(false);

            HTMLTextArea area = new HTMLTextArea(buildInfo());
            area.setOpaque(false);
            panel.add(area, BorderLayout.CENTER);

            DataViewComponent.DetailsView details = new DataViewComponent.DetailsView("SJSAS/GlassFish",
                                                                                      "Application server details", panel, null);

            return details;
        }

        private Collection<String> getHTTPPorts(HTTPServiceConfig config) {
            Map<String, HTTPListenerConfig> listeners = config.getHTTPListenerConfigMap();
            Collection<String> ports = new ArrayList<String>();

            for (String key : listeners.keySet()) {
                String port = listeners.get(key).getPort();

                if (port.startsWith("$")) {
                    port = resolveToken((port.substring(2, port.length() - 1)));
                }

                ports.add(port);
            }
            return ports;
        }
        
        private Collection<String> getIIOPPorts(IIOPServiceConfig config) {
            //iiop ports
                Map<String,IIOPListenerConfig> iiopListeners = config.getIIOPListenerConfigMap();
            Collection<String> iports = new ArrayList<String>();
            for(String key : iiopListeners.keySet()){
                String iport = iiopListeners.get(key).getPort();
                if (iport.startsWith("$")){
                    iport = resolveToken( (iport.substring(2, iport.length()-1) ));
                }
                iports.add(iport);
            }
            return iports;
        }
        
        private String getDomain() {
            String domain;
            domain = JMXUtil.getServerDomain(jmxModel);
            return domain != null ? domain : "<UNRESOLVED";
        }

        private String buildInfo() {
            ConfigConfig cc = domainRoot.getDomainConfig().getConfigConfigMap().get(JMXUtil.getServerConfig(jmxModel));

            StringBuilder sb = new StringBuilder();

            sb.append("<b>Server Name: </b>").append(serverName).append("<br/>");
            sb.append("<b>Domain: </b>").append(getDomain()).append("<br/>");
            sb.append("<b>Config Dir: </b>").append(JMXUtil.getServerConfigDir(jmxModel)).append("<br/>");
            sb.append("<br/><");
            sb.append("<b>HTTP Port(s): </b>");

            Collection<String> hports = getHTTPPorts(cc.getHTTPServiceConfig());
            for(Iterator<String> iter=hports.iterator();iter.hasNext();) {
                sb.append(iter.next());
                if (iter.hasNext()) sb.append(",");
            }
            sb.append("<br/>");
            
            sb.append("<b>IOOP Port(s): </b> ");

            Collection<String> iports = getIIOPPorts(cc.getIIOPServiceConfig());
            for(Iterator<String> iter=iports.iterator();iter.hasNext();) {
                sb.append(iter.next());
                if (iter.hasNext()) sb.append(",");
            }
            sb.append("<br/>");
            sb.append("<br/>");
            
            String version = domainRoot.getJ2EEDomain().getJ2EEServerMap().get(serverName).getserverVersion();
            sb.append("<b>Installed Version: </b>").append(version).append("<br/>");
            return sb.toString();
        }

        private String resolveToken(String pn) {
            //For EE, the instance will have its own override System Properties value instead of using the one from config.
            if (AMXUtil.isEE(domainRoot)) {
                SystemPropertiesAccess sprops = domainRoot.getDomainConfig().getStandaloneServerConfigMap().get(serverName);

                if (sprops == null) {
                    sprops = domainRoot.getDomainConfig().getClusteredServerConfigMap().get(serverName);
                }

                if (sprops != null) {
                    if (sprops.existsSystemProperty(pn)) {
                        return sprops.getSystemPropertyValue(pn);
                    }
                }
            }

            ConfigConfig config = domainRoot.getDomainConfig().getConfigConfigMap().get(configName);

            return config.getSystemPropertyValue(pn);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Set<AreaDescriptor> getAreasFor(Application application) {
        return Collections.EMPTY_SET;
    }

    public Set<? extends ViewDescriptor> getViewsFor(Application application) {
        if (ApplicationTypeFactory.getApplicationTypeFor(application) instanceof GlassFishApplicationType) {
            Set<GlassFishRoot> roots = application.getRepository().getDataSources(GlassFishRoot.class);

            if (roots.size() == 1) {
                GlassFishRoot root = roots.iterator().next();
                JMXModel jmx = JMXModelFactory.getJmxModelFor(application);
                return Collections.singleton(new GlassfishViewDescriptor(root.getDomainRoot(), jmx));
            } else {
                return Collections.EMPTY_SET;
            }
        } else {
            return Collections.EMPTY_SET;
        }
    }

    public void initialize() {
        OverviewViewSupport.getInstance().getApplicationPluggableView().addPlugin(this, Application.class);
    }
}
