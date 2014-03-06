/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.lib.profiler.ui;

import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ResourceBundle;

/**
 *
 * @author Jiri Sedlacek
 */
public final class Formatters {
    
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("org.netbeans.lib.profiler.ui.Bundle"); // NOI18N
    
    private static NumberFormat NUMBER_FORMAT;
    /**
     * Returns Format instance for formatting numbers according to current Locale.
     * 
     * @return Format instance for formatting numbers according to current Locale
     */
    public static Format numberFormat() {
        if (NUMBER_FORMAT == null) {
            NUMBER_FORMAT = NumberFormat.getNumberInstance();
            NUMBER_FORMAT.setGroupingUsed(true);
        }
        return NUMBER_FORMAT;
    }
    
    private static NumberFormat PERCENT_FORMAT;
    /**
     * Returns Format instance for formatting percents according to current Locale.
     * 
     * @return Format instance for formatting percents according to current Locale
     */
    public static Format percentFormat() {
        if (PERCENT_FORMAT == null) {
            PERCENT_FORMAT = NumberFormat.getPercentInstance();
            PERCENT_FORMAT.setMaximumFractionDigits(1);
            PERCENT_FORMAT.setMinimumFractionDigits(0);
        }
        return PERCENT_FORMAT;
    }
    
    private static Format MILLISECONDS_FORMAT;
    /**
     * Returns Format instance to post-process a formatted milliseconds value.
     * By default adds a " ms" suffix to a formatted long value.
     * 
     * @return Format instance to post-process a formatted milliseconds value
     */
    public static Format millisecondsFormat() {
        if (MILLISECONDS_FORMAT == null) {
            MILLISECONDS_FORMAT = new MessageFormat(BUNDLE.getString("Formatters.MillisecondsFormat")); // NOI18N
        }
        return MILLISECONDS_FORMAT;
    }
    
    private static Format BYTES_FORMAT;
    /**
     * Returns Format instance to post-process a formatted Bytes (B) value.
     * By default adds a " B" suffix to a formatted long value.
     * 
     * @return Format instance to post-process a formatted Bytes value
     */
    public static Format bytesFormat() {
        if (BYTES_FORMAT == null) {
            BYTES_FORMAT = new MessageFormat(BUNDLE.getString("Formatters.BytesFormat")); // NOI18N
        }
        return BYTES_FORMAT;
    }
    
}
