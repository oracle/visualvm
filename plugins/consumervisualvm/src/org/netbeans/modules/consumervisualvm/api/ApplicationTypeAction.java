/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.consumervisualvm.api;

import org.graalvm.visualvm.application.Application;
import org.graalvm.visualvm.application.jvm.Jvm;
import org.graalvm.visualvm.application.jvm.JvmFactory;
import org.graalvm.visualvm.core.ui.actions.MultiDataSourceAction;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.Set;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.modules.consumervisualvm.DecoratedFileSystem;
import org.netbeans.modules.consumervisualvm.engine.FindComponentModules;
import org.netbeans.modules.consumervisualvm.engine.ModulesActivator;
import org.netbeans.modules.consumervisualvm.engine.ModulesInstaller;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jirka Rechtacek
 */
public class ApplicationTypeAction extends MultiDataSourceAction<Application> {
    public static final String MAIN_CLASS_NAME = "mainClassName";
    public static final String PLUGIN_CODE_NAME = "pluginCodeName";
    public static final String ACTION_NAME = "ActionName";
    public static final String LOCALIZING_BUNDLE = "SystemFileSystem.localizingBundle";

    private String mainClassName;
    private String pluginCodeName;
    private String displayName;

    public static synchronized ApplicationTypeAction newAction (FileObject fo) {
        return new ApplicationTypeAction (fo);
    }

    private ApplicationTypeAction (FileObject fo) {
        super (Application.class);
        mainClassName = (String) fo.getAttribute (MAIN_CLASS_NAME);
        pluginCodeName = (String) fo.getAttribute (PLUGIN_CODE_NAME);
        String bundle = (String) fo.getAttribute (LOCALIZING_BUNDLE);
        ResourceBundle b = NbBundle.getBundle (bundle);
        displayName = b.getString ((String) fo.getAttribute (ACTION_NAME));
        putValue(NAME, displayName);
    }

    protected void actionPerformed (Set arg0, ActionEvent arg1) {
        RequestProcessor.getDefault ().post (new Runnable () {
            public void run () {
                FindComponentModules findModules = new FindComponentModules (pluginCodeName);
                findModules.createFindingTask ().waitFinished ();
                Collection<UpdateElement> toInstall = findModules.getModulesForInstall ();
                Collection<UpdateElement> toEnable = findModules.getModulesForEnable ();
                if (toInstall != null && ! toInstall.isEmpty ()) {
                    ModulesInstaller installer = new ModulesInstaller (toInstall);
                    installer.getInstallTask ().waitFinished ();
                    DecoratedFileSystem.getInstance ().refresh ();
                } else if (toEnable != null && ! toEnable.isEmpty ()) {
                    ModulesActivator enabler = new ModulesActivator (toEnable);
                    enabler.getEnableTask ().waitFinished ();
                    DecoratedFileSystem.getInstance ().refresh ();
                } else {
                    DialogDisplayer.getDefault ().notifyLater (new NotifyDescriptor.Message (
                            NbBundle.getMessage (ApplicationTypeAction.class, "ApplicationTypeAction_ProblemDescription",
                            findModules.getProblemDescription ())));
                }
            }
        }); 
    }
    
    protected boolean isEnabled (Set<Application> sources) {
        if (sources == null || sources.isEmpty ()) {
            return false;
        }
        for (Application app : sources) {
            Jvm jvm = JvmFactory.getJVMFor (app);
            if (mainClassName.equals (jvm.getMainClass ())) {
                return true;
            }
        }
        return false;
    }

}
