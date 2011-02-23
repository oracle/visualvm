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

package com.sun.tools.visualvm.uisupport;

import java.awt.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;


/**
 * Copy of org.netbeans.lib.profiler.ui.components.HTMLLabel to be used in
 * VisualVM tool an plugins.
 *
 * @author Jiri Sedlacek
 */
public class HTMLLabel extends JEditorPane implements HyperlinkListener {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public HTMLLabel() {
        setEditorKit(new javax.swing.text.html.HTMLEditorKit());
        setEditable(false);
        setOpaque(false);
        setNavigationFilter(new NavigationFilter() {
                public void moveDot(FilterBypass fb, int dot, Position.Bias bias) {
                    super.moveDot(fb, 0, bias);
                }

                public void setDot(FilterBypass fb, int dot, Position.Bias bias) {
                    super.setDot(fb, 0, bias);
                }

                public int getNextVisualPositionFrom(JTextComponent text, int pos, Position.Bias bias, int direction,
                                                     Position.Bias[] biasRet)
                                              throws BadLocationException {
                    return 0;
                }
            });
        setFont(UIManager.getFont("Label.font")); //NOI18N
        addHyperlinkListener(this);
    }

    public HTMLLabel(String text) {
        this();
        setText(text);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setText(String value) {
        Font font = getFont();
        Color textColor = getForeground();
        
        value = value.replaceAll("\\n\\r|\\r\\n|\\n|\\r", "<br>"); //NOI18N
        value = value.replace("<code>", "<code style=\"font-size: " + font.getSize() + "pt;\">"); //NOI18N
        
        String colorText = "rgb(" + textColor.getRed() + "," + textColor.getGreen() + "," + textColor.getBlue() + ")"; //NOI18N
        super.setText("<html><body text=\"" + colorText + "\" style=\"font-size: " + font.getSize() + "pt; font-family: " + font.getName() + ";\">" + value
                      + "</body></html>"); //NOI18N
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (!isEnabled()) {
            return;
        }

        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            showURL(e.getURL());
        } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    protected void showURL(URL url) {
        // override to react to URL clicks
    }
}
