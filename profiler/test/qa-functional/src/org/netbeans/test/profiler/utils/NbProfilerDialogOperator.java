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

import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.plaf.basic.BasicBorders.RadioButtonBorder;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.ButtonOperator;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JComponentOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JRadioButtonOperator;
import org.netbeans.jemmy.util.NameComponentChooser;
import org.netbeans.lib.profiler.common.filters.FilterUtils;
import org.netbeans.lib.profiler.ui.components.ComponentMorpher;
import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.netbeans.modules.profiler.ui.stp.SelectProfilingTask;

/**
 * Operator for profiler dialog. Should be able to set the profiler session.
 * @author Matus Dekanek
 */
public class NbProfilerDialogOperator extends NbDialogOperator {

	protected static final String PROFILER_STP_BUNDLE = "org.netbeans.modules.profiler.ui.stp.Bundle";
	protected static final String PROFILER_UTILS_BUNDLE = "org.netbeans.modules.profiler.utils.Bundle";
	protected static final String PROFILER_FILTERS_BUNDLE = "org.netbeans.lib.profiler.common.filters.Bundle";
	/**
	 * String constant for title of 'profiler' dialog.
	 */
	static String tprofileTitle = Bundle.getStringTrimmed("org.netbeans.modules.profiler.ui.stp.Bundle",
			"SelectProfilingTask_ProfileDialogCaption");
	/**
	 * String constant for enable thread monitoring.
	 */
	static String tcpuPartOfApp = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE, "CPUSettingsBasicPanel_PartAppRadioText");
	/**
	 * String constant for enable thread monitoring.
	 */
	static String tenableMonitoring = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE, "MonitorSettingsBasicPanel_EnableThreadsCheckboxText");
	/**
	 * String constant for enable thread monitoring.
	 */
	static String tcpuProfileAll = Bundle.getStringTrimmed(PROFILER_FILTERS_BUNDLE, "FilterUtils_ProfileAllClassesFilterName");
	/**
	 * String constant for enable thread monitoring.
	 */
	static String tcpuProfileOnlyProject = Bundle.getStringTrimmed(PROFILER_UTILS_BUNDLE, "ProjectUtilities_ProfileProjectClassesString");
	/**
	 * String constant for run button.
	 */
	static String trun = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE,
			"SelectProfilingTask_RunButtonText");
	/**
	 * String constant for 'monitor' button.
	 */
	static String tmonitor = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE,
			"SelectProfilingTask_MonitorString");
	/**
	 * String constant for 'CPU' button.
	 */
	static String tcpu = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE,
			"SelectProfilingTask_CpuString");
	/**
	 * String const for Entire application (cpu).
	 */
	static String tcpuEntireApp = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE,
			"CPUSettingsBasicPanel_EntireAppRadioText");
	/**
	 * String constant for 'memory' button.
	 */
	static String tmemory = Bundle.getStringTrimmed(PROFILER_STP_BUNDLE,
			"SelectProfilingTask_MemoryString");

	/**
	 * Finds the profiler dialog. If there is none, runtime exception is thrown.
	 */
	public NbProfilerDialogOperator() {
		super(tprofileTitle);
	}

	/**
	 * Select monitoring profiler session.
	 */
	public void selectMonitor() {
		try {
			//finding the right component morpher and it`s operator
			ComponentOperator cpmo = new ComponentOperator(findComponentMorpher(tmonitor));
			cpmo.clickMouse();
		
		} catch (Exception e) {
			print("ERROR: cound not find the right button.");
			print(e.getMessage());
		}
		ProfilerControlPanel2 foo;
	}

	/**
	 * Select cpu and performance measuring profiling session.
	 */
	public void selectCpu() {
		try {
			//JLabelOperator foo = new JLabelOperator(this, BaseProfiledProject.tcpu);
			//foo.clickMouse();
			//finding the right component morpher and it`s operator
			ComponentOperator cpmo = new ComponentOperator(findComponentMorpher(tcpu));
			cpmo.clickMouse();

		} catch (Exception e) {
			print("ERROR: cound not find the right button.");
			print(e.getMessage());
		}
	}

	/**
	 * Select cpu and performance measuring profiling session.
	 */
	public void selectMemory() {
		try {
			ComponentOperator cpmo = new ComponentOperator(findComponentMorpher(tmemory));
			cpmo.clickMouse();

		} catch (Exception e) {
			print("ERROR: cound not find the right button.");
			print(e.getMessage());
		}
	}

	/**
	 * Run the profiling session.
	 */
	public void run() {
		new JButtonOperator(this, trun).push(); //"run"
	}

	//various profiler settings

	/**
	 * Select to profile entire application in the CPU task. Do not use with other
	 * profiling task.
	 */
	public void selectEntireApplication() {
		JRadioButtonOperator rbo = new JRadioButtonOperator(this, tcpuEntireApp);
		if (!rbo.isSelected()) {
			rbo.clickMouse();
			print("click on entire app");
		}
		print("entire app button selected");
	}

	/**
	 * Select to profile only part of the application in the CPU task. Do not use with other
	 * profiling task.
	 */
	public void selectPartOfApp() {
		JRadioButtonOperator rbo = new JRadioButtonOperator(this, tcpuPartOfApp);
		if (!rbo.isSelected()) {
			rbo.clickMouse();
			print("click on entire app");
		}
		print("entire app button selected");
	}

	/**
	 * Enable or disable thred monitoring in the monitoring task. Do not use with another
	 * profiling task (cpu/memory).
	 * @param enable
	 */
	public void enableThreadMonitoring(Boolean enable){
			JCheckBoxOperator cbo = new JCheckBoxOperator(this, tenableMonitoring);
			System.out.println("thread monitrong button found");
			if (cbo.isSelected()!=enable) {
				cbo.clickMouse();
				print("clicked on thread monitoring");
			}
	}

	/**
	 * Wrapper method over enableThreadMonitoring(Boolean enable)
	 */
	public void enableThreadMonitoring(){
		enableThreadMonitoring(true);
	}

	/**
	 * Wrapper method over enableThreadMonitoring(Boolean enable)
	 */
	public void disableThreadMonitoring(){
		enableThreadMonitoring(false);
	}

	/**
	 * Select 'profille all classes' for CPU profiling task. Do not use with other
	 * profiling tasks.
	 */
	public void selectProfileAll(){
		JComboBoxOperator jcbo = new JComboBoxOperator(this);
		print("found combo box");
		jcbo.selectItem(tcpuProfileAll);
		print("profile all selected");
	}

	/**
	 * Select 'profille only project classes' for CPU profiling task. Do not use with other
	 * profiling tasks.
	 */
	public void selectProfileOnlyProject(){
		JComboBoxOperator jcbo = new JComboBoxOperator(this);
		print("found combo box");
		jcbo.selectItem(tcpuProfileOnlyProject);
		print("profile only project selected");
	}

	//various convenience methods
	
	/**
	 * Find ComponentMorpher in the source component (dialog), that contains
	 * JLabel with specifield label. ComponentMorpher is the clickable component
	 * with profiling task (monitoring/cpu/memory).
	 * @param label string to be found in the component morpher
	 * @return found ComponentMorpher instance or null if not found
	 */
	public ComponentMorpher findComponentMorpher(final String label) {
		Component cmpnnt = findSubComponent(
				new ComponentChooser() {

					public String getDescription() {
						return ("find ComponentMorpherClass"); // NOI18N
						}

					public boolean checkComponent(java.awt.Component comp) {
						if (comp.getClass().getName().contains("ComponentMorpher")) {
							Boolean result = true;
							ComponentMorpher cmpm = (ComponentMorpher) comp;
							try {
								JLabelOperator lo = new JLabelOperator(new ContainerOperator(cmpm), label);
							} catch (Exception e) {
								result = false;
							}
							return result;
						}
						return false;
					}
				});

		if (cmpnnt == null) {
			print("Warning: NULL was found as resultant component morpher");
		}
		ComponentMorpher result = (ComponentMorpher) cmpnnt;
		return result;
	}

	/**
	 * Convenience function for printing string, with prefix 'NbProfilerDialogOperator: '.
	 * @param s
	 */
	void print(String s) {
		System.out.println("ProfilerDialogOperator: " + s);
	}
}
