/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package org.graalvm.visualvm.modules.tracer.dynamic.impl;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.modules.tracer.TracerPackage;
import org.graalvm.visualvm.modules.tracer.TracerPackageProvider;
import org.graalvm.visualvm.modules.tracer.TracerSupport;
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

    final private static ImageIcon defaultIcon = ImageUtilities.loadImageIcon("org/graalvm/visualvm/modules/tracer/dynamic/resources/default.png", true); // NOI18N
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
                if (probesRoot != null) {
                    jsEngine.eval(readResource("org/graalvm/visualvm/modules/tracer/dynamic/resources/configurator.js").toString(), bindings);

                    Enumeration<? extends FileObject> data = probesRoot.getData(false);

                    while (data.hasMoreElements()) {
                        FileObject cfg = data.nextElement();
                        jsEngine.eval(cfg.asText(), bindings);
                    }

                    packages.addAll((Collection<TracerPackage<Application>>)bindings.get("configuredPackages"));
                }
            } catch (IOException e) {
            } catch (ScriptException e) {
                e.printStackTrace();
            }
            return packages.toArray(new TracerPackage[packages.size()]);
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
