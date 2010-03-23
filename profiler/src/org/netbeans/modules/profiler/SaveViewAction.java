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

package org.netbeans.modules.profiler;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.lib.profiler.client.AppStatusHandler;
import org.netbeans.modules.profiler.ui.ImagePreviewPanel;
import org.netbeans.modules.profiler.ui.ProfilerDialogs;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.NotifyDescriptor;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;


class SaveViewAction extends AbstractAction {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    static interface ViewProvider {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        public BufferedImage getViewImage(boolean onlyVisibleArea);

        public String getViewName();

        public boolean fitsVisibleArea();

        public boolean hasView();
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class SelectedFile {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        BufferedImage image;
        File folder;
        String fileExt;
        String fileName;
        boolean visibleArea;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        SelectedFile(File folder, String fileName, String fileExt, BufferedImage image, boolean visibleArea) {
            this.folder = folder;
            this.fileName = fileName;
            this.fileExt = fileExt;
            this.image = image;
            this.visibleArea = visibleArea;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        File getSelectedFile() {
            return new File(folder + File.separator + fileName + "." + fileExt); //NOI18N
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String SAVE_VIEW_ACTION_NAME = NbBundle.getMessage(SaveViewAction.class,
                                                                            "SaveViewAction_SaveViewActionName"); //NOI18N
    private static final String SAVE_VIEW_ACTION_DESCR = NbBundle.getMessage(SaveViewAction.class,
                                                                             "SaveViewAction_SaveViewActionDescr"); //NOI18N
    private static final String NO_VIEW_MSG = NbBundle.getMessage(SaveViewAction.class, "SaveViewAction_NoViewMsg"); //NOI18N
    private static final String SAVING_VIEW_MSG = NbBundle.getMessage(SaveViewAction.class, "SaveViewAction_SavingViewMsg"); //NOI18N
    private static final String OVERWRITE_FILE_CAPTION = NbBundle.getMessage(SaveViewAction.class,
                                                                             "SaveViewAction_OverwriteFileCaption"); //NOI18N
    private static final String OVERWRITE_FILE_MSG = NbBundle.getMessage(SaveViewAction.class,
                                                                         "SaveViewAction_OverwriteFileMsg"); //NOI18N
    private static final String CANNOT_OVERWRITE_FILE_MSG = NbBundle.getMessage(SaveViewAction.class,
                                                                                "SaveViewAction_CannotOverwriteFileMsg"); //NOI18N
    private static final String SAVE_DIALOG_TITLE = NbBundle.getMessage(SaveViewAction.class,
                                                                        "SaveViewAction_SaveDialogTitle"); //NOI18N
    private static final String SAVE_DIALOG_BUTTON = NbBundle.getMessage(SaveViewAction.class,
                                                                         "SaveViewAction_SaveDialogButton"); //NOI18N
    private static final String SAVE_DIALOG_FILTER = NbBundle.getMessage(SaveViewAction.class,
                                                                         "SaveViewAction_SaveDialogFilter"); //NOI18N
    private static final String SAVE_DIALOG_PREVIEW = NbBundle.getMessage(SaveViewAction.class,
                                                                          "SaveViewAction_SaveDialogPreview"); //NOI18N
    private static final String SAVE_DIALOG_VISIBLE = NbBundle.getMessage(SaveViewAction.class,
                                                                          "SaveViewAction_SaveDialogVisible"); //NOI18N
    private static final String OOME_SAVING_MSG = NbBundle.getMessage(SaveViewAction.class, "SaveViewAction_OomeSavingMsg"); //NOI18N
                                                                                                                                   // -----
    private static final ImageIcon ICON = ImageUtilities.loadImageIcon("org/netbeans/modules/profiler/resources/saveView.png", false); // NOI18N
    private static File exportDir;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private BufferedImage image;
    private ImagePreviewPanel imagePreview;
    private JCheckBox visibleAreaCheckBox;
    private JFileChooser fileChooser;
    private ViewProvider viewProvider;
    private boolean visibleArea;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public SaveViewAction(ViewProvider viewProvider) {
        putValue(Action.NAME, SAVE_VIEW_ACTION_NAME);
        putValue(Action.SHORT_DESCRIPTION, SAVE_VIEW_ACTION_DESCR);
        putValue(Action.SMALL_ICON, ICON);
        putValue("iconBase", "org/netbeans/modules/profiler/resources/export.png"); // NOI18N
        this.viewProvider = viewProvider;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(ActionEvent evt) {
        if (!viewProvider.hasView()) { // nothing to save in current view
            NetBeansProfiler.getDefaultNB().displayError(NO_VIEW_MSG);

            return;
        }

        final LiveResultsWindow lrw = (viewProvider instanceof LiveResultsWindow) ? (LiveResultsWindow) viewProvider : null;
        final AppStatusHandler statusHandler = NetBeansProfiler.getDefaultNB().getTargetAppRunner().getAppStatusHandler();

        if (lrw != null) {
            statusHandler.pauseLiveUpdates();
        }

        SelectedFile saveFile = selectSnapshotTargetFile(viewProvider);

        if (saveFile == null) {
            if (lrw != null) {
                statusHandler.resumeLiveUpdates();
            }

            return; // cancelled
        }

        final File file = saveFile.getSelectedFile();

        if (!checkFileExists(file)) {
            if (lrw != null) {
                statusHandler.resumeLiveUpdates();
            }

            return; // user doesn't want to overwrite existing file
        }

        final boolean visibleArea = saveFile.visibleArea;
        final BufferedImage bImage = saveFile.image;

        saveFile = null;
        image = null;
        imagePreview.reset();

        IDEUtils.runInProfilerRequestProcessor(new Runnable() {
                public void run() {
                    ProgressHandle pHandle = null;

                    try {
                        pHandle = ProgressHandleFactory.createHandle(SAVING_VIEW_MSG);
                        pHandle.setInitialDelay(0);
                        pHandle.start();
                        
                        BufferedImage img = (bImage == null) ? viewProvider.getViewImage(visibleArea) : bImage;
                        if (img != null) {
                            FileImageOutputStream stream = new FileImageOutputStream( file );
                            ImageIO.write(img, "png", stream); //NOI18N
                            stream.close();
                        }
                    } catch (OutOfMemoryError e) {
                        NetBeansProfiler.getDefaultNB().displayError(OOME_SAVING_MSG);
                    } catch (IOException ex) {
                        NetBeansProfiler.getDefaultNB().displayError(
                                NbBundle.getMessage(SaveViewAction.class,
                                "ExportAction_FileWriteErrorMsg", //NOI18N
                                file.getAbsolutePath()));
                    }
                    finally {
                        if (bImage != null) {
                            bImage.flush();
                        }

                        if (pHandle != null) {
                            pHandle.finish();
                        }

                        if (lrw != null) {
                            statusHandler.resumeLiveUpdates();
                        }
                    }
                }
            });
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            // File chooser
            fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setDialogTitle(SAVE_DIALOG_TITLE);
            fileChooser.setApproveButtonText(SAVE_DIALOG_BUTTON);
            fileChooser.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".png") || f.getName().endsWith(".PNG");
                    } //NOI18N

                    public String getDescription() {
                        return SAVE_DIALOG_FILTER;
                    }
                });

            // Preview label
            JLabel previewLabel = new JLabel(SAVE_DIALOG_PREVIEW);
            previewLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 5, 0));

