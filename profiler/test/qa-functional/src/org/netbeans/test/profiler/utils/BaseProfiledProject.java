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
package org.netbeans.test.profiler.utils;

import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.nodes.ProjectRootNode;
import org.netbeans.jemmy.DialogWaiter;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.JavaProjectsTabOperator;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.openide.util.Exceptions;

/**
 * Abstract base class representing a project to be used for profiler test. This class should
 * cover the tasks performed with a project when testing the profiler. That
 * means creating the project, building, profiling and finding it in already
 * opened projects. 
 * @author Matus Dekanek
 */
public class BaseProfiledProject {

	protected static final String PROFILER_ACTIONS_BUNDLE = "org.netbeans.modules.profiler.actions.Bundle";
	protected static final String PROFILER_STP_BUNDLE = "org.netbeans.modules.profiler.ui.stp.Bundle";
	protected static final String PROFILER_UI_PANELS_BUNDLE = "org.netbeans.modules.profiler.ui.panels.Bundle";
	protected static final String PROFILER_LIB_BUNDLE = "org.netbeans.lib.profiler.Bundle";
	/**
	 * String constant for 'Build' menu item.
	 */
	public static String BUILD = "Build";
	/**
	 * String constant for 'Profile' menu item.
	 */
	public static String PROFILE = "Profile";
			//Bundle.getStringTrimmed("org.netbeans.modules.profiler.ui.stp.Bundle", "Menu/Profile"); //"Profile"
	/**
	 * String constant for title of 'select java platform' dialog.
	 */
	public static String SELECT_JAVA = "Select Java Platform";
	/**
	 * String constant for title of 'Enable profiler integration' dialog.
	 */
	public static String ENABLE = "Enable";
	/**
	 * String constant for profiling menu.
	 */
	public static String PROFILE_MENU = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile"); //"Profile"
	/**
	 * String constant for stopping the profiling session
	 */
	public static String STOP = "Stop Profiling Session";
	//members------------------------
	/**
	 * Name of the project. Should be given/created in the constructor of the
	 * derived class.
	 */
	protected String m_name;

	/**
	 *	Enumerated type for profiling options.
	 */
	public enum ProfilingOption{
		NONE,MONITORING,CPU,MEMORY
	};

	/**
	 * Current profiling option.
	 */
	protected ProfilingOption m_profilingOption = ProfilingOption.NONE;

	/**
	 * Get the name of the project
	 * @return project name
	 */
	public String getName() {
		return m_name;
	}

	/**
	 * Get the status of profiled project. The status indicates whether the project is profiled and
	 * the current profiler session.
	 * @return profiling option
	 */
	public ProfilingOption getProfilingStatus(){
		return m_profilingOption;
	}

	/**
	 * Get the project node in the projects view.
	 * @return project node
	 */
	public ProjectRootNode getProjectNode() {
		return new JavaProjectsTabOperator().getJavaProjectRootNode(m_name);
	}

	/**
	 * Build the project with default parameters.
	 */
	public void build() {
		try {
			getProjectNode().performPopupAction(BUILD);
		} catch (Exception e) {
			print("ERROR: build not possible.");
		}
	}

	/**
	 * Profile the project - thread monitoring.
	 * Only starts profiling and quits.
	 */
	public void profileThreads() {
		if (startProfilingMonitoring(true)) {
			try {
				
				stopProfiling();
			} catch (Exception e) {
				print("ERROR: there was a problem after the profiling started: " + e);
			}
		}
	}

	/**
	 * Profile the project - monitoring the cpu performance.
	 * Only starts profiling and quits.
	 */
	public void profileCPU() {
		print("profile CPU");
		if (startProfilingCPU(true, true)) {
			try {
				stopProfiling();
			} catch (Exception e) {
				print("ERROR: there was a problem after the profiling started: " + e);
			}
		}

	}

	/**
	 * Profile the project - monitoring the memory.
	 * Only starts profiling and quits.
	 */
	public void profileMemory() {
		print("profile memory");
		if (startProfilingMemory()) {
			try {
				print("stop");
				stopProfiling();
			} catch (Exception e) {
				print("ERROR: there was a problem after the profiling started: " + e);
			}
		}
	}

