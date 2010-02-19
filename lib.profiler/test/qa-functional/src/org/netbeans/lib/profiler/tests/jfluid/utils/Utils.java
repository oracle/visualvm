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

package org.netbeans.lib.profiler.tests.jfluid.utils;

import org.netbeans.lib.profiler.ProfilingEventListener;
import org.netbeans.lib.profiler.tests.jfluid.CommonProfilerTestCase;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Utils {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public Utils() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void copyFile(File file, File target) {
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
            byte[] buffer = new byte[10240];
            int len = 0;

            while ((len = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }

            bis.close();
            bos.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void copyFolder(File folder, File target) {
        File[] lst = folder.listFiles();

        if (!target.exists()) {
            target.mkdirs();
        }

        for (int i = 0; i < lst.length; i++) {
            File nw = new File(target, lst[i].getName());

            if (lst[i].isDirectory()) {
                copyFolder(lst[i], nw);
            } else {
                copyFile(lst[i], nw);
            }
        }
    }

    public static ProfilingEventListener createProfilingListener(final CommonProfilerTestCase test) {
        return new ProfilingEventListener() {
                public void targetAppStarted() {
                    test.log("app started");
                    test.setStatus(CommonProfilerTestCase.STATUS_RUNNING);
                }

                public void targetAppStopped() {
                    test.log("app stoped");
                }

                public void targetAppSuspended() {
                    test.log("app suspended");
                }

                public void targetAppResumed() {
                    test.log("app resumed");
                }

                public void attachedToTarget() {
                    test.log("app attached to target");
                    test.setStatus(CommonProfilerTestCase.STATUS_RUNNING);
                }

                public void detachedFromTarget() {
                    test.log("app detached from target");
                }

                public void targetVMTerminated() {
                    test.log("vm terminated");
                    test.setStatus(CommonProfilerTestCase.STATUS_FINISHED);
                }
            };
    }

    public static void removeFolder(File folder) {
        File[] lst = folder.listFiles();

        if (lst == null) {
            System.err.println("null files " + folder.getAbsolutePath());

            return;
        }

        for (int i = 0; i < lst.length; i++) {
            if (lst[i].isDirectory()) {
                removeFolder(lst[i]);
            } else {
                lst[i].delete();
            }
        }

        folder.delete();
    }
}
