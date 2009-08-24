/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.test.profiler;

import java.awt.Container;
import javax.swing.JCheckBox;
import junit.textui.TestRunner;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.JavaProjectsTabOperator;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.NewJavaProjectNameLocationStepOperator;
import org.netbeans.jellytools.NewProjectWizardOperator;
import org.netbeans.jellytools.OptionsOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jellytools.actions.OptionsViewAction;
import org.netbeans.jellytools.nodes.JavaProjectRootNode;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.ProjectRootNode;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.test.ide.WatchProjects;

/** Validation test of profiler.
 *
 * @author Alexandr Scherbatiy, Jiri Skrivanek
 */
public class ProfilerValidation extends JellyTestCase {

    //private static final String SAMPLE_PROJECT_NAME = "AnagramGame";

    protected static final String  PROFILER_ACTIONS_BUNDLE = "org.netbeans.modules.profiler.actions.Bundle";
    protected static final String  PROFILER_UI_PANELS_BUNDLE = "org.netbeans.modules.profiler.ui.panels.Bundle";
    protected static final String  PROFILER_LIB_BUNDLE = "org.netbeans.lib.profiler.Bundle";

    static String[] tests = new String[]{
            "testProfilerMenus",
            //Commented out, because the for some unknown reason the
            //test fails on being unable to open the Miscellaneous tab in Options
            //"testProfilerProperties",
            "testProfilerCalibration"//,
          //  "testProfiler"
    };
    /** Default constructor.
     * @param name test case name
     */
    public ProfilerValidation(String name){
        super(name);
    }

    /** Defaine order of test cases.
     * @return NbTestSuite instance
     */
//    public static NbTestSuite suite() {
//        NbTestSuite suite = new NbTestSuite();
//        suite.addTest(new ProfilerValidation("testProfilerMenus"));
//        suite.addTest(new ProfilerValidation("testProfilerProperties"));
//        //suite.addTest(new ProfilerValidation("testCreateProject"));
//        //suite.addTest(new ProfilerValidation("testProfiler"));
//        return suite;
//    }

    public static NbTestSuite suite() {

        return (NbTestSuite) createModuleTest(ProfilerValidation.class,
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
    public void setUp() {
        System.out.println("########  "+getName()+"  #######");
    }

    /** Test Profiler Menus. */
    public void testProfilerMenus(){
        String ProfileMenu = org.netbeans.jellytools.Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile"); //"Profile"
        //Profile|Profile Project
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_ProfileMainProjectAction"), null).isEnabled();
        //Profile|Attach Profiler...
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_AttachMainProjectAction"), null).isEnabled();
        //Profile|Take Snapshot of Collected Results
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_TakeSnapshotAction"), null).isEnabled();
        //Profile|Stop Profiling Session
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_StopAction"), null).isEnabled();

    }

    /** Test Profiler Properties. */
    public void testProfilerProperties() throws Exception {
         
        OptionsOperator options = OptionsOperator.invoke();

        options.selectMiscellaneous();// "Miscellaneous"
        
        JTabbedPaneOperator tabbedPane = new JTabbedPaneOperator(options);
        tabbedPane.selectPage( Bundle.getStringTrimmed("org.netbeans.modules.profiler.options.Bundle", "ProfilerOptionsCategory_Title") ); //"Profiler"

        JLabelOperator javaPlatform = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_JavaPlatformLabelText")); //"Profiler Java Platform"

        JLabelOperator communicationPort = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_CommPortLabelText") );//"Communication Port"

        JLabelOperator openThreads = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_ThreadsViewLabelText") );//"Open Threads View"
        JCheckBoxOperator cpu    = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_CpuChckBoxText") );//"CPU"
        JCheckBoxOperator memory = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_MemoryChckBoxText") );//"Memory"

        JComboBoxOperator openNewSnapshot= new JComboBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_OpenSnapshotRadioText") );//"Open New Snapshot"

        JCheckBoxOperator enableHeapAnalisys = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_EnableAnalysisCheckbox") ); //"Enable Rule-Based Heap Analysis"

        JButtonOperator reset = new JButtonOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                                                                "ProfilerOptionsPanel_ResetButtonName") ); //"Reset"

        options.ok();
        //java.util.logging.Logger.getLogger("global").log( java.util.logging.Level.SEVERE, "ok pushed" );
    }

