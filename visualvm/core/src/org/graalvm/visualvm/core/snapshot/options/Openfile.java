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
package org.graalvm.visualvm.core.snapshot.options;

import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategoriesListener;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Handling of --openfile commandline option
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=OptionProcessor.class)
public final class Openfile extends OptionProcessor {
    private Option openfile = Option.requiredArgument(Option.NO_SHORT_NAME,"openfile");    // NOI18N
    private static final int TIMEOUT = 5000;
    
    public Openfile() {
        openfile = Option.shortDescription(openfile, "org.graalvm.visualvm.core.snapshot.options.Bundle", "MSG_OPENFILE");
    }
    
    protected Set<Option> getOptions() {
        return Collections.singleton(openfile);
    }

    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        String[] files = optionValues.get(openfile);
        String fileStr = files[0];
        File file = new File(fileStr);
        if (!file.isAbsolute()) {
            file = new File(env.getCurrentDirectory(),fileStr);
        }
        RegisteredSnapshotCategories cats = RegisteredSnapshotCategories.sharedInstance();
        List<SnapshotCategory> snapshotList = cats.getOpenSnapshotCategories();
        if (openSnapshot(file, snapshotList)) {
            return;
        }
        Listener l = new Listener(file);
        cats.addCategoriesListener(l);
    }

    private boolean openSnapshot(final File file, final List<SnapshotCategory> snapshots) {
        if (file.isFile()) {
            for (SnapshotCategory s : snapshots) {
                FileFilter filter = s.getFileFilter();

                if (filter.accept(file)) {
                    s.openSnapshot(file);
                    return true;
                }
            }
        }
        return false;
    }

    private class Listener implements SnapshotCategoriesListener, ActionListener {
        private final File file;
        private volatile boolean removed;
        private final Timer timer;
        
        private Listener(File f) {
            file = f;
            timer = new Timer(TIMEOUT,this);
            timer.start();
        }

        public synchronized void actionPerformed(ActionEvent e) {
            if (!removed) {
                RegisteredSnapshotCategories.sharedInstance().removeCategoriesListener(this);
                removed = true;
                String msg = NbBundle.getMessage(Openfile.class,"MSG_NO_CATEGORY_FILE",new Object[] {file.getAbsolutePath()});    // NOI18N
                NotifyDescriptor desc = new NotifyDescriptor.Message(msg,NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notifyLater(desc);
            }
        }

        public void categoryRegistered(SnapshotCategory category) {
            if (openSnapshot(file,Collections.singletonList(category))) {
                if (!removed) {
                    RegisteredSnapshotCategories.sharedInstance().removeCategoriesListener(this);
                    removed = true;
                    timer.stop();
                }
            }
        }

        public void categoryUnregistered(SnapshotCategory category) {
        }

    }
}
