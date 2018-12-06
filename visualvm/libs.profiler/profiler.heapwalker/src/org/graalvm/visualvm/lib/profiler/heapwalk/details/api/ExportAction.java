/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.graalvm.visualvm.lib.profiler.heapwalk.details.api;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.graalvm.visualvm.lib.jfluid.results.ExportDataDumper;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.graalvm.visualvm.lib.profiler.ui.NBSwingWorker;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Petr Cyhelsky
 */


@NbBundle.Messages({
    "ExportAction_BasicExportActionName=Export to...",
    "ExportAction_BasicExportActionDescr=Export to...",
    "ExportAction_ExportDialogTitle=Select File or Directory",
    "ExportAction_ExportDialogButton=Export",
    "ExportAction_OverwriteFileCaption=Overwrite Existing File",
    "ExportAction_OverwriteFileMsg=<html><b>File {0} already exists.</b><br><br>Do you want to replace it?</html>",
    "ExportAction_ExportDialogCSVFilter=CSV File (*.csv)",
    "ExportAction_ExportDialogTXTFilter=Text File (*.txt)",
    "ExportAction_ExportDialogBINFilter=Binary File (*.bin)",
    "ExportAction_ExportingViewMsg=Exporting...",
    "ExportAction_NoViewMsg=No view to export.",
    "ExportAction_OomeExportingMsg=<html><b>Not enough memory to save the file.</b><br><br>To avoid this error please increase the -Xmx value<br>in the etc/visualvm.conf file in VisualVM directory.</html>",
    "ExportAction_IOException_Exporting_Msg=<html>IOException occurred during export, see IDE log for details</html>",
    "ExportAction_CannotWriteFileMsg=Failed to export File. Reason: {0}."})
public final class ExportAction extends AbstractAction {
    private static final Logger LOGGER = Logger.getLogger(ExportAction.class.getName());

//~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface ExportProvider {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void exportData(int exportedFileType, ExportDataDumper eDD);

        public String getViewName();

        public boolean isExportable();

        public boolean hasRawData();
        
        public boolean hasBinaryData();

        public boolean hasText();

    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------


    private static class SelectedFile {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        File folder;
        String fileExt;
        String fileName;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        SelectedFile(File folder, String fileName, String fileExt) {
            this.folder = folder;
            this.fileName = fileName;
            this.fileExt = fileExt;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        File getSelectedFile() {
            String folderPath=folder.getAbsolutePath();
            if (folderPath.endsWith(File.separator)) {
                folderPath=folderPath.substring(0, folderPath.length()-1);
            }
            return new File(folderPath + File.separator + fileName+ "." + fileExt);
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Icon ICON = Icons.getIcon(GeneralIcons.EXPORT);
    private static final String FILE_EXTENSION_CSV = "csv"; // NOI18N
    private static final String FILE_EXTENSION_TXT = "txt"; // NOI18N
    private static final String FILE_EXTENSION_BIN = "bin"; // NOI18N
    public static final int MODE_CSV = 1;
    public static final int MODE_TXT = 2;
    public static final int MODE_BIN = 3;
    private static File exportDir;


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JFileChooser fileChooser;
    private final ExportProvider exportProvider;
    private int exportedFileType;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ExportAction(ExportProvider exportProvider) {
        putValue(Action.NAME, Bundle.ExportAction_BasicExportActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.ExportAction_BasicExportActionDescr());
        putValue(Action.SMALL_ICON, ICON);
        putValue("iconBase", Icons.getResource(GeneralIcons.EXPORT)); // NOI18N
        this.exportProvider = exportProvider;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    private void setFilters() {
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        if (exportProvider.hasRawData()) {
            fileChooser.addChoosableFileFilter(new FileFilterImpl(FILE_EXTENSION_CSV));
        }
        if (exportProvider.hasText()) {
            fileChooser.addChoosableFileFilter(new FileFilterImpl(FILE_EXTENSION_TXT));
        }
        if (exportProvider.hasBinaryData()) {
            fileChooser.addChoosableFileFilter(new FileFilterImpl(FILE_EXTENSION_BIN));
        }
    }


    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            // File chooser
            fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setDialogTitle(Bundle.ExportAction_ExportDialogTitle());
            fileChooser.setApproveButtonText(Bundle.ExportAction_ExportDialogButton());
        }
        fileChooser.resetChoosableFileFilters();
        setFilters();
        return fileChooser;
    }

    private boolean checkFileExists(File target) {
        if (target.exists()) {
            if (!ProfilerDialogs.displayConfirmation(
                    Bundle.ExportAction_OverwriteFileMsg(target.getName()),
                    Bundle.ExportAction_OverwriteFileCaption())) {  // choose whether to overwrite
                  return false; // user chose not to overwrite
              }
          }
          return true;
      }

    private SelectedFile selectExportTargetFile(final ExportProvider exportProvider) {
        File targetDir;
        String targetName;
        String defaultName = exportProvider.getViewName();

        // 1. let the user choose file or directory
        final JFileChooser chooser = getFileChooser();
        if (exportDir != null) {
            chooser.setCurrentDirectory(exportDir);
        }
        int result = chooser.showSaveDialog(WindowManager.getDefault().getRegistry().getActivated());
        if (result != JFileChooser.APPROVE_OPTION) {
            return null; // cancelled by the user
        }

        // 2. process both cases and extract file name and extension to use and set exported file type
        File file = chooser.getSelectedFile();
        String targetExt = null;
        FileFilter selectedFileFilter = chooser.getFileFilter();
        if (selectedFileFilter==null  // workaround for #227659
                ||  selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogCSVFilter())) {
            targetExt=FILE_EXTENSION_CSV;
            exportedFileType=MODE_CSV;
        } else if (selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogTXTFilter())) {
            targetExt=FILE_EXTENSION_TXT;
            exportedFileType=MODE_TXT;
        } else if (selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogBINFilter())) {
            targetExt=FILE_EXTENSION_BIN;
            exportedFileType=MODE_BIN;
        }

