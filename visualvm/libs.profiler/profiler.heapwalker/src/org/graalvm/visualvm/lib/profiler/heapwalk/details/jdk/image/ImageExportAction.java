/*
 * Copyright (c) 2013, 2022, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.profiler.heapwalk.details.jdk.image;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.graalvm.visualvm.lib.profiler.api.ProfilerDialogs;
import org.graalvm.visualvm.lib.profiler.api.icons.GeneralIcons;
import org.graalvm.visualvm.lib.profiler.api.icons.Icons;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 * Action used to export images.
 *
 * @author Jan Taus
 */
@NbBundle.Messages({
    "ImageExportAction_Title=Export Image", // NOI18N
    "ImageExportAction_Ok=Export", // NOI18N
    "ImageExportAction_PNG=Portable Network Graphics (*.png)", // NOI18N
    "ImageExportAction_OverwriteFileCaption=Overwrite Existing File", // NOI18N
    "ImageExportAction_OverwriteFileMsg=<html><b>File {0} already exists.</b><br><br>Do you want to replace it?</html>", // NOI18N
    "ImageExportAction_WrongFormat=File type {0} is not supported.", // NOI18N
    "ImageExportAction_InvalidLoc=Invalid location for file.", // NOI18N
    "ImageExportAction_Failed=<html>Failed to export File.<br>Reason: {0}</html>" // NOI18N
})
class ImageExportAction extends AbstractAction {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final String DEFAULT_EXPORT_TYPE = "png"; //NOI18N
    private static File exportDir;
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    private JFileChooser fileChooser;
    private final Image image;

    //~ Constructors -------------------------------------------------------------------------------------------------------------
    ImageExportAction(Image image) {
        super();
        putValue(Action.NAME, Bundle.ImageDetailProvider_Action_Export());
        putValue(Action.SMALL_ICON, Icons.getIcon(GeneralIcons.EXPORT));
        putValue(Action.SHORT_DESCRIPTION, Bundle.ImageDetailProvider_Action_Export());
        putValue("iconBase", Icons.getResource(GeneralIcons.EXPORT)); // NOI18N
        this.image = image;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setDialogTitle(Bundle.ImageExportAction_Title());
            fileChooser.setApproveButtonText(Bundle.ImageExportAction_Ok());
            fileChooser.addChoosableFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith("." + DEFAULT_EXPORT_TYPE);
                }

                @Override
                public String getDescription() {
                    return Bundle.ImageExportAction_PNG();
                }
            });
        }
        if (exportDir != null) {
            fileChooser.setCurrentDirectory(exportDir);
        }
        int result = fileChooser.showSaveDialog(WindowManager.getDefault().getRegistry().getActivated());
        if (result == JFileChooser.APPROVE_OPTION) {
            exportDir = fileChooser.getCurrentDirectory();
            File target = fileChooser.getSelectedFile();
            if (target.exists()) {
                if (!ProfilerDialogs.displayConfirmation(
                        Bundle.ImageExportAction_OverwriteFileMsg(target.getName()),
                        Bundle.ImageExportAction_OverwriteFileCaption())) {
                    return;
                }
            }

            try {
                writeImage(fileChooser.getSelectedFile());
            } catch (IOException ex) {
                ProfilerDialogs.displayError(Bundle.ImageExportAction_Failed(ex.getMessage()));
            }
        }
    }

    private void writeImage(File file) throws IOException {
        String type;
        int idx = file.getName().lastIndexOf('.');
        if (idx == -1) {
            file = new File(file.getPath() + '.' + DEFAULT_EXPORT_TYPE);
            type = DEFAULT_EXPORT_TYPE;
        } else {
            type = file.getName().substring(idx + 1);
        }
        BufferedImage bi;
        if (image instanceof BufferedImage) {
            bi = (BufferedImage) image;
        } else {
            bi = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
            bi.createGraphics().drawImage(image, 0, 0, null);
        }
        FileObject fo = FileUtil.createData(file);
        if (fo == null) {
            throw new IOException(Bundle.ImageExportAction_InvalidLoc());
        }
        try (OutputStream output = fo.getOutputStream()) {
            if (!ImageIO.write(bi, type, output)) {
                throw new IOException(Bundle.ImageExportAction_WrongFormat(type));
            }
        }
    }
}
