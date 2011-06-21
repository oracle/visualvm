/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.profiler.impl;

import org.netbeans.modules.profiler.spi.ProfilerDialogsProvider;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;

/**
 *
 * @author Jiri Sedlacek
 */
@org.openide.util.lookup.ServiceProvider(service=org.netbeans.modules.profiler.spi.ProfilerDialogsProvider.class)
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
        ProfilerDialogs.DNSAConfirmationChecked dnsa = new ProfilerDialogs.DNSAConfirmationChecked(
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
        NotifyDescriptor nd = details == null ? new NotifyDescriptor.Message(message, type) :
                        new ProfilerDialogs.MessageWithDetails(message, details, type, false);
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
    
}
