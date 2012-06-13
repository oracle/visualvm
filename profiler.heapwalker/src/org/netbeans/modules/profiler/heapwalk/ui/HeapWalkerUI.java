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

package org.netbeans.modules.profiler.heapwalk.ui;

import org.netbeans.modules.profiler.heapwalk.HeapWalker;
import org.netbeans.modules.profiler.heapwalk.HeapWalkerManager;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import org.netbeans.modules.profiler.ProfilerTopComponent;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.api.icons.ProfilerIcons;
import org.openide.util.HelpCtx;


/**
 *
 * @author Jiri Sedlacek
 */
public class HeapWalkerUI extends ProfilerTopComponent {
    private static final String HELP_CTX_KEY = "HeapWalker.HelpCtx"; // NOI18N
    private static final HelpCtx HELP_CTX = new HelpCtx(HELP_CTX_KEY);
    
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private HeapWalker heapWalker;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    // --- Constructors ----------------------------------------------------------
    public HeapWalkerUI(HeapWalker heapWalker) {
        this.heapWalker = heapWalker;

        initDefaults();
        initComponents();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    protected Component defaultFocusOwner() {
        return heapWalker.getMainHeapWalker().getPanel();
    }

    // --- TopComponent support --------------------------------------------------
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    protected void componentClosed() {
        HeapWalkerManager.getDefault().heapWalkerClosed(heapWalker);
    }

    protected String preferredID() {
        return this.getClass().getName();
    }
    
    public HelpCtx getHelpCtx() {
        return HELP_CTX;
    }

    // --- UI definition ---------------------------------------------------------
    private void initComponents() {
        setLayout(new BorderLayout());
        add(heapWalker.getMainHeapWalker().getPanel(), BorderLayout.CENTER);
    }

    @NbBundle.Messages("HeapWalkerUI_ComponentDescr=Profiler HeapWalker")
    private void initDefaults() {
        setName(heapWalker.getName());
        setIcon(Icons.getImage(ProfilerIcons.HEAP_DUMP));
        if (heapWalker.getHeapDumpFile() != null)
            setToolTipText(heapWalker.getHeapDumpFile().getAbsolutePath());
        getAccessibleContext().setAccessibleDescription(Bundle.HeapWalkerUI_ComponentDescr());
        
        putClientProperty("HeapDumpFileName", heapWalker.getHeapDumpFile().toURI().getPath()); // NOI18N
    }
}
