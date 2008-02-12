/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.sun.tools.visualvm.modules.appui.about;

import com.sun.tools.visualvm.core.ui.DesktopUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;
import org.netbeans.lib.profiler.ui.components.ImagePanel;

/**
 *
 * @author Jiri Sedlacek
 */
public class AboutDialogPanel extends JPanel {


    // --- Internal API --------------------------------------------------------

    AboutDialogPanel() {
        initComponents();

        final Runnable repainter = new Runnable() {
            public void run() { AboutDialogPanel.this.repaint(); }
        };
        
        addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                    SwingUtilities.invokeLater(repainter);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { SwingUtilities.invokeLater(repainter); }
                    });
                }
            }
        });
    }
    
    void updateAppearance() {
        String message = AboutDialog.getInstance().getMessage();
        String htmlMessage = AboutDialog.getInstance().getHTMLMessage();
                
        licenseArea.setVisible(message != null);
        if (message != null) {
            if (htmlMessage != null && DesktopUtils.isBrowseAvailable()) {
                licenseArea.setText(htmlMessage);
            } else {
                licenseArea.setText(message);
            }
        }
    }
    
    
    // --- Private implementation ----------------------------------------------

    private void initComponents() {
        JPanel splashImageContainer = new ImagePanel(AboutDialog.getInstance().getAboutImage());
        
        JSeparator separator = new JSeparator() {
            public Dimension getMinimumSize() { return getPreferredSize(); }
            public Dimension getMaximumSize() { return getPreferredSize(); }
        };

        licenseArea = new HTMLTextArea() {
            protected void showURL(URL url) {
                if (DesktopUtils.isBrowseAvailable()) {
                    try {
                        DesktopUtils.browse(url.toURI());
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            }
        };
        licenseArea.setOpaque(true);
        licenseArea.setBackground(new Color(255, 255, 255, 80));
        licenseArea.setForeground(Color.BLACK);
        licenseArea.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        licenseArea.setCaret(new NullCaret());
        licenseArea.setShowPopup(false);
        licenseArea.setFocusable(false);

        splashImageContainer.setLayout(new BorderLayout());
        splashImageContainer.add(licenseArea, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(splashImageContainer, BorderLayout.CENTER);
        add(separator, BorderLayout.SOUTH);
    }
    
    private HTMLTextArea licenseArea;

    private static final class NullCaret implements Caret {
        public void install(javax.swing.text.JTextComponent c) {}
        public void deinstall(javax.swing.text.JTextComponent c) {}
        public void paint(Graphics g) {}
        public void addChangeListener(ChangeListener l) {}
        public void removeChangeListener(ChangeListener l) {}
        public boolean isVisible() { return false; }
        public void setVisible(boolean v) {}
        public boolean isSelectionVisible() { return false; }
        public void setSelectionVisible(boolean v) {}
        public void setMagicCaretPosition(Point p) {}
        public Point getMagicCaretPosition() { return new Point(0, 0); }
        public void setBlinkRate(int rate) {}
        public int getBlinkRate() { return 0; }
        public int getDot() { return 0; }
        public int getMark() { return 0; }
        public void setDot(int dot) {}
        public void moveDot(int dot) {}
    }
}
