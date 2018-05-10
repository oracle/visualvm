/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.visualvm.modules.appui.about;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class AboutDialog {


    // --- Public API ----------------------------------------------------------

    public synchronized static AboutDialog createInstance(Frame parentFrame, Image aboutImage) {
        if (instance != null) instance.close();
        instance = new AboutDialog(parentFrame, aboutImage);
        instance.initComponents();
        return instance;
    }
    
    public synchronized static AboutDialog getInstance() {
        if (instance == null) {
            instance = new AboutDialog();
            instance.initComponents();
        }
        return instance;
    }
    
    public void setCaption(String caption) {
        dialog.setTitle(caption);
    }
    
    public String getCaption() {
        return dialog.getTitle();
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setHTMLMessage(String htmlMessage) {
        this.htmlMessage = htmlMessage;
    }
    
    public String getHTMLMessage() {
        return htmlMessage;
    }
    
    public void setBuildID(String buildID) {
        this.buildID = buildID;
    }
    
    public String getBuildID() {
        return buildID;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }
    
    public String getLogfile() {
        return logfile;
    }
    
    public void show() {
        aboutDialogPanel.updateAppearance();
        aboutDialogControls.updateAppearance();
        dialog.pack();
        aboutDialogControls.getDefaultButton().requestFocusInWindow();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }
    
    public void close() {
        dialog.setVisible(false);
        dialog.dispose();
    }
    
    
    // --- Internal API --------------------------------------------------------    
    
    Image getAboutImage() {
        return aboutImage;
    }
    
    void setDefaultButton(JButton button) {
        dialog.getRootPane().setDefaultButton(button);
    }
    
    JDialog getDialog() {
        return dialog;
    }
    
    
    // --- Private implementation ----------------------------------------------
    
    private AboutDialog() {
        this(null, new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB));
    }
    
    private AboutDialog(Frame parentFrame, Image aboutImage) {
        this.parentFrame = parentFrame;
        this.aboutImage = aboutImage;
    }
    
    private void initComponents() {
        dialog = new JDialog(parentFrame, NbBundle.getMessage(AboutDialog.class, "LBL_About"), true);   // NOI18N
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { cleanup(); }
        });
        
        JComponent contentPane = (JComponent)dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                 .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_ACTION"); // NOI18N
        contentPane.getActionMap().put("CLOSE_ACTION", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) { close(); }});
        
        aboutDialogPanel = new AboutDialogPanel();
        aboutDialogControls = new AboutDialogControls();
        
        contentPane.add(aboutDialogPanel, BorderLayout.CENTER);
        contentPane.add(aboutDialogControls, BorderLayout.SOUTH);
        
        dialog.getRootPane().setDefaultButton(aboutDialogControls.getDefaultButton());
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
    
    private void cleanup() {
    }
    
    private static AboutDialog instance;
    
    private AboutDialogPanel aboutDialogPanel;
    private AboutDialogControls aboutDialogControls;
    
    private JDialog dialog;
    
    private String buildID;
    private String message;
    private String htmlMessage;
    private String details;
    private String logfile;
    private Frame parentFrame;
    private Image aboutImage;
    
    
    // --- main method for testing purposes ------------------------------------

//    public static void main(String[] args) {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {}
//        
//        AboutDialog aboutDialog = AboutDialog.getInstance();
//        
//        aboutDialog.setCaption("About VisualVM");
//        aboutDialog.setBuildID("Version: Dev (Build xxx)");
//        aboutDialog.setMessage("<b>VisualVM for JDK 6.0</b> has been licensed under the GNU General Public License (GPL) Version 2. It is built on NetBeans Platform. For more information, please visit https://visualvm.github.io.");
//        aboutDialog.setHTMLMessage("<b>VisualVM for JDK 6.0</b> has been licensed under the GNU General Public License (GPL) Version 2. It is built on NetBeans Platform. For more information, please visit <a href=\"https://visualvm.github.io\">https://visualvm.github.io</a>.");
//        aboutDialog.setDetails("<b>Version: </b> Dev");
//        aboutDialog.setLogfile("E:\\Dev\\userdirdev\\var\\log\\messages.log");
//        
//        aboutDialog.show();
//    }
    
}
