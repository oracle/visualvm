/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.sources.viewer.internal;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.graalvm.visualvm.sources.SourceHandle;
import org.graalvm.visualvm.sources.SourcesViewer;
import org.openide.util.NbBundle;
//import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@NbBundle.Messages({
    "InternalSourcesViewer_Name=Internal Viewer",                               // NOI18N
    "InternalSourcesViewer_Description=simple built-in text viewer"             // NOI18N                                 
})
//@ServiceProvider(service=SourcesViewer.class, position = 100)
public final class InternalSourcesViewer extends SourcesViewer {
    
    private static final String ID = "InternalSourcesViewer";                   // NOI18N
    
    
    private InternalSourceAppearance appearance;
    
    
    public InternalSourcesViewer() {
        super(ID, Bundle.InternalSourcesViewer_Name(), Bundle.InternalSourcesViewer_Description());
    }
    
    
    @Override
    public boolean open(SourceHandle handle) {
        final String uri = handle.getSourceUri();
        final String text = handle.getText();
        final int offset  = handle.getOffset();
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                InternalSourceViewerTopComponent.showSource(uri, text, offset == -1 ? 0 : offset, getAppearance());
            }
        });
        
        return true;
    }
    
    
    @Override
    public void loadSettings() { if (appearance != null) appearance.loadSettings(); }
    
    @Override
    public void saveSettings() { if (appearance != null) appearance.saveSettings(); }
    
    @Override
    public boolean settingsDirty() { return appearance == null ? false : appearance.currentSettingsDirty(); }
    
    
    @Override
    public JComponent getSettingsComponent() { return getAppearance().getSettingsComponent(); }
    
    
    private InternalSourceAppearance getAppearance() {
        if (appearance == null) appearance = new InternalSourceAppearance();
        return appearance;
    }
    
}
