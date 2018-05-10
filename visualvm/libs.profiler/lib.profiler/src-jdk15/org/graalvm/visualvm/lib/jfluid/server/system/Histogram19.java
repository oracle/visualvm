/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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
 */
package org.graalvm.visualvm.lib.jfluid.server.system;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 *
 * @author Tomas Hurka
 */
class Histogram19 {
    private static final String DIAGNOSTIC_COMMAND_MXBEAN_NAME =
            "com.sun.management:type=DiagnosticCommand";    // NOI18N
    private static final String ALL_OBJECTS_OPTION = "-all";    // NOI18N
    private static final String HISTOGRAM_COMMAND = "gcClassHistogram";       // NOI18N
    private static MBeanServer mserver;
    private static ObjectName hotspotDiag;

    static boolean initialize() {
        boolean initok = false;
        try {
            mserver = ManagementFactory.getPlatformMBeanServer();
            hotspotDiag = new ObjectName(DIAGNOSTIC_COMMAND_MXBEAN_NAME);
            mserver.getObjectInstance(hotspotDiag);
            initok = true;
        } catch (MalformedObjectNameException ex) {
            ex.printStackTrace();
        } catch (InstanceNotFoundException ex) {
            System.err.println("Heap Histogram is not available"); // NOI18N
        } catch (SecurityException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return initok;
    }

    static InputStream getRawHistogram() {
        try {
            Object histo = mserver.invoke(hotspotDiag,
                    HISTOGRAM_COMMAND,
                    new Object[] {new String[] {ALL_OBJECTS_OPTION}},
                    new String[] {String[].class.getName()}
            );
            if (histo instanceof String) {
                return new ByteArrayInputStream(((String)histo).getBytes(StandardCharsets.UTF_8));
            }
        } catch (InstanceNotFoundException ex) {
            ex.printStackTrace();
        } catch (MBeanException ex) {
            ex.printStackTrace();
        } catch (ReflectionException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
}
