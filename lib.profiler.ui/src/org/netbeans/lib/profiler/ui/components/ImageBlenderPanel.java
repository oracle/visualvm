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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.UIManager;


/**
 *
 * @author Jiri Sedlacek
 */
public class ImageBlenderPanel extends ImagePanel {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private Color background;
    private Image image1;
    private Image image2;
    private float blendAlpha;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public ImageBlenderPanel(Image image1, Image image2) {
        this(image1, image2, UIManager.getColor("Panel.background"), 0); // NOI18N
    }

    public ImageBlenderPanel(Image image1, Image image2, Color background, float blendAlpha) {
        super(createBlendedImage(image1, image2, background, blendAlpha));
        this.background = background;
        this.blendAlpha = blendAlpha;
        this.image1 = image1;
        this.image2 = image2;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setBlendAlpha(float blendAlpha) {
        setImage(createBlendedImage(image1, image2, background, blendAlpha));
        this.blendAlpha = blendAlpha;
    }

    private static Image createBlendedImage(Image image1, Image image2, Color background, float blendAlpha) {
        Image i1 = loadImage(image1);
        Image i2 = loadImage(image2);

        int blendedImageWidth = Math.max(i1.getWidth(null), i2.getWidth(null));
        int blendedImageHeight = Math.max(i1.getHeight(null), i2.getHeight(null));

        BufferedImage blendedImage = new BufferedImage(blendedImageWidth, blendedImageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D blendedImageGraphics = (Graphics2D) blendedImage.getGraphics();

        blendedImageGraphics.setColor(background);
        blendedImageGraphics.fillRect(0, 0, blendedImageWidth, blendedImageHeight);
        blendedImageGraphics.drawImage(i1, 0, 0, null);
        blendedImageGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, blendAlpha));
        blendedImageGraphics.drawImage(i2, 0, 0, null);

        return blendedImage;
    }
}
