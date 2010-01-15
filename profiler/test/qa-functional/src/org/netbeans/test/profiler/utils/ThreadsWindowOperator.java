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
import javax.swing.table.TableModel;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.lib.profiler.ui.components.JExtendedTable;
import org.netbeans.lib.profiler.ui.threads.ThreadsTablePanel;
import org.netbeans.modules.profiler.ThreadsWindow;
import org.openide.util.NbBundle;

/**
 *
 * @author tester
 */
public class ThreadsWindowOperator extends TopComponentOperator {

	protected static final String THREADS_WINDOW_NAME = NbBundle.getMessage(ThreadsWindow.class, "ThreadsWindow_ThreadsWindowName"); // NOI18N
    protected static final String THREADS_TIMELINE_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsTimelineTabName"); // NOI18N
    protected static final String THREADS_TABLE_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsTableTabName"); // NOI18N
    protected static final String THREADS_DETAILS_TAB_NAME = NbBundle.getMessage(ThreadsWindow.class,
                                                                               "ThreadsWindow_ThreadsDetailsTabName"); // NOI18N
    protected static final String THREADS_TIMELINE_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                 "ThreadsWindow_ThreadsTimelineTabDescr"); // NOI18N
    protected static final String THREADS_TABLE_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                 "ThreadsWindow_ThreadsTableTabDescr"); // NOI18N
    protected static final String THREADS_DETAILS_TAB_DESCR = NbBundle.getMessage(ThreadsWindow.class,
                                                                                "ThreadsWindow_ThreadsDetailsTabDescr"); // NOI18N
    protected static final String THREADS_ACCESS_DESCR = NbBundle.getMessage(ThreadsWindow.class, "ThreadsWindow_ThreadsAccessDescr"); // NOI18N
    
	protected static final String CAPTION = "Threads";

	/**
	 * Default constructor
	 * @param tw
	 */
	public ThreadsWindowOperator(ThreadsWindow tw){
		super(tw);
	}


	/**
	 * Static method to get new threads window operator. It does not invoke
	 * creation of a new threads window, it only tries to find one.
	 * @return new operator in case of success, NULL otherwise
	 */
	public static ThreadsWindowOperator getDefault(){
		MainWindowOperator.getDefault().toFront();

		ThreadsWindow tw = (ThreadsWindow) waitTopComponent(
				null, CAPTION, 0,
				//MainWindowOperator.getDefault().waitSubComponent(
				new ClassChooser("ThreadsWindow"));

		if (tw == null) {
			System.out.println("WARNING: NULL ThreadsWindow FOUND!!!");
			return null;
		}
		return new ThreadsWindowOperator(tw);
	}

	public String getThreads(){
		JTableOperator jto = new JTableOperator(new ContainerOperator(getThreadsTablePanel()));
		JExtendedTable jet = (JExtendedTable) jto.getSource();
		TableModel jetm = jet.getModel();
		int count = jetm.getRowCount();
		String result = "There is "+count +" threads";
		for(int i = 0;i<count;i++){
			result += "\n" + jetm.getValueAt(i, 0) + " : "+ jetm.getValueAt(i, 1);
		}
		return result;
	}


	/**
	 * Get the threads panel of the threads window.
	 * @return
	 */
	protected ThreadsTablePanel getThreadsTablePanel(){
		System.out.println("getting thread table panel");
		MainWindowOperator.getDefault().toFront();
		JTabbedPane jtp = (JTabbedPane) findSubComponent(
				new ClassChooser("JTabbedPane"));
		jtp.setSelectedIndex(1);
		ThreadsTablePanel threadsTablePanel = (ThreadsTablePanel) findSubComponent(
				new ClassChooser("ThreadsTablePanel"));
		if(threadsTablePanel==null){
			System.out.println("Warning: no ThreadsTablePanel found, test will probably crash");
		}
		return threadsTablePanel;
	}


}
