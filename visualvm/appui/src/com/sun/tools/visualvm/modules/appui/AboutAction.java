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
package com.sun.tools.visualvm.modules.appui;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import javax.swing.AbstractAction;
import javax.swing.Action;
import com.sun.tools.visualvm.modules.appui.about.AboutDialog;
import java.util.logging.Logger;
import org.openide.util.Enumerations;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;


public final class AboutAction extends AbstractAction {
    private final static Logger LOGGER = Logger.getLogger(AboutAction.class.getName());
    
    private String versionString = "1.2.1"; // Use "Dev" for development builds   // NOI18N


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
                URL imageURL = getClass().getResource("/com/sun/tools/visualvm/modules/appui/about/image.png"); // NOI18N
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
            buildNumber = "unknown";
            try {
                InputStream manifestStream = getClass().getResourceAsStream("/META-INF/MANIFEST.MF"); // NOI18N
                buildNumber = new Manifest(manifestStream).getMainAttributes().getValue("OpenIDE-Module-Implementation-Version"); // NOI18N
            } catch (IOException ex) {}
        }
        
        return buildNumber;
    }
    
    private String getLogfile() {
        String sep = File.separator;
        String logfilePath = System.getProperty("netbeans.user") + sep + "var" + sep + "log" + sep + "messages.log"; // NOI18N
        File logfile = new File(logfilePath);
        if (logfile.exists() && logfile.isFile() && logfile.canRead()) return logfile.getAbsolutePath();
        else return null;
    }
    
    private String getDetails() {
        if (details == null) {
            StringBuffer sb = new StringBuffer();
            
            sb.append("<table border=\"0\">");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Version: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + versionString + " (Build " + getBuildNumber() + "); platform " + System.getProperty("netbeans.buildnumber") + "</td>");
            sb.append("</tr>");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>System: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + getOSInfo() + "</td>");
            sb.append("</tr>");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Java: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + getJavaInfo() + "</td>");
            sb.append("</tr>");

            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Vendor: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + getJavaVendor() + "</td>");
            sb.append("</tr>");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Environment: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + getEnvironment() + "</td>");
            sb.append("</tr>");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Userdir: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + System.getProperty("netbeans.user", "unknown") + "</td>");
            sb.append("</tr>");
            
            sb.append("<tr>");
            sb.append("<td valign=\"top\" nowrap>" + "<b>Clusters: </b>" + "</td>");
            sb.append("<td valign=\"top\" nowrap>" + getIDEInstallValue() + "</td>");
            sb.append("</tr>");
            
            sb.append("</table>");
            
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
    
    private static String getIDEInstallValue() {
        String nbhome = System.getProperty("netbeans.home");    // NOI18N
        String nbdirs = System.getProperty("netbeans.dirs");    // NOI18N
        
        Enumeration<Object> more;
        if (nbdirs != null) {
            more = new StringTokenizer(nbdirs, File.pathSeparator);
        } else {
            more = Enumerations.empty();
        }
            
        Enumeration<Object> all = Enumerations.concat(Enumerations.singleton(nbhome), more);
        
        Set<File> files = new HashSet<File>();
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
