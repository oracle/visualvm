/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.profiler;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.lib.profiler.common.Profiler;
import org.netbeans.lib.profiler.results.ExportDataDumper;
import org.netbeans.modules.profiler.api.ProfilerDialogs;
import org.netbeans.modules.profiler.api.icons.GeneralIcons;
import org.netbeans.modules.profiler.api.icons.Icons;
import org.netbeans.modules.profiler.ui.NBSwingWorker;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author cyhelsky
 */
@NbBundle.Messages({
    "ExportAction_ExportActionName=Export to...",
    "ExportAction_ExportActionDescr=Export to...",
    "ExportAction_ExportingViewMsg=Exporting...",
    "ExportAction_NoViewMsg=No view to export.",
    "ExportAction_OomeExportingMsg=<html><b>Not enough memory to save the file.</b><br><br>To avoid this error increase the -Xmx<br>value in the etc/netbeans.conf file in NetBeans IDE installation.</html>",
    "ExportAction_IOException_Exporting_Msg=<html>IOException occurred during export, see IDE log for details</html>",
    "ExportAction_ExportToItselfMsg=Exporting the snapshot to itself.",
    "ExportAction_OverwriteFileCaption=Overwrite Existing File",
    "ExportAction_OverwriteFileMsg=<html><b>File {0} already exists.</b><br><br>Do you want to replace it?</html>",
    "ExportAction_CannotWriteFileMsg=Failed to export File. Reason: {0}.",
    "ExportAction_InvalidLocationForFileMsg=Invalid location for file.",
    "ExportAction_ExportDialogTitle=Select File or Directory",
    "ExportAction_ExportDialogButton=Export",
    "ExportAction_ExportDialogCSVFilter=CSV File (*.csv)",
    "ExportAction_ExportDialogExcelFilter=Excel Compatible CSV (*.csv)",
    "ExportAction_ExportDialogXMLFilter=XML File (*.xml)",
    "ExportAction_ExportDialogHTMLFilter=Web page (*.html)",
    "ExportAction_ExportDialogNPSFilter=Profiler Snapshot File (*.nps)",
    "ExportAction_SavingSnapshot=Saving snapshot..."
})
public final class ExportAction extends AbstractAction {
    private static final Logger LOGGER = Logger.getLogger(ExportAction.class.getName());
    
//~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    public static interface ExportProvider {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void exportData(int exportedFileType, ExportDataDumper eDD);

        public String getViewName();

        public boolean hasExportableView();

        public boolean hasLoadedSnapshot();
        
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
    private static final String FILE_EXTENSION_XML = "xml"; // NOI18N
    private static final String FILE_EXTENSION_HTML = "html"; // NOI18N
    private static final String FILE_EXTENSION_NPS = "nps"; // NOI18N
    public static final int MODE_CSV = 1;
    public static final int MODE_EXCEL = 2;
    public static final int MODE_XML = 3;
    public static final int MODE_HTML = 4;
    public static final int MODE_NPS = 5;
    private static File exportDir;


    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private JFileChooser fileChooser;
    private ExportProvider exportProvider;
    private int exportedFileType;
    private LoadedSnapshot snapshot;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ExportAction(ExportProvider exportProvider, LoadedSnapshot loadedSnapshot) {
        putValue(Action.NAME, Bundle.ExportAction_ExportActionName());
        putValue(Action.SHORT_DESCRIPTION, Bundle.ExportAction_ExportActionDescr());
        putValue(Action.SMALL_ICON, ICON);
        putValue("iconBase", Icons.getResource(GeneralIcons.EXPORT)); // NOI18N
        this.exportProvider = exportProvider;
        if (!(loadedSnapshot==null)) {
            this.snapshot=loadedSnapshot;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    private void setFilters() {
        fileChooser.removeChoosableFileFilter(fileChooser.getAcceptAllFileFilter());
        if (exportProvider.hasExportableView()) {
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith("."+FILE_EXTENSION_XML);
                }

                @Override
                public String getDescription() {
                    return Bundle.ExportAction_ExportDialogXMLFilter();
                }
            });
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith("."+FILE_EXTENSION_HTML);
                }

