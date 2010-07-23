/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.SwingUtilities;
import org.netbeans.modules.profiler.SampledCPUSnapshot;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

public final class IdeSnapshotAction implements ActionListener {
    
    public void actionPerformed(ActionEvent e) {
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                final SampledCPUSnapshot snapshot = snapshot();
                if (snapshot == null) return;

                final TracerModel model = new TracerModel(snapshot);
                final TracerController controller = new TracerController(model);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        TopComponent ui = ui(model, controller);
                        ui.open();
                        ui.requestActive();
                    }
                });
            }
        });
    }


    private TopComponent ui(TracerModel model, TracerController controller) {
        TopComponent tc = new IdeSnapshotComponent();
        TracerView tracer = new TracerView(model, controller);
        tc.add(tracer.createComponent(), BorderLayout.CENTER);
        return tc;
    }

    private SampledCPUSnapshot snapshot() {
        File file = snapshotFile();
        if (file == null) return null;

        try { return new SampledCPUSnapshot(file); }
        catch (Throwable t) { Exceptions.printStackTrace(t); return null; }
    }

    private File snapshotFile() {
        return new File("C:\\Users\\JiS\\Documents\\snapshot_tracer_test.npss");
    }

    
    private static class IdeSnapshotComponent extends TopComponent {

        IdeSnapshotComponent() {
            setDisplayName("IDE Snapshot");
            setLayout(new BorderLayout());
        }

        public int getPersistenceType() { return PERSISTENCE_NEVER; }

    }

}
