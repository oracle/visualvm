/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.modules.profiler.attach.wizard.screen;

import org.openide.WizardDescriptor;
import java.awt.Component;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.profiler.attach.wizard.WizardContext;


/**
 *
 * @author j.bachorik
 */
public abstract class AbstractWizardScreen implements WizardScreen, WizardDescriptor.FinishablePanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Collection changeListeners;
    private boolean trackUpdates;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //    private String title;
    public AbstractWizardScreen() {
        this.changeListeners = new LinkedList();
        this.trackUpdates = true;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Component getComponent() {
        Component component = this.getRenderPanel();

        return component;
    }

    public void setTitle(String title) {
        this.getRenderPanel().setName(title);
    }

    public void addChangeListener(ChangeListener listener) {
        this.changeListeners.add(listener);
    }

    public void readSettings(Object object) {
        WizardContext ctx = (WizardContext) ((WizardDescriptor) object).getProperty(WizardContext.CONTEXT_PROPERTY_NAME);
        onPanelShow(ctx);
    }

    public void removeChangeListener(ChangeListener listener) {
        this.changeListeners.remove(listener);
    }

    public void storeSettings(Object object) {
        //        WizardContext ctx = (WizardContext)((WizardDescriptor)object).getProperty(WizardContext.CONTEXT_PROPERTY_NAME);
        //        onStoreToContext(ctx);
    }

    protected abstract JPanel getRenderPanel();

    protected void setTrackUpdates(boolean value) {
        this.trackUpdates = value;
    }

    protected boolean isTrackUpdates() {
        return this.trackUpdates;
    }

    protected abstract void onPanelShow(WizardContext ctx);

    protected void publishUpdate(ChangeEvent event) {
        if (!isTrackUpdates()) {
            return;
        }

        for (Iterator it = changeListeners.iterator(); it.hasNext();) {
            ChangeListener listener = (ChangeListener) it.next();
            listener.stateChanged(event);
        }
    }
}
