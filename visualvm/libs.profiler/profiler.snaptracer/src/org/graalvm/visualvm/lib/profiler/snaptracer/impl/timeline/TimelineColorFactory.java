/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2007-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.profiler.snaptracer.impl.timeline;

import java.awt.Color;

/**
 * Utility class to access colors predefined for VisualVM.
 *
 * @author Jiri Sedlacek
 */
final class TimelineColorFactory {
    
    private static final Color[] PREDEFINED_COLORS = new Color[] {
                                                new Color(241, 154,  42),
                                                new Color( 32, 171, 217),
                                                new Color(144,  97, 207),
                                                new Color(158, 156,   0)
    };

    private static final Color[][] PREDEFINED_GRADIENTS = new Color[][] {
        new Color[] { new Color(245, 204, 152), new Color(255, 243, 226) },
        new Color[] { new Color(151, 223, 246), new Color(227, 248, 255) },
        new Color[] { new Color(200, 163, 248), new Color(242, 232, 255) },
        new Color[] { new Color(212, 211, 131), new Color(244, 243, 217) }
    };
    

    /**
     * Returns a color predefined for VisualVM charts.
     *
     * @param index index of the predefined color
     * @return color predefined for VisualVM charts
     */
    private static Color getPredefinedColor(int index) {
        return PREDEFINED_COLORS[index];
    }

    static Color getColor(int index) {
        Color color;

        if (index >= PREDEFINED_COLORS.length) {
            color = getPredefinedColor(index % PREDEFINED_COLORS.length);
            int darkerFactor = index / PREDEFINED_COLORS.length;
            while (darkerFactor-- > 0) color = color.darker();
        } else {
            color = getPredefinedColor(index);
        }

        return color;
    }


    /**
     * Returns a color pair predefined for VisualVM charts gradients.
     *
     * @param index index of the predefined color pair
     * @return color pair predefined for VisualVM charts gradients
     */
    private static Color[] getPredefinedGradient(int index) {
        return PREDEFINED_GRADIENTS[index];
    }

    static Color[] getGradient(int index) {
        Color[] colors = null;

        if (index >= PREDEFINED_GRADIENTS.length) {
            colors = getPredefinedGradient(index % PREDEFINED_GRADIENTS.length);
            int darkerFactor = index / PREDEFINED_GRADIENTS.length;
            while (darkerFactor-- > 0) {
                colors[0] = colors[0].darker();
                colors[1] = colors[1].darker();
            }
        } else {
            colors = getPredefinedGradient(index);
        }

        return colors;

    }

}