	/**
	 *	Starts monitoring profiling session.
	 *	@param enableThreads if true, threads monitoring is enabled.
	 *	@return true if profiling session is running, false otherwise
	 */
	public Boolean startProfilingMonitoring(Boolean enableThreads) {
		if(m_profilingOption!=ProfilingOption.NONE){
			print("WARNING: trying to start profiling session, but there is one already running. Ignoring");
			return false;
		}
		print("start profiling: monitoring");
		try {
			//profiler dialog
			NbProfilerDialogOperator profileDialog = prepareProfiling();
			//monitor button
			profileDialog.selectMonitor();
			//monitor threads
			if (enableThreads) {
				profileDialog.enableThreadMonitoring();
			} else {
				profileDialog.disableThreadMonitoring();
			}
			//run
			runProfilerSession(profileDialog);
			Thread.sleep(1000);
		} catch (Exception e) {
			print("ERROR: profiling could not start: " + e);
			return false;
		}
		m_profilingOption = ProfilingOption.MONITORING;
		return true;
	}

	/**
	 *	Starts CPU profiling session.
	 *	@param entireApplication if true, entire application will be profiled, otherwise only
	 *	part of appplication will be profiled (uses only selected root methods)
	 *	@param allClasses if true, all classes will be profiled, otherwise only project classes will be profiled
	 *	@return true if profiling session is running, false otherwise
	 */
	public Boolean startProfilingCPU(Boolean entireApplication, Boolean allClasses) {
		if(m_profilingOption!=ProfilingOption.NONE){
			print("WARNING: trying to start profiling session, but one is already running. Ignoring");
			return false;
		}
		print("start profiling: CPU");
		try {
			//profile dialog
			NbProfilerDialogOperator profileDialog = prepareProfiling();
			//monitor button
			profileDialog.selectCpu();
			//entire application switch
			if (entireApplication) {
				profileDialog.selectEntireApplication();
				Thread.sleep(1000);
			} else {
				profileDialog.selectPartOfApp();
				Thread.sleep(1000);
			}

			//all classes switch
			if (allClasses) {
				profileDialog.selectProfileAll();
				Thread.sleep(1000);
			} else {
				profileDialog.selectProfileOnlyProject();
				Thread.sleep(1000);
			}
			
			//run
			runProfilerSession(profileDialog);
		} catch (Exception e) {
			print("ERROR: profiling could not start: " + e);
			return false;
		}
		m_profilingOption = ProfilingOption.CPU;
		return true;
	}

	/**
	 *	Starts memory profiling session.
	 *	@return true if profiling session is running, false otherwise
	 */
	public Boolean startProfilingMemory() {
		if(m_profilingOption!=ProfilingOption.NONE){
			print("WARNING: trying to start profiling session, but one is already running. Ignoring");
			return false;
		}
		print("start profiling: memory");
		try {
			//profile dialog
			NbProfilerDialogOperator profileDialog = prepareProfiling();
			//monitor button
			profileDialog.selectMemory();
			//run
			runProfilerSession(profileDialog);
		} catch (Exception e) {
			print("ERROR: profiling could not start: " + e);
			return false;
		}
		m_profilingOption = ProfilingOption.MEMORY;
		return true;
	}

	/**
	 * Wait until it is possible to stop a profiling session and stop the current 
	 * profiling session. If there is no profiling session, resturn immediately.
	 */
	public void stopProfiling() {
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to stop not yet started profiling session. Ignoring");
			return;
		}