            // Preview area
            imagePreview = new ImagePreviewPanel();
            imagePreview.reset();
            imagePreview.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

            // Mode checkbox
            visibleAreaCheckBox = new JCheckBox(SAVE_DIALOG_VISIBLE);
            visibleAreaCheckBox.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 0));

            visibleAreaCheckBox.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        image = null;
                        imagePreview.clearImage();
                        visibleArea = visibleAreaCheckBox.isSelected();

                        imagePreview.setImage(new ImagePreviewPanel.ImageProvider() {
                                public BufferedImage getImage() {
                                    image = viewProvider.getViewImage(visibleArea);

                                    return image;
                                }
                                ;
                            });
                    }
                });

            // Accessory container
            JPanel accessoryPanel = new JPanel(new BorderLayout());
            accessoryPanel.add(previewLabel, BorderLayout.NORTH);
            accessoryPanel.add(imagePreview, BorderLayout.CENTER);
            accessoryPanel.add(visibleAreaCheckBox, BorderLayout.SOUTH);

            fileChooser.setAccessory(accessoryPanel);
        }

        return fileChooser;
    }

    private boolean checkFileExists(File file) {
        if (file.exists()) {
            if (ProfilerDialogs.notify(new NotifyDescriptor.Confirmation(MessageFormat.format(OVERWRITE_FILE_CAPTION,
                                                                                                  new Object[] { file.getName() }),
                                                                             OVERWRITE_FILE_CAPTION,
                                                                             NotifyDescriptor.YES_NO_OPTION)) != NotifyDescriptor.YES_OPTION) {
                return false; // cancelled by the user
            }

            if (!file.delete()) {
                NetBeansProfiler.getDefaultNB()
                                .displayError(MessageFormat.format(CANNOT_OVERWRITE_FILE_MSG, new Object[] { file.getName() }));

                return false;
            }
        }

        return true;
    }

    private SelectedFile selectSnapshotTargetFile(final ViewProvider viewProvider) {
        File targetDir;
        String targetName;
        String defaultName = viewProvider.getViewName();

        // 1. let the user choose file or directory
        final JFileChooser chooser = getFileChooser();

        if (exportDir != null) {
            chooser.setCurrentDirectory(exportDir);
        }

        visibleAreaCheckBox.setSelected(true);
        visibleAreaCheckBox.setEnabled(!viewProvider.fitsVisibleArea());

        image = null;
        imagePreview.clearImage();
        visibleArea = visibleAreaCheckBox.isSelected();
        imagePreview.setImage(new ImagePreviewPanel.ImageProvider() {
                public BufferedImage getImage() {
                    image = viewProvider.getViewImage(true);

                    return image;
                }
                ;
            });

        int result = chooser.showSaveDialog(IDEUtils.getMainWindow());
        imagePreview.reset();

        if (result != JFileChooser.APPROVE_OPTION) {
            image = null;
            imagePreview.reset();
            return null; // cancelled by the user
        }

        // 2. process both cases and extract file name and extension to use
        File file = chooser.getSelectedFile();
        String targetExt = "png"; //NOI18N

        if (file.isDirectory()) { // save to selected directory under default name
            exportDir = chooser.getCurrentDirectory();
            targetDir = file;
            targetName = defaultName;
        } else { // save to selected file
            exportDir = chooser.getCurrentDirectory();

            targetDir = exportDir;

            String fName = file.getName();

            // divide the file name into name and extension
            int idx = fName.lastIndexOf("."); // NOI18N

            if (idx == -1) { // no extension
                targetName = fName;

                // extension will be used from source file
            } else { // extension exists
                targetName = fName.substring(0, idx);
                targetExt = fName.substring(idx + 1);
            }
        }

        // 3. return a newly created FileObject
        return new SelectedFile(targetDir, targetName, targetExt, image, visibleArea);
    }
}
