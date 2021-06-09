/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.gotosource.impl;

import java.util.Collection;
import java.util.Collections;
import org.graalvm.visualvm.gotosource.viewer.ExternalSourcesViewer;
import org.graalvm.visualvm.gotosource.SourcesViewer;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;

/**
 *
 * @author Jiri Sedlacek
 */
public final class SourceViewers {
    
    private static final String PROP_SELECTED_VIEWER = "prop_SourceViewers_selected"; // NOI18N
    
    private static final String DEFAULT_VIEWER_ID = ExternalSourcesViewer.ID;
    
    
    private static SourcesViewer FORCED_VIEWER;
    
    
    private SourceViewers() {}
    
    
    public static Collection<? extends SourcesViewer> getRegisteredViewers() {
        if (isForcedViewer()) return Collections.singletonList(FORCED_VIEWER);
        
        return Lookup.getDefault().lookupAll(SourcesViewer.class);
    }
    
    public static SourcesViewer getSelectedViewer() {
        if (isForcedViewer()) return FORCED_VIEWER;
        
        String selectedID = NbPreferences.forModule(SourcesViewer.class).get(PROP_SELECTED_VIEWER, DEFAULT_VIEWER_ID);
        
        if (selectedID != null)
            for (SourcesViewer registered : getRegisteredViewers())
                if (selectedID.equals(registered.getID()))
                    return registered;
        
        return null;
    }
    
    public static void saveSelectedViewer(SourcesViewer selected) {
        if (isForcedViewer()) return;
        
        NbPreferences.forModule(SourcesViewer.class).put(PROP_SELECTED_VIEWER, selected.getID());
    }
    
    
    public static void forceExternalViewer(String command) {
        FORCED_VIEWER = command == null || command.isEmpty() ? null : new ExternalSourcesViewer(command);
    }
    
    public static boolean isForcedViewer() {
        return FORCED_VIEWER != null;
    }
    
}
