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

package org.graalvm.visualvm.lib.ui.cpu.statistics;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TimingData {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private int incInv;
    private int outInv;
    private long time0Acc;
    private long time1Acc;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TimingData() {
        time0Acc = 0;
        time1Acc = 0;
        incInv = 0;
        outInv = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized int getIncInv() {
        return incInv;
    }

    public synchronized int getOutInv() {
        return outInv;
    }

    public synchronized long getTime0Acc() {
        return time0Acc;
    }

    public synchronized long getTime1Acc() {
        return time1Acc;
    }

    public synchronized void addIncomming(int invocations) {
        incInv += invocations;
    }

    public synchronized void addOutgoing(int invocations) {
        outInv += invocations;
    }

    public synchronized void addTime0(long time0) {
        time0Acc += time0;
    }

    public synchronized void addTime1(long time1) {
        time1Acc += time1;
    }

    public synchronized void incrementIncomming() {
        incInv++;
    }

    public synchronized void incrementOutgoing() {
        outInv++;
    }
}
