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

import com.sun.tools.visualvm.uisupport.HTMLTextArea;
import com.sun.tools.visualvm.uisupport.SeparatorLine;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public class TextBrowser {

    private boolean copyingAllHtmlToClipboard = false;

    // --- Internal API --------------------------------------------------------

    synchronized static TextBrowser getInstance() {
        if (instance == null) {
            instance = new TextBrowser();
            instance.initComponents();
        }
        return instance;
    }
    
    void setCaption(String caption) {
        dialog.setTitle(caption);
    }
    
    void setPreferredBrowserSize(Dimension preferredSize) {
        this.preferredSize = preferredSize;
    }
    
    void showCodeText(String text) {
        textDisplayer.setText(text);
        displayerScrollPane.setViewportView(textDisplayer);
        try { textDisplayer.setCaretPosition(0); } catch (Exception e) {}
        displayerScrollPane.setPreferredSize(preferredSize);
        dialog.pack();
        closeButton.requestFocusInWindow();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(true);
        dialog.setVisible(true);
    }
    
    void showHTMLText(String text) {
        htmlTextDisplayer.setText(text);
        displayerScrollPane.setViewportView(htmlTextDisplayer);
        try { htmlTextDisplayer.setCaretPosition(0); } catch (Exception e) {}
        Dimension htmlSize = htmlTextDisplayer.getPreferredSize();
        htmlSize.width = Math.min(htmlSize.width, 700);
        htmlSize.height = Math.min(htmlSize.height, 500);
        displayerScrollPane.setPreferredSize(htmlSize);
        dialog.pack();
        closeButton.requestFocusInWindow();
        dialog.setLocationRelativeTo(null);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
    
    void setHelperButton(JButton helperButton) {
        if (this.helperButton != null && helperButton == null)
            buttonsContainer.remove(this.helperButton);
        else if (this.helperButton == null && helperButton != null)
            buttonsContainer.add(helperButton, BorderLayout.WEST);
        this.helperButton = helperButton;
    }
    
    void close() {
        dialog.setVisible(false);
        dialog.dispose();
    }
    
    void copyAllHtmlToClipboard() {
        if (!htmlTextDisplayer.getText().isEmpty()) {
            copyingAllHtmlToClipboard = true;
            try { htmlTextDisplayer.copy(); }
            finally { copyingAllHtmlToClipboard = false; }
        }
    }
    
    
    // --- Private implementation ----------------------------------------------
    
    private TextBrowser() {}
    
    private void initComponents() {
        dialog = new JDialog(AboutDialog.getInstance().getDialog(), "", true); // NOI18N
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) { cleanup(); }
        });

        contentPane = (JComponent)dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                 .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "CLOSE_ACTION"); // NOI18N
        contentPane.getActionMap().put("CLOSE_ACTION", new AbstractAction() { // NOI18N
                public void actionPerformed(ActionEvent e) { close(); }});
        
        textDisplayer = new TextViewerComponent();
        htmlTextDisplayer = new HTMLTextArea() {
            public int getSelectionStart() {
                if (copyingAllHtmlToClipboard) return 0;
                else return super.getSelectionStart();
            }
            public int getSelectionEnd() {
                if (copyingAllHtmlToClipboard) return getText().length();
                else return super.getSelectionEnd();
            }
        };
        displayerScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        displayerScrollPane.setBorder(BorderFactory.createEmptyBorder());
        displayerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        
        textDisplayer.setForeground(htmlTextDisplayer.getForeground());
        textDisplayer.setBackground(htmlTextDisplayer.getBackground());
        textDisplayer.setSelectionColor(htmlTextDisplayer.getSelectionColor());
        textDisplayer.setSelectedTextColor(htmlTextDisplayer.getSelectedTextColor());
        
        closeButton = new JButton(); // NOI18N
        Mnemonics.setLocalizedText(closeButton, NbBundle.getMessage(AboutDialogControls.class, "LBL_Close")); // NOI18N
        closeButton.setDefaultCapable(true);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() { close(); }
                });
            }
        });
        
        buttonsContainer = new JPanel(new BorderLayout());
        buttonsContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        buttonsContainer.add(closeButton, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        final SeparatorLine separator = new SeparatorLine();
        bottomPanel.add(separator, BorderLayout.NORTH);
        bottomPanel.add(buttonsContainer, BorderLayout.CENTER);

        final JScrollBar horizontalScroll = displayerScrollPane.getHorizontalScrollBar();
        if (horizontalScroll != null) horizontalScroll.addHierarchyListener(new HierarchyListener() {
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    separator.setVisible(!horizontalScroll.isShowing());
                }
            }
        });
        
        contentPane.add(displayerScrollPane, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.setResizable(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }
    
    private void cleanup() {
        setHelperButton(null);
        displayerScrollPane.getViewport().removeAll();
        textDisplayer.setText(""); // NOI18N
        htmlTextDisplayer.setText(""); // NOI18N
    }
    
    private static TextBrowser instance;
    
    private JDialog dialog;
    private JComponent contentPane;
    private JPanel buttonsContainer;
    private JButton closeButton;
    private JButton helperButton;
    private TextViewerComponent textDisplayer;
    private HTMLTextArea htmlTextDisplayer;
    private JScrollPane displayerScrollPane;
    private Dimension preferredSize = new Dimension(400, 300);

}
