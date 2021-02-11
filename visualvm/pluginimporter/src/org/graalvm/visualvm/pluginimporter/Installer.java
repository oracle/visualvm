/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.pluginimporter;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.openide.filesystems.FileUtil;
import org.openide.modules.OnStart;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.windows.WindowManager;

@OnStart
public class Installer implements Runnable {

    public static final String KEY_IMPORT_FROM = "import-from";
    public static final String CODE_NAME = "ClusterUpdateProvider";
    public static final String REMOVED = "_removed"; // NOI18N

    private static final Logger LOG = Logger.getLogger(Installer.class.getName());
    private static final String IMPORTED = "imported"; // NOI18N
    private static final String AUTOUPDATE_PREF = "/org/netbeans/modules/autoupdate";

    @Override
    public void run() {
        // remove ClusterUpdateProvider from available update providers
        Preferences au_pref = NbPreferences.root().node(AUTOUPDATE_PREF); // NOI18N
        au_pref.node(Installer.CODE_NAME + Installer.REMOVED).putBoolean(Installer.REMOVED, true);

        // install plugin importer when UI is ready (main window shown)
        WindowManager.getDefault().invokeWhenUIReady(() ->
                RequestProcessor.getDefault().post(doCheck, getImportDelay()) // XXX: Need to wait until UC downloaded&parsed
            );
    }

    private Runnable doCheck = new Runnable() {
        @Override
        public void run() {
            // check user wants to import previous userdir
            File importFrom = null;
            String from = System.getProperty("plugin.manager.import.from", ""); // NOI18N
            Preferences pref = NbPreferences.forModule (Installer.class);
            Preferences au_pref = NbPreferences.root().node(AUTOUPDATE_PREF); // NOI18N
            if (!from.isEmpty()) {
                importFrom = new File(from);
                // check if the userdir was imported already
                boolean imported = au_pref.getBoolean(IMPORTED, false);
                if (!imported) {
                    // don't import
                    importFrom = null;
                }
            } else if (pref.get (KEY_IMPORT_FROM, null) != null) {
                // was remind later
                importFrom = new File (pref.get (KEY_IMPORT_FROM, "")); // NOI18N
            }
            // don't import again from previous userdir
            au_pref.putBoolean(IMPORTED, false);

            if (importFrom == null || !importFrom.exists()) {
                // nothing to do => return
                LOG.fine("Nothing to import from " + importFrom); // NOI18N
                return;
            }
            try {
                // XXX: Hack Autoupdate API
                // find own provider
                Preferences p = au_pref.node(CODE_NAME + REMOVED);
                p.removeNode();
            } catch (BackingStoreException ex) {
                LOG.log(Level.INFO, ex.getLocalizedMessage(), ex);
                return;
            }
            UpdateUnitProvider clusterUpdateProvider = null;
            for (UpdateUnitProvider p : UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(false)) {
                if (CODE_NAME.contains(p.getName())) {
                    clusterUpdateProvider = p;
                }
            }
            assert clusterUpdateProvider != null : "clusterUpdateProvider must found";
            if (clusterUpdateProvider != null) {
                try {
                    assert importFrom != null && importFrom.exists() : importFrom + " exists.";
                    ClusterUpdateProvider.attachCluster(importFrom);
                    Collection<UpdateUnit> units = clusterUpdateProvider.getUpdateUnits(UpdateManager.TYPE.MODULE);
                    UpdateUnitProviderFactory.getDefault().remove(clusterUpdateProvider);
                    PluginImporter importer = new PluginImporter(units);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Already installed plugins: " + importer.getInstalledPlugins());
                        LOG.fine("Plugins available on UC: " + importer.getPluginsAvailableToInstall());
                        LOG.fine("Plugins available for import: " + importer.getPluginsToImport());
                    }
                    if (!importer.getBrokenPlugins().isEmpty()) {
                        LOG.info("Plugins for import with broken dependencies: " + importer.getBrokenPlugins());
                    }
                    if (!importer.getPluginsToImport().isEmpty() || !importer.getPluginsAvailableToInstall().isEmpty()) {
                        LOG.info((importer.getPluginsToImport().size() + importer.getPluginsAvailableToInstall().size())
                                + " available plugins for import in " + importFrom); // NOI18N
                        ImportManager notifier = new ImportManager(importFrom, getUserDir(), importer);
                        notifier.notifyAvailable();
                    } else {
                        LOG.fine((importer.getPluginsToImport().size() + importer.getPluginsAvailableToInstall().size())
                                + " available plugins for import in " + importFrom); // NOI18N
                    }
                } catch (Exception x) {
                    LOG.log(Level.INFO, x.getLocalizedMessage() + " while importing plugins from " + importFrom, x);
                } finally {
                    UpdateUnitProviderFactory.getDefault().remove(clusterUpdateProvider);
                }
            }
        }
    };

    private static File getUserDir() {
        String user = System.getProperty("netbeans.user"); // NOI18N
        File userDir = null;
        if (user != null) {
            userDir = FileUtil.normalizeFile(new File(user));
        }
        return userDir;
    }

    private int getImportDelay() {
        int delay = 30000; // the defalut value
        String delay_prop = System.getProperty("plugin.manager.import.delay");
        try {
            delay = Integer.parseInt(delay_prop);
        } catch (NumberFormatException x) {
            // ignore, use the default value
        }
        return delay;
    }
}
