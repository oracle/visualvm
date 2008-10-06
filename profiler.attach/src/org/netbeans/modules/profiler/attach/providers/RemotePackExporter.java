/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.attach.providers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.tools.ant.module.api.AntProjectCookie;
import org.apache.tools.ant.module.api.AntTargetExecutor;
import org.apache.tools.ant.module.spi.AntEvent;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.common.integration.IntegrationUtils;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.modules.InstalledFileLocator;

/**
 *
 * @author Jaroslav Bachorik
 */
public class RemotePackExporter {

    private static final class Singleton {

        private static final RemotePackExporter INSTANCE = new RemotePackExporter();
    }
    private static final Map<String, String> scriptMapper = new HashMap<String, String>() {
        {
            put(IntegrationUtils.PLATFORM_LINUX_AMD64_OS, "linuxamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_LINUX_OS, "linux"); //NOI18N
            put(IntegrationUtils.PLATFORM_MAC_OS, "mac"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_AMD64_OS, "solamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_INTEL_OS, "solx86"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC_OS, "solsparc"); //NOI18N
            put(IntegrationUtils.PLATFORM_SOLARIS_SPARC64_OS, "solsparcv9"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_AMD64_OS, "winamd64"); //NOI18N
            put(IntegrationUtils.PLATFORM_WINDOWS_OS, "win"); //NOI18N
        }
    };
    private static final Map<String, String> jdkMapper = new HashMap<String, String>() {
        {
            put(TargetPlatformEnum.JDK5.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK6.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK7.toString(), "15"); //NOI18N
            put(TargetPlatformEnum.JDK_CVM.toString(), "cvm"); //NOI18N
        }
    };
    private AntProjectCookie cookie;

    public static final RemotePackExporter getInstance() {
        return Singleton.INSTANCE;
    }

    private RemotePackExporter() throws ExceptionInInitializerError {
        try {
            File antFile = InstalledFileLocator.getDefault().locate("remote-pack-defs/build.xml", "org-netbeans-lib-profiler", false); //NOI18N
            cookie = DataObject.find(FileUtil.toFileObject(antFile)).getCookie(AntProjectCookie.class);
        } catch (DataObjectNotFoundException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public String export(final String exportPath, final String hostOS, final String jvm) throws IOException {
        String packPath = getRemotePackPath(exportPath, hostOS);
        ProgressHandle ph = ProgressHandleFactory.createHandle("Generating Remote Pack to " + packPath);
        ph.setInitialDelay(500);
        ph.start();
        try {
            AntTargetExecutor.Env env = new AntTargetExecutor.Env();
            env.setVerbosity(AntEvent.LOG_VERBOSE);
            Properties antProperties = new Properties();
            antProperties.setProperty("lib.dir", "../lib");

            antProperties.setProperty("dest.dir", exportPath != null ? exportPath : System.getProperty("java.io.tmpdir")); //NOI18N
            env.setProperties(antProperties);
            AntTargetExecutor ate = AntTargetExecutor.createTargetExecutor(env);
            ate.execute(cookie, new String[]{"profiler-server-" + scriptMapper.get(hostOS) + "-" + jdkMapper.get(jvm)}).result();            
        } finally {
            ph.finish();
        }
        return packPath;
    }

    public String getRemotePackPath(String exportPath, String hostOS) {
        return exportPath + File.separator + "profiler-server-" + scriptMapper.get(hostOS) + ".zip";
    }

    public void export(String hostOS, final String jvm) throws IOException {
        export(null, hostOS, jvm);
    }
}