		Action stopAction = new Action(PROFILE_MENU + "|" + STOP, null);
		sleep(1000);
		//waiting until it is possible to stop the sesion
		MainWindowOperator.getDefault().toFront();
		while (!stopAction.isEnabled()) {
			sleep(1000);
			print("before stop: waiting for menu...");
			MainWindowOperator.getDefault().toFront();
		}
		//stoppping
		assert(stopAction.isEnabled());
		stopAction.perform();
		print("stop invoked");
		//waiting until it is not possible to stop anything - that indicates that there is no
		//profiling session running
		MainWindowOperator.getDefault().toFront();
		Action stopAction2 = new Action(PROFILE_MENU + "|" + STOP, null);
		MainWindowOperator.getDefault().toFront();
		while (stopAction2.isEnabled()) {
			sleep(1000);
			print("stoping: waiting for menu...");
			MainWindowOperator.getDefault().toFront();
		}
		m_profilingOption=ProfilingOption.NONE;
		print("profiling stopped");
	}

	/**
	 * Method for writing basic telemetry data as read from the profiler panel.
	 */
	public void writeBasicTelemetryData() {
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to write profiler information but no session is running. Ignoring");
			return;
		}
		ProfilerControlPanelOperator pcpo = ProfilerControlPanelOperator.getDefault();
		print("TELEMETRY: Instrumented methods: " + pcpo.getInstrumentedMethods());
		print("TELEMETRY: Filter: " + pcpo.getFilter());
		print("TELEMETRY: Threads: " + pcpo.getThreads());
		print("TELEMETRY: Total memory: " + pcpo.getTotalMemory());
		print("TELEMETRY: Used memory: " + pcpo.getUsedMemory());
		print("TELEMETRY: Time in GC: " + pcpo.getTimeInGC());
	}

	/**
	 * Method for printing the list of threads.
	 */
	public void writeThreads(){
		//show threads
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to write profiler information but no session is running. Ignoring");
			return;
		}
		ThreadsWindowOperator two = showThreads();
		System.out.println(two.getThreads());
	}

	/**
	 * Write content of the live results window. If the window is not shown yet,
	 * it also invokes it from profiler control panel.
	 */
	public void writeLiveResults(){
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to write profiler information but no session is running. Ignoring");
			return;
		}
		LiveResultsWindowOperator lrwo =  showLiveResults();
		//during next 10 seconds the profiled program is expected to give at least some data
		sleep(10000);
		lrwo.writeData(m_profilingOption);
	}

	/**
	 * Show the Threads panel, using the profiler control panel.
	 */
	public ThreadsWindowOperator showThreads() {//this is possible regardless of profiler status
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to show profiler information but no session is running. Ignoring");
			return null;
		}
		ProfilerControlPanelOperator pcpo = ProfilerControlPanelOperator.getDefault();
		pcpo.showThreads();
		return ThreadsWindowOperator.getDefault();
	}

	/**
	 * Show the VM Telemetry panel, using the profiler control panel.
	 */
	public void showVMTelemetry() {//this is possible regardless of profiler status
		ProfilerControlPanelOperator pcpo = ProfilerControlPanelOperator.getDefault();
		pcpo.showVMTelemetry();
	}

	/**
	 * Show the Live results window panel, using the profiler control panel.
	 */
	public LiveResultsWindowOperator showLiveResults(){
		if(m_profilingOption==ProfilingOption.NONE){
			print("WARNING: trying to show profiler information but no session is running. Ignoring");
			return null;
		}
		ProfilerControlPanelOperator pcpo = ProfilerControlPanelOperator.getDefault();
		if(pcpo == null){
			print("ERROR: profiler control panle not found");
			return null;
		}
		pcpo.showLiveResults();
		return LiveResultsWindowOperator.getDefault();
	}

	/**
	 * Invoke profiling of the project and if needed, confirm dialog for
	 * modifying the build script.
	 * @return profiler dialog operator
	 * @throws Exception, in case that profile dialog operator could not be
	 * created.
	 */
	protected NbProfilerDialogOperator prepareProfiling() throws Exception {
		//invoke popup action
		getProjectNode().performPopupAction(PROFILE);
		//enable profilier integration if run for the first time
		print("profile popup menu action performed");
		try {
			new NbDialogOperator(Bundle.getStringTrimmed("org.netbeans.modules.profiler.j2se.Bundle",
					"J2SEProjectTypeProfiler_ModifyBuildScriptCaption")).ok(); //"Enable Profiling of {0}"
		} catch (Exception e) {
			print("profiler already integrated into the project");//this is not an error
		}

		//profile dialog
		return new NbProfilerDialogOperator();
	}

	/**
	 * Start profiler session according to the settings in profiler
	 * dialog. After invoking the session, waits for the profiler session to start.
	 * @param pdo profiler dialog operator with already set session
	 */
	protected void runProfilerSession(NbProfilerDialogOperator pdo) {
		pdo.run();
		pdo.waitClosed();
		waitProfilingStart();
	}

	/**
	 * Wait until profiled project is run. This is indicated by the enabled
	 * menu item "Profile|Stop profiling session".
	 */
	protected void waitProfilingStart() {
		System.out.println("Waiting for enabling menuitem 'stop profiling session' ");
		Action stopAction = new Action(PROFILE_MENU + "|" + STOP, null);
		MainWindowOperator.getDefault().toFront();
		while (!stopAction.isEnabled()) {
			sleep(1000);
			print("waiting for menu...");
			MainWindowOperator.getDefault().toFront();
		}
		print("stopping the session is enabled");
	}
	/**
	 * Convenience function for printing a string.
	 */
	protected void print(String s) {
		System.out.println("PROFILER TEST:" + s);
	}

	/**
	 * Convenience function for sleep.
	 * @param milis tiem to wait
	 */
	protected void sleep(int milis) {
		try {
			Thread.sleep(milis);
		} catch (Exception e) {
			print("could not wait: " + e);
		}
	}

}
