/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.netbeans.modules.profiler.api.JavaPlatform;


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

    private String version;
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
            String javaExe = platform.getPlatformJavaFile();
            this.displayName = platform.getDisplayName();
            this.version = platform.getVersion();
            this.javaHome = new File(javaExe).getParentFile().getParent();

            validFlag = true;
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
        if (this.version.startsWith("1.5") || this.version.startsWith("5")) { // NOI18N

            return TargetPlatformEnum.JDK5;
        }

        if (this.version.startsWith("1.6") || this.version.startsWith("6")) { // NOI18N

            return TargetPlatformEnum.JDK6;
        }

        if (this.version.startsWith("1.7") || this.version.startsWith("7")) { // NOI18N

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
                JavaPlatform defaultPlatform = JavaPlatform.getDefaultPlatform();

                if (defaultPlatform != null) {
                    supportedPlatforms.add(new TargetPlatform(defaultPlatform, true));
                }
            } catch (Exception e) {
                // IGNORE
                e.printStackTrace();
            }

            List<JavaPlatform> platforms = JavaPlatform.getPlatforms();

            for (JavaPlatform jp : platforms) {
                TargetPlatform platform = new TargetPlatform(jp);

                if (!platform.isValid() || supportedPlatforms.contains(platform)) {
                    continue;
                }

                supportedPlatforms.add(platform);
            }
        }

        return supportedPlatforms;
    }
}
