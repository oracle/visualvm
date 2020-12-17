/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.modules.startup.dialogs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import org.openide.util.ImageUtilities;

/**
 * 
 * Utility class to create dialogs displayed on VisualVM startup before the main window is shown.
 * Ensures that the dialog shows on top and its presenter is displayed in Windows taskbar.
 *
 * @author Jiri Sedlacek
 */
public final class StartupDialog {
    
    public static JDialog create(String caption, String message, int messageType) {
        // Bugfix #361, set the JDialog to appear in the Taskbar on Windows (ModalityType.APPLICATION_MODAL)
        Window parent = null;
        for (Window window : Window.getWindows()) {
            if (window.isVisible() && window.isShowing()) {
                parent = window;
                break;
            }
        }
        final JDialog d = parent == null ?
                new JDialog(null, caption, JDialog.ModalityType.APPLICATION_MODAL) :
                new JDialog((Frame)null, caption, true); // NOTE: should a (Frame)parent be used here?
        
        if (message != null) initDialog(d, message, messageType);
        
        // Bugfix #361, JDialog should use the VisualVM icon for better identification
        List<Image> icons = new ArrayList();
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame.gif", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame24.gif", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame32.gif", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame48.gif", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame256.png", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame512.png", true)); // NOI18N
        icons.add(ImageUtilities.loadImage("org/netbeans/core/startup/frame1024.png", true)); // NOI18N
        d.setIconImages(icons);
        
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setResizable(false);
        d.setLocationRelativeTo(null);
        
        // Bugfix #361, ensure that the dialog will be the topmost visible window after opening
        d.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (d.isShowing()) {
                        // For some reason the dialog created with defined JDialog.ModalityType
                        // isn't displayed on Windows when opened by the NetBeans launcher. This
                        // code seems to workaround it while not breaking anything elsewhere.
                        // Disabled to fix jigsaw, the problem is not reproducible using Java7u80@Win7
                        // ComponentPeer peer = d.getPeer();
                        // if (peer != null) peer.setVisible(true);

                        d.removeHierarchyListener(this);
                        d.setAlwaysOnTop(true);
                        d.toFront();
                        d.setAlwaysOnTop(false);
                    }
                }
            }
        });
        
        return d;
    }
    
    private static void initDialog(final JDialog dialog, String message, int messageType) {
        final JOptionPane content = new JOptionPane(message, messageType);
        Container contentPane = dialog.getContentPane();

        contentPane.setLayout(new BorderLayout());
        contentPane.add(content, BorderLayout.CENTER);
        dialog.setResizable(false);
        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
              UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                dialog.setUndecorated(true);
                content.getRootPane().setWindowDecorationStyle(
                        styleFromMessageType(messageType));
            }
        }
        dialog.pack();

        final PropertyChangeListener listener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                // Let the defaultCloseOperation handle the closing
                // if the user closed the window without selecting a button
                // (newValue = null in that case).  Otherwise, close the dialog.
                if (dialog.isVisible() && event.getSource() == content &&
                        (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) &&
                        event.getNewValue() != null &&
                        event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                    dialog.setVisible(false);
                }
            }
        };

        WindowAdapter adapter = new WindowAdapter() {
            private boolean gotFocus = false;
            public void windowClosing(WindowEvent we) {
                content.setValue(null);
            }

            public void windowClosed(WindowEvent e) {
                content.removePropertyChangeListener(listener);
                dialog.getContentPane().removeAll();
            }

            public void windowGainedFocus(WindowEvent we) {
                // Once window gets focus, set initial focus
                if (!gotFocus) {
                    content.selectInitialValue();
                    gotFocus = true;
                }
            }
        };
        dialog.addWindowListener(adapter);
        dialog.addWindowFocusListener(adapter);
        dialog.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                // reset value to ensure closing works properly
                content.setValue(JOptionPane.UNINITIALIZED_VALUE);
            }
        });

        content.addPropertyChangeListener(listener);
    }
    
    private static int styleFromMessageType(int messageType) {
        switch (messageType) {
        case JOptionPane.ERROR_MESSAGE:
            return JRootPane.ERROR_DIALOG;
        case JOptionPane.QUESTION_MESSAGE:
            return JRootPane.QUESTION_DIALOG;
        case JOptionPane.WARNING_MESSAGE:
            return JRootPane.WARNING_DIALOG;
        case JOptionPane.INFORMATION_MESSAGE:
            return JRootPane.INFORMATION_DIALOG;
        case JOptionPane.PLAIN_MESSAGE:
        default:
            return JRootPane.PLAIN_DIALOG;
        }
    }
    
}
