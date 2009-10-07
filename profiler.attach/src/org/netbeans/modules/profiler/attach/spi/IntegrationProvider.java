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

package org.netbeans.modules.profiler.attach.spi;

import java.io.IOException;
import java.util.ArrayList;
import org.netbeans.lib.profiler.common.AttachSettings;
import java.util.List;


/**
 *
 * @author Tomas Hurka
 * @author Jaroslav Bachorik
 */
public interface IntegrationProvider {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public class IntegrationHints {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private List hints;
        private List steps;
        private List warnings;
        private boolean warningsFirst = true;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public IntegrationHints() {
            steps = new ArrayList(10);
            hints = new ArrayList(10);
            warnings = new ArrayList(10);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public List getHints() {
            return copyLock(this.hints);
        }

        public List getSteps() {
            return copyLock(this.steps);
        }

        public List getWarnings() {
            return copyLock(this.warnings);
        }

        public void setWarningsFirst(boolean warningsFirst) {
            this.warningsFirst = warningsFirst;
        }

        public boolean isWarningsFirst() {
            return warningsFirst;
        }

        public void addHint(String hint) {
            this.hints.add(hint);
        }

        public void addStep(String step) {
            this.steps.add(step);
        }

        public void addStep(int stepIndex, String step) {
            this.steps.add(stepIndex, step);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public void clear() {
            this.hints.clear();
            this.steps.clear();
            this.warnings.clear();
        }

        public void removeHint(String hint) {
            this.hints.remove(hint);
        }

        public void removeStep(String step) {
            this.steps.remove(step);
        }

        public void removeWarning(String warning) {
            this.warnings.remove(warning);
        }

        private List copyLock(List source) {
            List immutable = new ArrayList();
            immutable.addAll(source);

            return immutable;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public IntegrationHints getAfterInstallationHints(AttachSettings settings, boolean automation);

    public IntegrationHints getIntegrationReview(AttachSettings settings);

    public IntegrationHints getModificationHints(AttachSettings settings);

    public void setTargetJava(String targetJava);

    public String getTargetJava();

    public void setTargetJavaHome(String targetJavaHome);

    public String getTargetJavaHome();

    public String getTitle();

    public void modify(AttachSettings settings) throws ModificationException;

    public void run(AttachSettings settings) throws RunException;
    
    public boolean supportsAutomation();

    public boolean supportsDirect();

    public boolean supportsDynamic();

    public boolean supportsLocal();

    public boolean supportsManual();

    public boolean supportsRemote();
}
