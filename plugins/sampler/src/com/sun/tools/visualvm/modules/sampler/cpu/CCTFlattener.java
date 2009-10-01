package com.sun.tools.visualvm.modules.sampler.cpu;

import java.util.Stack;
import org.netbeans.lib.profiler.global.InstrumentationFilter;
import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.MethodInfoMapper;
import org.netbeans.lib.profiler.results.cpu.cct.CPUCCTVisitorAdapter;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ThreadCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.TimedCPUCCTNode;

/**
 *
 * @author Tomas Hurka
 */
class CCTFlattener extends CPUCCTVisitorAdapter {

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object containerGuard = new Object();

    // @GuardedBy containerGuard
    private FlatProfileContainer container;
    private Stack parentStack;
    private int[] invDiff;
    private int[] invPM;
    private int[] nCalleeInvocations;
    private long[] timePM0;
    private long[] timePM1;
    private int nMethods;
    private boolean collectingTwoTimeStamps;
    private MethodInfoMapper methodInfoMapper;
    private InstrumentationFilter filter;
    
    //~ Constructors -------------------------------------------------------------------------------------------------------------

    CCTFlattener(boolean twoStamps, MethodInfoMapper mapper, InstrumentationFilter f) {
        parentStack = new Stack();
        nMethods = mapper.getMaxMethodId();
        methodInfoMapper = mapper;
        collectingTwoTimeStamps = twoStamps;
        filter = f;
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    FlatProfileContainer getFlatProfile() {
        synchronized (containerGuard) {
            return container;
        }
    }

    public void afterWalk() {
        // Now convert the data into microseconds
        long wholeGraphTime0 = 0;

        // Now convert the data into microseconds
        long wholeGraphTime1 = 0;
        long totalNInv = 0;

        for (int i = 0; i < nMethods; i++) {
            // convert to microseconds
            double time = timePM0[i] / 1000.0;

            if (time < 0) {
                // in some cases the combination of cleansing the time by calibration and subtracting wait/sleep
                // times can lead to <0 time
                // see http://profiler.netbeans.org/issues/show_bug.cgi?id=64416
                time = 0;
            }

            timePM0[i] = (long) time;

            // don't include the Thread time into wholegraphtime
            if (i > 0) {
                wholeGraphTime0 += time;
            }

            if (collectingTwoTimeStamps) {
                // convert to microseconds
                time = timePM1[i] / 1000.0;
                timePM1[i] = (long) time;

                // don't include the Thread time into wholegraphtime
                if (i > 0) {
                    wholeGraphTime1 += time;
                }
            }

            totalNInv += invPM[i];
        }

        synchronized (containerGuard) {
            container = new FlatProfilerContainer(methodInfoMapper, collectingTwoTimeStamps, timePM0, timePM1, invPM, new char[0], wholeGraphTime0,
                                                     wholeGraphTime1, invPM.length);
        }

        timePM0 = timePM1 = null;
        invPM = invDiff = nCalleeInvocations = null;
        parentStack.clear();
//        currentFilter = null;
    }

    public void beforeWalk() {
        timePM0 = new long[nMethods];
        timePM1 = new long[collectingTwoTimeStamps ? nMethods : 0];
        invPM = new int[nMethods];
        invDiff = new int[nMethods];
        nCalleeInvocations = new int[nMethods];
        parentStack.clear();

//        currentFilter = (CCTResultsFilter)Lookup.getDefault().lookup(CCTResultsFilter.class);
        
        synchronized (containerGuard) {
            container = null;
        }
    }

    public void visit(MethodCPUCCTNode node) {
        MethodCPUCCTNode currentParent = parentStack.isEmpty() ? null : (MethodCPUCCTNode) parentStack.peek();
        boolean filteredOut = node.getFilteredStatus() == TimedCPUCCTNode.FILTERED_YES; // filtered out by rootmethod/markermethod rules

        if (!filteredOut) {
            String jvmClassName = methodInfoMapper.getInstrMethodClass(node.getMethodId()).replace('.', '/');
            filteredOut = !filter.passesFilter(jvmClassName);
        }
        if (filteredOut) {
            if ((currentParent != null) && !currentParent.isRoot()) {
                invDiff[currentParent.getMethodId()] += node.getNCalls();

                timePM0[currentParent.getMethodId()] += node.getNetTime0();

                if (collectingTwoTimeStamps) {
                    timePM1[currentParent.getMethodId()] += node.getNetTime1();
                }
            }
        } else {
            timePM0[node.getMethodId()] += node.getNetTime0();

            if (collectingTwoTimeStamps) {
                timePM1[node.getMethodId()] += node.getNetTime1();
            }

            invPM[node.getMethodId()] += node.getNCalls();

            if ((currentParent != null) && !currentParent.isRoot()) {
                nCalleeInvocations[currentParent.getMethodId()] += node.getNCalls();
            }

            currentParent = node;
        }

        parentStack.push(currentParent);
    }

    public void visitPost(MethodCPUCCTNode node) {
        parentStack.pop();
    }

    public void visitPost(ThreadCPUCCTNode node) {
        parentStack.clear();
    }
}