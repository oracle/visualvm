/*
 *  Copyright (c) 2009, 2011, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 * 
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 * 
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package com.sun.tools.visualvm.modules.oqlsyntax;

import javax.swing.JEditorPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.profiler.oql.spi.*;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jaroslav Bachorik <jaroslav.bachorik@sun.com>
 */
@ServiceProvider(service=OQLEditorImpl.class)
public class OQLSyntaxEditor extends OQLEditorImpl {

    private class DocumentListenerEx implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() > 0) {
                getValidationCallback(e.getDocument()).callback(true);
            } else {
                getValidationCallback(e.getDocument()).callback(false);
            }
        }
    };

    @Override
    public JEditorPane getEditorPane() {
        JEditorPane pane = new JEditorPane();
        pane.setContentType("text/x-oql");
        pane.getDocument().addDocumentListener(new DocumentListenerEx());
        return pane;
    }
}
