/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.Position;
import javax.swing.text.html.HTMLEditorKit;
import org.graalvm.visualvm.lib.ui.UIUtils;


/**
 * @author Ian Formanek
 */
public class HTMLLabel extends JEditorPane implements HyperlinkListener {

    private int halign = SwingConstants.LEADING;


    public HTMLLabel() {
        this(null);
    }

    public HTMLLabel(String text) {
        setEditorKit(new HTMLEditorKit());
        setEditable(false);
        setOpaque(false);
        setNavigationFilter(new NavigationFilter() {
                public void moveDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
                    super.moveDot(fb, 0, bias);
                }

                public void setDot(NavigationFilter.FilterBypass fb, int dot, Position.Bias bias) {
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

        if (text != null) setText(text);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    public void setOpaque(boolean o) {
        super.setOpaque(o);
        if (UIUtils.isNimbusLookAndFeel() && !o)
            setBackground(new Color(0, 0, 0, 0));
        if (txt != null) setText(txt);
    }
    
    private String txt;

    public void setText(String value) {
        txt = value;
        
        Font font = getFont();
        Color fgColor = getForeground();
        Color bgColor = getBackground();
        
        value = value.replaceAll("\\n\\r|\\r\\n|\\n|\\r", "<br>"); //NOI18N
        value = value.replace("<code>", "<code style=\"font-size: " + font.getSize() + "pt;\">"); //NOI18N
        
        String fgText = "rgb(" + fgColor.getRed() + "," + fgColor.getGreen() + "," + fgColor.getBlue() + ")"; //NOI18N
        String bgText = isOpaque() ? "rgb(" + bgColor.getRed() + "," + bgColor.getGreen() + "," + bgColor.getBlue() + ")" : null; //NOI18N
        
        String alignText = null;
        switch (halign) {
            case SwingConstants.CENTER:
                alignText = "center"; //NOI18N
                break;
            case SwingConstants.RIGHT:
            case SwingConstants.TRAILING:
                alignText = "right"; //NOI18N
                break;
        }
        
        String bodyFlags = "text=\"" + fgText + "\""; //NOI18N
        if (bgText != null) bodyFlags += " bgcolor=\"" + bgText + "\""; //NOI18N
        if (alignText != null) bodyFlags += " align=\"" + alignText + "\""; //NOI18N
        
        super.setText("<html><body " + bodyFlags + " style=\"font-size: " + font.getSize() //NOI18N
                      + "pt; font-family: " + font.getName() + ";\">" + value + "</body></html>"); //NOI18N
    }
    
    public void setForeground(Color fg) {
        super.setForeground(fg);
        if (txt != null) setText(txt);
    }
    
    public void setBackground(Color bg) {
        super.setBackground(bg);
//        setBorder(getBorder());
        if (txt != null) setText(txt);
    }
    
//    public void setBorder(Border b) {
//        Insets i = b == null ? new Insets(0, 0, 0, 0) : b.getBorderInsets(this);
//        if (!isOpaque()) super.setBorder(BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right));
//        else super.setBorder(BorderFactory.createMatteBorder(i.top, i.left, i.bottom, i.right, getBackground()));
//    }
    
    public void setHorizontalAlignment(int alignment) {
        if (alignment == halign) return;
        halign = alignment;
        if (txt != null) setText(txt);
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
