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

package org.graalvm.visualvm.lib.jfluid.jps;


/**
 * A container for various information available for a running JVM.
 * Note that "VM flags" that we have for the VM in principle, is various -XX:+... options, which are supposed to
 * be used only by real expert users, or for debugging. We have them here just for completeness, but since they
 * are used very rarely, there is probably no reason to display them in the attach dialog or whatever.
 *
 * @author Misha Dmitriev
 */
public class RunningVM {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private String mainArgs;
    private String mainClass;
    private String vmArgs;
    private String vmFlags;
    private int pid;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /** Creates a new instance of RunningVM */
    public RunningVM(int pid, String vmFlags, String vmArgs, String mainClass, String mainArgs) {
        this.pid = pid;
        this.vmFlags = vmFlags;
        this.vmArgs = vmArgs;
        this.mainClass = mainClass;
        this.mainArgs = mainArgs;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getMainArgs() {
        return mainArgs;
    }

    public String getMainClass() {
        return mainClass;
    }

    public int getPid() {
        return pid;
    }

    public String getVMArgs() {
        return vmArgs;
    }

    public String getVMFlags() {
        return vmFlags;
    }

    public String toString() {
        return getPid() + "  " + getVMFlags() + "  " + getVMArgs() + "  " + getMainClass() + "  " + getMainArgs(); // NOI18N
    }

    public int hashCode() {
        return toString().hashCode();
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof RunningVM)) return false;
        return toString().equals(o.toString());
    }
}
