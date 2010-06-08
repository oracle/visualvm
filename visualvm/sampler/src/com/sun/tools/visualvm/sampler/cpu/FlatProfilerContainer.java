package com.sun.tools.visualvm.sampler.cpu;

import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.MethodInfoMapper;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatter;
import org.netbeans.lib.profiler.utils.formatting.MethodNameFormatterFactory;


/**
 *
 * @author Tomas Hurka
 */
class FlatProfilerContainer extends FlatProfileContainer {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    protected double wholeGraphNetTime0;
    protected double wholeGraphNetTime1;
    private MethodInfoMapper methodInfoMapper;

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    /**
     * The data passed to this constructor may contain some zero-invocation rows. That's because the size of passed arrays
     * is equal to the number of currently instrumented methods, but in general not all of the methods may be invoked even
     * once at an arbitrary moment.
     *
     * @param timeInMcs0         Array of Absolute timer values for each method - always used
     * @param timeInMcs1         Array of CPU timer values for each method - optional, may be null
     * @param nInvocations       Array of number of invocations for each method
     * @param wholeGraphNetTime0 Total absolute time
     * @param wholeGraphNetTime1 Total CPU time - not used if CPU timer is not used
     * @param nMethods           Total number of profiled methods - length of the provided arrays
     */
    FlatProfilerContainer(MethodInfoMapper mapper,boolean twoStamps,long[] timeInMcs0, long[] timeInMcs1, int[] nInvocations,
                                    char[] marks, double wholeGraphNetTime0, double wholeGraphNetTime1, int nMethods) {
        super(timeInMcs0, timeInMcs1, nInvocations, marks, nMethods);
        this.wholeGraphNetTime0 = wholeGraphNetTime0;
        this.wholeGraphNetTime1 = wholeGraphNetTime1;

        collectingTwoTimeStamps = twoStamps;
        methodInfoMapper = mapper;

        // Now get rid of zero-invocation entries once and forever. Also set nTotalInvocations and set negative times
        // (that may be possible due to time cleansing inaccuracies) to zero.
        removeZeroInvocationEntries();
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public String getMethodNameAtRow(int row) {
        int methodId = methodIds[row];
        MethodNameFormatter formatter = MethodNameFormatterFactory.getDefault().getFormatter(null);

        String className = methodInfoMapper.getInstrMethodClass(methodId);
        String methodName = methodInfoMapper.getInstrMethodName(methodId);
        String signature = methodInfoMapper.getInstrMethodSignature(methodId);

        return formatter.formatMethodName(className, methodName, signature).toFormatted();
    }

    public double getWholeGraphNetTime0() {
        return wholeGraphNetTime0;
    }

    public double getWholeGraphNetTime1() {
        return wholeGraphNetTime1;
    }
}
