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

package org.netbeans.modules.profiler.ui;

import org.netbeans.modules.profiler.ProfilerIDESettings;
import org.netbeans.modules.profiler.utils.IDEUtils;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;


public final class ProfilerDialogs {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class DNSAConfirmation extends NotifyDescriptor.Confirmation {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final String dnsaKey;
        private String dnsaMessage = DONT_SHOW_AGAIN_MSG;
        private boolean dnsaDefault = true;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Create a yes/no/cancel question with default title.
         *
         * @param message the message object
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message) {
            super(message);
            this.dnsaKey = key;
        }

        /**
         * Create a yes/no/cancel question.
         *
         * @param message the message object
         * @param title   the dialog title
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message, final String title) {
            super(message, title);
            this.dnsaKey = key;
        }

        /**
         * Create a question with default title.
         *
         * @param message    the message object
         * @param optionType the type of options to display to the user
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message, final int optionType) {
            super(message, optionType);
            this.dnsaKey = key;
        }

        /**
         * Create a question.
         *
         * @param message    the message object
         * @param title      the dialog title
         * @param optionType the type of options to display to the user
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message, final String title, final int optionType) {
            super(message, title, optionType);
            this.dnsaKey = key;
        }

        /**
         * Create a confirmation with default title.
         *
         * @param message     the message object
         * @param optionType  the type of options to display to the user
         * @param messageType the type of message to use
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message, final int optionType, final int messageType) {
            super(message, optionType, messageType);
            this.dnsaKey = key;
        }

        /**
         * Create a confirmation.
         *
         * @param message     the message object
         * @param title       the dialog title
         * @param optionType  the type of options to display to the user
         * @param messageType the type of message to use
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmation(final String key, final Object message, final String title, final int optionType,
                                final int messageType) {
            super(message, title, optionType, messageType);
            this.dnsaKey = key;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setDNSADefault(boolean value) {
            this.dnsaDefault = value;
        }

        public boolean getDNSADefault() {
            return dnsaDefault;
        }

        public String getDNSAKey() {
            return dnsaKey;
        }

        public void setDNSAMessage(String value) {
            this.dnsaMessage = value;
        }

        public String getDNSAMessage() {
            return dnsaMessage;
        }
    }

    // If No is selected the Don't show again is reset
    public static final class DNSAConfirmationChecked extends NotifyDescriptor.Confirmation {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final String dnsaKey;
        private String dnsaMessage = DONT_SHOW_AGAIN_MSG;
        private boolean dnsaDefault = false;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Create a yes/no/cancel question with default title.
         *
         * @param message the message object
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message) {
            super(message);
            this.dnsaKey = key;
        }

        /**
         * Create a yes/no/cancel question.
         *
         * @param message the message object
         * @param title   the dialog title
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message, final String title) {
            super(message, title);
            this.dnsaKey = key;
        }

        /**
         * Create a question with default title.
         *
         * @param message    the message object
         * @param optionType the type of options to display to the user
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message, final int optionType) {
            super(message, optionType);
            this.dnsaKey = key;
        }

        /**
         * Create a question.
         *
         * @param message    the message object
         * @param title      the dialog title
         * @param optionType the type of options to display to the user
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message, final String title, final int optionType) {
            super(message, title, optionType);
            this.dnsaKey = key;
        }

        /**
         * Create a confirmation with default title.
         *
         * @param message     the message object
         * @param optionType  the type of options to display to the user
         * @param messageType the type of message to use
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message, final int optionType, final int messageType) {
            super(message, optionType, messageType);
            this.dnsaKey = key;
        }

        /**
         * Create a confirmation.
         *
         * @param message     the message object
         * @param title       the dialog title
         * @param optionType  the type of options to display to the user
         * @param messageType the type of message to use
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAConfirmationChecked(final String key, final Object message, final String title, final int optionType,
                                       final int messageType) {
            super(message, title, optionType, messageType);
            this.dnsaKey = key;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setDNSADefault(boolean value) {
            this.dnsaDefault = value;
        }

        public boolean getDNSADefault() {
            return dnsaDefault;
        }

        public String getDNSAKey() {
            return dnsaKey;
        }

        public void setDNSAMessage(String value) {
            this.dnsaMessage = value;
        }

        public String getDNSAMessage() {
            return dnsaMessage;
        }
    }

    public static final class DNSAMessage extends NotifyDescriptor.Message {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final String dnsaKey;
        private String dnsaMessage = DONT_SHOW_AGAIN_MSG;
        private boolean dnsaDefault = true;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Create an informational report about the results of a command.
         *
         * @param message the message object
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAMessage(final String key, final Object message) {
            super(message);
            this.dnsaKey = key;
        }

        /**
         * Create a report about the results of a command.
         *
         * @param message     the message object
         * @param messageType the type of message to be displayed
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public DNSAMessage(final String dnsaKey, final Object message, final int messageType) {
            super(message, messageType);
            this.dnsaKey = dnsaKey;
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public void setDNSADefault(boolean value) {
            this.dnsaDefault = value;
        }

        public boolean getDNSADefault() {
            return dnsaDefault;
        }

        public String getDNSAKey() {
            return dnsaKey;
        }

        public void setDNSAMessage(String value) {
            this.dnsaMessage = value;
        }

        public String getDNSAMessage() {
            return dnsaMessage;
        }
    }

    public static final class MessageWithDetails extends DialogDescriptor {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private JButton showDetailsButton = new JButton(SHOW_DETAILS_BUTTON_TEXT);
        private Object detailsMsg;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        /**
         * Create an informational report about the results of a command.
         *
         * @param shortMsg the short message object
         * @param detailsMsg the details message object
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public MessageWithDetails(final Object shortMsg, final Object detailsMsg, final boolean respectParent) {
            super(shortMsg, NotifyDescriptor.getTitleForType(NotifyDescriptor.INFORMATION_MESSAGE));
            this.detailsMsg = detailsMsg;
            showDetailsButton.setDefaultCapable(false);
            setOptions(new Object[] { OK_OPTION });
            setAdditionalOptions(new Object[] { showDetailsButton });
            setLeaf(!respectParent);
        }

        /**
         * Create a report about the results of a command.
         *
         * @param shortMsg     the message object
         * @param detailsMsg the details message object
         * @param messageType the type of message to be displayed
         * @see org.openide.NotifyDescriptor#NotifyDescriptor
         */
        public MessageWithDetails(final Object shortMsg, final Object detailsMsg, final int messageType,
                                  final boolean respectParent) {
            super(shortMsg, NotifyDescriptor.getTitleForType(messageType));
            this.setMessageType(messageType);
            this.detailsMsg = detailsMsg;
            showDetailsButton.setDefaultCapable(false);
            setOptions(new Object[] { OK_OPTION });
            setAdditionalOptions(new Object[] { showDetailsButton });
            setLeaf(!respectParent);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Object getDetailsMessage() {
            return detailsMsg;
        }

        public Object getShortMessage() {
            return this.getMessage();
        }

        public JButton getShowDetailsButton() {
            return showDetailsButton;
        }
    }

