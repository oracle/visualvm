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

package org.graalvm.visualvm.host.impl;

import org.graalvm.visualvm.core.properties.PropertiesPanel;
import org.graalvm.visualvm.core.properties.PropertiesProvider;
import org.graalvm.visualvm.host.Host;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.net.InetAddress;
import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jiri Sedlacek
 */
public class GeneralPropertiesProvider extends PropertiesProvider<Host> {

    public GeneralPropertiesProvider() {
        super(NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_HostProperties"), // NOI18N
              NbBundle.getMessage(GeneralPropertiesProvider.class, "DESCR_HostProperties"), // NOI18N
              CATEGORY_GENERAL, 50);
    }


    public PropertiesPanel createPanel(final Host dataSource) {
        PropertiesPanel panel = new PropertiesPanel();
        panel.setLayout(new BorderLayout());
        final JTextArea textArea = new JTextArea() {
            public Dimension getMinimumSize() {
                Dimension prefSize = getPreferredSize();
                Dimension minSize = super.getMinimumSize();
                prefSize.width = 0;
                if (minSize.height < prefSize.height) return prefSize;
                else return minSize;
            }
        };
        textArea.setBorder(BorderFactory.createEmptyBorder());
        textArea.setOpaque(false);
        // Nimbus LaF doesn't respect setOpaque(false), this is a workaround.
        // May cause delays for remote X sessions due to color transparency.
        if (UIManager.getLookAndFeel().getID().equals("Nimbus")) // NOI18N
            textArea.setBackground(new Color(0, 0, 0, 0));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        String resolving = NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_Resolving"); // NOI18N
        updateProperties(textArea, resolving, resolving);
        textArea.setMinimumSize(new Dimension(1, 1));
        panel.add(textArea, BorderLayout.CENTER);
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                InetAddress address = dataSource.getInetAddress();
                final String hostname = address.getCanonicalHostName();
                final String ip = address.getHostAddress();
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        updateProperties(textArea, hostname, ip);
                    }
                });
            }
        });
        return panel;
    }

    private static void updateProperties(JTextArea textArea, String hostname, String ip) {
        String dnsName = NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_DnsName"); // NOI18N
        String ipAddress = NbBundle.getMessage(GeneralPropertiesProvider.class, "LBL_IpAddress"); // NOI18N
        textArea.setText(dnsName + " " + hostname + "\n" + ipAddress + " " + ip); // NOI18N
        textArea.setCaretPosition(0);
    }


    public boolean supportsDataSource(Host dataSource) { return dataSource != null; }

    public void propertiesDefined(PropertiesPanel panel, Host dataSource) {}

    public void propertiesChanged(PropertiesPanel panel, Host dataSource) {}

    public void propertiesCancelled(PropertiesPanel panel, Host dataSource) {}

}
