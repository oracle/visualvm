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
package org.netbeans.modules.profiler.spi;

/**
 *
 * @author Jiri Sedlacek
 */
public abstract class ProfilerDialogsProvider {
    
    /**
     * Displays a user-level info message. Can be run from any thread.
     * @param message The info message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public abstract void displayInfo(String message, String caption, String details);
    
    /**
     * Displays a user-level info message with a checkbox. Can be run from any thread.
     * @param message The info message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     */
    public abstract void displayInfoDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault);
    
    /** Displays a user-level warning message. Can be run from any thread.
     * @param message The warning message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public abstract void displayWarning(String message, String caption, String details);
    
    /**
     * Displays a user-level warning message with a checkbox. Can be run from any thread.
     * @param message The warning message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     */
    public abstract void displayWarningDNSA(String message, String caption, String dnsaMessage, String key, boolean dnsaDefault);
    
    /** Displays a user-level error message. Can be run from any thread.
     * @param message The error message to display
     * @param caption The caption of the dialog, null for default
     * @param details Optional message with detailed information, null for no details
     */
    public abstract void displayError(String message, String caption, String details);
    
    /**
     * Displays a user-level confirmation message. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @param cancellable true if the dialog should display Cancel option, false otherwise
     * @return Boolean.TRUE if the user has confirmed the dialog, Boolean.FALSE if the user has rejected the dialog, null if the dialog has been cancelled
     */
    public abstract Boolean displayConfirmation(String message, String caption, boolean cancellable);
    
    /**
     * Displays a user-level confirmation message with a checkbox. Can be run from any thread.
     * @param message The confirmation message to display
     * @param caption The caption of the dialog, null for default
     * @param dnsaMessage The dnsa checkbox label, null for default
     * @param cancellable true if the dialog should display Cancel option, false otherwise
     * @param key dialog ID, must be unique for each DNSA dialog
     * @param dnsaDefault true if the dnsa checkbox should be selected by default, false otherwise
     * @return Boolean.TRUE if the user has confirmed the dialog, Boolean.FALSE if the user has rejected the dialog, null if the dialog has been cancelled
     */
    public abstract Boolean displayConfirmationDNSA(String message, String caption, String dnsaMessage, boolean cancellable, String key, boolean dnsaDefault);
    
}
