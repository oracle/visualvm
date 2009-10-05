/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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

package org.netbeans.lib.profiler.ui.components;

import org.netbeans.lib.profiler.ui.UIUtils;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;
import javax.swing.JRadioButton;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class JExtendedRadioButton extends JRadioButton {
    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    public static class DoubleIcon implements Icon {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private Icon icon1;
        private Icon icon2;
        private int icon1VertOffset = 0;
        private int icon2HorzOffset;
        private int icon2VertOffset = 0;
        private int iconHeight;
        private int iconWidth;
        private int iconsGap;

        //~ Constructors ---------------------------------------------------------------------------------------------------------

        public DoubleIcon(Icon icon1, Icon icon2, int iconsGap) {
            this.icon1 = icon1;
            this.icon2 = icon2;
            this.iconsGap = iconsGap;

            initInternals();
        }

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public Icon getIcon1() {
            return icon1;
        }

        public Icon getIcon2() {
            return icon2;
        }

        public int getIconHeight() {
            return iconHeight;
        }

        public int getIconWidth() {
            return iconWidth;
        }

        public int getIconsGap() {
            return iconsGap;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            icon1.paintIcon(c, g, x, y + icon1VertOffset);
            icon2.paintIcon(c, g, x + icon2HorzOffset, y + icon2VertOffset);
        }

        private void initInternals() {
            int icon1Width = icon1.getIconWidth();
            int icon1Height = icon1.getIconHeight();
            int icon2Height = icon2.getIconHeight();

            iconWidth = icon1Width + icon2.getIconWidth() + iconsGap;
            iconHeight = Math.max(icon1Height, icon2Height);

            if (icon1Height > icon2Height) {
                icon2VertOffset = (int) Math.ceil((float) (icon1Height - icon2Height) / (float) 2);
            } else if (icon1Height < icon2Height) {
                icon1VertOffset = (int) Math.ceil((float) (icon2Height - icon1Height) / (float) 2);
            }

            icon2HorzOffset = icon1Width + iconsGap;
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Icon extraIcon;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public JExtendedRadioButton(Icon extraIcon) {
        super();
        setExtraIcon(extraIcon);
    }

    public JExtendedRadioButton(String text) {
        super(text);
    }

    public JExtendedRadioButton(String text, Icon extraIcon) {
        this(text);
        setExtraIcon(extraIcon);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setExtraIcon(Icon extraIcon) {
        if (!isSupportedLaF()) {
            return;
        }

        this.extraIcon = extraIcon;

        if (extraIcon != null) {
            createExtraIcon();
        } else {
            resetExtraIcon();
        }
    }

    public Icon getExtraIcon() {
        return extraIcon;
    }

    private static Icon getDefaultIcon() {
        return UIManager.getIcon("RadioButton.icon"); // NOI18N
    }

    private static Icon getDisabledIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon disabledIcon = radio.getDisabledIcon();

        return (disabledIcon != null) ? disabledIcon : getIconSafe(radio);
    }

    private static Icon getDisabledSelectedIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon disabledSelectedIcon = radio.getDisabledSelectedIcon();

        return (disabledSelectedIcon != null) ? disabledSelectedIcon : getIconSafe(radio);
    }

    private static Icon getIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        return (icon != null) ? icon : getDefaultIcon();
    }

    private static Icon getPressedIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon pressedIcon = radio.getPressedIcon();

        if (pressedIcon == null) {
            pressedIcon = radio.getSelectedIcon();
        }

        return (pressedIcon != null) ? pressedIcon : getIconSafe(radio);
    }

    private static Icon getRolloverIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon rolloverIcon = radio.getRolloverIcon();

        return (rolloverIcon != null) ? rolloverIcon : getIconSafe(radio);
    }

    private static Icon getRolloverSelectedIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon rolloverSelectedIcon = radio.getRolloverSelectedIcon();

        if (rolloverSelectedIcon == null) {
            rolloverSelectedIcon = radio.getSelectedIcon();
        }

        return (rolloverSelectedIcon != null) ? rolloverSelectedIcon : getIconSafe(radio);
    }

    private static Icon getSelectedIconSafe(JRadioButton radio) {
        Icon icon = radio.getIcon();

        if (icon == null) {
            return getDefaultIcon();
        }

        Icon selectedIcon = radio.getSelectedIcon();

        return (selectedIcon != null) ? selectedIcon : getIconSafe(radio);
    }

    private static boolean isSupportedLaF() {
        return !UIUtils.isGTKLookAndFeel() && !UIUtils.isAquaLookAndFeel();
    }

    private void createExtraIcon() {
        JRadioButton reference = new JRadioButton();
        int iconTextGap = reference.getIconTextGap();

        Icon disabledIcon = getDisabledIconSafe(reference);
        Icon disabledSelectedIcon = getDisabledSelectedIconSafe(reference);
        Icon icon = getIconSafe(reference);
        Icon pressedIcon = getPressedIconSafe(reference);
        Icon rolloverIcon = getRolloverIconSafe(reference);
        Icon rolloverSelectedIcon = getRolloverSelectedIconSafe(reference);
        Icon selectedIcon = getSelectedIconSafe(reference);

        setDisabledIcon((disabledIcon == null) ? extraIcon : new DoubleIcon(disabledIcon, extraIcon, iconTextGap));
        setDisabledSelectedIcon((disabledSelectedIcon == null) ? extraIcon
                                                               : new DoubleIcon(disabledSelectedIcon, extraIcon, iconTextGap));
        setIcon((icon == null) ? extraIcon : new DoubleIcon(icon, extraIcon, iconTextGap));
        setPressedIcon((pressedIcon == null) ? extraIcon : new DoubleIcon(pressedIcon, extraIcon, iconTextGap));
        setRolloverIcon((rolloverIcon == null) ? extraIcon : new DoubleIcon(rolloverIcon, extraIcon, iconTextGap));
        setRolloverSelectedIcon((rolloverSelectedIcon == null) ? extraIcon
                                                               : new DoubleIcon(rolloverSelectedIcon, extraIcon, iconTextGap));
        setSelectedIcon((selectedIcon == null) ? extraIcon : new DoubleIcon(selectedIcon, extraIcon, iconTextGap));
    }

    private void resetExtraIcon() {
        JRadioButton reference = new JRadioButton();

        setDisabledIcon(reference.getDisabledIcon());
        setDisabledSelectedIcon(reference.getDisabledSelectedIcon());
        setIcon(reference.getIcon());
        setPressedIcon(reference.getPressedIcon());
        setRolloverIcon(reference.getRolloverIcon());
        setRolloverSelectedIcon(reference.getRolloverSelectedIcon());
        setSelectedIcon(reference.getSelectedIcon());
    }
}
