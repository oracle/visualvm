/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.jfr.views.components;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.graalvm.visualvm.core.ui.components.ScrollableContainer;

/**
 *
 * @author Jiri Sedlacek
 */
public final class MessageComponent extends JPanel {
    
    public MessageComponent(String message) {
        JLabel notSupportedLabel = new JLabel(message, SwingConstants.CENTER);    // NOI18N
        notSupportedLabel.setEnabled(false);

        setLayout(new BorderLayout());
        setOpaque(false);
        
        add(notSupportedLabel, BorderLayout.CENTER);
    }
    
    public static JComponent notAvailable() {
        return new MessageComponent("Not available for this JFR snapshot.");
    }
    
    public static JComponent scrollable(String message) {
        MessageComponent cm = new MessageComponent(message);
        cm.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return new ScrollableContainer(cm);
    }
    
    public static JComponent noData(String viewName, String[] eventTypes) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String eventType : eventTypes) {
            if (!first) sb.append(", ");
            else first = false;
            sb.append(eventType);
        }
        
        return scrollable("<html><b>No " + viewName + " data recorded.</b><br><br><br>" +
                          "To analyze the " + viewName + " data make sure the JFR snapshot contains events of the following type(s):<br><br>" +
                          "<code>" + sb.toString() + "</code></html>");
    }
    
}
