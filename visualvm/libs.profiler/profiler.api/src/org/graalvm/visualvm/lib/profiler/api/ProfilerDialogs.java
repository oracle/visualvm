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
package org.graalvm.visualvm.lib.profiler.api;

import org.graalvm.visualvm.lib.profiler.spi.ProfilerDialogsProvider;
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
