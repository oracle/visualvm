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

import java.awt.Container;
import javax.swing.JLabel;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.modules.profiler.ProfilerControlPanel2;
import org.openide.util.Exceptions;

/**
 * Operator for the profiler controll panel.
 *
 * @author Matus Dekanek
 */
public class ProfilerControlPanelOperator extends TopComponentOperator {

	//Basic telemetry information
	protected int m_instrumentedMethods = -1;
	protected String m_filter = "none";
	protected int m_threads = -1;
	protected int m_totalMemory = -1;
	protected int m_usedMemory = -1;
	protected int m_timeInGC = -1;
	public static String CAPTION = "Profiler";//!!

	/**
	 * Typical constructor.
	 * @param pcp
	 */
	ProfilerControlPanelOperator(ProfilerControlPanel2 pcp) {
		super(pcp);
	}

	/**Returns the default instance of the operator.
	 *
	 * @return if no instance is found, null is returned
	 */
	static ProfilerControlPanelOperator getDefault() {

		MainWindowOperator.getDefault().toFront();

		ProfilerControlPanel2 pcp = (ProfilerControlPanel2) waitTopComponent(
				null, CAPTION, 0,
				//MainWindowOperator.getDefault().waitSubComponent(
				new ClassChooser("ProfilerControlPanel2"));

				//ProfilerControlPanel2.getDefault();
		if (pcp == null) {
			System.out.println("ERROR: NULL ProfilerControlPanel2 FOUND!!!");
			return null;
		}
		return new ProfilerControlPanelOperator(pcp);
	}

	// \todo get basic data (telemetry), invoke actions (snapshots,live results)

	/**
	 * Instrumented method count as read from basic telemetry panel.
	 * @return Instrumented method count
	 */
	public int getInstrumentedMethods(){
		receiveBasicTelemetry();
		return m_instrumentedMethods;
	}

	/**
	 * Get instrumentation filter description as read from basic telemetry panel.
	 * @return
	 */
	public String getFilter(){
		return m_filter;
	}

	/**
	 * Get thread count read from basic telemetry panel.
	 * @return
	 */
	public int getThreads(){
		receiveBasicTelemetry();
		return m_threads;
	}

	/**
	 * Get total memory (bytes) used by the profiled program.
	 * @return
	 */
	public int getTotalMemory(){
		receiveBasicTelemetry();
		return m_totalMemory;
	}

	/**
	 * Get memory (bytes) used by the profiled program.
	 * @return
	 */
	public int getUsedMemory(){
		receiveBasicTelemetry();
		return m_usedMemory;
	}

	/**
	 * Get the time spent in garbage collector.
	 * @return
	 */
	public int getTimeInGC(){
		receiveBasicTelemetry();
		return m_timeInGC;
	}

	/**
	 * Shows the VM telemetry panel in the editor area.
	 */
	public void showVMTelemetry(){
		Container viewPanel = (Container) findSubComponent(
				new ClassChooser("ViewPanel"));
		JButtonOperator telemetryButtonOperator = new JButtonOperator(new ContainerOperator(viewPanel),0);
		telemetryButtonOperator.push();
		telemetryButtonOperator.doClick(500);
	}

	/**
	 * Shows the Threads panel in the editor area.
	 */
	public void showThreads(){
		Container viewPanel = (Container) findSubComponent(
				new ClassChooser("ViewPanel"));
		JButtonOperator threadsButtonOperator = new JButtonOperator(new ContainerOperator(viewPanel),1);
		threadsButtonOperator.push();
		threadsButtonOperator.doClick(500);
	}

	public void showLiveResults(){
		Container resultsPanel = (Container) findSubComponent(
				new ClassChooser("ResultsSnippetPanel"));
		JButtonOperator resultsButtonOperator = new JButtonOperator(new ContainerOperator(resultsPanel),1);
		System.out.println("pushing the button: "+ resultsButtonOperator.getText());
		resultsButtonOperator.push();
		resultsButtonOperator.doClick(500);
		System.out.println("Waiting...");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException ex) {
			System.out.println("ERROR: could not sleep" + ex);
			Exceptions.printStackTrace(ex);
		}
	}

	/**
	 * Read basic telemetry values from basic telemetry panel.
	 */
	public void receiveBasicTelemetry() {
		Container basicTelemetryPanel = (Container) findSubComponent(
				new ClassChooser("BasicTelemetryPanel"));
		if(basicTelemetryPanel==null)
			System.out.println("basicTelemetryPanel is NULL!!!");
		ContainerOperator telemetryPanelOper = new ContainerOperator(basicTelemetryPanel);
		String value;
		value = new JLabelOperator(telemetryPanelOper,1).getText();
		m_instrumentedMethods = subStr2int(value);
		m_filter = new JLabelOperator(telemetryPanelOper,3).getText();
		value = new JLabelOperator(telemetryPanelOper,5).getText();
		m_threads = subStr2int(value);
		value = new JLabelOperator(telemetryPanelOper,7).getText();
		m_totalMemory = subStr2int(value);
		value = new JLabelOperator(telemetryPanelOper,9).getText();
		m_usedMemory = subStr2int(value);
		value = new JLabelOperator(telemetryPanelOper,11).getText();
		m_timeInGC = subStr2int(value);
	}

	/**
	 * Convenience method for converting a string beginning with a number to the number.
	 * Digits may be delimited by coma and/or dot character,
	 * therefore this must be written from scratch. Both these characters are ignored.
	 * Number is parsed as a decimal number. Only fist number is converted.
	 *
	 * This methods is used for strings such as '1,000,255 Methods' or '0.2 %'.
	 * @param s string
	 * @return converted int, if the string does not begin with a number, 0 is returned
	 * If the string length is 0, 0 is returned as well. In any other case of an incorrect input
	 * format, 0 is returned.
	 */
	protected int subStr2int(String s){
		//the code is a bit ugly...
		if(s.length()==0)return 0;
		int zeroCode = Character.getNumericValue('0');
		char [] chs = s.toCharArray();
		int len = s.length();
		int stnum = -1;

		boolean minus = false;
		int value = 0;

		//find 1st digit
		for(int i=0;i<len;i++){
			char ch = chs[i];
			int chcode = Character.getNumericValue(ch);
			if((chcode>=zeroCode)&&(chcode<zeroCode+10)){
				stnum = i;
				i=len;
			}
		}
		if(stnum==-1)return 0;
		//minus sign
		if(stnum>0)
			minus = chs[stnum-1]=='-';
		//reading number
		for(int i=stnum;i<len;i++){
			char ch = chs[i];
			int chcode = Character.getNumericValue(ch);
			if((chcode>=zeroCode)&&(chcode<zeroCode+10)){
				value = value*10 + chcode - zeroCode;
			}else{
				if(!((ch=='.')||(ch==','))){
					//finished reading
					i = len;
				}
			}
		}
		if (minus) value = -value;
		return value;
	}
}
