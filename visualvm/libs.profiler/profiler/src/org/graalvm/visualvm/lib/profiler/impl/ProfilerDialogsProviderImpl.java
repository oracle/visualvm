/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.graalvm.visualvm.lib.profiler.spi.ProfilerDialogsProvider;
import org.graalvm.visualvm.lib.profiler.ui.NBHTMLLabel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.graalvm.visualvm.lib.profiler.spi.ProfilerDialogsProvider.class)
public final class ProfilerDialogsProviderImpl extends ProfilerDialogsProvider {

    @Override
    public void displayInfo(String message, String caption, String details) {
        displayMessage(message, caption, details, NotifyDescriptor.INFORMATION_MESSAGE);
    }

    @Override
    public void displayInfoDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        displayDNSAMessage(message, caption, dnsaMessage, key, dnsaDefault, NotifyDescriptor.INFORMATION_MESSAGE);
    }

    @Override
    public void displayWarning(String message, String caption, String details) {
        displayMessage(message, caption, details, NotifyDescriptor.WARNING_MESSAGE);
    }

    @Override
    public void displayWarningDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        displayDNSAMessage(message, caption, dnsaMessage, key, dnsaDefault, NotifyDescriptor.WARNING_MESSAGE);
    }

    @Override
    public void displayError(String message, String caption, String details) {
        displayMessage(message, caption, details, NotifyDescriptor.ERROR_MESSAGE);
    }

    @Override
    public Boolean displayConfirmation(String message, String caption, boolean cancellable) {
        NotifyDescriptor nd = new NotifyDescriptor.Confirmation(message,
                cancellable ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
        if (caption != null) nd.setTitle(caption);
        Object ret = DialogDisplayer.getDefault().notify(nd);
        if (ret == NotifyDescriptor.YES_OPTION) return Boolean.TRUE;
        if (ret == NotifyDescriptor.NO_OPTION) return Boolean.FALSE;
        return null;
    }

    @Override
    public Boolean displayConfirmationDNSA(String message, String caption, String dnsaMessage, boolean cancellable, String key, boolean dnsaDefault) {
        ProfilerDialogs.DNSAConfirmation dnsa = new ProfilerDialogs.DNSAConfirmation(
                key, message, cancellable ? NotifyDescriptor.YES_NO_CANCEL_OPTION : NotifyDescriptor.YES_NO_OPTION);
        if (caption != null) dnsa.setTitle(caption);
        if (dnsaMessage != null) dnsa.setDNSAMessage(dnsaMessage);
        dnsa.setDNSADefault(dnsaDefault);
        Object ret = ProfilerDialogs.notify(dnsa);
        if (ret == NotifyDescriptor.YES_OPTION) return Boolean.TRUE;
        if (ret == NotifyDescriptor.NO_OPTION) return Boolean.FALSE;
        return null;
    }
    
    private void displayMessage(String message, String caption, String details, int type) {
        Object msg = message;
        Object det = details;
        if (isHtmlString(message)) msg = new NBHTMLLabel(message);
        if (isHtmlString(details)) det = new NBHTMLLabel(message);
        NotifyDescriptor nd = det == null ? new NotifyDescriptor.Message(msg, type) :
                        new ProfilerDialogs.MessageWithDetails(msg, det, type, false);
        if (caption != null) nd.setTitle(caption);
        ProfilerDialogs.notify(nd);
    }
    
    private void displayDNSAMessage(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault, int type) {
        ProfilerDialogs.DNSAMessage dnsa = new ProfilerDialogs.DNSAMessage(key, message, type);
        if (caption != null) dnsa.setTitle(caption);
        if (dnsaMessage != null) dnsa.setDNSAMessage(dnsaMessage);
        dnsa.setDNSADefault(dnsaDefault);
        ProfilerDialogs.notify(dnsa);
    }
    
    private static boolean isHtmlString(String string) {
        if (string == null) return false;
        // Simple heuristics, seems to work fine
        return string.contains("<") && string.contains(">"); // NOI18N
    }
    
}
