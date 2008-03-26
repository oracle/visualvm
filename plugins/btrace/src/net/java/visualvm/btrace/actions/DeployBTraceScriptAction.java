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
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.ui.actions.SingleDataSourceAction;
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
import javax.swing.Action;
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
public class DeployBTraceScriptAction extends SingleDataSourceAction<Application> {

    public DeployBTraceScriptAction() {
        super(Application.class);
        putValue(Action.NAME, "Deploy BTrace Script...");
        putValue(Action.SHORT_DESCRIPTION, "Opens wizard for deployment of a BTrace script");
    }

    @Override
    protected void actionPerformed(Application application, ActionEvent event) {
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
                ScriptDataSourceProvider.sharedInstance().deploy((ProbeConfig) wizardDescriptor.getProperty("probe"), application);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    protected boolean isEnabled(Application application) {
        if (!application.isLocalApplication()) {
            return false;
        }
        return JvmFactory.getJVMFor(application).isAttachable() && probesRegistered() && ScriptDataSourceProvider.sharedInstance().isReady();
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
}
