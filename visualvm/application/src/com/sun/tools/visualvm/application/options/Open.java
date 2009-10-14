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
package com.sun.tools.visualvm.application.options;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.application.jvm.Jvm;
import com.sun.tools.visualvm.application.jvm.JvmFactory;
import com.sun.tools.visualvm.core.datasource.DataSourceContainer;
import com.sun.tools.visualvm.core.datasupport.DataChangeEvent;
import com.sun.tools.visualvm.core.datasupport.DataChangeListener;
import com.sun.tools.visualvm.core.ui.DataSourceWindowManager;
import com.sun.tools.visualvm.host.Host;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.Timer;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 * Handling of --openpid and --openid commandline option
 *
 * @author Tomas Hurka
 */
public final class Open extends OptionProcessor {
    private Option openpid = Option.requiredArgument(Option.NO_SHORT_NAME,"openpid");    // NOI18N
    private Option openid = Option.requiredArgument(Option.NO_SHORT_NAME,"openid");    // NOI18N
    private static final int TIMEOUT = 5000;
    private static final String ID = "visualvm.id";

    public Open() {
        openpid = Option.shortDescription(openpid,"com.sun.tools.visualvm.application.options.Bundle","MSG_OPENPID"); // NOI18N
        openid = Option.shortDescription(openid,"com.sun.tools.visualvm.application.options.Bundle","MSG_OPENID"); // NOI18N
    }

    protected Set<Option> getOptions() {
        Set<Option> options = new HashSet();
        options.add(openpid);
        options.add(openid);
        return options;
    }

    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        Integer pid = null;
        String id = null;
        String[] pids = optionValues.get(openpid);
        String[] ids = optionValues.get(openid);
        
        if (pids != null && pids.length>0) {
            pid = Integer.valueOf(pids[0]);
        }
        if (ids != null && ids.length>0) {
            id = "-D"+ID+"="+ids[0];
        }
        DataSourceContainer container = Host.LOCALHOST.getRepository();
        Set<Application> apps = container.getDataSources(Application.class);
        if (openApplication(id, pid, apps)) {
            return;
        }
        Listener l = new Listener(id, pid, container);
        container.addDataChangeListener(l,Application.class);
    }

    private boolean openApplication(final String id, final Integer pid, final Set<Application> apps) {
        for (Application app : apps) {
            if (pid != null && app.getPid() == pid.intValue()) {
                DataSourceWindowManager.sharedInstance().openDataSource(app);
                return true;
            }
            if (id != null) {
                Jvm jvm = JvmFactory.getJVMFor(app);
                if (jvm.isBasicInfoSupported()) {
                    String args = jvm.getJvmArgs();
                    if (args != null && args.contains(id)) {
                        DataSourceWindowManager.sharedInstance().openDataSource(app);
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
        private final DataSourceContainer container;
        private volatile boolean removed;
        private final Timer timer;

        private Listener(String i,Integer p,DataSourceContainer c) {
            id = i;
            pid = p;
            container = c;
            timer = new Timer(TIMEOUT,this);
            timer.start();
        }

        public synchronized void dataChanged(DataChangeEvent<Application> event) {
            Set<Application> added = event.getAdded();
            if (openApplication(id,pid,added)) {
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
                String msg = "";
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
