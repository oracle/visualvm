/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import javax.swing.Action;
import org.junit.Test;
import org.netbeans.junit.Log;
import org.netbeans.modules.profiler.ui.NpsDataObject;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import static org.junit.Assert.*;

/** Shows how o.n.core uses the SelfSamplerAction to start and stop self profiling.
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
public class SelfSamplerActionTest {

    public SelfSamplerActionTest() {
    }

    @Test
    public void testSelfProfileToStream() throws Exception {
        FileObject afo = FileUtil.getConfigFile("Actions/Profile/org-netbeans-modules-profiler-actions-SelfSamplerAction.instance");
        assertNotNull("SelfSamplerAction is in the right fileobject", afo);
        Action a = (Action)afo.getAttribute("delegate"); // NOI18N
        Object obj = a.getValue("logger-testprofile");
        assertTrue("It is runnable", obj instanceof Runnable);
        assertTrue("It is action listener", obj instanceof ActionListener);

        Runnable r = (Runnable)obj;
        ActionListener al = (ActionListener)obj;

        r.run();
        Thread.sleep(1000);
        assertLoggerThread("logger-testprofile shall be there", true);

        FileObject fo = FileUtil.createMemoryFileSystem().getRoot().createData("slow.nps");
        OutputStream os = fo.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        al.actionPerformed(new ActionEvent(dos, 0, "write")); // NOI18N
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

        assertLoggerThread("no logger- thread shall be there", false);
    }

    @Test
    public void testSelfProfileCancel() throws Exception {
        SelfSamplerAction result = SelfSamplerAction.getInstance();
        Object obj = result.getValue("logger-testprofile");
        assertTrue("It is runnable", obj instanceof Runnable);
        assertTrue("It is action listener", obj instanceof ActionListener);

        Runnable r = (Runnable)obj;
        ActionListener al = (ActionListener)obj;

        r.run();
        Thread.sleep(1000);
        assertLoggerThread("logger-testprofile shall be there", true);

        al.actionPerformed(new ActionEvent(this, 0, "cancel")); // NOI18N

        assertLoggerThread("no logger- thread shall be there", false);
    }

    private void assertLoggerThread(String msg, boolean exist) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getName().startsWith("logger-")) {
                assertTrue(msg + "There is " + t.getName() + " thread", exist);
                return;
            }
        }
        assertFalse(msg + "There is no logger- thread", exist);
    }


}
