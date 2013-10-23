/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.Level;
import javax.swing.JCheckBox;
import junit.framework.Test;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.JavaProjectsTabOperator;
import org.netbeans.jellytools.JellyTestCase;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.NewJavaProjectNameLocationStepOperator;
import org.netbeans.jellytools.NewProjectWizardOperator;
import org.netbeans.jellytools.OptionsOperator;
import org.netbeans.jellytools.OutputTabOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jellytools.actions.EditAction;
import org.netbeans.jellytools.nodes.JavaProjectRootNode;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.SourcePackagesNode;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.test.ide.WatchProjects;

/**
 * Validation test of profiler.
 *
 * @author Alexandr Scherbatiy, Jiri Skrivanek
 */
public class ProfilerValidationTest extends JellyTestCase {

    //private static final String SAMPLE_PROJECT_NAME = "AnagramGame";
    protected static final String PROFILER_ACTIONS_BUNDLE = "org.netbeans.modules.profiler.actions.Bundle";
    protected static final String PROFILER_UI_PANELS_BUNDLE = "org.netbeans.modules.profiler.options.ui.Bundle";
    protected static final String PROFILER_LIB_BUNDLE = "org.netbeans.lib.profiler.Bundle";

    /**
     * Default constructor.
     *
     * @param name test case name
     */
    public ProfilerValidationTest(String name) {
        super(name);
    }

    /**
     * Define order of test cases.
     *
     * @return NbTestSuite instance
     */
    public static Test suite() {
        NbModuleSuite.Configuration conf = NbModuleSuite.createConfiguration(
                ProfilerValidationTest.class).clusters(".*").enableModules(".*").honorAutoloadEager(true).failOnException(Level.SEVERE).failOnMessage(Level.SEVERE);
        conf = conf.addTest(
                "testProfilerCalibration",
                "testProfilerProperties",
                "testProfilerMenus",
                "testProfiler");
        return conf.suite();
    }

    /**
     * Setup before every test case.
     */
    @Override
    public void setUp() {
        System.out.println("########  " + getName() + "  #######");
    }

    /**
     * Test Profiler Menus.
     */
    public void testProfilerMenus() {
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

    /**
     * Test Profiler Properties.
     */
    public void testProfilerProperties() throws Exception {

        OptionsOperator options = OptionsOperator.invoke();

        options.selectJava(); // Changed to Java from Miscellaneous - see #200878

        JTabbedPaneOperator tabbedPane = new JTabbedPaneOperator(options);
        tabbedPane.selectPage(Bundle.getStringTrimmed("org.netbeans.modules.profiler.options.Bundle", "ProfilerOptionsCategory_Title")); //"Profiler"

//        JLabelOperator javaPlatform = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
//                                                                "ProfilerOptionsPanel_JavaPlatformLabelText")); //"Profiler Java Platform"

        JLabelOperator communicationPort = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_CommPortLabelText"));//"Communication Port"

        JLabelOperator openThreads = new JLabelOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_ThreadsViewLabelText"));//"Open Threads View"
        JCheckBoxOperator cpu = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_CpuChckBoxText"));//"CPU"
        JCheckBoxOperator memory = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_MemoryChckBoxText"));//"Memory"

        JComboBoxOperator openNewSnapshot = new JComboBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_OpenSnapshotRadioText"));//"Open New Snapshot"

        JCheckBoxOperator enableHeapAnalisys = new JCheckBoxOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_EnableAnalysisCheckbox")); //"Enable Rule-Based Heap Analysis"

        JButtonOperator reset = new JButtonOperator(options, Bundle.getStringTrimmed(PROFILER_UI_PANELS_BUNDLE,
                "ProfilerOptionsPanel_ResetButtonName")); //"Reset"

        options.ok();
        //java.util.logging.Logger.getLogger("global").log( java.util.logging.Level.SEVERE, "ok pushed" );
    }

    /**
     * Test profiler calibration - run profiler calibration Profile|Advanced
     * Commands|Manage Calibration Data - select default platform -
     * start calibration - wait for calibration results and
     * confirm information dialog
     */
    public void testProfilerCalibration() {
        String ProfileMenu = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile");
        String AdvansedCmds = Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE, "Menu/Profile/Advanced");
        String manageCalibrationLabel = "Manage Calibration Data";

        new ActionNoBlock(ProfileMenu + "|" + AdvansedCmds + "|" + manageCalibrationLabel, null).perform();
        NbDialogOperator manageOper = new NbDialogOperator(manageCalibrationLabel);
        JTableOperator platformsOper = new JTableOperator(manageOper);
        platformsOper.selectCell(0, 0);
        JButtonOperator calibrateButton = new JButtonOperator(manageOper, "Calibrate");
        calibrateButton.pushNoBlock();
        NbDialogOperator infoOper = new NbDialogOperator("Information");
        infoOper.ok();
        manageOper.closeByButton();
    }

