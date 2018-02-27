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
package org.netbeans.modules.profiler.api;

import org.netbeans.modules.profiler.spi.ProfilerDialogsProvider;
import org.openide.util.Lookup;

/**
 * Support for displaying dialogs by the profiler.
 *
 * @author Jiri Sedlacek
 */
public final class ProfilerDialogs {
    
    /**
     * Displays a user-level info message. Can be run from any thread.
     * @param message The info message to display
     */
    public static void displayInfo(String message) {
        displayInfo(message, null, null);
    }
    
    /**
     * Displays a user-level info message. Can be run from any thread.
     * @param message The info message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public static void displayInfo(String message, String caption, String details) {
        ProfilerDialogsProvider p = provider();
        if (p != null) p.displayInfo(message, caption, details);
    }
    
    /**
     * Displays a user-level info message with a checkbox. Can be run from any thread.
     * @param message The info message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     */
    public static void displayInfoDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        ProfilerDialogsProvider p = provider();
        if (p != null) p.displayInfoDNSA(message, caption, dnsaMessage, key, dnsaDefault);
    }
    
    /** Displays a user-level warning message. Can be run from any thread.
     * @param message The warning message to display
     */
    public static void displayWarning(String message) {
        displayWarning(message, null, null);
    }
    
    /** Displays a user-level warning message. Can be run from any thread.
     * @param message The warning message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public static void displayWarning(String message, String caption, String details) {
        ProfilerDialogsProvider p = provider();
        if (p != null) p.displayWarning(message, caption, details);
    }
    
    /**
     * Displays a user-level warning message with a checkbox. Can be run from any thread.
     * @param message The warning message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     */
    public static void displayWarningDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        ProfilerDialogsProvider p = provider();
        if (p != null) p.displayWarningDNSA(message, caption, dnsaMessage, key, dnsaDefault);
    }
    
    /** Displays a user-level error message. Can be run from any thread.
     * @param message The error message to display
     */
    public static void displayError(String message) {
        displayError(message, null, null);
    }
    
    /** Displays a user-level error message. Can be run from any thread.
     * @param message The error message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public static void displayError(String message, String caption, String details) {
        ProfilerDialogsProvider p = provider();
        if (p != null) p.displayError(message, caption, details);
    }
    
    /**
     * Displays a user-level confirmation message. Can be run from any thread.
     * @param message The confirmation message to display
     * @return true if the user has confirmed the dialog, false otherwise
     */
    public static boolean displayConfirmation(String message) {
        return displayConfirmation(message, null);
    }
    
    /**
     * Displays a user-level confirmation message. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @return true if the user has confirmed the dialog, false otherwise
     */
    public static boolean displayConfirmation(String message, String caption) {
        ProfilerDialogsProvider p = provider();
        if (p != null) return Boolean.TRUE.equals(p.displayConfirmation(message, caption, false));
        else return false;
    }
    
    /**
     * Displays a user-level cancellable confirmation message. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @return Boolean.TRUE if the user has confirmed the dialog, Boolean.FALSE if the user has rejected the dialog, null if the dialog has been cancelled
     */
    public static Boolean displayCancellableConfirmation(String message, String caption) {
        ProfilerDialogsProvider p = provider();
        if (p != null) return p.displayConfirmation(message, caption, true);
        else return false;
    }
    
    /**
     * Displays a user-level confirmation message with a checkbox. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     * @return true if the user has confirmed the dialog, false otherwise
     */
    public static boolean displayConfirmationDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        ProfilerDialogsProvider p = provider();
        if (p != null) return Boolean.TRUE.equals(p.displayConfirmationDNSA(message, caption, dnsaMessage, false, key, dnsaDefault));
        else return false;
    }
    
    /**
     * Displays a user-level cancellable confirmation message with a checkbox. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     * @return Boolean.TRUE if the user has confirmed the dialog, Boolean.FALSE if the user has rejected the dialog, null if the dialog has been cancelled
     */
    public static Boolean displayCancellableConfirmationDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault) {
        ProfilerDialogsProvider p = provider();
        if (p != null) return p.displayConfirmationDNSA(message, caption, dnsaMessage, true, key, dnsaDefault);
        else return false;
    }
    
    private static ProfilerDialogsProvider provider() {
        return Lookup.getDefault().lookup(ProfilerDialogsProvider.class);
    }
    
}
