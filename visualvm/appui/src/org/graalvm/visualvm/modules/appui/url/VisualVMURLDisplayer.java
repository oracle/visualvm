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

package org.graalvm.visualvm.modules.appui.url;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import org.graalvm.visualvm.core.ui.DesktopUtils;
import org.openide.awt.HtmlBrowser;
import org.openide.awt.HtmlBrowser.URLDisplayer;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Hurka
 */
@ServiceProvider(service=URLDisplayer.class,position=1000)
public final class VisualVMURLDisplayer extends URLDisplayer {
    
    public void showURL(final URL url) {
        new SwingWorker<Boolean,Object>() {
            
            protected Boolean doInBackground() throws Exception {
                if (DesktopUtils.isBrowseAvailable()) {
                    try {
                        DesktopUtils.browse(url.toURI());
                        return Boolean.TRUE;
                    } catch (Exception ex) {
                        Logger.getLogger(VisualVMURLDisplayer.class.getName()).log(Level.INFO, "Showing: " + url, ex);
                    }
                }
                return Boolean.FALSE;
            }
            
            protected void done() {
                try {
                    if (!get().booleanValue()) {
                        // Fallback implementation:
                        HtmlBrowser browser = new HtmlBrowser();
                        browser.setURL(url);
                        
                        JFrame frame = new JFrame();
                        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                        frame.getContentPane().add(browser);
                        frame.pack();
                        frame.setVisible(true);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(VisualVMURLDisplayer.class.getName()).log(Level.INFO, "Showing: " + url, ex);
                }
            }
        }.execute();
    }
    
}
