/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.profiler.impl;

import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import org.graalvm.visualvm.lib.profiler.api.ProfilerIDESettings;
import org.graalvm.visualvm.lib.profiler.ui.NBHTMLLabel;

@NbBundle.Messages({
    "ProfilerDialogs_DontShowAgainMsg=Do not show this message again",
    "ProfilerDialogs_ShowDetailsButtonText=Show Details"
})
final class ProfilerDialogs {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static final class DNSAConfirmation extends NotifyDescriptor.Confirmation {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private final String dnsaKey;
        private String dnsaMessage = Bundle.ProfilerDialogs_DontShowAgainMsg();
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
        private String dnsaMessage = Bundle.ProfilerDialogs_DontShowAgainMsg();
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
        private String dnsaMessage = Bundle.ProfilerDialogs_DontShowAgainMsg();
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

        private JButton showDetailsButton = new JButton(Bundle.ProfilerDialogs_ShowDetailsButtonText());
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

    private static boolean DEBUG = System.getProperty("org.graalvm.visualvm.lib.profiler.ui.ProfilerDialogs") != null; //NOI18N
    private static final DialogDisplayer standard = DialogDisplayer.getDefault();
    private static boolean silent = false;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    private ProfilerDialogs() {
    } // avoid direct instance creation

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public static void setSilent(boolean value) {
        silent = value;
    }

    public static Dialog createDialog(final DialogDescriptor descriptor) {
        descriptor.setLeaf(true);

        return standard.createDialog(descriptor);
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
