/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
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
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
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
