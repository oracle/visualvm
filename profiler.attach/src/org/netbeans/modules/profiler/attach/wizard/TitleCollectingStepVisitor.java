/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.profiler.attach.wizard;

import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStep;
import org.netbeans.modules.profiler.attach.wizard.steps.WizardStepVisitor;


/**
 *
 * @author j.bachorik
 */
public class TitleCollectingStepVisitor implements WizardStepVisitor {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private List<String> titles;

    private int currentTitleIndex = -1;
    private int titleIndexCounter = 0;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of TitleCollectingStepVisitor */
    public TitleCollectingStepVisitor() {
        this.titles = new ArrayList<String>(50);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String[] getTitleArray() {
        return (String[]) this.titles.toArray(new String[] {  });
    }

    public int getTitleIndex() {
        return currentTitleIndex;
    }

    public void visit(final WizardStep wizardStep, final WizardContext context, int level) {
        if ((level > 0) && (wizardStep.getTitle() != null) && (wizardStep.getTitle().length() != 0)) {
            StringBuffer title = new StringBuffer();
            title.append(wizardStep.getTitle());
            titles.add(title.toString());

            if (wizardStep.isCurrent()) {
                currentTitleIndex = titleIndexCounter;
            }

            titleIndexCounter++;
        }
    }
}
