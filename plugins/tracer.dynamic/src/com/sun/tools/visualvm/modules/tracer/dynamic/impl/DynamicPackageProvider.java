/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

package com.sun.tools.visualvm.modules.tracer.dynamic.impl;

import com.sun.tools.visualvm.application.Application;
import com.sun.tools.visualvm.modules.tracer.TracerPackage;
import com.sun.tools.visualvm.modules.tracer.TracerPackageProvider;
import com.sun.tools.visualvm.modules.tracer.TracerSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.ImageIcon;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.ModuleInstall;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Jaroslav Bachorik
 */
class DynamicPackageProvider extends ModuleInstall {
    final private static Logger LOGGER = Logger.getLogger(DynamicPackageProvider.class.getName());

    final private static ImageIcon defaultIcon = ImageUtilities.loadImageIcon("com/sun/tools/visualvm/modules/tracer/dynamic/resources/default.png", true); // NOI18N
    private Impl provider;
    private ScriptEngine jsEngine;
    private boolean enabled = false;

    @Override
    public synchronized void restored() {
        if (provider == null) provider = new Impl();
        TracerSupport.getInstance().registerPackageProvider(provider);
        jsEngine = new ScriptEngineManager().getEngineByExtension("js");
        if (jsEngine != null) {
            enabled = true;
        }
    }

    @Override
    public synchronized void uninstalled() {
        if (provider == null) return;
        TracerSupport.getInstance().unregisterPackageProvider(provider);
        provider = null;
    }


    private class Impl extends TracerPackageProvider<Application> {

        Impl() { super(Application.class); }

        @Override
        public TracerPackage<Application>[] getPackages(Application application) {
            if (!enabled) return new TracerPackage[0];
            Set<TracerPackage<Application>> packages = new HashSet<TracerPackage<Application>>();

            Bindings bindings = jsEngine.createBindings();
            bindings.put("application", application);

            try {
                FileObject probesRoot = FileUtil.getConfigFile("VisualVM/Tracer/packages"); // NOI18N
                
                StringBuilder sb = new StringBuilder(readResource("com/sun/tools/visualvm/modules/tracer/dynamic/resources/configurator.js"));

                Enumeration<? extends FileObject> data = probesRoot.getData(false);
                while (data.hasMoreElements()) {
                    FileObject cfg = data.nextElement();
                    sb.append(cfg.asText()).append('\n');
                }

                jsEngine.eval(sb.toString(), bindings);

                packages.addAll((Collection<TracerPackage<Application>>)bindings.get("configuredPackages"));
//                for(int i=0;i<counter;i++) {
//                    Object ret = jsEngine.eval("configure(pkgs" + i + ")", bindings);
//                    packages.addAll((Collection<TracerPackage<Application>>)ret);
//                }
            } catch (IOException e) {
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return packages.toArray(new TracerPackage[packages.size()]);

//            int defaultCounter = 1000;
//            for(FileObject pkg : probesRoot.getChildren()) {
//                Object name = pkg.getAttribute("displayName"); // NOI18N
//                Object desc = pkg.getAttribute("desc"); // NOI18N
//                Object icon = pkg.getAttribute("icon"); // NOI18N
//                Object position = pkg.getAttribute("position"); // NOI18N
//
//                ApplicationValidator validator = (ApplicationValidator)pkg.getAttribute("validator"); // NOI18N
//                packages.add(new DynamicPackage(pkg, (String)name, desc != null ? (String)desc : "", icon != null ? ImageUtilities.loadImageIcon((String)icon, true) : defaultIcon, position != null ? ((Integer)position).intValue() : defaultCounter++, isAvailable(validator, application)));
//            }
//            return (DynamicPackage[])packages.toArray(new DynamicPackage[packages.size()]);
        }

        private CharSequence readResource(String resName) {
            StringBuilder sb = new StringBuilder();
            InputStream is = getClass().getClassLoader().getResourceAsStream(resName);
            if (is != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                try {
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                } catch (IOException e) {
                } finally {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }
            return sb;
        }
//        private static boolean isAvailable(ApplicationValidator validator, Application application) {
//            if (validator == null) return true;
//
//            if (!application.isLocalApplication() ||
//                 application.getState() != Stateful.STATE_AVAILABLE)
//            return false;
//
//            return validator.isPackageApplicable(application);
//        }

    }

}
