/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.application.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.Timer;
import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.datasource.DataSourceContainer;
import org.graalvm.visualvm.core.datasupport.DataChangeEvent;
import org.graalvm.visualvm.core.datasupport.DataChangeListener;
import org.graalvm.visualvm.core.ui.DataSourceWindowManager;
import org.graalvm.visualvm.host.Host;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Handling of --openpid and --openid commandline option
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=OptionProcessor.class)
public final class Open extends OptionProcessor { // TODO: rewrite to use org.graalvm.visualvm.application.ApplicationFinder
    private Option openpid = Option.requiredArgument(Option.NO_SHORT_NAME,"openpid");    // NOI18N
    private Option openid = Option.requiredArgument(Option.NO_SHORT_NAME,"openid");    // NOI18N
    private static final int TIMEOUT = Integer.getInteger("visualvm.search.process.timeout", 5000); // NOI18N
    private static final String ID = "visualvm.id"; // NOI18N

    public Open() {
        openpid = Option.shortDescription(openpid,"org.graalvm.visualvm.application.options.Bundle","MSG_OPENPID"); // NOI18N
        openid = Option.shortDescription(openid,"org.graalvm.visualvm.application.options.Bundle","MSG_OPENID"); // NOI18N
    }

    protected Set<Option> getOptions() {
        Set<Option> options = new HashSet<>();
        options.add(openpid);
        options.add(openid);
        return options;
    }

    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        Integer pid = null;
        String id = null;
        String[] pids = optionValues.get(openpid);
        String[] ids = optionValues.get(openid);
        
        Integer viewIndex = null;
        
        if (pids != null && pids.length>0) {
            String pidStr = pids[0];
            int idx = pidStr.indexOf('@'); // NOI18N
            if (idx > -1) {
                try {
                    viewIndex = Integer.valueOf(pidStr.substring(idx + 1));
                } catch (NumberFormatException e) {
                    throw new CommandException(0, NbBundle.getMessage(Open.class,"MSG_VIEWIDX_FAILED",new Object[] {e.toString()})); // NOI18N
                }
                pidStr = pidStr.substring(0, idx);
            }
            try {
                pid = Integer.valueOf(pidStr);
            } catch (NumberFormatException e) {
                throw new CommandException(0, NbBundle.getMessage(Open.class,"MSG_PID_FAILED",new Object[] {e.toString()})); // NOI18N
            }
        }
        if (ids != null && ids.length>0) {
            String idStr = ids[0];
            int idx = idStr.indexOf('@'); // NOI18N
            if (idx > -1) {
                try {
                    viewIndex = Integer.valueOf(idStr.substring(idx + 1));
                } catch (NumberFormatException e) {
                    throw new CommandException(0, NbBundle.getMessage(Open.class,"MSG_VIEWIDX_FAILED",new Object[] {e.toString()})); // NOI18N
                }
                idStr = idStr.substring(0, idx);
            }
            id = "-D"+ID+"="+idStr; // NOI18N
        }
        
        DataSourceContainer container = Host.LOCALHOST.getRepository();
        Set<Application> apps = container.getDataSources(Application.class);
        if (openApplication(id, pid, viewIndex, apps)) {
            return;
        }
        Listener l = new Listener(id, pid, viewIndex, container);
        container.addDataChangeListener(l,Application.class);
    }

    private boolean openApplication(final String id, final Integer pid, final Integer viewIndex, final Set<Application> apps) {
        for (Application app : apps) {
            if (pid != null && app.getPid() == pid.intValue()) {
                int index = viewIndex != null ? viewIndex.intValue() - 1 : 0;
                DataSourceWindowManager.sharedInstance().openDataSource(app, true, index);
                return true;
            }
            if (id != null) {
                Jvm jvm = JvmFactory.getJVMFor(app);
                if (jvm.isBasicInfoSupported()) {
                    String args = jvm.getJvmArgs();
                    if (args != null && args.contains(id)) {
                        int index = viewIndex != null ? viewIndex.intValue() - 1 : 0;
                        DataSourceWindowManager.sharedInstance().openDataSource(app, true, index);
                        return true;                        
                    }
                }
            }
        }
        return false;
    }

    private class Listener implements DataChangeListener<Application>, ActionListener {
        private final Integer pid;
        private final String id;
        private final Integer viewIndex;
        private final DataSourceContainer container;
        private volatile boolean removed;
        private final Timer timer;

        private Listener(String i,Integer p,Integer x,DataSourceContainer c) {
            id = i;
            pid = p;
            viewIndex = x;
            container = c;
            timer = new Timer(TIMEOUT,this);
            timer.start();
        }

        public synchronized void dataChanged(DataChangeEvent<Application> event) {
            Set<Application> added = event.getAdded();
            if (openApplication(id,pid,viewIndex,added)) {
                if (!removed) {
                    container.removeDataChangeListener(this);
                    removed = true;
                    timer.stop();
                }
            }
        }

        public synchronized void actionPerformed(ActionEvent e) {
            if (!removed) {
                container.removeDataChangeListener(this);
                removed = true;
                String msg = ""; // NOI18N
                if (pid != null) {
                    msg = NbBundle.getMessage(Open.class,"MSG_NO_APP_PID",new Object[] {Integer.toString(pid)});    // NOI18N
                }
                if (id != null) {
                    msg = NbBundle.getMessage(Open.class,"MSG_NO_APP_ID");    // NOI18N
                }
                
                NotifyDescriptor desc = new NotifyDescriptor.Message(msg,NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notifyLater(desc);
            }
        }
    }
}
