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
