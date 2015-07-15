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

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
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
import org.netbeans.jellytools.TopComponentOperator;
import org.netbeans.jellytools.actions.Action;
import org.netbeans.jellytools.actions.EditAction;
import org.netbeans.jellytools.nodes.JavaProjectRootNode;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.SourcePackagesNode;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.operators.ContainerOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JListOperator;
import org.netbeans.jemmy.operators.JPopupMenuOperator;
import org.netbeans.jemmy.operators.JSpinnerOperator;
import org.netbeans.jemmy.operators.JTabbedPaneOperator;
import org.netbeans.jemmy.operators.JTableOperator;
import org.netbeans.lib.profiler.common.Profiler;

/**
 * Validation test of profiler.
 *
 * @author Alexandr Scherbatiy, Jiri Skrivanek
 */
public class ProfilerValidationTest extends JellyTestCase {

    //private static final String SAMPLE_PROJECT_NAME = "AnagramGame";
    private static final String VISIBLE_TOKEN = "VISIBLE";

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
        return emptyConfiguration().failOnException(Level.SEVERE).failOnMessage(Level.SEVERE)
                .addTest(ProfilerValidationTest.class,
                        "testCreateProject",
                        "testMenus",
                        "testOptions",
                        "testProfiler")
                .suite();
    }

    /**
     * Setup before every test case.
     */
    @Override
    public void setUp() {
        System.out.println("########  " + getName() + "  #######");
    }

    /**
     * Test create sample project to be profiled
     */
    public void testCreateProject() {
        // create sample Anagram Game Java project
        NewProjectWizardOperator npwo = NewProjectWizardOperator.invoke();
        String samplesLbl = Bundle.getStringTrimmed("org.netbeans.modules.project.ui.Bundle", "Templates/Project/Samples"); // "Samples"
        String javaLbl = Bundle.getStringTrimmed("org.netbeans.modules.java.examples.Bundle", "Templates/Project/Samples/Standard"); // "Java"
        npwo.selectCategory(samplesLbl + "|" + javaLbl);
        npwo.selectProject(Bundle.getStringTrimmed("org.netbeans.modules.java.examples.Bundle",
                "Templates/Project/Samples/Standard/anagrams.zip")); //"Anagram Game"
        npwo.next();
        NewJavaProjectNameLocationStepOperator npnlso = new NewJavaProjectNameLocationStepOperator();
        String anagramGamePrName = npnlso.txtProjectName().getText();
        npnlso.txtProjectLocation().setText(System.getProperty("netbeans.user")); // NOI18N
        npnlso.btFinish().pushNoBlock();
        npnlso.getTimeouts().setTimeout("ComponentOperator.WaitStateTimeout", 120000);
        npnlso.waitClosed();
        // wait project appear in projects view
        JavaProjectRootNode projectNode = new JavaProjectsTabOperator().getJavaProjectRootNode(anagramGamePrName);
        // add log message to application to 
        Node anagramsNode = new Node(new SourcePackagesNode(projectNode), "ui|Anagrams.java");
        new EditAction().perform(anagramsNode);
        EditorOperator eo = new EditorOperator("Anagrams.java");
        eo.replace("setVisible(true);", "setVisible(true);\nSystem.out.println(\"" + VISIBLE_TOKEN + "\");");
        // build project
        projectNode.buildProject();
        MainWindowOperator.getDefault().waitStatusText(Bundle.getStringTrimmed("org.apache.tools.ant.module.run.Bundle", "FMT_finished_target_status")); // "Finished Building"
        eo.closeDiscard();
        waitScanFinished();
    }

    /**
     * Test Profiler Menus.
     */
    public void testMenus() {
        String item = "Profile|Profile Project";
        assertTrue("Menu item in incorrect state: " + item, new Action(item, null).isEnabled());
        item = "Profile|Attach to External Process";
        assertTrue("Menu item in incorrect state: " + item, new Action(item, null).isEnabled());
        item = "Profile|Take Snapshot of Collected Results";
        assertFalse("Menu item in incorrect state: " + item, new Action(item, null).isEnabled());
        item = "Profile|Finish Profiler Session";
        assertFalse("Menu item in incorrect state: " + item, new Action(item, null).isEnabled());
        MainWindowOperator.getDefault().pushKey(KeyEvent.VK_ESCAPE);
    }

    /**
     * Test Profiler options.
     */
    public void testOptions() {
        OptionsOperator options = OptionsOperator.invoke();
        options.selectJava();
        JTabbedPaneOperator tabbedPane = new JTabbedPaneOperator(options);
        tabbedPane.selectPage("Profiler");
        JListOperator categoriesOper = new JListOperator(options);
        // General category
        assertEquals("Wrong profiling port.", 5140, new JSpinnerOperator(options).getValue());
        // manage calibration data
        new JButtonOperator(options, "Manage").pushNoBlock();
        NbDialogOperator manageOper = new NbDialogOperator("Manage Calibration data");
        JTableOperator platformsOper = new JTableOperator(manageOper);
        platformsOper.selectCell(0, 0);
        new JButtonOperator(manageOper, "Calibrate").pushNoBlock();
        new NbDialogOperator("Information").ok();
        manageOper.closeByButton();
        // reset
        new JButtonOperator(options, "Reset").push();
        // Snapshots category
        categoriesOper.selectItem("Snapshots");
        JLabelOperator lblSnapshotOper = new JLabelOperator(options, "When taking snapshot:");
        assertEquals("Wrong value for " + lblSnapshotOper.getText(), "Open snapshot", new JComboBoxOperator((JComboBox) lblSnapshotOper.getLabelFor()).getSelectedItem());
        JLabelOperator lblOpenOper = new JLabelOperator(options, "Open automatically:");
        assertEquals("Wrong value for " + lblOpenOper.getText(), "On first saved snapshot", new JComboBoxOperator((JComboBox) lblOpenOper.getLabelFor()).getSelectedItem());
        // Engine category
        categoriesOper.selectItem("Engine");
        JLabelOperator lblSamplingOper = new JLabelOperator(options, "Sampling frequency");
        assertEquals("Wrong value for " + lblSamplingOper.getText(), 10, new JSpinnerOperator((JSpinner) lblSamplingOper.getLabelFor()).getValue());
        options.cancel();
    }

    /**
     * Test profiler<br>
     * - call Profile|Profile Project<br>
     * - click arrow button in opened profiling TopComponent<br>
     * - in opened popup click Methods<br>
     * - click Profile button to start profiling<br>
     * - wait until text token appears in output window<br>
     * - click "Snapshot" button in tool bar<br>
     * - wait for snapshot TopComponent<br>
     * - save snapshot<br>
     * - call "Profile|Finish Profiler Session"<br>
     */
    public void testProfiler() {
        new Action("Profile|Profile Project", null).perform();
        TopComponentOperator tcProfiler = new TopComponentOperator("AnagramGame");
        JButtonOperator btnArrow = new JButtonOperator(tcProfiler, new ComponentChooser() {

            @Override
            public boolean checkComponent(Component comp) {
                return comp.getClass().getName().endsWith("Popup");
            }

            @Override
            public String getDescription() {
                return "org.netbeans.modules.profiler.v2.ui.DropdownButton$Popup";
            }
        });
        //using doClick because btnArrow is unstandard component and btnArrow.push(); sometimes fails
        btnArrow.doClick();
        new JPopupMenuOperator().pushMenu("Methods");
        new JButtonOperator(tcProfiler, "Profile").push();
        // wait for application visible
        OutputTabOperator oto = new OutputTabOperator("profile");
        oto.getTimeouts().setTimeout("ComponentOperator.WaitStateTimeout", 120000);
        oto.waitText(VISIBLE_TOKEN);
        new EventTool().waitNoEvent(1000);
        // create snapshot
        new JButtonOperator(tcProfiler, new ComponentChooser() {

            private final String TOOLTIP = "Take snapshot of collected results";

            @Override
            public boolean checkComponent(Component comp) {
                return TOOLTIP.equals(((JComponent) comp).getToolTipText());
            }

            @Override
            public String getDescription() {
                return "tooltip is " + TOOLTIP;
            }
        }).push();
        TopComponentOperator tcSnapshot = new TopComponentOperator(new ContainerOperator(MainWindowOperator.getDefault(), new ComponentChooser() {

            @Override
            public boolean checkComponent(Component comp) {
                return comp.getClass().getName().endsWith("SnapshotResultsWindow");
            }

            @Override
            public String getDescription() {
                return "org.netbeans.modules.profiler.SnapshotResultsWindow";
            }
        }));
        tcSnapshot.save();
        // stop profiler
        new Action("Profile|Finish Profiler Session", null).perform();
        waitProfilerStopped();
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
