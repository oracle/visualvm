/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.tools.profiler;

import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.Engine;

/**
 *
 * @author thurka
 */
public class CPUSampler {

    public static CPUSampler find(Engine engine) {
        return null;
    }

    public void setDelaySamplingUntilNonInternalLangInit(boolean b) {
    }

    public Map<Thread, List<StackTraceEntry>> takeSample() {
        return null;
    }
    
}
