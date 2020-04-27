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

package org.graalvm.visualvm.modules.appui.welcome;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import org.openide.awt.StatusDisplayer;
import org.openide.windows.WindowManager;

/**
 *
 * @author S. Aubrecht
 */
public class WebLink extends LinkButton {

    private static String sourceId;
    private String url;
    private boolean includeSource;

    /** Creates a new instance of WebLink */
    public WebLink( String key ) {
        this( BundleSupport.getLabel( key ), BundleSupport.getURL( key ), true );
    }
    
    public WebLink( String key, boolean includeSource ) {
        this( BundleSupport.getLabel( key ), BundleSupport.getURL( key ), includeSource );
    }
    
    public WebLink( String label, String url, boolean includeSource ) {
        super( label );
        this.url = url;
        this.includeSource = includeSource;
    }

    public void actionPerformed(ActionEvent e) {
        Utils.showURL( includeSource ? url + getSourceId() : url );
    }
    
    @Override
    protected void onMouseExited(MouseEvent e) {
        StatusDisplayer.getDefault().setStatusText( "" );
    }

    @Override
    protected void onMouseEntered(MouseEvent e) {
        StatusDisplayer.getDefault().setStatusText( url );
    }
    
    private static synchronized String getSourceId() {
        if (sourceId == null) {
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            if (mainWindow == null) sourceId = "VisualVM (Unknown Version)"; // NOI18N
            else {
                String sourceName = mainWindow.getTitle();
                if (sourceName == null) sourceId = "VisualVM (Unknown Version)"; // NOI18N
                else sourceId = "?" + sourceName.trim().replace(" ", "_"); // NOI18N
            }
        }
        
        return sourceId;
    }
}

