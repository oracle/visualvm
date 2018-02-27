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


package org.netbeans.modules.profiler.snaptracer.impl.export;

import org.netbeans.modules.profiler.snaptracer.TracerProgressObject;
import java.io.IOException;
import java.io.Writer;
import javax.swing.table.TableModel;

/**
 *
 * @author Jiri Sedlacek
 */
final class XMLExporter extends Exporter {

    private float step = 1;
    private int lastStep = 0;


    protected int getSteps(TableModel model) {
        int steps = model.getRowCount();
        if (steps > MAX_STEPS) {
            step = MAX_STEPS / (float)steps;
            steps = MAX_STEPS;
        }
        return steps;
    }

    protected void writeHeader(TableModel model, String title, Writer writer,
                               TracerProgressObject progress) throws IOException {
        writeLine(writer, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"); // NOI18N
        writeLine(writer, "<ExportedView Name=\"" + title + "\">"); // NOI18N
    }

    protected void writeData(TableModel model, String title, Writer writer,
                             TracerProgressObject progress) throws IOException {
        int columnsCount = model.getColumnCount();
        int rowsCount = model.getRowCount();

        writeLine(writer, "  <TableData NumRows=\"" + rowsCount + // NOI18N
                             "\" NumColumns=\"" + columnsCount + "\">"); // NOI18N

        writeLine(writer, "    <TableHeader>"); // NOI18N
        for (int c = 0; c < columnsCount; c++)
            writeLine(writer, "      <TableColumn>" + model.getColumnName(c) + "</TableColumn>"); // NOI18N
        writeLine(writer, "    </TableHeader>"); // NOI18N

        writeLine(writer, "    <TableBody>"); // NOI18N
        for (int r = 0; r < rowsCount; r++) {
            writeLine(writer, "      <TableRow>"); // NOI18N
            for (int c = 0; c < columnsCount; c++)
                writeLine(writer, "        <TableColumn>" + model.getValueAt(r, c) + "</TableColumn>"); // NOI18N
            writeLine(writer, "      </TableRow>"); // NOI18N
            
            if (progress.isFinished()) break;

            if (step == 1) {
                progress.addStep();
            } else {
                int currentStep = (int)(r * step);
                if (currentStep > lastStep) {
                    progress.addStep();
                    lastStep = currentStep;
                }
            }
        }
        writeLine(writer, "    </TableBody>"); // NOI18N

        writeLine(writer, "  </TableData>"); // NOI18N
    }

    protected void writeFooter(TableModel model, String title, Writer writer,
                               TracerProgressObject progress) throws IOException {
        writeLine(writer, "</ExportedView>"); // NOI18N
    }

}
