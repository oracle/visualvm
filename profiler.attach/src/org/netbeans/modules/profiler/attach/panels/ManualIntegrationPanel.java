/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
package org.netbeans.modules.profiler.attach.panels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import org.netbeans.lib.profiler.common.AttachSettings;
import org.netbeans.modules.profiler.attach.providers.RemotePackExporter;
import org.netbeans.modules.profiler.attach.providers.TargetPlatformEnum;
import org.netbeans.modules.profiler.attach.spi.IntegrationProvider;
import org.netbeans.modules.profiler.attach.wizard.AttachWizardContext;
import org.openide.util.HelpCtx;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ManualIntegrationPanel extends AttachWizardPanel {
    AtomicBoolean exportRunning = new AtomicBoolean(false);
    
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------
    /* default */ class Model {
        //~ Instance fields ------------------------------------------------------------------------------------------------------
        private TargetPlatformEnum jvm = null;        
        //~ Methods --------------------------------------------------------------------------------------------------------------
        public String getApplication() {
            return getContext().getAttachSettings().getServerType();
        }

        public String getApplicationType() {
            return getContext().getAttachSettings().getTargetType();
        }

        public IntegrationProvider.IntegrationHints getIntegrationHints() {
            return getContext().getIntegrationProvider().getModificationHints(getContext().getAttachSettings());
        }

        public boolean isRemote() {
            return getContext().getAttachSettings().isRemote();
        }
        
        public void setJvm(TargetPlatformEnum jvm) {
            this.jvm = jvm;

            if (jvm != null) {
                getContext().getIntegrationProvider().setTargetJava(jvm.toString());
            }
        }

        public TargetPlatformEnum getJvm() {
            return this.jvm;
        }

        public List getSupportedJvms() {
            List<TargetPlatformEnum> supportedJvms = new ArrayList<TargetPlatformEnum>();

            if (getContext() != null) {
                AttachSettings settings = getContext().getAttachSettings();

                for (Iterator it = TargetPlatformEnum.iterator(); it.hasNext();) {
                    TargetPlatformEnum jvm = (TargetPlatformEnum) it.next();

                    if (settings.isDirect() || settings.isRemote() || (settings.isDynamic16() && (jvm.equals(TargetPlatformEnum.JDK6) || jvm.equals(TargetPlatformEnum.JDK7)))) {
                        if (getContext().getIntegrationProvider().supportsJVM(jvm, settings)) {
                            supportedJvms.add(jvm);
                        }
                    }
                }
            }

            return supportedJvms;
        }

        public String exportRemotePack(String path) throws IOException {
            if (exportRunning.compareAndSet(false, true)) {
                try {
                    publishUpdate(new ChangeEvent(this));
                    return RemotePackExporter.getInstance().export(path, getContext().getAttachSettings().getHostOS(), getContext().getIntegrationProvider().getTargetJava());
                } finally {
                    exportRunning.compareAndSet(true, false);
                    publishUpdate(new ChangeEvent(exportRunning));
                }
            } else {
                throw new IOException();
            }
        }
        
        public String getRemotePackPath(String exportPath) {
            return RemotePackExporter.getInstance().getRemotePackPath(exportPath, getContext().getAttachSettings().getHostOS());
        }
    }    
    
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------
    private static final String HELP_CTX_KEY = "ManualIntegrationPanel.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private ManualIntegrationPanelUI panel = null;
    private Model model = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    public ManualIntegrationPanel() {
        this.model = new Model();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    public HelpCtx getHelp() {
        return HELP_CTX;
    }

    public boolean isValid() {
        return !exportRunning.get();
    }

    public boolean canBack(AttachWizardContext context) {
        return true;
    }

    public boolean canFinish(AttachWizardContext context) {
        return isValid();
    }

    public boolean canNext(AttachWizardContext context) {
        return true;
    }

    public boolean onCancel(AttachWizardContext context) {
        return true;
    }

    public void onEnter(AttachWizardContext context) {
    }

    public void onExit(AttachWizardContext context) {
        //    context.setJvm(this.model.getJvm());
        this.model.setJvm(null);
    }

    public void onFinish(AttachWizardContext context) {
        this.model.setJvm(null);
    }

    public void onPanelShow() {
        this.panel.refresh();
    }

    protected JPanel getRenderPanel() {
        if (this.panel == null) {
            this.panel = new ManualIntegrationPanelUI(this.model);
        }

        return this.panel;
    }
}
