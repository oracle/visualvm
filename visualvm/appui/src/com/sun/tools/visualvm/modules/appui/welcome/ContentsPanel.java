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

package com.sun.tools.visualvm.modules.appui.welcome;

import com.sun.tools.visualvm.core.ui.DesktopUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import org.openide.awt.StatusDisplayer;
import org.netbeans.lib.profiler.ui.components.HTMLTextArea;

/**
 *
 * @author Jiri Sedlacek
 */
class ContentsPanel extends JPanel implements Constants {
    
    public ContentsPanel() {
        initComponents();
    }
    
    
    private void initComponents() {
        HTMLTextArea welcomeArea = new HTMLTextArea(getText()) {
            protected void showURL(URL url) {
                if (DesktopUtils.isBrowseAvailable()) {
                    try {
                        DesktopUtils.browse(url.toURI());
                    } catch (Exception e) {}
                }
            }
            public void fireHyperlinkUpdate(HyperlinkEvent e) {
                EventType type = e.getEventType();

                if (type == EventType.ENTERED)
                    StatusDisplayer.getDefault().setStatusText(
                            e.getURL().toExternalForm().replace("%20", " "));
                else if (type == EventType.EXITED)
                    StatusDisplayer.getDefault().setStatusText("");

                super.fireHyperlinkUpdate(e);
            }
        };
        welcomeArea.setOpaque(true);
        welcomeArea.setForeground(Color.BLACK);
        welcomeArea.setBackground(Utils.getColor(COLOR_CONTENT_BACKGROUND));
        welcomeArea.setBorder(BorderFactory.createEmptyBorder());

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Utils.getColor(BORDER_COLOR)));
        add(welcomeArea, BorderLayout.CENTER);
    }

    private static String getText() {
        Color background = Utils.getColor(COLOR_CONTENT_BACKGROUND);
        String backgroundText = "rgb(" + background.getRed() + ","
                                       + background.getGreen() + ","
                                       + background.getBlue() + ")"; //NOI18N

        StringBuilder b = new StringBuilder();

        b.append("<div bgcolor='" + backgroundText + "' style='padding:10px;'>");

        b.append("<div><b>VisualVM 1.2 Test Build </b>is a public development build for testing<br>and evaluation purposes. See the <a href='https://visualvm.dev.java.net/relnotes12tb.html'>Release notes</a> for details.</div><br>");
        b.append("<div>If you find a bug or have any feedback please let the developers<br>know on a <a href='mailto:feedback@visualvm.dev.java.net?subject=VisualVM%201.2%20Test%20Build%20Feedback'>mailing list</a>. You may also file a <a href='https://visualvm.dev.java.net/issues/enter_bug.cgi?issue_type=DEFECT'>bug report</a>.</div><br>");
        b.append("<div><nobr>VisualVM 1.2 will be released soon at <a href='https://visualvm.dev.java.net'>https://visualvm.dev.java.net</a>!</nobr></div>");

        b.append("</div>");

        return b.toString();
    }

}
