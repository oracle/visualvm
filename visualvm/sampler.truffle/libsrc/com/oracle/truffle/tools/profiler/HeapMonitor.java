/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.tools.profiler;

import com.oracle.truffle.api.nodes.LanguageInfo;
import java.util.Map;
import org.graalvm.polyglot.Engine;

/**
 *
 * @author thurka
 */
public class HeapMonitor {

    public static HeapMonitor find(Engine engine) {
        return null;
    }

    public boolean isCollecting() {
        return false;
    }

    public void setCollecting(boolean b) {
    }

    public boolean hasData() {
        return false;
    }

    public Map<LanguageInfo, Map<String, HeapSummary>> takeMetaObjectSummary() {
        return null;
    }
    
}
