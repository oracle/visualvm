/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans;

import org.openide.util.NbBundle;

/**
 * Provides resource support for the containing java package.
 */
final class Resources {

    private Resources() {
        throw new AssertionError();
    }

    /**
     * Returns the text of the resource for the specified
     * key formatted with the specified arguments.
     */
    public static String getText(String key, Object... args) {
        return NbBundle.getMessage(Resources.class, key, args);
    }

    /**
     * Returns the mnemonic keycode int of the resource for
     * the specified key.
     */
    public static int getMnemonicInt(String key) {
        String m = getText(key + ".mnemonic"); // NOI18N
        int mnemonic = m.charAt(0);
        if (mnemonic >= 'a' && mnemonic <= 'z') {
            mnemonic -= ('a' - 'A');
        }
        return mnemonic;
    }
}
