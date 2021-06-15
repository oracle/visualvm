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

package org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes;

/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class TimedCPUCCTNode extends BaseCPUCCTNode implements Cloneable, RuntimeCPUCCTNode {

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int FILTERED_NO = 0;
    public static final int FILTERED_YES = 2;
    public static final int FILTERED_MAYBE = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // timing data
    private long netTime0;
    private long netTime1;
    private long sleepTime0;
    private long waitTime0;

    private char filteredStatus;
    private int nCalls;
    private int nCallsDiff;
    private long lastWaitOrSleepStamp;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TimedCPUCCTNode() {
        super();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Sets the "filtered out" status of the node
     * @param status Use one of the following:
     *               TimedCPUCCTNode.FILTERED_NO - if the node is not filtered out at all
     *               TimedCPUCCTNode.FILTERED_YES - if the node is unconditionally filtered out
     *               TimedCPUCCTNode.FILTERED_MAYBE - if the node might be filtered out, depending on other profiling settings
     */
    public synchronized void setFilteredStatus(int status) {
        filteredStatus = (char) (status & 0xff);
    }

    //  public synchronized char getMarkID() {
    //    return markId;
    //  }
    //
    //  public synchronized void setMarkID(final char markId) {
    //    this.markId = markId;
    //  }

    /**
     * Returns the "filtered out" status of the node
     * @return Returns one of the following values:
     *         TimedCPUCCTNode.FILTERED_NO - if the node is not filtered out at all
     *         TimedCPUCCTNode.FILTERED_YES - if the node is unconditionally filtered out
     *         TimedCPUCCTNode.FILTERED_MAYBE - if the node might be filtered out, depending on other profiling settings
     */
    public synchronized int getFilteredStatus() {
        return filteredStatus;
    }

    public synchronized void setLastWaitOrSleepStamp(final long time) {
        lastWaitOrSleepStamp = time;
    }

    public synchronized long getLastWaitOrSleepStamp() {
        return lastWaitOrSleepStamp;
    }

    public synchronized void setNCalls(final int calls) {
        nCalls = calls;
    }

    public synchronized int getNCalls() {
        return nCalls;
    }

    public synchronized void setNCallsDiff(final int calls) {
        nCallsDiff = calls;
    }

    public synchronized int getNCallsDiff() {
        return nCallsDiff;
    }

    public void setNetTime0(final long time) {
        netTime0 = time;
    }

    public long getNetTime0() {
        return netTime0;
    }

    public void setNetTime1(final long time) {
        netTime1 = time;
    }

    public long getNetTime1() {
        return netTime1;
    }

    public void setSleepTime0(final long time) {
        sleepTime0 = time;
    }

    public long getSleepTime0() {
        return sleepTime0;
    }

    public void setWaitTime0(final long time) {
        waitTime0 = time;
    }

    public synchronized long getWaitTime0() {
        return waitTime0;
    }

    public synchronized int addNCalls(final int calls) {
        nCalls += calls;

        return nCalls;
    }

    public synchronized int addNCallsDiff(final int calls) {
        nCallsDiff += calls;

        return nCallsDiff;
    }

    public long addNetTime0(final long time) {
        netTime0 += time;
        return netTime0;
    }

    public long addNetTime1(final long time) {
        netTime1 += time;
        return netTime1;
    }

    public long addSleepTime0(final long time) {
        sleepTime0 += time;
        return sleepTime0;
    }

    public long addWaitTime0(final long time) {
        waitTime0 += time;
        return waitTime0;
    }

    // @Override
    public synchronized Object clone() {
        TimedCPUCCTNode node = createSelfInstance();
        node.setNCalls(getNCalls());
        node.setNetTime0(getNetTime0());
        node.setNetTime1(getNetTime1());
        node.setSleepTime0(getSleepTime0());
        node.setWaitTime0(getWaitTime0());
        //    node.setMarkID(getMarkID());
        node.setFilteredStatus(getFilteredStatus());
        node.setNCallsDiff(0);

        return node;
    }

    protected abstract TimedCPUCCTNode createSelfInstance();
}
