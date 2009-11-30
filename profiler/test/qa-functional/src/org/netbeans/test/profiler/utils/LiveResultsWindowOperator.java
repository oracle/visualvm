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
import javax.swing.JTabbedPane;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.openide.util.NbBundle;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.cpu.LiveFlatProfilePanel;
import org.netbeans.modules.profiler.LiveResultsWindow;
import javax.swing.JTable;

/**
 * Live results window operator class. Mainly responsible for showing live results.
 * @author Matus Dekanek
 */
public class LiveResultsWindowOperator extends TopComponentOperator {

	protected static final String LIVE_RESULTS_WINDOW_NAME = NbBundle.getMessage(LiveResultsWindow.class, "LAB_ResultsWindowName");
	protected static final String CAPTION = LIVE_RESULTS_WINDOW_NAME;

	/**
	 * Default constructor
	 * @param tw
	 */
	public LiveResultsWindowOperator(LiveResultsWindow lrw) {
		super(lrw);
	}

	/**
	 * Static method to get new live results window operator. It does not invoke
	 * creation of a new live results window, it only tries to find one.
	 * @return new operator in case of success, NULL otherwise
	 */
	public static LiveResultsWindowOperator getDefault() {
		LiveResultsWindow lrw = null;
		try {
			MainWindowOperator.getDefault().toFront();
			lrw = (LiveResultsWindow) MainWindowOperator.getDefault().waitSubComponent(
					new ClassChooser("LiveResultsWindow"));
		} catch (Exception e) {
			print("ERROR: live results window not found: " + e);
			print("printing the list of subcomponents");
			printComponents((java.awt.Container) MainWindowOperator.getDefault().getSource());
			return null;
		}
		if(lrw == null) {
			print("ERROR: live results window is null!!!!");
			return null;
		}
		return new LiveResultsWindowOperator(lrw);
	}

	/**
	 * Write live results window content, according to the current profiling session.
	 * If no results are found, corresponding warning or message is written.
	 * @param option current profiling options
	 */
	public void writeData(BaseProfiledProject.ProfilingOption option) {
		JTable resultsTable = getResultsTable(option);
		if (resultsTable == null) {
			print("Warning: no results table found");
			try {
				java.awt.Component noResPanel = findSubComponent(
						new ClassChooser("EmptyLiveResultsPanel"));
				if (noResPanel != null) {
					print("There are no live results yet");
				} else {
					print("empty results panel not found either...");
					throw new Exception();
				}
			} catch (Exception e) {
				printComponents((java.awt.Container) this.getSource());
			}
			return;
		}
		TableColumnModel thm = resultsTable.getTableHeader().getColumnModel();
		TableModel jetm = resultsTable.getModel();
		int cols = jetm.getColumnCount();
		int rows = jetm.getRowCount();
		print("writing live results");
		String line = "";
		for (int i = 0; i < cols; i++) {
			line += thm.getColumn(i).getHeaderValue() + " | ";
		}
		print(line);
		for (int i = 0; i < rows; i++) {
			line = "";
			for (int j = 0; j < cols; j++) {
				line += jetm.getValueAt(i, j) + " | ";
			}
			print(line);
		}
	}

	/**
	 * Get the results table; method also handles the difference between cpu and memory profiling session.
	 * @param option profiling sesion option; currently only cpu option is working
	 * @return table with results in case of success, null otherwise
	 */
	public JTable getResultsTable(BaseProfiledProject.ProfilingOption option) {
		print("trying to get results table");
		if(option == BaseProfiledProject.ProfilingOption.MONITORING) return null;
		if(option == BaseProfiledProject.ProfilingOption.NONE) return null;
		
		if (option == BaseProfiledProject.ProfilingOption.MEMORY) {
			//making the memory results visible
			MainWindowOperator.getDefault().toFront();
			JTabbedPane jtp = (JTabbedPane) findSubComponent(
					new ClassChooser("JTabbedPane"));
			if(jtp==null) {
				print ("ERROR: no tabbed panel found");
				return null;
			}
			jtp.setSelectedIndex(0);
		}

		if(option== BaseProfiledProject.ProfilingOption.CPU){
			((LiveFlatProfilePanel) findSubComponent(new ClassChooser("LiveFlatProfilePanel"))).updateLiveResults();
			print("Live results updated");
		}

		return (JTable) findSubComponent(
				new ClassChooser("javax.swing.JTable", true));
	}

	/**
	 * Convenience method for line printing. prints prefix 'live results window'
	 * @param s line to be written
	 */
	static protected void print(String s) {
		System.out.println("live results window: " + s);
	}

	/**
	 * debug method for printing all subcomponents of a container
	 * @param cont
	 * @param depth this value is used for intending the components names
	 */
	static public void printComponents(java.awt.Container cont, int depth) {
		//print("printing components of mainWin");
		ContainerOperator contOper = new ContainerOperator(cont);
		java.awt.Component[] comps = contOper.getComponents();
		String prefix = "  ";
		for (int i = 0; i < depth; i++) {
			prefix += " ";
		}
		prefix += "->";
		for (java.awt.Component comp : comps) {
			print(prefix + comp.getClass().getName());
			if (comp instanceof java.awt.Container) {
				printComponents((java.awt.Container) comp, depth + 1);
			}
		}
	}

	/**
	 * Wrappr for printComponents(comp,0).
	 * @param cont
	 */
	static public void printComponents(java.awt.Container cont) {
		printComponents(cont, 0);
	}
}
