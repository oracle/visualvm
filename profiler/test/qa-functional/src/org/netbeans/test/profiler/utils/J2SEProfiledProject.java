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
import javax.swing.JCheckBox;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.JavaProjectsTabOperator;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.NewJavaProjectNameLocationStepOperator;
import org.netbeans.jellytools.NewProjectWizardOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jemmy.JemmyProperties;
import org.netbeans.jellytools.nodes.JavaProjectRootNode;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JTreeOperator;
import org.netbeans.test.ide.WatchProjects;


/**
 * Class representing basic javaSE project, inherited from BaseProfiledProject.
 * The project (anagram game) is created in the constructor of the class.
 * All other methods are implemented in the BaseProfiledProject class.
 * @author Matus Dekanek
 */
public class J2SEProfiledProject extends BaseProfiledProject{

	public J2SEProfiledProject() {
        // create sample Anagram Game Java project
        NewProjectWizardOperator npwo = NewProjectWizardOperator.invoke();
        String samplesLbl = Bundle.getStringTrimmed("org.netbeans.modules.project.ui.Bundle", "Templates/Project/Samples"); // "Samples"
        String javaLbl = Bundle.getStringTrimmed("org.netbeans.modules.java.examples.Bundle", "Templates/Project/Samples/Standard"); // "Java"
        npwo.selectCategory(samplesLbl + "|" + javaLbl);
        npwo.selectProject( Bundle.getStringTrimmed( "org.netbeans.modules.java.examples.Bundle",
                            "Templates/Project/Samples/Standard/anagrams.zip") ); //"Anagram Game"
        npwo.next();
        NewJavaProjectNameLocationStepOperator npnlso = new NewJavaProjectNameLocationStepOperator();
        m_name = npnlso.txtProjectName().getText();
        npnlso.txtProjectLocation().setText(System.getProperty("netbeans.user")); // NOI18N
        npnlso.finish();
        //wait project appear in projects view
        //wait 30 second
        JemmyProperties.setCurrentTimeout("JTreeOperator.WaitNextNodeTimeout", 30000); // NOI18N

        //disable the compile on save:
        ProjectsTabOperator.invoke().getProjectRootNode(m_name).properties();
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

        //wait classpath scanning finished
        WatchProjects.waitScanFinished();
	}



}