    private static final class DNSAPanel extends JPanel {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final JCheckBox check;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        DNSAPanel(final Object message, final boolean defaultSel, final String dnsaMessage) {
            Component messageComponent = null;

            if (message instanceof Component) {
                messageComponent = (Component) message;
            } else if (message instanceof String) {
                messageComponent = new NBHTMLLabel((String) message);
                messageComponent.setFocusable(false);
            }

            setLayout(new BorderLayout(0, 10));
            check = new JCheckBox(dnsaMessage, defaultSel);

            if (messageComponent != null) {
                add(messageComponent, BorderLayout.CENTER);
            }

            add(check, BorderLayout.SOUTH);
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public boolean isAutoChecked() {
            return check.isSelected();
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final Logger LOGGER = Logger.getLogger(ProfilerDialogs.class.getName());

    // -----
    // I18N String constants
    private static final String DONT_SHOW_AGAIN_MSG = NbBundle.getMessage(ProfilerDialogs.class,
                                                                          "ProfilerDialogs_DontShowAgainMsg"); //NOI18N
    private static final String SHOW_DETAILS_BUTTON_TEXT = NbBundle.getMessage(ProfilerDialogs.class,
                                                                               "ProfilerDialogs_ShowDetailsButtonText"); //NOI18N
                                                                                                                         // -----
    private static boolean DEBUG = System.getProperty("org.netbeans.modules.profiler.ui.ProfilerDialogs") != null; //NOI18N
    private static final DialogDisplayer standard = DialogDisplayer.getDefault();
    private static boolean silent = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilerDialogs() {
    } // avoid direct instance creation

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void setSilent(boolean value) {
        silent = value;
    }

    public static void close(final Dialog dialog) {
        final CountDownLatch latch = new CountDownLatch(1);

        final ComponentListener listener = new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                latch.countDown();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                super.componentShown(e);
            }
        };

