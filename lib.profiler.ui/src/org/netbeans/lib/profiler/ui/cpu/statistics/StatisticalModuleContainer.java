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

package org.netbeans.lib.profiler.ui.cpu.statistics;

import org.netbeans.lib.profiler.results.RuntimeCCTNode;
import org.netbeans.lib.profiler.results.cpu.CPUCCTProvider;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 *
 * @author Jaroslav Bachorik
 */
public class StatisticalModuleContainer implements CPUCCTProvider.Listener {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Set /*<StatisticalModule>*/ modules = new HashSet();

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of StatisticalModuleContainer */
    public StatisticalModuleContainer() {
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public Collection getAllModules() {
        return new ArrayList(modules);
    }

    public void addModule(StatisticalModule module) {
        modules.add(module);
    }

    public void cctEstablished(RuntimeCCTNode appNode, boolean empty) {
        if (empty) {
            return;
        }

        if (!(appNode instanceof RuntimeCPUCCTNode)) {
            return;
        }

        Set tmpModules = null;

        synchronized (modules) {
            if (modules.isEmpty()) {
                return;
            }

            tmpModules = new HashSet(modules);
        }

        for (Iterator iter = tmpModules.iterator(); iter.hasNext();) {
            ((StatisticalModule) iter.next()).refresh((RuntimeCPUCCTNode) appNode);
        }
    }

    public void cctReset() {
        cctEstablished(null, false);
    }

    public void removeAllModules() {
        modules.clear();
    }

    public void removeModule(StatisticalModule module) {
        modules.remove(module);
    }
}
