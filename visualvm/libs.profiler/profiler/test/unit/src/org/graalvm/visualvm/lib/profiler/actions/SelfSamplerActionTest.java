/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.actions;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import org.junit.Test;
import org.netbeans.junit.Log;
import org.graalvm.visualvm.lib.profiler.ui.NpsDataObject;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import static org.junit.Assert.*;
import org.netbeans.modules.sampler.Sampler;

/** Shows how o.n.core uses the SelfSamplerAction to start and stop self profiling.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 * @author Tomas Hurka
 */
public class SelfSamplerActionTest {

    public SelfSamplerActionTest() {
    }

    @Test
    public void testSelfProfileToStream() throws Exception {
        Sampler sampler = Sampler.createManualSampler("testprofile");
        assertTrue("sampler instance", sampler != null);

        sampler.start();
        Thread.sleep(1000);
        assertSamplerThread("sampler-testprofile shall be there", true);

        FileObject fo = FileUtil.createMemoryFileSystem().getRoot().createData("slow.nps");
        OutputStream os = fo.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        sampler.stopAndWriteTo(dos);
        dos.close();

        if (fo.getSize() < 100) {
            fail("The file shall have real content: " + fo.getSize());
        }

        DataObject dataObject = DataObject.find(fo);
        assertEquals("Nps DataObject", NpsDataObject.class, dataObject.getClass());
        OpenCookie oc = dataObject.getLookup().lookup(OpenCookie.class);
        assertNotNull("Open cookie exists", oc);

        CharSequence log = Log.enable("", Level.WARNING);
        oc.open();

        if (log.length() > 0) {
            fail("There shall be no warnings:\n" + log);
        }

        assertSamplerThread("no sampler- thread shall be there", false);
    }

    @Test
    public void testSelfProfileCancel() throws Exception {
        Sampler sampler = Sampler.createManualSampler("testprofile");
        assertTrue("sampler instance", sampler != null);

        sampler.start();
        Thread.sleep(1000);
        assertSamplerThread("sampler-testprofile shall be there", true);

        sampler.cancel();
        Thread.sleep(1000);

        assertSamplerThread("no sampler- thread shall be there", false);
    }

    private void assertSamplerThread(String msg, boolean exist) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("sampler-")) {
                assertTrue(msg + " There is " + t.getName() + " thread", exist);
                return;
            }
        }
        assertFalse(msg + " There is no sampler- thread", exist);
    }


}
