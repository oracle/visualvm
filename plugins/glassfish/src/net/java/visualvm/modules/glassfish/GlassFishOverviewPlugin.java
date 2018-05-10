/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package net.java.visualvm.modules.glassfish;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.HTTPListenerConfig;
import com.sun.appserv.management.config.HTTPServiceConfig;
import com.sun.appserv.management.config.IIOPListenerConfig;
import com.sun.appserv.management.config.IIOPServiceConfig;
import com.sun.appserv.management.config.ModuleMonitoringLevelsConfig;
import com.sun.appserv.management.config.SystemPropertiesAccess;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.core.ui.DataSourceViewPlugin;
import org.graalvm.visualvm.core.ui.components.DataViewComponent;
import org.graalvm.visualvm.core.ui.components.DataViewComponent.DetailsView;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.tools.jmx.JmxModel;
import org.graalvm.visualvm.tools.jmx.JmxModelFactory;
import org.graalvm.visualvm.uisupport.HTMLTextArea;
import javax.swing.event.HyperlinkEvent;
import net.java.visualvm.modules.glassfish.jmx.AMXUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import javax.swing.BorderFactory;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkListener;
import net.java.visualvm.modules.glassfish.jmx.JMXUtil;

/**
 *
 * @author Jaroslav Bachorik
 */
public class GlassFishOverviewPlugin extends DataSourceViewPlugin {
    private JmxModel model = null;
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    private static class GlassfishOverviewPanel extends HTMLTextArea {
        //~ Instance fields ------------------------------------------------------------------------------------------------------
        private DomainRoot domainRoot;
        private String serverName,  configName;
        private JmxModel jmxModel;

        //~ Constructors ---------------------------------------------------------------------------------------------------------
        public GlassfishOverviewPanel(DomainRoot root, JmxModel jmx) {
            domainRoot = root;
            jmxModel = jmx;
            assert domainRoot != null && jmxModel != null;
            serverName = JMXUtil.getServerName(jmx);
            configName = JMXUtil.getServerConfig(jmx);
            assert serverName != null && configName != null;
            initComponents();
        }

        private void initComponents() {
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder());
            addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        StringTokenizer st = new StringTokenizer(e.getDescription(), "#");
                        String service = st.nextToken();
                        String level = st.nextToken();
                        setMonitoringLevel(AMXUtil.getMonitoringConfig(jmxModel), service, cycleLevel(level));
                        setText(buildInfo());                        
                    }
                }
            });

           new  SwingWorker<Void, Void>() {
                private String areaText = null;
                @Override
                protected Void doInBackground() throws Exception {
                    areaText = buildInfo();
                    return null;
                }

                @Override
                protected void done() {
                    if (areaText != null) setText(areaText);
                
                }
            }.execute();
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
            Map<String, IIOPListenerConfig> iiopListeners = config.getIIOPListenerConfigMap();
            Collection<String> iports = new ArrayList<String>();
            for (String key : iiopListeners.keySet()) {
                String iport = iiopListeners.get(key).getPort();
                if (iport.startsWith("$")) {
                    iport = resolveToken((iport.substring(2, iport.length() - 1)));
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
            sb.append("<h2>General information</h2>");
            sb.append("<b>Server Name: </b>").append(serverName).append("<br>");
            sb.append("<b>Domain: </b>").append(getDomain()).append("<br>");
            sb.append("<b>Config Dir: </b>").append(JMXUtil.getServerConfigDir(jmxModel)).append("<br>");
            sb.append("<br>");
            sb.append("<b>HTTP Port(s): </b>");

            Collection<String> hports = getHTTPPorts(cc.getHTTPServiceConfig());
            for (Iterator<String> iter = hports.iterator(); iter.hasNext();) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("<br>");

            sb.append("<b>IIOP Port(s): </b> ");

            Collection<String> iports = getIIOPPorts(cc.getIIOPServiceConfig());
            for (Iterator<String> iter = iports.iterator(); iter.hasNext();) {
                sb.append(iter.next());
                if (iter.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("<br><br>");

            String version = domainRoot.getJ2EEDomain().getJ2EEServerMap().get(serverName).getserverVersion();
            sb.append("<b>Installed Version: </b>").append(version).append("<br><br>");
            ModuleMonitoringLevelsConfig monitoringConfig = AMXUtil.getMonitoringConfig(jmxModel);
            if (monitoringConfig != null) {
                sb.append("<hr>");
                sb.append("<h2>Monitoring Configuration</h2>");
                sb.append("<table>");
                for(Map.Entry<String, String> entry : monitoringConfig.getAllLevels().entrySet()) {
                    String color;
                    if (entry.getValue().toUpperCase().equals("OFF")) {
                        color = "red";
                    } else if (entry.getValue().toUpperCase().equals("LOW")) {
                        color = "yellow";
                    } else {
                        color = "green";
                    }
                    sb.append("<tr>");
                    sb.append("<td>").append(entry.getKey()).append("</td>");
                    sb.append("<td style=\"color: ").append(color).append("\">");
                    sb.append("<a href=\"").append(entry.getKey()).append("#").append(entry.getValue()).append("\" alt=\"Click to cycle\">");
                    sb.append(entry.getValue()).append("</a></td>");
                    sb.append("</tr>");
                }
                sb.append("</table>");
            }
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
        
        private static String cycleLevel(String level) {
            if (level.toUpperCase().equals("OFF")) {
                return "LOW";
            } else if (level.toUpperCase().equals("LOW")) {
                return "HIGH";
            } else {
                return "OFF";
            }
        }
        
        private static void setMonitoringLevel(ModuleMonitoringLevelsConfig config, String service, String level) {
            if (service.toUpperCase().equals("HTTPSERVICE")) {
                config.setHTTPService(level);
            } else if (service.toUpperCase().equals("CONNECTORSERVICE")) {
                config.setConnectorService(level);
            } else if (service.toUpperCase().equals("JDBCCONNECTIONPOOL")) {
                config.setJDBCConnectionPool(level);
            } else if (service.toUpperCase().equals("THREADPOOL")) {
                config.setThreadPool(level);
            } else if (service.toUpperCase().equals("ORB")) {
                config.setORB(level);
            } else if (service.toUpperCase().equals("CONNECTORCONNECTIONPOOL")) {
                config.setConnectorConnectionPool(level);
            } else if (service.toUpperCase().equals("JVM")) {
                config.setJVM(level);
            } else if (service.toUpperCase().equals("TRANSACTIONSERVICE")) {
                config.setTransactionService(level);
            } else if (service.toUpperCase().equals("WEBCONTAINER")) {
                config.setWebContainer(level);
            } else if (service.toUpperCase().equals("JMSSERVICE")) {
                config.setJMSService(level);
            } else if (service.toUpperCase().equals("EJBCONTAINER")) {
                config.setEJBContainer(level);
            }
        }
    }

    @Override
    public DetailsView createView(int position) {
        if (model == null) return null;
        if (position == DataViewComponent.TOP_RIGHT) {
            DomainRoot root = AMXUtil.getDomainRoot(model);
            if (root != null) {
                return new DataViewComponent.DetailsView("Application Server", null, 0,
                        new ScrollableContainer(new GlassfishOverviewPanel(root, model)), null);
            }
        }
        return null;
    }

    public GlassFishOverviewPlugin(Application app) {
        super(app);
        model = JmxModelFactory.getJmxModelFor(app);
    }
}
