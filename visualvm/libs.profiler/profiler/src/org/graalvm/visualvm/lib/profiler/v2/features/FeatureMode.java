/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.v2.features;

import java.util.Collection;
import java.util.HashSet;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.lib.jfluid.client.ClientUtils;
import org.graalvm.visualvm.lib.common.ProfilingSettings;

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
