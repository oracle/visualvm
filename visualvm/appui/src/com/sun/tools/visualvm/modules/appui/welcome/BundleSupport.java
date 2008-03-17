/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 *
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
