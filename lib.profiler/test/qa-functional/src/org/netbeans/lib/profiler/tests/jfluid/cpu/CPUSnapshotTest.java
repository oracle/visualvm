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

package org.netbeans.lib.profiler.tests.jfluid.cpu;

import junit.framework.Test;
import junit.textui.TestRunner;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.lib.profiler.ProfilerEngineSettings;


/**
 *
 * @author ehucka
 */
public class CPUSnapshotTest extends CPUSnapshotTestCase {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of BasicTest */
    public CPUSnapshotTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public static Test suite() {
        return NbModuleSuite.create(
            NbModuleSuite.createConfiguration(CPUSnapshotTest.class).addTest(
            "testMethods",
            "testMethodsServer",
            "testNoThreads",
            "testSimple",
            "testSimpleServer",
            "testThreads",
            "testThreadsServer",
            "testWaits",
            "testWaitsServer").enableModules(".*").clusters(".*").gui(false));
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void testMethods() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Methods", null);
        startSnapshotTest(settings, new String[] { "simple.cpu.Methods", "method27", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testMethodsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Methods", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.cpu.Methods", "method27", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testNoThreads() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        settings.setInstrumentSpawnedThreads(false);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testSimple() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.CPU", null);
        startSnapshotTest(settings, new String[] { "simple.CPU", "test20", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testSimpleServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.CPU", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.CPU", "test20", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testThreads() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        settings.setInstrumentSpawnedThreads(true);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testThreadsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.Region", null);
        addJVMArgs(settings, "-server");
        settings.setInstrumentSpawnedThreads(true);
        startSnapshotTest(settings, new String[] { "simple.cpu.Region", "run100", "()V" }, 0, 1.0, new String[] { "sun", "java" });
    }

    public void testWaits() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.WaitingTest", null);
        startSnapshotTest(settings, new String[] { "simple.cpu.WaitingTest", "method1000", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }

    public void testWaitsServer() {
        ProfilerEngineSettings settings = initSnapshotTest("j2se-simple", "simple.cpu.WaitingTest", null);
        addJVMArgs(settings, "-server");
        startSnapshotTest(settings, new String[] { "simple.cpu.WaitingTest", "method1000", "()V" }, 0, 1.0,
                          new String[] { "sun", "java" });
    }
}
