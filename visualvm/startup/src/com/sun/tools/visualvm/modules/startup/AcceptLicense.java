/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.modules.startup;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openide.util.NbBundle;
import org.openide.util.Utilities;
/**
 * Displays LicensePanel to user. User must accept license to continue. 
 * if user does not accept license UserCancelException is thrown.
 *
 * @author  Marek Slama
 * @author Jiri Sedlacek
 */

public final class AcceptLicense {
    private static final Logger LOGGER = Logger.getLogger(AcceptLicense.class.getName());

    private static final String YES_AC = "yes"; // NOI18N
    private static final String NO_AC  = "no" ; // NOI18N
    
    private static JDialog d;
    private static String command;
    
    /** If License was not accepted during installation user must accept it here. 
     */
    public static void showLicensePanel () throws Exception {
        setDefaultLookAndFeel();
        URL url = AcceptLicense.class.getResource("LICENSE.txt"); // NOI18N
        LicensePanel licensePanel = new LicensePanel(url);
        ResourceBundle bundle = NbBundle.getBundle(AcceptLicense.class);
        String yesLabel = bundle.getString("MSG_LicenseYesButton"); // NOI18N
        String noLabel = bundle.getString("MSG_LicenseNoButton"); // NOI18N
        JButton yesButton = new JButton();
        JButton noButton = new JButton();
        setLocalizedText(yesButton,yesLabel);
        setLocalizedText(noButton,noLabel);
        ActionListener listener = new ActionListener () {
            public void actionPerformed (ActionEvent e) {
                command = e.getActionCommand();
                d.setVisible(false);
                d = null;
            }            
        };
        yesButton.addActionListener(listener);
        noButton.addActionListener(listener);
        
        yesButton.setActionCommand(YES_AC);
        noButton.setActionCommand(NO_AC);
        
        yesButton.getAccessibleContext().setAccessibleName(bundle.getString("ACSN_AcceptButton")); // NOI18N
        yesButton.getAccessibleContext().setAccessibleName(bundle.getString("ACSD_AcceptButton")); // NOI18N
        
        noButton.getAccessibleContext().setAccessibleName(bundle.getString("ACSN_RejectButton")); // NOI18N
        noButton.getAccessibleContext().setAccessibleName(bundle.getString("ACSD_RejectButton")); // NOI18N
        
        Dimension yesPF = yesButton.getPreferredSize();
        Dimension noPF = noButton.getPreferredSize();
        int maxWidth = Math.max(yesPF.width, noPF.width);
        int maxHeight = Math.max(yesPF.height, noPF.height);
        yesButton.setPreferredSize(new Dimension(maxWidth, maxHeight));
        noButton.setPreferredSize(new Dimension(maxWidth, maxHeight));
        
        d = new JDialog((Frame) null,bundle.getString("MSG_LicenseDlgTitle"),true); // NOI18N
        
        d.getAccessibleContext().setAccessibleName(bundle.getString("ACSN_LicenseDlg")); // NOI18N
        d.getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_LicenseDlg")); // NOI18N
        
        d.getContentPane().add(licensePanel,BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(17,12,11,11));
        buttonPanel.add(yesButton);
        buttonPanel.add(noButton);
        d.getContentPane().add(buttonPanel,BorderLayout.SOUTH);
        d.setSize(new Dimension(600,600));
        d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        d.setModal(true);
        d.setResizable(true);
        //Center on screen
        d.setLocationRelativeTo(null);
        d.setVisible(true);
        
        if (YES_AC.equals(command)) {
            return;
        } else {
            throw new org.openide.util.UserCancelException();
        }
    }
    
    /**
     * Actual setter of the text & mnemonics for the AbstractButton or
     * their subclasses. We must copy necessary code from org.openide.awt.Mnemonics
     * because org.openide.awt module is not available yet when this code is called.
     * @param item AbstractButton
     * @param text new label
     */
    private static void setLocalizedText (AbstractButton button, String text) {
        if (text == null) {
            button.setText(null);
            return;
        }

        int i = findMnemonicAmpersand(text);

        if (i < 0) {
            // no '&' - don't set the mnemonic
            button.setText(text);
            button.setMnemonic(0);
        } else {
            button.setText(text.substring(0, i) + text.substring(i + 1));
            
            if (Utilities.isMac()) {
                // there shall be no mnemonics on macosx.
                //#55864
                return;
            }

            char ch = text.charAt(i + 1);

            // it's latin character or arabic digit,
            // setting it as mnemonics
            button.setMnemonic(ch);

            // If it's something like "Save &As", we need to set another
            // mnemonic index (at least under 1.4 or later)
            // see #29676
            button.setDisplayedMnemonicIndex(i);
        }
    }
    
    /**
     * Searches for an ampersand in a string which indicates a mnemonic.
     * Recognizes the following cases:
     * <ul>
     * <li>"Drag & Drop", "Ampersand ('&')" - don't have mnemonic ampersand.
     *      "&" is not found before " " (space), or if enclosed in "'"
     *     (single quotation marks).
     * <li>"&File", "Save &As..." - do have mnemonic ampersand.
     * <li>"Rock & Ro&ll", "Underline the '&' &character" - also do have
     *      mnemonic ampersand, but the second one.
     * </ul>
     * @param text text to search
     * @return the position of mnemonic ampersand in text, or -1 if there is none
     */
    public static int findMnemonicAmpersand(String text) {
        int i = -1;

        do {
            // searching for the next ampersand
            i = text.indexOf('&', i + 1); // NOI18N

            if ((i >= 0) && ((i + 1) < text.length())) {
                // before ' '
                if (text.charAt(i + 1) == ' ') { // NOI18N
                    continue;

                    // before ', and after '
                } else if ((text.charAt(i + 1) == '\'') && (i > 0) && (text.charAt(i - 1) == '\'')) { // NOI18N
                    continue;
                }

                // ampersand is marking mnemonics
                return i;
            }
        } while (i >= 0);

        return -1;
    }
    
    
    /**
     * Tries to set default L&F according to platform.
     */
    public static void setDefaultLookAndFeel () {
        String lafClassName = "<System LaF>"; // NOI18N
        try {
            lafClassName = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lafClassName);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Cannot set L&F " + lafClassName, ex); // NOI18N
        }
    }
    
}
