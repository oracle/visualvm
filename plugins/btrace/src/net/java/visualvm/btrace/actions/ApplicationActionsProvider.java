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
package net.java.visualvm.btrace.actions;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.explorer.ExplorerActionDescriptor;
import com.sun.tools.visualvm.core.explorer.ExplorerActionsProvider;
import com.sun.tools.visualvm.core.explorer.ExplorerContextMenuFactory;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import net.java.visualvm.btrace.config.ConfigParser;
import net.java.visualvm.btrace.config.ProbeConfig;
import net.java.visualvm.btrace.datasource.ScriptDataSourceProvider;
import net.java.visualvm.btrace.wizards.ScriptsWizardIterator;
import org.openide.DialogDisplayer;
import org.openide.WizardDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ApplicationActionsProvider implements ExplorerActionsProvider<Application> {

    private final static ApplicationActionsProvider INSTANCE = new ApplicationActionsProvider();

    private class DeployAction extends AbstractAction {

        private Application app;

        public DeployAction(Application app) {
            super("Deploy BTrace Script");
            this.app = app;
        }

        public void actionPerformed(ActionEvent e) {
            WizardDescriptor.Iterator iterator = new ScriptsWizardIterator(loadProbes());
            WizardDescriptor wizardDescriptor = new WizardDescriptor(iterator);
            // {0} will be replaced by WizardDescriptor.Panel.getComponent().getName()
            // {1} will be replaced by WizardDescriptor.Iterator.name()
            wizardDescriptor.setTitleFormat(new MessageFormat("{0} ({1})"));
            wizardDescriptor.setTitle("Deploy BTrace Script");
            Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
            dialog.setVisible(true);
            dialog.toFront();
            boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
            if (!cancelled) {
                try {
                    ScriptDataSourceProvider.sharedInstance().startProbe((ProbeConfig) wizardDescriptor.getProperty("probe"), app);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        @Override
        public boolean isEnabled() {
            return ScriptDataSourceProvider.sharedInstance().isReady();
        }
    }

    public Set<ExplorerActionDescriptor> getActions(Application app) {
        Jvm jvm = JvmFactory.getJVMFor(app);
        if (app.isLocalApplication() && jvm.isMonitoringSupported() && probesRegistered()) {
            Set<ExplorerActionDescriptor> actions = new HashSet<ExplorerActionDescriptor>();
            actions.add(new ExplorerActionDescriptor(null, 50));
            actions.add(new ExplorerActionDescriptor(new DeployAction(app), 51));
            return actions;
        }
        return Collections.EMPTY_SET;
    }

    public ExplorerActionDescriptor getDefaultAction(Application app) {
        return null;
    }

    public static void initialize() {
        ExplorerContextMenuFactory.sharedInstance().addExplorerActionsProvider(INSTANCE, Application.class);
    }

    public static void shutdown() {
        ExplorerContextMenuFactory.sharedInstance().removeExplorerActionsProvider(INSTANCE);
    }

    private boolean probesRegistered() {
        try {
            ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
            Enumeration<URL> rsrcs = cl.getResources("META-INF/probes.xml");
            return rsrcs.hasMoreElements();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private Collection<ProbeConfig> loadProbes() {
        try {
            Collection<ProbeConfig> masterList = new ArrayList<ProbeConfig>();
            ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
            Enumeration<URL> rsrcs = cl.getResources("META-INF/probes.xml");
            while (rsrcs.hasMoreElements()) {
                masterList.addAll(loadProbes(rsrcs.nextElement()));
            }
            return masterList;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return Collections.EMPTY_LIST;
    }

    private Collection<ProbeConfig> loadProbes(URL probeManifest) throws IOException {
        try {
            XMLReader reader = XMLUtil.createXMLReader();
            ConfigParser parser = new ConfigParser(getBaseURL(probeManifest));
            reader.setContentHandler(parser);
            reader.parse(new InputSource(probeManifest.openStream()));
            return parser.getConfig();
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    private static URL getBaseURL(URL probeManifest) throws MalformedURLException {
        String probeManifestPath = probeManifest.toString();
        int len = probeManifestPath.length();
        int index = probeManifestPath.lastIndexOf("META-INF/");
        String baseURL = probeManifestPath;
        if (index > -1) {
            baseURL = new String(probeManifestPath.substring(0, Math.min(len, index)));
        }
        return new URL(baseURL);
    }

    public ExplorerActionDescriptor getDefaultAction(Set<Application> apps) {
        return null;
    }

    public Set<ExplorerActionDescriptor> getActions(Set<Application> apps) {
        Set<ExplorerActionDescriptor> actionDescriptors = null;
        for(Application app : apps) {
            Set<ExplorerActionDescriptor> currentActions = getActions(app);
            if (actionDescriptors == null) {
                actionDescriptors = new HashSet<ExplorerActionDescriptor>(currentActions);
            } else {
                actionDescriptors.retainAll(currentActions);
            }
        }
        return actionDescriptors;
    }
}
