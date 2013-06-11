/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.global.Platform;
import org.netbeans.lib.profiler.utils.MiscUtils;
import org.netbeans.modules.profiler.spi.JavaPlatformManagerProvider;
import org.netbeans.modules.profiler.spi.JavaPlatformProvider;
import org.openide.util.Lookup;

/**
 * JavaPlatform describes a java platform in a way that the profiler tools may utilize. It may serve as
 * description of the platform a profiler targets, or it may provide access to tools from the
 * particular SDK installation. It also provides information about individual platforms, for example
 * the Java platform version implemented, vendor name or implementation version.
 *
 * @author Tomas Hurka
 */

public final class JavaPlatform {
    
    private final JavaPlatformProvider provider;
    
    /**
     * finds platform with specified platform id.
     * @param platformId unique id of the platform
     * @return platform which has plarformId as unique id
     * or <code>null</code> if the is no such platform
     */
    public static JavaPlatform getJavaPlatformById(String platformId) {
        if (platformId != null) {
            List<JavaPlatform> platforms = getPlatforms();

            for (JavaPlatform platform : platforms) {
                if (platformId.equals(platform.getPlatformId())) {
                    return platform;
                }
            }
        }
        return null;
    }
    
    /** Gets an list of JavaPlatfrom objects suitable for profiling.
     * @return the array of java platform definitions.
     */
    public static List<JavaPlatform> getPlatforms() {
        List<JavaPlatformProvider> platformProviders = provider().getPlatforms();
        List<JavaPlatform> platforms = new ArrayList(platformProviders.size());
        
        for (JavaPlatformProvider p : platformProviders) {
            if (p.getPlatformJavaFile() != null &&
                MiscUtils.isSupportedJVM(p.getSystemProperties())) {
                platforms.add(new JavaPlatform(p));
            }
        }
        return Collections.unmodifiableList(platforms);
    }

    /**
     * Get the "default platform", meaning the JDK on which profiler itself is running.
     * @return the default platform, if it can be found, or null
     */
    public static JavaPlatform getDefaultPlatform() {
        return new JavaPlatform(provider().getDefaultPlatform());        
    }
    
    /**
     * Shows java platforms customizer
     */
    public static void showCustomizer() {
        provider().showCustomizer();                
    }
    
    private static JavaPlatformManagerProvider provider() {
        return Lookup.getDefault().lookup(JavaPlatformManagerProvider.class);
    }
    
    JavaPlatform(JavaPlatformProvider p) {
        provider = p;
    }
    
    /**
     * @return  a descriptive, human-readable name of the platform
     */
    public String getDisplayName() {
        return provider.getDisplayName();
    }

    /**
     * @return  a unique name of the platform
     */
    public String getPlatformId() {
        return provider.getPlatformId();
    }
    
    /**
     * Returns the minor version of the Java SDK
     * @return String
     */
    public int getPlatformJDKMinor() {
        return Platform.getJDKMinorNumber(getVersion());
    }
    
    /** Gets a version for JavaPlatform.
     *
     * @return Java version string
     * @see CommonConstants.JDK_15_STRING
     * @see CommonConstants.JDK_16_STRING
     * @see CommonConstants.JDK_17_STRING
     * @see CommonConstants.JDK_18_STRING
     */
    public String getPlatformJDKVersion() {
        String ver = getVersion();

        if (ver == null) {
            return null;
        }

        if (ver.startsWith("1.5")) {
            return CommonConstants.JDK_15_STRING; // NOI18N
        } else if (ver.startsWith("1.6")) {
            return CommonConstants.JDK_16_STRING; // NOI18N
        } else if (ver.startsWith("1.7")) {
            return CommonConstants.JDK_17_STRING; // NOI18N
        } else if (ver.startsWith("1.8")) {
            return CommonConstants.JDK_18_STRING; // NOI18N
        } else {
            return null;
        }
    }

    /** Gets a path to java executable for specified platform. The platform passed cannot be null.
     * Errors when obtaining the java executable will be reported to the user and null will be returned.
     *
     * @param platform A JavaPlatform for which we need the java executable path
     * @return A path to java executable or null if not found
     */
    public String getPlatformJavaFile() {
        return provider.getPlatformJavaFile();
    }
    
    /** Gets the java platform system properties.
     * @return the java platform system properties
     */
    public Map<String,String> getSystemProperties() {
        return provider.getSystemProperties();
    }

    /** Gets the java platform architecture.
     * @return the java platform architecture - 32 or 64
     */
    public int getPlatformArchitecture() {
        String arch = getSystemProperties().get("sun.arch.data.model"); // NOI18N

        if (arch == null) {
            return 32;
        }
        return Integer.parseInt(arch);
    }
    
    /**
     * Returns the version of the Java SDK
     * @return String
     */
    public String getVersion() {
        return getSystemProperties().get("java.version");
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JavaPlatform) {
            return getPlatformId().equals(((JavaPlatform)obj).getPlatformId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getPlatformId().hashCode();
    }
    
}
