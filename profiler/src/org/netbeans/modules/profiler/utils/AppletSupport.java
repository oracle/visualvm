/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.profiler.utils;

import org.netbeans.api.java.classpath.*;
import org.netbeans.api.java.platform.*;
import org.openide.*;
import org.openide.filesystems.*;
import org.openide.modules.SpecificationVersion;
import org.openide.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.netbeans.modules.profiler.projectsupport.utilities.SourceUtils;


/**
 * Support for execution of applets.
 *
 * @author Tomas Hurka
 * @author Ales Novak, Martin Grebac
 */
public class AppletSupport {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // JDK issue #6193279: Appletviewer does not accept encoded URLs
    private static final SpecificationVersion JDK_15 = new SpecificationVersion("1.5"); // NOI18N

    /**
     * constant for html extension
     */
    private static final String HTML_EXT = "html"; // NOI18N

    /**
     * constant for class extension
     */
    private static final String CLASS_EXT = "class"; // NOI18N
    private static final String POLICY_FILE_NAME = "applet"; // NOI18N
    private static final String POLICY_FILE_EXT = "policy"; // NOI18N

    // Used only from unit tests to suppress detection of applet. If value
    // is different from null it will be returned instead.
    public static Boolean unitTestingSupport_isApplet = null;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private AppletSupport() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static boolean isApplet(FileObject file) {
        if (file == null) {
            return false;
        }

        // support for unit testing
        if (unitTestingSupport_isApplet != null) {
            return unitTestingSupport_isApplet.booleanValue();
        }

        return SourceUtils.isApplet(file);
    }

    /**
     * @return URL of the html file with the same name as sibling
     */
    public static URL generateHtmlFileURL(FileObject appletFile, FileObject buildDir, FileObject classesDir, String activePlatform)
                                   throws FileStateInvalidException {
        FileObject html = null;
        IOException ex = null;

        if ((appletFile == null) || (buildDir == null) || (classesDir == null)) {
            return null;
        }

        try {
            html = generateHtml(appletFile, buildDir, classesDir);
        } catch (IOException iex) {
            ex = iex;
        }

        URL url = null;

        try {
            if (ex == null) {
                // JDK issue #6193279: Appletviewer does not accept encoded URLs
                JavaPlatformManager pm = JavaPlatformManager.getDefault();
                JavaPlatform platform = null;

                if (activePlatform == null) {
                    platform = pm.getDefaultPlatform();
                } else {
                    JavaPlatform[] installedPlatforms = pm.getPlatforms(null, new Specification("j2se", null)); //NOI18N

                    for (int i = 0; i < installedPlatforms.length; i++) {
                        String antName = (String) installedPlatforms[i].getProperties().get("platform.ant.name"); //NOI18N

                        if ((antName != null) && antName.equals(activePlatform)) {
                            platform = installedPlatforms[i];

                            break;
                        }
                    }
                }

                boolean workAround6193279 = (platform != null //In case of nonexisting platform don't use the workaround
                ) && (platform.getSpecification().getVersion().compareTo(JDK_15) >= 0); //JDK1.5 and higher

                if (workAround6193279) {
                    File f = FileUtil.toFile(html);

                    try {
                        url = new URL("file", null, f.getAbsolutePath()); // NOI18N
                    } catch (MalformedURLException e) {
                        ErrorManager.getDefault().notify(e);
                    }
                } else {
                    url = html.getURL();
                }
            }
        } catch (FileStateInvalidException f) {
            throw new FileStateInvalidException();
        }

        return url;
    }

