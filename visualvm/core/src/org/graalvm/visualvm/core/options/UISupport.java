/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.core.options;

import org.graalvm.visualvm.core.ui.components.ScrollableContainer;
import org.graalvm.visualvm.core.ui.components.SectionSeparator;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * Helper class to create UI components for the Options dialog.
 *
 * @author Jiri Sedlacek
 */
public final class UISupport {

    /**
     * Creates preformated instance of SectionSeparator to be used in Options
     * dialog.
     *
     * @param caption section name
     * @return preformated instance of SectionSeparator
     */
    public static SectionSeparator createSectionSeparator(String caption) {
        return new SectionSeparator(caption);
    }

    /**
     * Creates preformatted instance of ScrollableContainer to be used in Options
     * dialog. All insets are already initialized to defaults, the client components
     * should have zero outer insets.
     * 
     * @param contents component to be displayed
     * @return preformatted instance of ScrollableContainer
     */
    public static ScrollableContainer createScrollableContainer(JComponent contents) {
        ScrollableContainer container = new ScrollableContainer(contents,
                                        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        container.setViewportBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        container.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 5));
        return container;
    }


    private UISupport() {}

}
