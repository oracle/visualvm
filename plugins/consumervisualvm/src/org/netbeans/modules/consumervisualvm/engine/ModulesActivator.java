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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.consumervisualvm.engine;

import java.util.Collection;
import java.util.LinkedList;
import javax.swing.SwingUtilities;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jirka Rechtacek
 */
public class ModulesActivator {
    
    private Collection<UpdateElement> modules4enable;
    private RequestProcessor.Task enableTask = null;
    private OperationContainer<OperationSupport> enableContainer;
    
    public ModulesActivator (Collection<UpdateElement> modules) {
        if (modules == null || modules.isEmpty ()) {
            throw new IllegalArgumentException ("Cannot construct ModulesActivator with null or empty Collection " + modules);
        }
        modules4enable = modules;
    }
    
    public RequestProcessor.Task getEnableTask () {
        if (enableTask == null) {
            enableTask = createEnableTask ();
        }
        return enableTask;
    }
    
    private RequestProcessor.Task createEnableTask () {
        assert enableTask == null || enableTask.isFinished () : "The Enable Task cannot be started nor scheduled.";
        enableTask = RequestProcessor.getDefault ().create (doEnable);
        return enableTask;
    }
    
    private Runnable doEnable = new Runnable () {
        public void run() {
            enableModules ();
        }

    };
    
    private void enableModules () {
        try {
            doEnableModules ();
        } catch (Exception x) {
            Exceptions.printStackTrace (x);
        }
    }
    
    private void doEnableModules () throws OperationException {
        assert ! SwingUtilities.isEventDispatchThread () : "Cannot be called in EQ.";
        enableContainer = null;
        for (UpdateElement module : modules4enable) {
            if (enableContainer == null) {
                enableContainer = OperationContainer.createForEnable ();
            }
            if (enableContainer.canBeAdded (module.getUpdateUnit (), module)) {
                enableContainer.add (module);
            }
        }
        if (enableContainer.listAll ().isEmpty ()) {
            return ;
        }
        assert enableContainer.listInvalid ().isEmpty () :
            "No invalid Update Elements " + enableContainer.listInvalid ();
        if (! enableContainer.listInvalid ().isEmpty ()) {
            throw new IllegalArgumentException ("Some are invalid for enable: " + enableContainer.listInvalid ());
        }
        OperationSupport enableSupport = enableContainer.getSupport ();
        ProgressHandle enableHandle = ProgressHandleFactory.createHandle (
                getBundle ("ModulesActivator_Enable",
                presentUpdateElements (FindComponentModules.getVisibleUpdateElements (modules4enable))));
        enableSupport.doOperation (enableHandle);
    }
    
    public static String presentUpdateElements (Collection<UpdateElement> elems) {
        String res = "";
        for (UpdateElement el : new LinkedList<UpdateElement> (elems)) {
            res += res.length () == 0 ? el.getDisplayName () : ", " + el.getDisplayName (); // NOI18N
        }
        return res;
    }

    private static String getBundle (String key, Object... params) {
        return NbBundle.getMessage (ModulesActivator.class, key, params);
    }
    
}
