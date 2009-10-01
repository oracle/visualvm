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

import java.awt.event.ActionListener;
import javax.swing.*;


/**
 * This class animates two <code>AnimatedContainers</code> - the selected one is enlarging, the previously selected container is shrinking.
 * The animation parameters are set here as static final fields.
 * CURRENTLY WE DO NOT USE THIS CLASS.
 */
public class Animator implements ActionListener {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int STEPS = 5;
    private static final int DURATION = 100; //miliseconds

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //containers that will be animated
    AnimatedContainer cont1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    //containers that will be animated
    AnimatedContainer cont2;

    //timer producing animation frames events
    Timer timer;
    private int stepsCounter;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Animator */
    public Animator(AnimatedContainer cont1, AnimatedContainer cont2) {
        this.cont1 = cont1;
        this.cont2 = cont2;

        timer = new Timer(DURATION / STEPS, null);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void actionPerformed(java.awt.event.ActionEvent e) {
        performAnimationStep();
    }

    public void startAnimation() {
        //reset counter
        stepsCounter = 0;

        //register itself to timer
        timer.addActionListener(this);

        //start animation
        timer.start();
    }

    private void performAnimationStep() {
        int percIncrement = (int) 100 / STEPS;

        stepsCounter++;

        if (stepsCounter == STEPS) {
            cont1.setFinishState();

            if (cont2 != null) {
                cont2.setFinishState();
            }

            cont1.revalidate();

            if (cont2 != null) {
                cont2.revalidate();
            }

            //stop animation
            timer.stop();
            //unregister
            timer.removeActionListener(this);
        } else {
            cont1.setState(stepsCounter * percIncrement);

            if (cont2 != null) {
                cont2.setState(stepsCounter * percIncrement);
            }

            cont1.revalidate();

            if (cont2 != null) {
                cont2.revalidate();
            }
        }
    }
}
