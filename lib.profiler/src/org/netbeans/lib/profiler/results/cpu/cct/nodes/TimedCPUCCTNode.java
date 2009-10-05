/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.profiler.results.cpu.cct.nodes;

import org.netbeans.lib.profiler.results.cpu.cct.*;


/**
 *
 * @author Jaroslav Bachorik
 */
public abstract class TimedCPUCCTNode extends BaseCPUCCTNode implements Cloneable, RuntimeCPUCCTNode {
    //~ Inner Interfaces ---------------------------------------------------------------------------------------------------------

    private static interface TimingData {
        //~ Methods --------------------------------------------------------------------------------------------------------------

        void setNetTime0(final long time);

        long getNetTime0();

        void setNetTime1(final long time);

        long getNetTime1();

        void setSleepTime0(final long time);

        long getSleepTime0();

        void setWaitTime0(final long time);

        long getWaitTime0();

        long addNetTime0(final long time);

        long addNetTime1(final long time);

        long addSleepTime0(final long time);

        long addWaitTime0(final long time);
    }

    //~ Inner Classes ------------------------------------------------------------------------------------------------------------

    private static class TimingDataExtended extends TimingDataSimple {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long netTime1;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized void setNetTime1(final long time) {
            netTime1 = time;
        }

        public synchronized long getNetTime1() {
            return netTime1;
        }

        public synchronized long addNetTime1(final long time) {
            netTime1 += time;

            return netTime1;
        }
    }

    private static class TimingDataSimple implements TimingData {
        //~ Instance fields ------------------------------------------------------------------------------------------------------

        private long netTime0;
        private long sleepTime0;
        private long waitTime0;

        //~ Methods --------------------------------------------------------------------------------------------------------------

        public synchronized void setNetTime0(final long time) {
            netTime0 = time;
        }

        public synchronized long getNetTime0() {
            return netTime0;
        }

        public void setNetTime1(final long time) {
        }

        public long getNetTime1() {
            return 0;
        }

        public synchronized void setSleepTime0(final long time) {
            sleepTime0 = time;
        }

        public synchronized long getSleepTime0() {
            return sleepTime0;
        }

        public synchronized void setWaitTime0(final long time) {
            waitTime0 = time;
        }

        public synchronized long getWaitTime0() {
            return waitTime0;
        }

        public synchronized long addNetTime0(final long time) {
            netTime0 += time;

            return netTime0;
        }

        public long addNetTime1(final long time) {
            return 0;
        }

        public synchronized long addSleepTime0(final long time) {
            sleepTime0 += time;

            return sleepTime0;
        }

        public synchronized long addWaitTime0(final long time) {
            waitTime0 += time;

            return waitTime0;
        }
    }

    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    public static final int FILTERED_NO = 0;
    public static final int FILTERED_YES = 2;
    public static final int FILTERED_MAYBE = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private TimingData timingData;
    private char filteredStatus;
    private int nCalls;
    private int nCallsDiff;
    private long lastWaitOrSleepStamp;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    //  volatile protected boolean twoStamps;
    public TimedCPUCCTNode(CPUCCTNodeFactory factory, boolean collectingTwoTimestamps) {
        super(factory);

        if (collectingTwoTimestamps) {
            timingData = new TimingDataExtended();
        } else {
            timingData = new TimingDataSimple();
        }

        //    twoStamps = collectingTwoTimestamps;
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
        timingData.setNetTime0(time);
    }

    public long getNetTime0() {
        return timingData.getNetTime0();
    }

    public void setNetTime1(final long time) {
        timingData.setNetTime1(time);
    }

    public long getNetTime1() {
        return timingData.getNetTime1();
    }

    public void setSleepTime0(final long time) {
        timingData.setSleepTime0(time);
    }

    public long getSleepTime0() {
        return timingData.getSleepTime0();
    }

    public void setWaitTime0(final long time) {
        timingData.setWaitTime0(time);
    }

    public synchronized long getWaitTime0() {
        return timingData.getWaitTime0();
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
        return timingData.addNetTime0(time);
    }

    public long addNetTime1(final long time) {
        return timingData.addNetTime1(time);
    }

    public long addSleepTime0(final long time) {
        return timingData.addSleepTime0(time);
    }

    public long addWaitTime0(final long time) {
        return timingData.addWaitTime0(time);
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