    /**
     * @return html file with the same name as applet
     */
    public static FileObject generateSecurityPolicy(FileObject projectDir, FileObject buildDir) {
        FileObject policyFile = buildDir.getFileObject(POLICY_FILE_NAME, POLICY_FILE_EXT);

        try {
            if (policyFile == null) {
                FileObject original = projectDir.getFileObject(POLICY_FILE_NAME, POLICY_FILE_EXT);

                if (original != null) {
                    policyFile = FileUtil.copyFile(original, buildDir, POLICY_FILE_NAME, POLICY_FILE_EXT);
                } else {
                    policyFile = buildDir.createData(POLICY_FILE_NAME, POLICY_FILE_EXT);

                    FileLock lock = policyFile.lock();
                    PrintWriter writer = null;

                    try {
                        writer = new PrintWriter(policyFile.getOutputStream(lock));
                        fillInPolicyFile(writer);
                    } finally {
                        lock.releaseLock();

                        if (writer != null) {
                            writer.close();
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            ErrorManager.getDefault().log(ErrorManager.INFORMATIONAL, "Failed to generate applet policy file: " + ioe); //NOI18N
        }

        return policyFile;
    }

    /**
     * fills in file with html source so it is html file with applet
     *
     * @param name is name of the applet
     */
    private static void fillInFile(PrintWriter writer, String name, String codebase) {
        ResourceBundle bundle = NbBundle.getBundle(AppletSupport.class);

        writer.println("<HTML>"); // NOI18N
        writer.println("<HEAD>"); // NOI18N

        writer.print("   <TITLE>"); // NOI18N
        writer.print(bundle.getString("GEN_title")); // NOI18N
        writer.println("</TITLE>"); // NOI18N

        writer.println("</HEAD>"); // NOI18N
        writer.println("<BODY>\n"); // NOI18N

        writer.print(bundle.getString("GEN_warning")); // NOI18N

        writer.print("<H3><HR WIDTH=\"100%\">"); // NOI18N
        writer.print(bundle.getString("GEN_header")); // NOI18N
        writer.println("<HR WIDTH=\"100%\"></H3>\n"); // NOI18N

        writer.println("<P>"); // NOI18N
                               //        String codebase = getCodebase (name);

        if (codebase == null) {
            writer.print("<APPLET code="); // NOI18N
        } else {
            writer.print("<APPLET " + codebase + " code="); // NOI18N
        }

        writer.print("\""); // NOI18N

        writer.print(name);
        writer.print("\""); // NOI18N

        writer.println(" width=350 height=200></APPLET>"); // NOI18N
        writer.println("</P>\n"); // NOI18N

        writer.print("<HR WIDTH=\"100%\"><FONT SIZE=-1><I>"); // NOI18N
        writer.print(bundle.getString("GEN_copy")); // NOI18N
        writer.println("</I></FONT>"); // NOI18N

        writer.println("</BODY>"); // NOI18N
        writer.println("</HTML>"); // NOI18N
        writer.flush();
    }

    /**
     * fills in policy file with all permissions granted
     *
     * @param writer is a file to be filled
     */
    private static void fillInPolicyFile(PrintWriter writer) {
        writer.println("grant {"); // NOI18N
        writer.println("permission java.security.AllPermission;"); // NOI18N
        writer.println("};"); // NOI18N
        writer.flush();
    }

    //  public static String getAppletClassName(FileObject fo) {
    //    boolean isApplet = false;
    //    if (SourceUtils.isInstanceOf(fo, "java.applet.Applet") || SourceUtils.isInstanceOf(fo, "javax.swing.Applet")) { // NOI18N
    //      return SourceUtils.getToplevelClassName(fo);
    //    }
    ////    JavaMetamodel.getDefaultRepository().beginTrans(false);
    ////    try {
    ////      JavaModel.setClassPath(fo); // will limit the classpath to this project only => can be faster
    ////      Resource res = JavaModel.getResource(fo);
    ////      JavaClass[] classes = (JavaClass[])res.getClassifiers().toArray(new JavaClass[0]);
    ////      JavaClass applet = (JavaClass) JavaModel.getDefaultExtent().getType().resolve("java.applet.Applet"); // NOI18N
    ////      JavaClass jApplet = (JavaClass) JavaModel.getDefaultExtent().getType().resolve("javax.swing.JApplet"); // NOI18N
    ////
    ////      for (int i = 0; i < classes.length; i++) {
    ////        JavaClass javaClass = classes[i];
    ////        if (!(javaClass instanceof UnresolvedClass)) {
    ////          if (javaClass.isSubTypeOf(applet) || (javaClass.isSubTypeOf(jApplet))) {
    ////            return javaClass.getName();
    ////          }
    ////        }
    ////      }
    ////      return null;
    ////    } finally {
    ////      JavaMetamodel.getDefaultRepository().endTrans();
    ////    }
    //    return null;
    //  }

    /**
     * @return html file with the same name as applet
     */
    private static FileObject generateHtml(FileObject appletFile, FileObject buildDir, FileObject classesDir)
                                    throws IOException {
        FileObject htmlFile = buildDir.getFileObject(appletFile.getName(), HTML_EXT);

        if (htmlFile == null) {
            htmlFile = buildDir.createData(appletFile.getName(), HTML_EXT);
        }

        FileLock lock = htmlFile.lock();
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(htmlFile.getOutputStream(lock));

            ClassPath cp = ClassPath.getClassPath(appletFile, ClassPath.EXECUTE);
            ClassPath sp = ClassPath.getClassPath(appletFile, ClassPath.SOURCE);
            String path = FileUtil.getRelativePath(sp.findOwnerRoot(appletFile), appletFile);
            path = path.substring(0, path.length() - 5);
            fillInFile(writer, path + "." + CLASS_EXT, "codebase=\"" + classesDir.getURL() + "\""); // NOI18N
        } finally {
            lock.releaseLock();

            if (writer != null) {
                writer.close();
            }
        }

        return htmlFile;
    }
}
