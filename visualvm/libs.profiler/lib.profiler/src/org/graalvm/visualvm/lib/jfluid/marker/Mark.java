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

package org.graalvm.visualvm.lib.jfluid.marker;


/**
 *
 * @author Jaroslav Bachorik
 */
public class Mark implements Cloneable {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final short DEFAULT_ID = 0;
    public static final char ID_NONE = (char) 0;
    private static short counter = 1;
    public static final Mark DEFAULT = new Mark(DEFAULT_ID);

    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    public final short id;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of Mark */
    public Mark(short value) {
        this.id = value;
    }

    public Mark() {
        this.id = counter++;
    }

    public boolean isDefault() {
        return this.equals(DEFAULT);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public short getId() {
        return id;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof Mark)) {
            return false;
        }

        return id == ((Mark) other).id;
    }

    public int hashCode() {
        int hash = 5;
        hash = 43 * hash + this.id;
        return hash;
    }
}
