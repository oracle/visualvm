/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.v2.features;

import java.util.Collection;
import java.util.HashSet;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.lib.profiler.client.ClientUtils;
import org.netbeans.lib.profiler.common.ProfilingSettings;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class FeatureMode {
    
    // --- API -----------------------------------------------------------------
    
    abstract String getID();
    
    abstract String getName();
    
    abstract void configureSettings(ProfilingSettings settings);
    
    abstract void confirmSettings();
    
    abstract boolean pendingChanges();
    
    abstract boolean currentSettingsValid();
    
    abstract JComponent getUI();
    
    
    // --- External implementation ---------------------------------------------
    
    abstract void settingsChanged();
    
    abstract String readFlag(String flag, String defaultValue);

    abstract void storeFlag(String flag, String value);
    
    
    // --- Roots Set -----------------------------------------------------------
    
    // To be only accessed in EDT
    static class Selection extends HashSet<ClientUtils.SourceCodeSelection> {
        
        private boolean trans;
        private boolean dirty;
        private boolean events;
        private boolean changing;
        
        final void beginTrans() {
            assert SwingUtilities.isEventDispatchThread();
            trans = true;
        }
        
        final void endTrans() {
            assert SwingUtilities.isEventDispatchThread();
            trans = false;
            if (dirty) _changed();
        }
        
        final void enableEvents() {
            assert SwingUtilities.isEventDispatchThread();
            events = true;
        }
        
        final void disableEvents() {
            assert SwingUtilities.isEventDispatchThread();
            events = false;
        }
        
        public final boolean add(ClientUtils.SourceCodeSelection selection) {
            assert SwingUtilities.isEventDispatchThread();
            _changing();
            if (super.add(selection)) return _changed();
            else return false;
        }
        
        public final boolean addAll(Collection<? extends ClientUtils.SourceCodeSelection> selections) {
            boolean _trans = trans;
            beginTrans();
            
            boolean addAll = super.addAll(selections);
            
            endTrans();
            trans = _trans;
            
            return addAll;
        }
        
        public final boolean remove(Object selection) {
            assert SwingUtilities.isEventDispatchThread();
            _changing();
            if (super.remove(selection)) return _changed();
            else return false;
        }
        
        public final boolean removeAll(Collection<?> selections) {
            boolean _trans = trans;
            beginTrans();
            
            _changing();
            boolean removeAll = super.removeAll(selections);
            if (removeAll) _changed();
            
            endTrans();
            trans = _trans;
            
            return removeAll;
        }
        
        public final boolean retainAll(Collection<?> selections) {
            boolean _trans = trans;
            beginTrans();
            
            _changing();
            boolean retainAll = super.retainAll(selections);
            if (retainAll) _changed();
            
            endTrans();
            trans = _trans;
            
            return retainAll;
        }
        
        public final void clear() {
            assert SwingUtilities.isEventDispatchThread();
            _changing();
            super.clear();
            _changed();
        }
        
        private void _changing() {
            if (trans) {
                if (!changing) changing = true;
                else return;
            }
            if (events) changing();
        }
        
        protected void changing() {}
        
        private boolean _changed() {
            changing = false;
            if (!trans) {
                if (events) changed();
                dirty = false;
            } else {
                dirty = true;
            }
            return true;
        }
        
        protected void changed() {}
        
    }
    
}
