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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationSupport;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jirka Rechtacek
 */
public final class FindComponentModules {
    private Collection<String> codeNames;
    private String problemDescription;

    public FindComponentModules (String... components) {
        if (components == null) {
            codeNames = Collections.emptySet ();
        } else {
            codeNames = Arrays.asList (components);
        }
    }

    public final String DO_CHECK = "do-check";

    private final String ENABLE_LATER = "enable-later";
    private Collection<UpdateElement> forInstall = null;
    private Collection<UpdateElement> forEnable = null;
    private RequestProcessor.Task componentModulesFindingTask = null;
    private RequestProcessor.Task enableLaterTask = null;

    public RequestProcessor.Task getFindingTask () {
        return componentModulesFindingTask;
    }

    public RequestProcessor.Task createFindingTask () {
        assert componentModulesFindingTask == null || componentModulesFindingTask.isFinished () : "The Finding Task cannot be started nor scheduled.";
        componentModulesFindingTask = RequestProcessor.getDefault ().create (doFind);
        return componentModulesFindingTask;
    }

    public Collection<UpdateElement> getModulesForInstall () {
        assert forInstall != null : "candidates cannot be null if getModulesForInstall() is called.";
        return forInstall;
    }
    
    public String getProblemDescription () {
        return problemDescription;
    }
    
    public void clearModulesForInstall () {
        forInstall = null;
        componentModulesFindingTask = null;
        enableLaterTask = null;
    }
    
    public void writeEnableLater (Collection<UpdateElement> modules) {
        Preferences pref = FindComponentModules.getPreferences ();
        if (modules == null) {
            pref.remove (ENABLE_LATER);
            return ;
        }
        String value = "";
        for (UpdateElement m : modules) {
            value += value.length () == 0 ? m.getCodeName () : ", " + m.getCodeName (); // NOI18N
        }
        if (value.trim ().length () == 0) {
            pref.remove (ENABLE_LATER);
        } else {
            pref.put (ENABLE_LATER, value);
        }
    }

    public Collection<UpdateElement> getModulesForEnable () {
        assert forEnable != null : "candidates cannot be null if getModulesForInstall() is called.";
        return forEnable;
    }
    
    private Collection<UpdateElement> readEnableLater () {
        Set<UpdateElement> res = new HashSet<UpdateElement> ();
        Preferences pref = FindComponentModules.getPreferences ();
        String value = pref.get (ENABLE_LATER, null);
        if (value != null && value.trim ().length () > 0) {
            Enumeration en = new StringTokenizer (value, ","); // NOI18N
            while (en.hasMoreElements ()) {
                String codeName = ((String) en.nextElement ()).trim ();
                UpdateElement el = findUpdateElement (codeName, true);
                if (el != null) {
                    res.add (el);
                }
            }
        }
        return res;
    }
    
    public static Collection<UpdateElement> getVisibleUpdateElements (Collection<UpdateElement> elems) {
        Collection<UpdateElement> res = new HashSet<UpdateElement> ();
        for (UpdateElement el : new LinkedList<UpdateElement> (elems)) {
            if (UpdateManager.TYPE.KIT_MODULE.equals (el.getUpdateUnit ().getType ())) {
                res.add (el);
            }
        }
        return res;
    }

    public static Preferences getPreferences () {
        return NbPreferences.forModule (FindComponentModules.class);
    }

    private Runnable doFind = new Runnable () {
        public void run() {
            if (SwingUtilities.isEventDispatchThread ()) {
                RequestProcessor.getDefault ().post (doFind);
                return ;
            }
            findComponentModules ();
        }
    };