        if (file.isDirectory()) { // save to selected directory under default name
            exportDir = file;
            targetDir = file;
            targetName = defaultName;
        } else { // save to selected file
            targetDir = fileChooser.getCurrentDirectory();
            String fName = file.getName();

            // divide the file name into name and extension
            if (fName.endsWith("."+targetExt)) {  // NOI18N
                int idx = fName.lastIndexOf('.'); // NOI18N
                targetName = fName.substring(0, idx);
            } else {            // no extension
                targetName=fName;
            }
        }

        // 3. set type of exported file and return a newly created FileObject

        return new ExportAction.SelectedFile(targetDir, targetName, targetExt);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (!exportProvider.hasRawData() && !exportProvider.hasText()) { // nothing to export
            ProfilerDialogs.displayError(Bundle.ExportAction_NoViewMsg());
            return;
        }

        SelectedFile saveFile = selectExportTargetFile(exportProvider);

        if (saveFile == null) return; // cancelled

        final File file = saveFile.getSelectedFile();
        if (!checkFileExists(file)) return; // user doesn't want to overwrite existing file or it can't be overwritten

        new NBSwingWorker(true) {
            final private ProgressHandle ph = ProgressHandle.createHandle(Bundle.ExportAction_ExportingViewMsg());
            @Override
            protected void doInBackground() {
                ph.setInitialDelay(500);
                ph.start();

                try {
                    FileOutputStream fo;
                    fo = new FileOutputStream(file);
                    ExportDataDumper eDD = new ExportDataDumper(fo);
                    exportProvider.exportData(exportedFileType, eDD);
                    if (eDD.getCaughtException()!=null) {
                        ProfilerDialogs.displayError(eDD.getNumExceptions()+Bundle.ExportAction_IOException_Exporting_Msg());
                    }
                } catch (OutOfMemoryError e) {
                    ProfilerDialogs.displayError(Bundle.ExportAction_OomeExportingMsg()+e.getMessage());
                } catch (IOException e1) {
                    ProfilerDialogs.displayError(Bundle.ExportAction_CannotWriteFileMsg(e1.getLocalizedMessage()));
                    LOGGER.log(Level.WARNING, e1.toString());
                }
            }

            @Override
            protected void done() {
                ph.finish();
            }
        }.execute();
    }

    private static class FileFilterImpl extends FileFilter {
        
        private final String extension;

        public FileFilterImpl(String ext) {
            extension = ext;
        }

        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase(Locale.US).endsWith("."+extension); //NOI18N
        }

        @Override
        public String getDescription() {
            if (FILE_EXTENSION_CSV.equals(extension)) {
                return Bundle.ExportAction_ExportDialogCSVFilter();
            } else if (FILE_EXTENSION_TXT.equals(extension)) {
                return Bundle.ExportAction_ExportDialogTXTFilter();
            } else if (FILE_EXTENSION_BIN.equals(extension)) {
                return Bundle.ExportAction_ExportDialogBINFilter();
            } else {
                return null;
            }
            
        }
    }
}

