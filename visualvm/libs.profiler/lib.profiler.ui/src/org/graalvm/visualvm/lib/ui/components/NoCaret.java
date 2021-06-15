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

import java.awt.Graphics;
import java.awt.Point;
import javax.swing.event.ChangeListener;
import javax.swing.text.Caret;
import javax.swing.text.JTextComponent;

/**
 *
 * @author Jiri Sedlacek
 */
public final class NoCaret implements Caret {

    public void install(JTextComponent c) {}
    public void deinstall(JTextComponent c) {}
    public void paint(Graphics g) {}
    public void addChangeListener(ChangeListener l) {}
    public void removeChangeListener(ChangeListener l) {}
    public boolean isVisible() { return false; }
    public void setVisible(boolean v) {}
    public boolean isSelectionVisible() { return false; }
    public void setSelectionVisible(boolean v) {}
    public void setMagicCaretPosition(Point p) {}
    public Point getMagicCaretPosition() { return new Point(0, 0); }
    public void setBlinkRate(int rate) {}
    public int getBlinkRate() { return 1; }
    public int getDot() { return 0; }
    public int getMark() { return 0; }
    public void setDot(int dot) {}
    public void moveDot(int dot) {}

}