    private void findComponentModules () {
        Collection<UpdateUnit> units = UpdateManager.getDefault ().getUpdateUnits (UpdateManager.TYPE.MODULE);
        problemDescription = null;
        
        // install missing modules
        Collection<UpdateElement> elementsForInstall = getMissingModules (units);
        forInstall = getAllForInstall (elementsForInstall);
        
        // install disabled modules
        Collection<UpdateElement> elementsForEnable = getDisabledModules (units);
        forEnable = getAllForEnable (elementsForEnable);
        
        if (problemDescription == null && elementsForInstall.isEmpty () && elementsForEnable.isEmpty ()) {
            problemDescription = NbBundle.getMessage (FindComponentModules.class, "FindComponentModules_Problem_PluginNotFound", codeNames);
        }
    }
    
    private Collection<UpdateElement> getMissingModules (Collection<UpdateUnit> allUnits) {
        Set<UpdateElement> res = new HashSet<UpdateElement> ();
        for (UpdateUnit unit : allUnits) {
            if (unit.getInstalled () == null && codeNames.contains(unit.getCodeName ())) {
                res.add (unit.getAvailableUpdates ().get (0));
            }
        }
        return res;
    }
    
    private Collection<UpdateElement> getAllForInstall (Collection<UpdateElement> elements) {
        Collection<UpdateElement> all = new HashSet<UpdateElement> ();
        for (UpdateElement el : elements) {
            OperationContainer<InstallSupport> ocForInstall = OperationContainer.createForInstall ();
            if (ocForInstall.canBeAdded (el.getUpdateUnit (), el)) {
                OperationContainer.OperationInfo<InstallSupport> info = ocForInstall.add (el);
                if (info == null) {
                    continue;
                }
                Set<UpdateElement> reqs = info.getRequiredElements ();
                ocForInstall.add (reqs);
                Set<String> breaks = info.getBrokenDependencies ();
                if (breaks.isEmpty ()) {
                    all.add (el);
                    all.addAll (reqs);
                } else {
                    problemDescription = NbBundle.getMessage (FindComponentModules.class,
                            "FindComponentModules_Problem_DependingPluginNotFound",
                            codeNames,
                            breaks);
                }
            }
        }
        return all;
    }
    
    private Collection<UpdateElement> getDisabledModules (Collection<UpdateUnit> allUnits) {
        Set<UpdateElement> res = new HashSet<UpdateElement> ();
        for (UpdateUnit unit : allUnits) {
            if (unit.getInstalled () != null && codeNames.contains(unit.getCodeName ())) {
                if (! unit.getInstalled ().isEnabled ()) {
                    res.add (unit.getInstalled ());
                }
            }
        }
        return res;
    }
    
    private Collection<UpdateElement> getAllForEnable (Collection<UpdateElement> elements) {
        Collection<UpdateElement> all = new HashSet<UpdateElement> ();
        for (UpdateElement el : elements) {
            OperationContainer<OperationSupport> ocForEnable = OperationContainer.createForEnable ();
            if (ocForEnable.canBeAdded (el.getUpdateUnit (), el)) {
                OperationContainer.OperationInfo<OperationSupport> info = ocForEnable.add (el);
                if (info == null) {
                    continue;
                }
                Set<UpdateElement> reqs = info.getRequiredElements ();
                ocForEnable.add (reqs);
                Set<String> breaks = info.getBrokenDependencies ();
                if (breaks.isEmpty ()) {
                    all.add (el);
                    all.addAll (reqs);
                } else {
                    problemDescription = NbBundle.getMessage (FindComponentModules.class,
                            "FindComponentModules_Problem_DependingPluginNotFound",
                            codeNames,
                            breaks);
                }
            }
        }
        return all;
    }
    
    private static UpdateElement findUpdateElement (String codeName, boolean isInstalled) {
        UpdateElement res = null;
        for (UpdateUnit u : UpdateManager.getDefault ().getUpdateUnits (UpdateManager.TYPE.MODULE)) {
            if (codeName.equals (u.getCodeName ())) {
                if (isInstalled && u.getInstalled () != null) {
                    res = u.getInstalled ();
                } else if (! isInstalled && ! u.getAvailableUpdates ().isEmpty ()) {
                    res = u.getAvailableUpdates ().get (0);
                }
                break;
            }
        }
        return res;
    }
}
