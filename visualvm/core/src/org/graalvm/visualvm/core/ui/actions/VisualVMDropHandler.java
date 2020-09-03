/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.core.ui.actions;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graalvm.visualvm.core.snapshot.RegisteredSnapshotCategories;
import org.graalvm.visualvm.core.snapshot.SnapshotCategory;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.ExternalDropHandler;
import org.openide.windows.TopComponent;

/**
 *
 * @author S. Aubrecht
 * @author Tomas Hurka
 */
@ServiceProvider(service = ExternalDropHandler.class, position = 1000)
public class VisualVMDropHandler extends ExternalDropHandler {

    private static final Logger LOG = Logger.getLogger(VisualVMDropHandler.class.getName());

    @Override
    public boolean canDrop(DropTargetDragEvent e) {
        return canDrop(e.getCurrentDataFlavors());
    }

    @Override
    public boolean canDrop(DropTargetDropEvent e) {
        return canDrop(e.getCurrentDataFlavors());
    }

    private boolean canDrop(DataFlavor[] flavors) {
        for (DataFlavor df : flavors) {
            if (DataFlavor.javaFileListFlavor.equals(df)
                    || getUriListDataFlavor().equals(df)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean handleDrop(DropTargetDropEvent e) {
        Transferable t = e.getTransferable();
        if (t == null) {
            return false;
        }
        List<File> fileList = getFileList(t);
        if ((fileList == null) || fileList.isEmpty()) {
            return false;
        }

        //#158473: Activate target TC to inform winsys in which mode new editor
        //component should be opened. It assumes that openFile opens new editor component
        //in some editor mode. If there would be problem with activating another TC first
        //then another way how to infrom winsys must be used.
        Component c = e.getDropTargetContext().getComponent();
        while (c != null) {
            if (c instanceof TopComponent) {
                ((TopComponent) c).requestActive();
                break;
            }
            c = c.getParent();
        }

        Object errMsg = null;
        if (fileList.size() == 1) {
            errMsg = openFile(fileList.get(0));
        } else {
            boolean hasSomeSuccess = false;
            List<String> fileErrs = null;
            for (File file : fileList) {
                String fileErr = openFile(file);
                if (fileErr == null) {
                    hasSomeSuccess = true;
                } else {
                    if (fileErrs == null) {
                        fileErrs = new ArrayList<>();
                    }
                    fileErrs.add(fileErr);
                }
            }
            if (fileErrs != null) {         //some file could not be opened
                String mainMsgKey;
                if (hasSomeSuccess) {
                    mainMsgKey = "MSG_could_not_open_some_files";       //NOI18N
                } else {
                    mainMsgKey = "MSG_could_not_open_any_file";         //NOI18N
                }
                String mainMsg = NbBundle.getMessage(VisualVMDropHandler.class, mainMsgKey);
                JComponent msgPanel = new JPanel();
                msgPanel.setLayout(new BoxLayout(msgPanel, BoxLayout.PAGE_AXIS));
                msgPanel.add(new JLabel(mainMsg));
                msgPanel.add(Box.createVerticalStrut(12));
                for (String fileErr : fileErrs) {
                    msgPanel.add(new JLabel(fileErr));
                }
                errMsg = msgPanel;
            }
        }
        if (errMsg != null) {
            showWarningMessageFileNotOpened(errMsg);
            return false;
        }
        return true;
    }

    private static void showWarningMessageFileNotOpened(Object errMsg) {
        DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(
                        errMsg,
                        NotifyDescriptor.WARNING_MESSAGE));
    }

    private List<File> getFileList(Transferable t) {
        try {
            if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                //windows & mac
                try {
                    return (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                } catch (InvalidDnDOperationException ex) { // #212390
                    LOG.log(Level.FINE, null, ex);
                }
            }
            if (t.isDataFlavorSupported(getUriListDataFlavor())) {
                //linux
                String uriList = (String) t.getTransferData(getUriListDataFlavor());
                return textURIListToFileList(uriList);
            }
        } catch (UnsupportedFlavorException ex) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
        } catch (IOException ex) {
            // Ignore. Can be just "Owner timed out" from sun.awt.X11.XSelection.getData.
            LOG.log(Level.FINE, null, ex);
        }
        return null;
    }

    /**
     * Opens the given file.
     *
     * If the file doesn't open in a reasonable time (2 seconds), let's assume
     * it will open successfully later (return null).
     *
     * @param file file to be opened
     * @return {@code null} if the file was successfully opened; or a localized
     * error message in case of failure
     */
    private String openFile(final File file) {
        if (file.exists() && file.canRead() && file.isFile()) {
            List<SnapshotCategory> categories = RegisteredSnapshotCategories.sharedInstance().getOpenSnapshotCategories();

            for (SnapshotCategory category : categories) {
                if (category.getFileFilter().accept(file)) {
                    category.openSnapshot(file);
                    LoadRecentSnapshot.instance().addFile(file);
                    return null;
                }
            }
            return MessageFormat.format(NbBundle.getMessage(VisualVMDropHandler.class, "LoadRecentSnapshot_CannotLoadMsg"), file.getName());
        }
        return MessageFormat.format(NbBundle.getMessage(VisualVMDropHandler.class, "LoadRecentSnapshot_NotAvailableMsg"), file.getName());
    }

    private static DataFlavor uriListDataFlavor;

    private DataFlavor getUriListDataFlavor() {
        if (null == uriListDataFlavor) {
            try {
                uriListDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
            } catch (ClassNotFoundException cnfE) {
                //cannot happen
                throw new AssertionError(cnfE);
            }
        }
        return uriListDataFlavor;
    }

    private List<File> textURIListToFileList(String data) {
        List<File> list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(data,"\r\n\u0000");

        while (st.hasMoreTokens()) {
            String s = st.nextToken();

            if (s.startsWith("#")) {
                // the line is a comment (as per the RFC 2483)
                continue;
            }
            try {
                File file = new File(new URI(s));
                list.add(file);
            } catch (java.net.URISyntaxException e) {
                // malformed URI
            } catch (IllegalArgumentException e) {
                // the URI is not a valid 'file:' URI
            }
        }
        return list;
    }
}
