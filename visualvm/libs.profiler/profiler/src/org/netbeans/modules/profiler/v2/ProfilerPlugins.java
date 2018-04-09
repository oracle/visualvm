/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.profiler.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.common.event.ProfilingStateAdapter;
import org.netbeans.lib.profiler.common.event.ProfilingStateEvent;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
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
