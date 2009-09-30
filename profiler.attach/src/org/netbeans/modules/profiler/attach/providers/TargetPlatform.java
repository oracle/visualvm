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

package org.netbeans.modules.profiler.attach.providers;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.modules.SpecificationVersion;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TargetPlatform {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public interface TargetPlatformFilter {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isSupported(TargetPlatform javaPlatform);
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static List supportedPlatforms = null;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private SpecificationVersion version;
    private String displayName;
    private String javaHome;
    private boolean defaultFlag;
    private boolean validFlag;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TargetPlatform(final JavaPlatform platform) {
        this(platform, false);
    }

    public TargetPlatform(final JavaPlatform platform, final boolean setDefault) {
        this.defaultFlag = setDefault;

        try {
            this.displayName = platform.getDisplayName();
            this.version = platform.getSpecification().getVersion();

            FileObject folder = platform.getInstallFolders().iterator().next();
            final String hostOS = System.getProperty("os.name"); // NOI18N
            this.javaHome = URLDecoder.decode(folder.getURL().getPath(), "utf8"); // NOI18N

            if (this.javaHome.endsWith("/")) { // NOI18N
                this.javaHome = this.javaHome.substring(0, this.javaHome.length() - 1);
            }

            if (hostOS.contains("Windows") && this.javaHome.startsWith("/")) { // NOI18N
                this.javaHome = this.javaHome.substring(1).replace('/', '\\'); // NOI18N
            }

            validFlag = true;
        } catch (FileStateInvalidException ex) {
            validFlag = false;
        } catch (UnsupportedEncodingException e) {
            validFlag = false;
        } catch (Exception e) {
            validFlag = false;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static final List getPlatformList(boolean caching) {
        List platforms = getSupportedPlatforms(caching);

        return platforms;
    }

    public static final List getPlatformList(TargetPlatformFilter filter, boolean caching) {
        final List platformList = getSupportedPlatforms(caching);
        List newList = new Vector(); // IMPORTANT - at least as long as using DefaultComboBoxModel in JavaPlatformPanelComponent

        for (Iterator it = platformList.iterator(); it.hasNext();) {
            TargetPlatform platform = (TargetPlatform) it.next();

            if (filter.isSupported(platform)) {
                newList.add(platform);
            }
        }

        return newList;
    }

    public static void refresh() {
        getSupportedPlatforms(false);
    }

    public TargetPlatformEnum getAsEnum() {
        if (this.version.toString().startsWith("1.5") || this.version.toString().startsWith("5")) { // NOI18N

            return TargetPlatformEnum.JDK5;
        }

        if (this.version.toString().startsWith("1.6") || this.version.toString().startsWith("6")) { // NOI18N

            return TargetPlatformEnum.JDK6;
        }

        if (this.version.toString().startsWith("1.7") || this.version.toString().startsWith("7")) { // NOI18N

            return TargetPlatformEnum.JDK7;
        }

        return TargetPlatformEnum.JDK5;
    }

    public boolean isDefault() {
        return defaultFlag;
    }

    public String getHomePath() {
        return this.javaHome;
    }

    public String getName() {
        return getAsEnum().toString();
    }

    public boolean isValid() {
        return validFlag;
    }

    public boolean equals(Object otherPlatform) {
        if ((this.getHomePath() == null) || (otherPlatform == null)) {
            return false; // fail early
        }

        if (otherPlatform instanceof String) {
            return this.getHomePath().equals(otherPlatform) || this.toString().equals(otherPlatform);
        }

        if (!(otherPlatform instanceof TargetPlatform)) {
            return false;
        }

        return (this.getHomePath().equals(((TargetPlatform) otherPlatform).getHomePath()));
    }

    public int hashCode() {
        return this.getHomePath().hashCode();
    }

    public String toString() {
        return this.displayName;
    }

    private static List getSupportedPlatforms(final boolean cached) {
        if ((supportedPlatforms == null) || !cached) {
            supportedPlatforms = new LinkedList();

            try {
                JavaPlatform defaultPlatform = JavaPlatformManager.getDefault().getDefaultPlatform();

                if (defaultPlatform != null) {
                    supportedPlatforms.add(new TargetPlatform(defaultPlatform, true));
                }
            } catch (Exception e) {
                // IGNORE
                e.printStackTrace();
            }

            JavaPlatform[] platforms = JavaPlatformManager.getDefault().getPlatforms(null, new Specification("j2se", null)); // NOI18N

            for (int i = 0; i < platforms.length; i++) {
                TargetPlatform platform = new TargetPlatform(platforms[i]);

                if (!platform.isValid() || supportedPlatforms.contains(platform)) {
                    continue;
                }

                supportedPlatforms.add(platform);
            }
        }

        return supportedPlatforms;
    }
}