                @Override
                public String getDescription() {
                    return Bundle.ExportAction_ExportDialogHTMLFilter();
                }
            });
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith("."+FILE_EXTENSION_CSV);
                }

                @Override
                public String getDescription() {
                    return Bundle.ExportAction_ExportDialogExcelFilter();
                }
            });
            fileChooser.addChoosableFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith("."+FILE_EXTENSION_CSV);
                }

                @Override
                public String getDescription() {
                    return Bundle.ExportAction_ExportDialogCSVFilter();
                }
            });
        }
        if (exportProvider.hasLoadedSnapshot()) {
            fileChooser.addChoosableFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith("."+FILE_EXTENSION_NPS);
                }

                @Override
                public String getDescription() {
                    return Bundle.ExportAction_ExportDialogNPSFilter();
                }
            });
            // If there is snapshot, .nps must be selected as file filter
            FileFilter[] currentFilters = fileChooser.getChoosableFileFilters();
            for (int i = 0; i < currentFilters.length; i++) {
                if (currentFilters[i].getDescription().equals(Bundle.ExportAction_ExportDialogNPSFilter())) {
                    fileChooser.setFileFilter(currentFilters[i]);
                }
            }
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

      private boolean checkFileExists(File source, File target) {
          if (target.exists()) {
              if (source!=null && source.equals(target)) { // do not allow to overwrite the source nps
                  ProfilerDialogs.displayError(Bundle.ExportAction_ExportToItselfMsg());
                  return false;
              } else if (!ProfilerDialogs.displayConfirmation(
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
        String targetExt;        
        FileFilter selectedFileFilter = chooser.getFileFilter();
        if (selectedFileFilter==null // workaround for #233652
                || selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogXMLFilter())) {
            targetExt=FILE_EXTENSION_XML;
            exportedFileType=MODE_XML;
        } else if (selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogHTMLFilter())) {
            targetExt=FILE_EXTENSION_HTML;
            exportedFileType=MODE_HTML;
        } else if (selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogExcelFilter())) {
            targetExt = FILE_EXTENSION_CSV;
            exportedFileType=MODE_EXCEL;
        } else if (selectedFileFilter.getDescription().equals(Bundle.ExportAction_ExportDialogNPSFilter())) {
            targetExt = FILE_EXTENSION_NPS;
            exportedFileType=MODE_NPS;
        } else { // CSV is default
            targetExt = FILE_EXTENSION_CSV;
            exportedFileType=MODE_CSV;
        }

        if (file.isDirectory()) { // save to selected directory under default name
            exportDir = file;
            targetDir = file;
            targetName = defaultName;
        } else { // save to selected file
            targetDir = fileChooser.getCurrentDirectory();
            String fName = file.getName();

            // divide the file name into name and extension
            if (fName.endsWith("."+targetExt)) {  //.nps extension exists
                int idx = fName.lastIndexOf('.'); // NOI18N
                targetName = fName.substring(0, idx);
            } else {            // no extension
                targetName=fName;
            }
        }

        // 3. set type of exported file and return a newly created FileObject

        return new SelectedFile(targetDir, targetName, targetExt);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if (!exportProvider.hasExportableView() && !exportProvider.hasLoadedSnapshot()) { // nothing to export
            ProfilerDialogs.displayError(Bundle.ExportAction_NoViewMsg());
            return;
        }

        final LiveResultsWindow lrw = (exportProvider instanceof LiveResultsWindow) ? (LiveResultsWindow) exportProvider : null;
        final AppStatusHandler statusHandler = Profiler.getDefault().getTargetAppRunner().getAppStatusHandler();

        if (lrw != null) {
            statusHandler.pauseLiveUpdates();
        }

        SelectedFile saveFile = selectExportTargetFile(exportProvider);

        if (saveFile == null) {
            if (lrw != null) {
                statusHandler.resumeLiveUpdates();
            }
            return; // cancelled
        }
        
        final File file = saveFile.getSelectedFile();
        if (!checkFileExists(snapshot == null ? null : snapshot.getFile(),file)) {
            if (lrw != null) {
                statusHandler.resumeLiveUpdates();
            }
            return; // user doesn't want to overwrite existing file or it can't be overwritten
        }
        
        if (exportedFileType==MODE_NPS) {
            new NBSwingWorker(true) {
                final private ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.ExportAction_SavingSnapshot());
                @Override
                protected void doInBackground() {
                    try {
                        ph.setInitialDelay(500);
                        ph.start();
                        
                        if (!(file.getAbsolutePath().toLowerCase().endsWith("."+FILE_EXTENSION_NPS))) {
                            ProfilerDialogs.displayError(Bundle.ExportAction_InvalidLocationForFileMsg());
                            return;
                        }
                        FileObject fo=FileUtil.createData(file);
                        if (fo==null) {
                            ProfilerDialogs.displayError(Bundle.ExportAction_InvalidLocationForFileMsg());
                            return;
                        }

                        ResultsManager.getDefault().saveSnapshot(snapshot, fo);
                    } catch (IOException e1) {
                        ProfilerDialogs.displayError(Bundle.ExportAction_CannotWriteFileMsg(e1.getLocalizedMessage()));
                        LOGGER.log(Level.WARNING, e1.toString());
                    }
                }

                @Override
                protected void done() {
                    ph.finish();
                    if (lrw != null) {
                        statusHandler.resumeLiveUpdates();
                    }
                }
            }.execute();
            
        } else {
            new NBSwingWorker(true) {
                final private ProgressHandle ph = ProgressHandleFactory.createHandle(Bundle.ExportAction_ExportingViewMsg());
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
                    if (lrw != null) {
                        statusHandler.resumeLiveUpdates();
                    }
                }
            }.execute();
        }
    }
}
