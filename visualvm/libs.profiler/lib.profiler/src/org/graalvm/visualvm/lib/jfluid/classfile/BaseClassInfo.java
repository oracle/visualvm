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
 * Minimum representation of a class. Used as a base class for the full-fledged ClassInfo, but also
 * may used as is for e.g. array classes.
 *
 * @author Misha Dmitirev
 */
public class BaseClassInfo {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected String name;
    protected String nameAndLoader; // A combinarion of class name and loader, uniquely identifying this ClassInfo

    // Management of multiple versions for the same-named (but possibly not same-code) class, loaded by different classloaders
    protected int classLoaderId; // IDs of all loaders with which versions of this class are loaded

    // Data used by our object allocation instrumentation mechanism: integer class ID
    private int instrClassId;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public BaseClassInfo(String className, int classLoaderId) {
        this.name = className.intern();
        this.classLoaderId = classLoaderId;
        nameAndLoader = (name + "#" + classLoaderId).intern(); // NOI18N
        instrClassId = -1;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void setInstrClassId(int id) {
        instrClassId = id;
    }

    public int getInstrClassId() {
        return instrClassId;
    }

    public void setLoaderId(int loaderId) {
        classLoaderId = loaderId;
    }

    public int getLoaderId() {
        return classLoaderId;
    }

    public String getName() {
        return name;
    }

    public String getNameAndLoader() {
        return nameAndLoader;
    }

    public String toString() {
        return name;
    }
}
