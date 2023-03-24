/*
 * Copyright (c) 2007, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.modules.appui;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.graalvm.visualvm.modules.appui.about.AboutDialog;
import org.openide.modules.Modules;
import org.openide.modules.Places;
import org.openide.util.Enumerations;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;


public final class AboutAction extends AbstractAction {
    private final static Logger LOGGER = Logger.getLogger(AboutAction.class.getName());
    
    private String versionString = "2.1.6"; // Use "Dev" for development builds   // NOI18N


    public AboutAction() {
        putValue(Action.NAME, NbBundle.getMessage(AboutAction.class, "CTL_AboutAction")); // NOI18N
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }
    
    public void actionPerformed(final ActionEvent e) {
        AboutDialog ad = getAboutDialog();
        if (ad != null) ad.show();
    }
    
    static AboutAction getMenuAction() {
        if (Utilities.isMac()) {
            return null;
        }
        return new AboutAction();
    }
    
    private AboutDialog getAboutDialog() {
        if (aboutDialog == null) {
            try {
                URL imageURL = getClass().getResource("/org/graalvm/visualvm/modules/appui/about/image.png"); // NOI18N
                Image splashImage = Toolkit.getDefaultToolkit().createImage(imageURL);
                aboutDialog = AboutDialog.createInstance(WindowManager.getDefault().getMainWindow(), splashImage);
                aboutDialog.setCaption(NbBundle.getMessage(AboutAction.class, "LBL_About_VisualVM"));   // NOI18N
                aboutDialog.setBuildID(NbBundle.getMessage(AboutAction.class, "LBL_Version_Build", new Object[] {versionString,getBuildNumber()}));
                aboutDialog.setMessage(NbBundle.getMessage(AboutAction.class, "MSG_License") + NbBundle.getMessage(AboutAction.class, "MSG_License1"));
                aboutDialog.setHTMLMessage(NbBundle.getMessage(AboutAction.class, "MSG_Html_License") + NbBundle.getMessage(AboutAction.class, "MSG_Html_License1"));
                aboutDialog.setDetails(getDetails());
                aboutDialog.setLogfile(getLogfile());
            } catch (Exception e) {
                LOGGER.throwing(AboutAction.class.getName(), "getAboutDialog", e);
            }
        }
        
        return aboutDialog;
    }
    
    private String getBuildNumber() {
        if (buildNumber == null) {
            buildNumber = Modules.getDefault().ownerOf(AboutAction.class).getBuildVersion();
        }
        
        return buildNumber;
    }
    
    private String getLogfile() {
        String logfilePath = "var/log/messages.log"; // NOI18N
        File logfile = new File(Places.getUserDirectory(), logfilePath);
        if (logfile.exists() && logfile.isFile() && logfile.canRead()) return logfile.getAbsolutePath();
        else return null;
    }
    
    private String getDetails() {
        if (details == null) {
            StringBuilder sb = new StringBuilder();
            
            sb.append("<table border=\"0\">"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Version: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + versionString + " (Build " + getBuildNumber() + "); platform " + System.getProperty("netbeans.buildnumber") + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>System: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getOSInfo() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Java: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getJavaInfo() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N

            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Vendor: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getJavaVendor() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Environment: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getEnvironment() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>User directory: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getUserDir() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Cache directory: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + Places.getCacheDirectory().getAbsolutePath() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("<tr>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + "<b>Clusters: </b>" + "</td>"); // NOI18N
            sb.append("<td valign=\"top\" nowrap>" + getIDEInstallValue() + "</td>"); // NOI18N
            sb.append("</tr>"); // NOI18N
            
            sb.append("</table>"); // NOI18N
            
            details = sb.toString();
        }
        
        return details;
    }
    
    private static String getOSInfo() {
        Properties systemProperties = System.getProperties();
        String osName = systemProperties.getProperty("os.name", "&lt;not available&gt;"); // NOI18N
        String osVersion = systemProperties.getProperty("os.version", ""); // NOI18N
        String patchLevel = systemProperties.getProperty("sun.os.patch.level", ""); // NOI18N
        String osArch = systemProperties.getProperty("os.arch", "&lt;not available&gt;");   // NOI18N
        String sunArch = systemProperties.getProperty("sun.arch.data.model", "?") + "bit";  // NOI18N
        return osName + " (" + osVersion + ") " + ("unknown".equals(patchLevel) ? "" : patchLevel) + ", " + osArch + " " + sunArch; // NOI18N
    }
    
    private static String getJavaInfo() {
        Properties systemProperties = System.getProperties();
        String javaVersion = systemProperties.getProperty("java.version", "unknown");   // NOI18N
        String vmName = systemProperties.getProperty("java.vm.name", "&lt;not available&gt;");  // NOI18N
        String vmVerison = systemProperties.getProperty("java.vm.version", ""); // NOI18N
        String vmInfo = systemProperties.getProperty("java.vm.info", "");   // NOI18N
        return javaVersion + "; " + vmName + " (" + vmVerison + ", " + vmInfo + ")";
    }

    private static String getJavaVendor() {
        Properties systemProperties = System.getProperties();
        String javaVendor = systemProperties.getProperty("java.vendor", "unknown");   // NOI18N
        String javaVendorUrl = systemProperties.getProperty("java.vendor.url", null);  // NOI18N
        return javaVendor + (javaVendorUrl == null ? "" : ", " + javaVendorUrl); // NOI18N
    }
    
    private static String getEnvironment() {
        String branding = NbBundle.getBranding();
        String encoding = System.getProperty("file.encoding", "unknown");   // NOI18N
        String locale = Locale.getDefault().toString() + (branding == null ? "" : (" (" + branding + ")")); // NOI18N
        return encoding + "; " + locale;
    }
    
    private static String getUserDir() {
        File userdirf = Places.getUserDirectory();
        String userdir = userdirf != null ? userdirf.getAbsolutePath() : null;
        return userdir != null ? userdir : "unknown";   // NOI18N
    }
    
    private static String getIDEInstallValue() {
        String nbhome = System.getProperties().getProperty("netbeans.home");    // NOI18N
        String nbdirs = System.getProperty("netbeans.dirs");    // NOI18N
        
        Enumeration<Object> more;
        if (nbdirs != null) {
            more = new StringTokenizer(nbdirs, File.pathSeparator);
        } else {
            more = Enumerations.empty();
        }
            
        Enumeration<Object> all = Enumerations.concat(Enumerations.singleton(nbhome), more);
        
        Set<File> files = new HashSet<>();
        StringBuilder sb = new StringBuilder ();
        String prefix = "";
        while (all.hasMoreElements ()) {
            String s = (String)all.nextElement ();
            if (s == null) {
                continue;
            }
            File f = (new File(s));
            if (files.add (f)) {
                // new file
                sb.append (prefix);
                try {
                    sb.append(f.getCanonicalPath());
                } catch (IOException e) {
                    sb.append(f.getAbsolutePath());
                }
                prefix = "\n";  // NOI18N
            }
        }
        
        return sb.toString ();
    }
    
    
    private AboutDialog aboutDialog;
    private String buildNumber;
    private String details;
}