    /**
     * Test profiler
     * - create sample project to be tested
     * - call Profile|Profile Main Project
     * - confirm changes in project when profiled for the first time
     * - click Run in Profile AnagramGame dialog
     * - wait for Profiler view
     * - wait until text "Established local connection with the tool" appears in output window
     * - wait until "Profile|Take Snapshot of Collected Results" is enabled
     * - call Profile|Take Snapshot of Collected Results
     * - maximize results view
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
        npwo.selectProject(Bundle.getStringTrimmed("org.netbeans.modules.java.examples.Bundle",
                "Templates/Project/Samples/Standard/anagrams.zip")); //"Anagram Game"
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
        String buildCategoryTitle = Bundle.getStringTrimmed("org.netbeans.modules.java.j2seproject.ui.customizer.Bundle", "LBL_Config_BuildCategory");
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
        MainWindowOperator.getDefault().waitStatusText(Bundle.getStringTrimmed("org.apache.tools.ant.module.run.Bundle", "FMT_finished_target_status")); // "Finished Building"
        // add log message to application to 
        Node anagramsNode = new Node(new SourcePackagesNode(projectNode), "ui|Anagrams.java");
        new EditAction().perform(anagramsNode);
        String visibleToken = "VISIBLE";
        new EditorOperator("Anagrams.java").replace("setVisible(true);", "setVisible(true);\nSystem.out.println(\"" + visibleToken + "\");");
        // call Profile|Profile Main Project
        new ActionNoBlock(ProfileMenu + "|" + Bundle.getStringTrimmed("org.netbeans.modules.profiler.actions.Bundle", "LBL_ProfileMainProjectAction"), null).perform();
        // click Run in Profile AnagramGame dialog
        NbDialogOperator profileOper = new NbDialogOperator(Bundle.getStringTrimmed("org.netbeans.modules.profiler.stp.Bundle",
                "SelectProfilingTask_ProfileDialogCaption")); // "Profile "+anagramGamePrName
        new JButtonOperator(profileOper, Bundle.getStringTrimmed("org.netbeans.modules.profiler.stp.Bundle",
                "SelectProfilingTask_RunButtonText")).push(); //"Run"
        profileOper.waitClosed();
        waitProgressDialog(Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                "NetBeansProfiler_ProgressDialogCaption"), 50000); // "Progress ..."
        TopComponentOperator tco = new TopComponentOperator(Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                "LAB_ControlPanelName")); // "Profiler"
        // wait for application visible
        OutputTabOperator oto = new OutputTabOperator("profile");
        oto.getTimeouts().setTimeout("ComponentOperator.WaitStateTimeout", 120000);
        oto.waitText(visibleToken);
        new EventTool().waitNoEvent(1000);
        Action takeSnapshotAction = new Action(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                "LBL_TakeSnapshotAction"), null);
        Waiter waiter = new Waiter(new Waitable() {
            @Override
            public Object actionProduced(Object takeSnapshotAction) {
                MainWindowOperator.getDefault().toFront();
                return ((Action) takeSnapshotAction).isEnabled() ? Boolean.TRUE : null;
            }

            @Override
            public String getDescription() {
                return ("Wait menu item is enabled."); // NOI18N
            }
        });
        waiter.getTimeouts().setTimeout("Waiter.WaitingTime", 60000);
        waiter.waitAction(takeSnapshotAction);
        new EventTool().waitNoEvent(1000);
        takeSnapshotAction.perform();
        TopComponentOperator collectedResults;
        try {
            collectedResults = new TopComponentOperator(Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                    "ResultsManager_CpuSnapshotDisplayName")); //"CPU"
        } catch (Exception e) {
            issue144699Hack();
            collectedResults = new TopComponentOperator(Bundle.getStringTrimmed("org.netbeans.modules.profiler.Bundle",
                    "ResultsManager_CpuSnapshotDisplayName")); //"CPU"
        }
        collectedResults.save();
        // call "Profile|Stop Profiling Session"
        new Action(ProfileMenu + "|" + Bundle.getStringTrimmed(PROFILER_ACTIONS_BUNDLE,
                "LBL_StopAction"), null).perform();
        waitProfilerStopped();
    }

    public void waitProgressDialog(String title, int milliseconds) {
        try {
            // wait at most 120 second until progress dialog dismiss
            NbDialogOperator openingOper = new NbDialogOperator(title);
            openingOper.getTimeouts().setTimeout("ComponentOperator.WaitStateTimeout", milliseconds);  // NOI18N
            openingOper.waitClosed();
        } catch (TimeoutExpiredException e) {
            // ignore when progress dialog was closed before we started to wait for it
        }
    }

    public void issue144699Hack() {
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
    
    /**
     * Waits until profiler is not stopped.
     */
    private void waitProfilerStopped() {
        try {
            new Waiter(new Waitable() {
                @Override
                public Object actionProduced(Object object) {
                    final int state = Profiler.getDefault().getProfilingState();
                    final int mode = Profiler.getDefault().getProfilingMode();
                    if ((state == Profiler.PROFILING_PAUSED) || (state == Profiler.PROFILING_RUNNING)) {
                        if (mode == Profiler.MODE_PROFILE) {
                            return null;
                        }
                    }
                    return Boolean.TRUE;
                }

                @Override
                public String getDescription() {
                    return ("Wait profiler stopped."); // NOI18N
                }
            }).waitAction(null);
        } catch (InterruptedException ex) {
            throw new JemmyException("Waiting for profiler stopped failed.", ex);
        }
    }
}
