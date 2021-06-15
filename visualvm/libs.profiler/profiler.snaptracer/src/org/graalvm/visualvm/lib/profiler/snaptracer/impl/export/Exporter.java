/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.graalvm.visualvm.lib.profiler.snaptracer.impl.export;

import org.graalvm.visualvm.lib.profiler.snaptracer.TracerProgressObject;
import java.io.IOException;
import java.io.Writer;
import javax.swing.table.TableModel;

/**
 *
 * @author Jiri Sedlacek
 */
abstract class Exporter {

    protected static final int MAX_STEPS = 300; // Defines also JProgressBar width


    protected abstract int getSteps(TableModel model);

    protected void writeHeader(TableModel model, String title, Writer writer,
                               TracerProgressObject progress) throws IOException {}

    protected void writeData(TableModel model, String title, Writer writer,
                             TracerProgressObject progress) throws IOException {}

    protected void writeFooter(TableModel model, String title, Writer writer,
                               TracerProgressObject progress) throws IOException {}


    protected static void writeLine(Writer writer) throws IOException {
        writer.write("\n"); // NOI18N
    }

    protected static void writeLine(Writer writer, String line) throws IOException {
        writer.write(line + "\n"); // NOI18N
    }

    protected static void write(Writer writer, String text) throws IOException {
        writer.write(text);
    }


    final ExportBatch createBatch(final TableModel model, final String title,
                                  final Writer writer) {

        final TracerProgressObject progress = new TracerProgressObject(getSteps(model) + 2);

        ExportBatch.BatchRunnable worker = new ExportBatch.BatchRunnable() {
            public void run() throws IOException {
                doExport(model, title, writer, progress);
            }
        };

        return new ExportBatch(progress, worker);
    }

    private void doExport(TableModel model, String title, Writer writer,
                          TracerProgressObject progress) throws IOException {
        progress.setText("Initializing export...");
        writeHeader(model, title, writer, progress);

        if (progress.isFinished()) return;

        progress.addStep("Exporting data...");
        writeData(model, title, writer, progress);

        if (progress.isFinished()) return;

        progress.setText("Finishing export...");
        writeFooter(model, title, writer, progress);

        if (progress.isFinished()) return;

        progress.setText("Data exported");
        progress.finish();
    }

}
