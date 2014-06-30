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

package com.sun.tools.visualvm.modules.appui.welcome;

import java.text.MessageFormat;
import javax.swing.JComponent;
import org.openide.util.NbBundle;

public class BundleSupport {

    private static final String BUNDLE_NAME = "com.sun.tools.visualvm.modules.appui.welcome.resources.Bundle"; // NOI18N

    private static final String LABEL_PREFIX = "LBL_"; // NOI18N
    private static final String URL_PREFIX = "URL_"; // NOI18N
    private static final String CATEGORY_PREFIX = "CATEGORY_"; // NOI18N
    private static final String TEMPLATE_PREFIX = "TEMPLATE_"; // NOI18N
    private static final String ACN_PREFIX = "ACN_"; // NOI18N
    private static final String ACD_PREFIX = "ACD_"; // NOI18N
    private static final String MNM_PREFIX = "MNM_"; // NOI18N
    
    public static String getLabel(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(LABEL_PREFIX + bundleKey);
    }
    
    public static String getURL(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(URL_PREFIX + bundleKey);
    }
    
    public static char getMnemonic(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(MNM_PREFIX + bundleKey).charAt(0);
    }
    
    public static String getSampleCategory(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(CATEGORY_PREFIX + bundleKey);
    }

    public static String getSampleTemplate(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(TEMPLATE_PREFIX + bundleKey);
    }

    public static String getAccessibilityName(String bundleKey) {
        return NbBundle.getBundle(BUNDLE_NAME).getString(ACN_PREFIX + bundleKey);
    }
    
    public static String getAccessibilityName(String bundleKey, String param) {
        return MessageFormat.format( NbBundle.getBundle(BUNDLE_NAME).getString(ACN_PREFIX + bundleKey), param );
    }
    
    public static String getAccessibilityDescription(String bundleKey, String param) {
        return MessageFormat.format( NbBundle.getBundle(BUNDLE_NAME).getString(ACD_PREFIX + bundleKey), param );
    }
    
    public static void setAccessibilityProperties(JComponent component, String bundleKey) {
        String aName = NbBundle.getBundle(BUNDLE_NAME).getString(ACN_PREFIX + bundleKey);  
        String aDescr = NbBundle.getBundle(BUNDLE_NAME).getString(ACD_PREFIX + bundleKey);  
      
        component.getAccessibleContext().setAccessibleName(aName);
        component.getAccessibleContext().setAccessibleDescription(aDescr);
    }
    
    public static String getMessage( String key, Object param ) {
        return MessageFormat.format( NbBundle.getBundle(BUNDLE_NAME).getString(key), param );
    }
}
