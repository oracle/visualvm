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
package org.netbeans.test.profiler;

import junit.textui.TestRunner;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.test.profiler.utils.BaseProfiledProject.ProfilingOption;
import org.netbeans.test.profiler.utils.J2SEProfiledProject;
import org.openide.util.Exceptions;

/**
 * Profiler test, with projects.
 *
 * @author Matus Dekanek
 */
public class ProfilingTest extends JellyTestCase {

	protected static final String PROFILER_ACTIONS_BUNDLE = "org.netbeans.modules.profiler.actions.Bundle";
	protected static final String PROFILER_UI_PANELS_BUNDLE = "org.netbeans.modules.profiler.ui.panels.Bundle";
	protected static final String PROFILER_LIB_BUNDLE = "org.netbeans.lib.profiler.Bundle";
	static String[] tests = new String[]{
		"testJavaSEProject"
	};

	/** Default constructor.
	 * @param name test case name
	 */
	public ProfilingTest(String name) {
		super(name);
	}

	public static NbTestSuite suite() {

		return (NbTestSuite) createModuleTest(ProfilingTest.class,
				tests);
	}

	/** Use for execution inside IDE */
	public static void main(java.lang.String[] args) {
		// run whole suite
		TestRunner.run(suite());
		// run only selected test case
		//TestRunner.run(new ProfilerValidation("testProfiler"));
	}

	/** Setup before every test case. */
	@Override
	public void setUp() {
		System.out.println("########  " + getName() + "  #######");
	}

	/**
	 * Tests basic javaSE project(anagram game). Tries all three profiling settings (monitoring,
	 * cpu and memory).
	 */
	public void testJavaSEProject() {
		J2SEProfiledProject anagrams = new J2SEProfiledProject();
		anagrams.build();
		anagrams.startProfilingMonitoring(true);
		anagrams.showVMTelemetry();
		anagrams.writeBasicTelemetryData();
		anagrams.writeThreads();
		anagrams.stopProfiling();

		anagrams.startProfilingCPU(true, false);
		anagrams.showLiveResults();
		//now it is necesary to wait for showing the application window...
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ex) {
			Exceptions.printStackTrace(ex);
		}
		anagrams.writeLiveResults();
		//anagrams.showLiveResults();
		anagrams.stopProfiling();
		anagrams.startProfilingMemory();
		anagrams.writeLiveResults();
		anagrams.stopProfiling();
	}
}
