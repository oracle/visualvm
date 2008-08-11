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

package com.sun.tools.visualvm.modules.appui.proxysettings;

import com.sun.tools.visualvm.core.ui.DesktopUtils;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.lib.profiler.ui.components.HTMLLabel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jiri Sedlacek
 */
public class ProxySettingsHack {
    
    public static void hackProxySettings() {
        WindowManager.getDefault().invokeWhenUIReady(new Runnable() {
            public void run() {
                Logger logger = Logger.getLogger(OptionsDisplayer.class.getName());
                logger.addHandler(new Handler() {
                    public void flush() {}
                    public void close() throws SecurityException {}
                    public void publish(LogRecord record) {
                        if (record.getLevel() == Level.WARNING && record.getMessage().indexOf("Unknown categoryId: General") != -1) // NOI18N
                            displayProxySettingsWarning();
                    }
                });
            }
        });
    }
    
    private static void displayProxySettingsWarning() {
        String proxySettingsMessage = DesktopUtils.isBrowseAvailable() ?
            NbBundle.getMessage(ProxySettingsHack.class, "ProxySettingsHack_WarningLinkMsg") : // NOI18N
            NbBundle.getMessage(ProxySettingsHack.class, "ProxySettingsHack_WarningMsg"); // NOI18N
        
        final HTMLLabel label = new HTMLLabel(proxySettingsMessage) {
            protected void showURL(URL url) {
                try {
                    DesktopUtils.browse(url.toURI());
                } catch (Exception e) {
                }
            }
        };
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                NotifyDescriptor nd = new NotifyDescriptor.Message(label, NotifyDescriptor.WARNING_MESSAGE); // NOI18N
                DialogDisplayer.getDefault().notify(nd);
            }
        });
    }

}