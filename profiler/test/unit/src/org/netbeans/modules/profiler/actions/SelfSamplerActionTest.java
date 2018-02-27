/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.actions;

import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import org.junit.Test;
import org.netbeans.junit.Log;
import org.netbeans.modules.profiler.ui.NpsDataObject;
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
