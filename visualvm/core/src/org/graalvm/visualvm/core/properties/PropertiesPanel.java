/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.properties;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.PanelUI;

/**
 * UI component to display and modify the properties. Provides validity
 * notification support to correctly handle actual state of the user-provided
 * data - valid or invalid.
 *
 * Note: if using custom JPanel instances in the PropertiesPanel be sure to use
 * JPanel.setOpaque(false) whenever possible to keep the settings UI consistent.
 * For some Look and Feels the PropertiesPanel container doesn't have a standard
 * JPanel background.
 *
 * @since VisualVM 1.2
 * @author Jiri Sedlacek
 */
public class PropertiesPanel extends JPanel {

    private boolean settingsValid = true;
    private List<ChangeListener> listeners = new ArrayList();


    /**
     * Returns true if settings defined by this Panel are valid.
     * To be called in EDT.
     *
     * @return true if settings defined by this Panel are valid
     */
    public final boolean settingsValid() {
        return settingsValid;
    }

    /**
     * Add a ChangeListener. Use settingsValid() method to read the state.
     * To be called in EDT.
     *
     * @param listener ChangeListener
     */
    public final void addChangeListener(ChangeListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    /**
     * Remove a ChangeListener. To be called in EDT.
     * @param listener ChangeListener
     */
    public final void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }


    /**
     * Notifies the Panel that validity of the user-provided data changed.
     * To be called in EDT.
     *
     * @param valid true if the current settings are valid, false otherwise
     */
    protected final void setSettingsValid(boolean valid) {
        if (settingsValid != valid) {
            settingsValid = valid;
            fireStateChanged();
        }
    }


    public void setUI(PanelUI ui) {
        super.setUI(ui);
        setOpaque(false);
    }


    private void fireStateChanged() {
        for (ChangeListener listener : listeners)
            listener.stateChanged(new ChangeEvent(this));
    }

}
