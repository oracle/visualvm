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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author S. Aubrecht
 */
public class WelcomeOptions {

    private static WelcomeOptions theInstance;
    
    private static final String PROP_SHOW_ON_STARTUP = "showOnStartup";
    
    private PropertyChangeSupport propSupport;
    
    /** Creates a new instance of WelcomeOptions */
    private WelcomeOptions() {
    }

    private Preferences prefs() {
        return NbPreferences.forModule(WelcomeOptions.class);
    }

    public static synchronized WelcomeOptions getDefault() {
        if( null == theInstance ) {
            theInstance = new WelcomeOptions();
        }
        return theInstance;
    }
 
    public void setShowOnStartup( boolean show ) {
        boolean oldVal = isShowOnStartup();
        prefs().putBoolean(PROP_SHOW_ON_STARTUP, show);
        if( null != propSupport )
            propSupport.firePropertyChange( PROP_SHOW_ON_STARTUP, oldVal, show );
    }

    public boolean isShowOnStartup() {
        return prefs().getBoolean(PROP_SHOW_ON_STARTUP, true);
    }
    
    public void addPropertyChangeListener( PropertyChangeListener l ) {
        if( null == propSupport )
            propSupport = new PropertyChangeSupport( this );
        propSupport.addPropertyChangeListener( l );
    }
    
    public void removePropertyChangeListener( PropertyChangeListener l ) {
        if( null == propSupport )
            return;
        propSupport.removePropertyChangeListener( l );
    }
}
