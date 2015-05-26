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

package org.netbeans.lib.profiler.ui.swing.renderer;

import java.text.Format;
import org.netbeans.lib.profiler.ui.Formatters;

/**
 *
 * @author Jiri Sedlacek
 */
public class PercentRenderer extends FormattedLabelRenderer implements RelativeRenderer {
    
    private static final String NUL = Formatters.percentFormat().format(0);
    private static final String NAN = NUL.replace('0', '-');  // NOI18N
    
    private long maxValue = 100;
    
    protected boolean renderingDiff;

    
    public PercentRenderer() {
        super(Formatters.percentFormat());
    }
    
    
    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }
    
    public long getMaxValue() {
        return maxValue;
    }
    
    
    public void setDiffMode(boolean diffMode) {
        renderingDiff = diffMode;
    }

    public boolean isDiffMode() {
        return renderingDiff;
    }
    
    
    protected String getValueString(Object value, int row, Format format) {
        if (value == null) return "-"; // NOI18N
        
        StringBuilder s = new StringBuilder();
        s.append("("); // NOI18N
        
        if (maxValue == 0) {
            s.append(NAN);
        } else {
            double number = ((Number)value).doubleValue();
            if (number == 0) {
                if (renderingDiff) s.append('+'); // NOI18N
                s.append(NUL);
            } else {
                number = number / maxValue;
                if (renderingDiff && number > 0) s.append('+'); // NOI18N
                s.append(format.format(number));
            }
        }
        
        s.append(")"); // NOI18N
        return s.toString();
    }
    
}
