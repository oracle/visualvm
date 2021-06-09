/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.ui.components;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.openide.util.NbBundle;

/**
 * JPanel showing a predefined message.
 *
 * @author Jiri Sedlacek
 */
public final class NotSupportedDisplayer extends JPanel {

    /**
     * Not supported for this application.
     */
    public static final String APPLICATION = NbBundle.getMessage(NotSupportedDisplayer.class, "MSG_application");   // NOI18N
    /**
     * Not supported for this JVM.
     */
    public static final String JVM = NbBundle.getMessage(NotSupportedDisplayer.class, "MSG_JVM");   // NOI18N
    /**
     * Not supported for this host.
     */
    public static final String HOST = NbBundle.getMessage(NotSupportedDisplayer.class, "MSG_host"); // NOI18N
    /**
     * Not supported for this OS.
     */
    public static final String OS = NbBundle.getMessage(NotSupportedDisplayer.class, "MSG_OS"); // NOI18N

    /**
     * Creates new instance of NotSupportedDisplayer.
     * 
     * @param object type of the not supported object (any string or predefined constant).
     */
    public NotSupportedDisplayer(String object) {
        JLabel notSupportedLabel = new JLabel(NbBundle.getMessage(NotSupportedDisplayer.class, "MSG_Not_supported", object), SwingConstants.CENTER);    // NOI18N
        notSupportedLabel.setEnabled(false);

        setLayout(new BorderLayout());
        setOpaque(false);
        
        add(notSupportedLabel, BorderLayout.CENTER);
    }

}
