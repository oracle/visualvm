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

import java.awt.*;
import javax.swing.*;


/**
 * A container for two contents that are animated in traansition between them.
 *
 * @author Vlada Nemec
 */
public class AnimatedContainer extends javax.swing.JPanel {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int WIDTH = 10;
    public static final int HEIGHT = 20;
    private static final int BOTH = 30;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //custom layout manager used for placing content component
    private AnimationLayout layout = new AnimationLayout();
    private CustomTaskButtonBorder border;

    //content of the container - limited to one component
    private JComponent content;

    //target content for the animation
    private JComponent targetContent;

    //temporary content used during animation
    private JComponent transContent;
    private int animatedDimension = BOTH;
    private int origHeight;

    //original dimensions
    private int origWidth;
    private int targetHeight;

    //new dimension for animation (if needed)
    private int targetWidth;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * Creates a new instance of AnimatedContainer
     */
    public AnimatedContainer(Color backgroundColor) {
        setLayout(layout);

        border = new CustomTaskButtonBorder(backgroundColor, super.getBackground());
        setBorder(border);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setAnimatedDimension(int dimension) {
        animatedDimension = dimension;
    }

    /**
     * Setups animation parameters.
     */
    public void setAnimation(JComponent targetContent, JComponent transContent) {
        //setup starting point dimension
        origWidth = content.getWidth();
        origHeight = content.getHeight();

        //set contents
        if (targetContent == null) {
            this.targetContent = content;
        } else if (transContent == null) {
            this.transContent = content;
        } else {
            this.targetContent = targetContent;
            this.transContent = transContent;

            targetWidth = (int) targetContent.getPreferredSize().getWidth();
            targetHeight = (int) targetContent.getPreferredSize().getHeight();
        }
    }

    /**
     * Setups animation parameters. <code>transContent</code> is set to <code>content</code>
     */
    public void setAnimation(JComponent aTargetContent) {
        //setup starting point dimension
        origWidth = content.getWidth();
        origHeight = content.getHeight();

        if (aTargetContent == null) {
            targetContent = content;
        } else {
            targetContent = aTargetContent;

            targetWidth = (int) targetContent.getPreferredSize().getWidth();
            targetHeight = (int) targetContent.getPreferredSize().getHeight();
        }
    }

    public void setAnimation(Dimension dimension) {
        //setup starting point dimension
        origWidth = content.getWidth();
        origHeight = content.getHeight();

        targetContent = null;
        transContent = content;

        targetWidth = (int) dimension.getWidth();
        targetHeight = (int) dimension.getHeight();
    }

    public void setAnimation() {
        origWidth = content.getWidth();
        origHeight = content.getHeight();

        transContent = content;
        targetContent = content;
    }

    /**
     * Sets content component reference. This method does NOT place any component into container.
     */
    public void setContent(JComponent content) {
        this.content = content;
    }

    public void setDefaultBorder() {
        border.setDefault();
        repaint();
    }

    /**
     * Sets the state of the container to finish state  with target size and content
     */
    public void setFinishState() {
        //empty container
        removeAll();

        //add target content
        if (targetContent != null) {
            add(targetContent);
            //resize to final size
            setPreferredSize(new Dimension(targetWidth, targetHeight));
            setMinimumSize(new Dimension(targetWidth, targetHeight));
        } else if (transContent != null) {
            add(transContent);
            //resize to final size
            setPreferredSize(transContent.getPreferredSize());
            setMinimumSize(transContent.getPreferredSize());
        }

        //resize to final size
        setPreferredSize(content.getPreferredSize());
        setMinimumSize(content.getPreferredSize());
    }

    public void setFocusedBorder() {
        border.setFocused();
        repaint();
    }

    public void setSelectedBorder() {
        border.setSelected();
        repaint();
    }

    /**
     * Sets the state of the container - this method provides the resizing of container for animation
     */
    public void setState(int percents) {
        int newWidth;
        int newHeight;

        newWidth = targetWidth;
        newHeight = targetHeight;

        origWidth = (int) getSize().getWidth();
        origHeight = (int) getSize().getHeight();

        int deltaWidth = newWidth - origWidth;
        int deltaHeight = newHeight - origHeight;

        double perc = (double) percents / 100.0;

        Dimension d;

        if (animatedDimension == WIDTH) {
            d = new Dimension((int) (origWidth + (deltaWidth * perc)), origHeight);
        } else if (animatedDimension == HEIGHT) {
            d = new Dimension(origWidth, (int) (origHeight + (deltaHeight * perc)));
        } else {
            d = new Dimension((int) (origWidth + (deltaWidth * perc)), (int) (origHeight + (deltaHeight * perc)));
        }

        setPreferredSize(d);
        setMinimumSize(d);
    }

    /**
     * Sets target content component reference. This method does NOT place any component into container.
     */
    public void setTargetContent(JComponent targetContent) {
        this.targetContent = targetContent;
    }

    /**
     * Sets transient content component reference. This method does NOT place any component into container.
     */
    public void setTransContent(JComponent transContent) {
        this.transContent = transContent;
    }

    /**
     * Overridden - we need to store reference to the content component
     */
    public Component add(Component component) {
        content = (JComponent) component;

        return super.add(component);
    }

    /**
     * locks the content size - while resizing the content component size and layout remains the same
     */
    public void lockContentResizing(boolean lock) {
        if ((lock) && (transContent != null)) {
            layout.setLockedSize(transContent.getSize());
        } else {
            layout.setLockedSize(null);
        }
    }
}
