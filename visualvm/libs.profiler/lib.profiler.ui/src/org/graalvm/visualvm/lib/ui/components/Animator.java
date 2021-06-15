/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.ui.components;

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
