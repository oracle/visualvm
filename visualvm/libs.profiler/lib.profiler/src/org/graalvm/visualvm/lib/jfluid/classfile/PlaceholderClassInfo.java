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

package org.graalvm.visualvm.lib.jfluid.classfile;


/**
 * A placeholder for a real class that can be put into the table in ClassRepository. Used temporarily, in situations when some
 * info about class becomes known and/or has to be recorded before the class itself is loaded by the VM. Once a real class
 * is loaded, it replaces the placeholder, and the info from the latter is transferred into the real class using
 * transferDataIntoRealClass.
 *
 * @author Misha Dmitirev
 */
public class PlaceholderClassInfo extends BaseClassInfo {
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public PlaceholderClassInfo(String className, int classLoaderId) {
        super(className, classLoaderId);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void transferDataIntoRealClass(DynamicClassInfo clazz) {
        clazz.setInstrClassId(this.getInstrClassId());
    }
}