    /** Test profiler calibration
     * - run profiler calibration Profile|Advanced Commands|Run Profiler Calibration
     * - wait for calibration results and confirm information dialog */
    public void testProfilerCalibration() {
        String ProfileMenu = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile");
        String AdvansedCmds = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile/Advanced");
        String CalibrationAction = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "LBL_RunCalibrationAction");

        new ActionNoBlock(ProfileMenu + "|" + AdvansedCmds + "|"+ CalibrationAction , null).perform();
        new NbDialogOperator( Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                              "JavaPlatformSelector_SelectPlatformCalibrateDialogCaption") ).ok();
        // increase timeout for calibration
        JemmyProperties.setCurrentTimeout("DialogWaiter.WaitDialogTimeout", 120000); // NOI18N
        NbDialogOperator infoDlg = new NbDialogOperator( Bundle.getStringTrimmed("org.openide.Bundle",
                              "NTF_InformationTitle") ); // "Information"
        String lbl = Bundle.getStringTrimmed(PROFILER_LIB_BUNDLE,
                        "TargetAppRunner_CalibrationSummaryShortMsg");
        /* The calibration was successful.\nClick Show Details to see calibration results.\n\nWarning\: If your computer uses dynamic CPU frequency switching,\nplease disable it and rerun calibration as changing the CPU frequency\nduring profiling would produce inaccurate results. */
        new JLabelOperator(infoDlg, lbl.substring(0, lbl.indexOf("\n") ) ); // The calibration was successful.
        infoDlg.ok();
    }

    /** Test profiler
     * - create sample project to be tested
     * - call Profile|Profile Main Project
     * - confirm changes in project when profiled for the first time
     * - click Run in Profile AnagramGame dialog
     * - wait for Profiler view
     * - wait until text "Established local connection with the tool" appears in output window
     * - wait until "Profile|Take Snapshot of Collected Results" is enabled
     * - call Profile|Take Snapshot of Collected Results
     * - maximaze results view
     * - save collected results
     * - call "Profile|Stop Profiling Session"
     */
    public void testProfiler() throws Exception {
        String ProfileMenu = org.netbeans.jellytools.Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile"); //"Profile"
        String anagramGamePrName; // will be get from New Project wizard
        // create sample Anagram Game Java project
        NewProjectWizardOperator npwo = NewProjectWizardOperator.invoke();
        String samplesLbl = Bundle.getStringTrimmed("org.netbeans.modules.project.ui.Bundle", "Templates/Project/Samples"); // "Samples"
        String javaLbl = Bundle.getStringTrimmed("org.netbeans.modules.java.examples.Bundle", "Templates/Project/Samples/Standard"); // "Java"
        npwo.selectCategory(samplesLbl + "|" + javaLbl);
        npwo.selectProject( Bundle.getStringTrimmed( "org.netbeans.modules.java.examples.Bundle",
                            "Templates/Project/Samples/Standard/anagrams.zip") ); //"Anagram Game"
        npwo.next();
        NewJavaProjectNameLocationStepOperator npnlso = new NewJavaProjectNameLocationStepOperator();
        anagramGamePrName = npnlso.txtProjectName().getText();
        npnlso.txtProjectLocation().setText(System.getProperty("netbeans.user")); // NOI18N
        npnlso.finish();
        //wait project appear in projects view
        //wait 30 second
        JemmyProperties.setCurrentTimeout("JTreeOperator.WaitNextNodeTimeout", 30000); // NOI18N
        
        //disable the compile on save:
        ProjectsTabOperator.invoke().getProjectRootNode(anagramGamePrName).properties();
        // "Project Properties"
        String projectPropertiesTitle = Bundle.getStringTrimmed("org.netbeans.modules.java.j2seproject.ui.customizer.Bundle", "LBL_Customizer_Title");
        NbDialogOperator propertiesDialogOper = new NbDialogOperator(projectPropertiesTitle);
        // select "Compile" category
        String buildCategoryTitle = Bundle.getStringTrimmed("org.netbeans.modules.java.j2seproject.ui.customizer.Bundle", "Projects/org-netbeans-modules-java-j2seproject/Customizer/BuildCategory");
        String compileCategoryTitle = Bundle.getStringTrimmed("org.netbeans.modules.java.j2seproject.ui.customizer.Bundle", "LBL_Config_Build");
        new Node(new Node(new JTreeOperator(propertiesDialogOper), buildCategoryTitle), compileCategoryTitle).select();
        // actually disable the quick run:
        String compileOnSaveLabel = Bundle.getStringTrimmed("org.netbeans.modules.java.j2seproject.ui.customizer.Bundle", "CustomizerCompile.CompileOnSave");
        JCheckBox cb = JCheckBoxOperator.waitJCheckBox((Container) propertiesDialogOper.getSource(), compileOnSaveLabel, true, true);
        if (cb.isSelected()) {
            cb.doClick();
        }
        // confirm properties dialog
        propertiesDialogOper.ok();
        
        JavaProjectRootNode projectNode = new JavaProjectsTabOperator().getJavaProjectRootNode(anagramGamePrName);
        //wait classpath scanning finished
        WatchProjects.waitScanFinished();
        projectNode.buildProject();
        MainWindowOperator.getDefault().waitStatusText( Bundle.getStringTrimmed("org.apache.tools.ant.module.run.Bundle", "FMT_finished_target_status") ); // "Finished Building"

        // call Profile|Profile Main Project
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed("org.netbeans.modules.profiler.actions.Bundle", "LBL_ProfileMainProjectAction"), null).perform();
        // confirm changes in project when profiled for the first time
        new NbDialogOperator( Bundle.getStringTrimmed("org.netbeans.modules.profiler.j2se.Bundle",
                        "J2SEProjectTypeProfiler_ModifyBuildScriptCaption") ).ok(); //"Enable Profiling of {0}"
        //wait
        // click Run in Profile AnagramGame dialog
        NbDialogOperator profileOper = new NbDialogOperator( Bundle.getStringTrimmed("org.netbeans.modules.profiler.ui.stp.Bundle",
                                        "SelectProfilingTask_ProfileDialogCaption") ); // "Profile "+anagramGamePrName
        new JButtonOperator(profileOper, Bundle.getStringTrimmed("org.netbeans.modules.profiler.ui.stp.Bundle",
                                        "SelectProfilingTask_RunButtonText") ).push(); //"Run"
        profileOper.waitClosed();
        waitProgressDialog( Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                            "NetBeansProfiler_ProgressDialogCaption"), 50000); // "Progress ..."
        new TopComponentOperator( Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                                    "LAB_ControlPanelName") ); // "Profiler"
        //new OutputTabOperator(anagramGamePrName).waitText( Bundle.getStringTrimmed(PROFILER_LIB_BUNDLE,
        //                            "ProfilerServer_LocalConnectionMsg") ); //"Established local connection with the tool"
        Action takeSnapshotAction = new Action(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_TakeSnapshotAction"), null);
        new Waiter(new Waitable() {
            public Object actionProduced(Object takeSnapshotAction) {
		MainWindowOperator.getDefault().toFront();
                return ((Action)takeSnapshotAction).isEnabled() ? Boolean.TRUE : null;
            }
            public String getDescription() {
                return("Wait menu item is enabled."); // NOI18N
            }
        }).waitAction(takeSnapshotAction);
        new EventTool().waitNoEvent(5000);
        takeSnapshotAction.perform();
        TopComponentOperator collectedResults;
	try {
            collectedResults = new TopComponentOperator( Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                                                        "CPUSnapshotPanel_PanelTitle") ); //"CPU"
	} catch (Exception e) {
	    issue144699Hack();
	    collectedResults = new TopComponentOperator( Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                                                        "CPUSnapshotPanel_PanelTitle") ); //"CPU"
	}
        collectedResults.saveDocument();
        // call "Profile|Stop Profiling Session"
        new Action(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_StopAction"), null).perform();
    }


    public void waitProgressDialog(String title, int milliseconds){
        try {
            // wait at most 120 second until progress dialog dismiss
            NbDialogOperator openingOper = new NbDialogOperator(title);
            openingOper.getTimeouts().setTimeout("ComponentOperator.WaitStateTimeout", milliseconds);  // NOI18N
            openingOper.waitClosed();
        } catch (TimeoutExpiredException e) {
            // ignore when progress dialog was closed before we started to wait for it
        }
    }

    public void issue144699Hack(){
        try {
            NbDialogOperator errDlg = new NbDialogOperator(Bundle.getStringTrimmed("org.openide.Bundle",
                                                            "NTF_ErrorTitle")); //"Error"
            errDlg.ok();
            String ProfileMenu = org.netbeans.jellytools.Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile"); //"Profile"
            new Action(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                                        "LBL_TakeSnapshotAction"), null).perform(); // "Take Snapshot of Collected Results"
        } catch (TimeoutExpiredException e) {
            // ignore when Error dialog did not appear (not 100% reproducible)
        }
    }
}