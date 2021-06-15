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
package org.graalvm.visualvm.lib.profiler.impl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import org.graalvm.visualvm.lib.ui.components.CloseButton;
import org.openide.awt.CloseButtonFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jiri Sedlacek
 */
@ServiceProvider(service = CloseButton.Provider.class)
public class CloseButtonProvider extends CloseButton.Provider {

    @Override
    public JButton create(final Runnable onClose) {
        JButton close = CloseButtonFactory.createBigCloseButton();
        if (onClose != null) close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onClose.run(); }
        });
        return close;
    }

    @Override
    public JButton createSmall(final Runnable onClose) {
        JButton close = CloseButtonFactory.createCloseButton();
        if (onClose != null) close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { onClose.run(); }
        });
        return close;
    }

}
