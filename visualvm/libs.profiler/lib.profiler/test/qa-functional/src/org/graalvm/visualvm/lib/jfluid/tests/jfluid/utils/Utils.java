/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.tests.jfluid.utils;

import org.graalvm.visualvm.lib.jfluid.ProfilingEventListener;
import org.graalvm.visualvm.lib.jfluid.tests.jfluid.CommonProfilerTestCase;
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

        for (File lst1 : lst) {
            File nw = new File(target, lst1.getName());
            if (lst1.isDirectory()) {
                copyFolder(lst1, nw);
            } else {
                copyFile(lst1, nw);
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

        for (File lst1 : lst) {
            if (lst1.isDirectory()) {
                removeFolder(lst1);
            } else {
                lst1.delete();
            }
        }

        folder.delete();
    }
}
