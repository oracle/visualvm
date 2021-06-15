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

package org.graalvm.visualvm.lib.jfluid.instrumentation;

import java.util.ResourceBundle;


/**
 * An exception thrown when begin line or end line are incorrect during Code Fragment profiling
 *
 * @author Ian Formanek
 */
public class BadLocationException extends Exception {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    // -----
    // I18N String constants
    private static final String CANNOT_FIND_METHOD_CURSOR_MSG;
    private static final String CANNOT_FIND_METHOD_SELECTION_MSG;

    static {
        ResourceBundle messages = ResourceBundle.getBundle("org.graalvm.visualvm.lib.jfluid.instrumentation.Bundle"); // NOI18N
        CANNOT_FIND_METHOD_CURSOR_MSG = messages.getString("BadLocationException_CannotFindMethodCursorMsg"); // NOI18N
        CANNOT_FIND_METHOD_SELECTION_MSG = messages.getString("BadLocationException_CannotFindMethodSelectionMsg"); // NOI18N
    }

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public BadLocationException() {
    }

    public BadLocationException(String message) {
        super(message);
    }

    public BadLocationException(int code) {
        super((code == 1) ? CANNOT_FIND_METHOD_CURSOR_MSG : ((code == 2) ? CANNOT_FIND_METHOD_SELECTION_MSG : null));
    }
}
