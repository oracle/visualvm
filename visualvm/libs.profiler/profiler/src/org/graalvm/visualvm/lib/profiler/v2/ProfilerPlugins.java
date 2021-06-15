/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.common.Profiler;
import org.graalvm.visualvm.lib.common.event.ProfilingStateAdapter;
import org.graalvm.visualvm.lib.common.event.ProfilingStateEvent;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "ProfilerPlugins_PluginNotInitialized=<html><b>Profiler plugin failed to initialize:</b><br><br>{0}</html>",
    "ProfilerPlugins_PluginFailed=<html><b>Plugin {0} failed:</b><br><br>{1}</html>"
})
final class ProfilerPlugins {

    private final List<ProfilerPlugin> plugins;


    ProfilerPlugins(ProfilerSession session) {
        Collection<? extends ProfilerPlugin.Provider> providers =
                Lookup.getDefault().lookupAll(ProfilerPlugin.Provider.class);

        if (providers.isEmpty()) {
            plugins = null;
        } else {
            List<ProfilerPlugin> _plugins = new ArrayList();
            Lookup.Provider project = session.getProject();
            SessionStorage storage = session.getStorage();
            for (ProfilerPlugin.Provider provider : providers) {
                ProfilerPlugin plugin = null;
                try { plugin = provider.createPlugin(project, storage); }
                catch (Throwable t) { handleThrowable(plugin, t); }
                if (plugin != null) _plugins.add(plugin);
            }

            if (_plugins.isEmpty()) {
                plugins = null;
            } else {
                session.addListener(new ProfilingStateAdapter() {
                    public void profilingStateChanged(ProfilingStateEvent e) {
                        int state = e.getNewState();
                        if (state == Profiler.PROFILING_STARTED) notifyStarted();
                        else if (state == Profiler.PROFILING_INACTIVE) notifyStopped();
                    }
                });
                plugins = _plugins;
            }
        }
    }
    
    
    boolean hasPlugins() {
        return plugins != null;
    }
    
    List<JMenuItem> menuItems() {
        List<JMenuItem> menus = new ArrayList();
        
        if (plugins != null) for (ProfilerPlugin plugin : plugins) {
            try {
                JMenu menu = new JMenu(plugin.getName());
                plugin.createMenu(menu);
                if (menu.getItemCount() > 0) menus.add(menu);
            } catch (Throwable t) {
                handleThrowable(plugin, t);
            }
        }
        
        return menus;
    }
    
    
    void notifyStarting() {
        if (plugins != null) for (ProfilerPlugin plugin : plugins)
            try {
                plugin.sessionStarting();
            } catch (Throwable t) {
                handleThrowable(plugin, t);
            }
    }
    
    void notifyStarted() {
        if (plugins != null) for (ProfilerPlugin plugin : plugins)
            try {
                plugin.sessionStarted();
            } catch (Throwable t) {
                handleThrowable(plugin, t);
            }
    }
    
    void notifyStopping() {
        if (plugins != null) for (ProfilerPlugin plugin : plugins)
            try {
                plugin.sessionStopping();
            } catch (Throwable t) {
                handleThrowable(plugin, t);
            }
    }
    
    void notifyStopped() {
        if (plugins != null) for (ProfilerPlugin plugin : plugins)
            try {
                plugin.sessionStopped();
            } catch (Throwable t) {
                handleThrowable(plugin, t);
            }
    }
    
    private void handleThrowable(final ProfilerPlugin p, final Throwable t) {
        t.printStackTrace(System.err);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String log = t.getLocalizedMessage();
                String msg = p == null ? Bundle.ProfilerPlugins_PluginNotInitialized(log) :
                                  Bundle.ProfilerPlugins_PluginFailed(p.getName(), log);
                ProfilerDialogs.displayError(msg);
            }
        });
    }
    
}