        dialog.addComponentListener(listener);
        IDEUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    dialog.setVisible(false);
                }
            });

        try {
            latch.await();
            dialog.removeComponentListener(listener);
        } catch (InterruptedException e) {
        }
    }

    public static Dialog createDialog(final DialogDescriptor descriptor) {
        descriptor.setLeaf(true);

        return standard.createDialog(descriptor);
    }

    public static void display(final Dialog dialog) {
        final CountDownLatch latch = new CountDownLatch(1);

        final ComponentListener listener = new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                super.componentHidden(e);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                latch.countDown();
            }
        };

        dialog.addComponentListener(listener);
        IDEUtils.runInEventDispatchThread(new Runnable() {
                public void run() {
                    dialog.setVisible(true);
                }
            });

        try {
            latch.await();
            dialog.removeComponentListener(listener);
        } catch (InterruptedException e) {
        }
    }

    public static Object notify(final NotifyDescriptor descriptor) {
        if (silent) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Silently closed message: " + descriptor.getMessage()); //NOI18N
            }

            return NotifyDescriptor.CLOSED_OPTION;
        }

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest("notify: " + descriptor.getClass().getName()); //NOI18N
        }

        if (descriptor instanceof DNSAMessage) {
            final DNSAMessage dm = (DNSAMessage) descriptor;
            Object autoAccept = stringToNDOption(ProfilerIDESettings.getInstance().getDoNotShowAgain(dm.getDNSAKey()));

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("DNSAMessage key: " + dm.getDNSAKey() + ", autoAccept: " + autoAccept); //NOI18N
            }

            if (autoAccept != null) {
                return autoAccept;
            }

            final DNSAPanel dp = new DNSAPanel(dm.getMessage(), dm.getDNSADefault(), dm.getDNSAMessage());
            dm.setMessage(dp);

            final Object ret = standard.notify(descriptor);

            if ((ret != DialogDescriptor.CANCEL_OPTION) && (ret != DialogDescriptor.CLOSED_OPTION)) {
                if (dp.isAutoChecked()) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("DNSAMessage key: " + dm.getDNSAKey() + ", setting autoAccept: " + autoAccept); //NOI18N
                    }

                    ProfilerIDESettings.getInstance().setDoNotShowAgain(dm.getDNSAKey(), ndOptionToString(ret));
                }
            }

            return ret;
        } else if (descriptor instanceof DNSAConfirmation) {
            final DNSAConfirmation dm = (DNSAConfirmation) descriptor;
            Object autoAccept = stringToNDOption(ProfilerIDESettings.getInstance().getDoNotShowAgain(dm.getDNSAKey()));

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("DNSAConfirmation key: " + dm.getDNSAKey() + ", autoAccept: " + autoAccept); //NOI18N
            }

            if (autoAccept != null) {
                return autoAccept;
            }

            final DNSAPanel dp = new DNSAPanel(dm.getMessage(), dm.getDNSADefault(), dm.getDNSAMessage());
            dm.setMessage(dp);

            final Object ret = standard.notify(descriptor);

            if ((ret != DialogDescriptor.CANCEL_OPTION) && (ret != DialogDescriptor.CLOSED_OPTION)) {
                if (dp.isAutoChecked()) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("ProfilerDialogs: DNSAConfirmation key: " + dm.getDNSAKey() + ", setting autoAccept: "
                                      + autoAccept); //NOI18N
                    }

                    ProfilerIDESettings.getInstance().setDoNotShowAgain(dm.getDNSAKey(), ndOptionToString(ret));
                }
            }

            return ret;
        } else if (descriptor instanceof DNSAConfirmationChecked) {
            final DNSAConfirmationChecked dm = (DNSAConfirmationChecked) descriptor;
            Object autoAccept = stringToNDOption(ProfilerIDESettings.getInstance().getDoNotShowAgain(dm.getDNSAKey()));

            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("DNSAConfirmationChecked key: " + dm.getDNSAKey() + ", autoAccept: " + autoAccept); //NOI18N
            }

            if (autoAccept != null) {
                return autoAccept;
            }

            final DNSAPanel dp = new DNSAPanel(dm.getMessage(), dm.getDNSADefault(), dm.getDNSAMessage());
            dm.setMessage(dp);

            final Object ret = standard.notify(descriptor);

            if ((ret != DialogDescriptor.CANCEL_OPTION) && (ret != DialogDescriptor.CLOSED_OPTION)) {
                if (dp.isAutoChecked() && (ret != DialogDescriptor.NO_OPTION)) {
                    if (LOGGER.isLoggable(Level.FINEST)) {
                        LOGGER.finest("ProfilerDialogs: DNSAConfirmationChecked key: " + dm.getDNSAKey()
                                      + ", setting autoAccept: " + autoAccept); //NOI18N
                    }

                    ProfilerIDESettings.getInstance().setDoNotShowAgain(dm.getDNSAKey(), ndOptionToString(ret));
                }
            }

            return ret;
        } else if (descriptor instanceof MessageWithDetails) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("MessageWithDetails message:" + ((MessageWithDetails) descriptor).getDetailsMessage()); //NOI18N
            }

            final MessageWithDetails dm = (MessageWithDetails) descriptor;
            Object returnObj = standard.notify(dm);

            if (returnObj == dm.getShowDetailsButton()) {
                returnObj = standard.notify(new NotifyDescriptor.Message(dm.getDetailsMessage(), dm.getMessageType()));
            }

            return returnObj;
        } else {
            return standard.notify(descriptor);
        }
    }

    // Maps org.openide.NotifyDescriptor option to String
    private static String ndOptionToString(Object option) {
        if (option == NotifyDescriptor.CANCEL_OPTION) {
            return "CANCEL_OPTION"; // NOI18N
        } else if (option == NotifyDescriptor.CLOSED_OPTION) {
            return "CLOSED_OPTION"; // NOI18N
        } else if (option == NotifyDescriptor.NO_OPTION) {
            return "NO_OPTION"; // NOI18N
        } else if (option == NotifyDescriptor.OK_OPTION) {
            return "OK_OPTION"; // NOI18N
        } else if (option == NotifyDescriptor.YES_OPTION) {
            return "YES_OPTION"; // NOI18N
        }

        return null;
    }

    // Maps String to org.openide.NotifyDescriptor option
    private static Object stringToNDOption(String string) {
        if (string == null) {
            return null;
        }

        if (string.equals("CANCEL_OPTION")) { //NOI18N

            return NotifyDescriptor.CANCEL_OPTION;
        } else if (string.equals("CLOSED_OPTION")) { //NOI18N

            return NotifyDescriptor.CLOSED_OPTION;
        } else if (string.equals("NO_OPTION")) { //NOI18N

            return NotifyDescriptor.NO_OPTION;
        } else if (string.equals("OK_OPTION")) { //NOI18N

            return NotifyDescriptor.OK_OPTION;
        } else if (string.equals("YES_OPTION")) { //NOI18N

            return NotifyDescriptor.YES_OPTION;
        }

        return null;
    }
}
