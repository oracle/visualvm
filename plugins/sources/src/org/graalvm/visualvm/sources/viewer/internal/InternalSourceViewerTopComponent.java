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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import org.graalvm.visualvm.sources.SourceHandle;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
final class InternalSourceViewerTopComponent extends TopComponent {
    
    private static final String ICON_PATH = "org/graalvm/visualvm/sources/resources/sources.png"; // NOI18N
    
    
    private final InternalSourceViewerComponent viewerComponent;
    
    
    static void showSource(String uri, String text, int offset, InternalSourceAppearance appearance) {
        String file = SourceHandle.simpleUri(uri);
        InternalSourceViewerTopComponent container = findOpened(file);
        
        if (container == null) {
            container = new InternalSourceViewerTopComponent(file, text, offset, appearance);
            container.open();
        } else {
            container.setOffset(offset);
        }
        
        container.requestActive();
    }
    
    private static InternalSourceViewerTopComponent findOpened(String file) {
        for (TopComponent opened : WindowManager.getDefault().getRegistry().getOpened())
            if (opened instanceof InternalSourceViewerTopComponent && file.equals(opened.getToolTipText()))
                return (InternalSourceViewerTopComponent)opened;
        return null;
    }
    
    
    private InternalSourceViewerTopComponent(String file, String text, int offset, InternalSourceAppearance appearance) {
        super();
        
        setDisplayName(new File(file).getName());
        setIcon(ImageUtilities.loadImage(ICON_PATH, true));
        setToolTipText(file);
        
        viewerComponent = new InternalSourceViewerComponent(text, offset, appearance);
        
        setLayout(new BorderLayout());
        add(viewerComponent, BorderLayout.CENTER);
    }
    
    
    private void setOffset(int offset) {
        viewerComponent.setOffset(offset);
    }
    
    
    protected void componentClosed() {
        super.componentClosed();
        viewerComponent.cleanup();
    }
    
    
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }
    
    protected String preferredID() {
        return getDisplayName();
    }
    
    public HelpCtx getHelpCtx() {
        return null;
    }
    
    
    private Component lastFocusOwner;
    
    private final PropertyChangeListener focusListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
            Component c = evt.getNewValue() instanceof Component ?
                    (Component)evt.getNewValue() : null;
            processFocusedComponent(c);
        }
        private void processFocusedComponent(Component c) {
            Component cc = c;
            while (c != null) {
                if (c == InternalSourceViewerTopComponent.this) {
                    lastFocusOwner = cc;
                    return;
                }
                c = c.getParent();
            }
        }
    };
    
    protected void componentActivated() {
        super.componentActivated();
        if (lastFocusOwner != null) {
            lastFocusOwner.requestFocus();
        } else {
            Component defaultFocusOwner = defaultFocusOwner();
            if (defaultFocusOwner != null) defaultFocusOwner.requestFocus();
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addPropertyChangeListener("focusOwner", focusListener);         // NOI18N
    }

    protected void componentDeactivated() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().
                removePropertyChangeListener("focusOwner", focusListener);      // NOI18N
        super.componentDeactivated();
    }
    
    protected Component defaultFocusOwner() {
        return viewerComponent.defaultFocusOwner();
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
    }
    
}
