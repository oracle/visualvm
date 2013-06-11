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

package org.netbeans.lib.profiler.server;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


/**
 * Class that contains the premain() method, needed by the java.lang.instrument Java agent
 * mechanism, that we use for "attach on startup" operation with JDK 1.5.
 *
 * @author Tomas Hurka
 * @author  Misha Dmitriev
 */
public class ProfilerActivate15 {
    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        activate(agentArgs, inst, ProfilerServer.ATTACH_DYNAMIC);
    }

    /**
     * This method is called after the VM has been initialized, but before the TA's main() method.
     * A single arguments string passed to it is the "options" string specified to the -javaagent
     * argument, as java -javaagent:jarpath=options. It should contain the communication port number
     * and optional timeout separated by a comma.
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        activate(agentArgs, inst, ProfilerServer.ATTACH_DIRECT);
    }

    private static File getArchiveFile(URL url) {
        String protocol = url.getProtocol();

        if ("jar".equals(protocol)) { //NOI18N            

            String path = url.getPath();
            int index = path.indexOf("!/"); //NOI18N

            if (index >= 0) {
                try {
                    return new File(new URI(path.substring(0, index)));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(url.toString());
                }
            }
        }

        throw new IllegalArgumentException(url.toString());
    }

    private static void activate(String agentArgs, Instrumentation inst, int activateCode) {
        URL classUrl = getSelfClassUrl();
        File jar = getArchiveFile(classUrl);
        String fullJFluidPath = jar.getParent();

        if ((agentArgs == null) || (agentArgs.length() == 0)) { // no options, just load the native library. This is used for remote-pack calibration
            ProfilerServer.loadNativeLibrary(fullJFluidPath, false);

            return;
        }

        int timeOut = 0;
        int commaPos = agentArgs.indexOf(',');

        if (commaPos != -1) { // optional timeout is specified

            String timeOutStr = agentArgs.substring(commaPos + 1, agentArgs.length());

            try {
                timeOut = Integer.parseInt(timeOutStr);
            } catch (NumberFormatException ex) {
                System.err.println("*** Profiler Engine: invalid timeout number specified to premain(): " + timeOutStr); // NOI18N
                System.exit(-1);
            }

            agentArgs = agentArgs.substring(0, commaPos);
        }

        String portStr = agentArgs;
        int portNo = 0;

        try {
            portNo = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            System.err.println("*** Profiler Engine: invalid port number specified to premain(): " + portStr); // NOI18N
            System.exit(-1);
        }
        
        ProfilerServer.loadNativeLibrary(fullJFluidPath, false);
        ProfilerServer.activate(fullJFluidPath, portNo, activateCode, timeOut);
    }

    private static URL getSelfClassUrl() {
        String SELF_CLASS_NAME = "org/netbeans/lib/profiler/server/ProfilerActivate15.class"; // NOI18N
        
        URL classUrl = ClassLoader.getSystemClassLoader().getResource(SELF_CLASS_NAME);
        if (classUrl == null) {
            classUrl = Thread.currentThread().getContextClassLoader().getResource(SELF_CLASS_NAME);
        }
        return classUrl;
    }
}
