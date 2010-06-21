/*
 * Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.ui.components;

import java.awt.Dimension;
import javax.swing.JPanel;

/**
 * Subclass of a JPanel to be used as a spacer in GridBagLayout. Creates a
 * non-opaque JPanel with null Layout and zero preferred size.
 *
 * @author Jiri Sedlacek
 * @since VisualVM 1.3
 */
public final class Spacer extends JPanel {
    
    private static final Dimension DIMENSION_ZERO = new Dimension(0, 0);


    /**
     * Creates new instance of Spacer.
     *
     * @return new instance of Spacer
     */
    public static Spacer create() { return new Spacer(); }


    public Dimension getPreferredSize() { return DIMENSION_ZERO; }

    private Spacer() {
        super(null);
        setOpaque(false);
    }

}
