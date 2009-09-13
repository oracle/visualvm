package com.sun.tools.visualvm.modules.sampler.cpu;

import org.netbeans.lib.profiler.results.cpu.FlatProfileContainer;
import org.netbeans.lib.profiler.results.cpu.FlatProfileProvider;
import org.netbeans.lib.profiler.results.cpu.cct.CompositeCPUCCTWalker;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.RuntimeCPUCCTNode;

/**
 *
 * @author Tomas Hurka
 */
class FlatProfileBuilder implements FlatProfileProvider {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------
    
    private FlatProfileContainer lastFlatProfile = null;
    private RuntimeCPUCCTNode appNode;
    private CCTFlattener cctFlattener;
    
    
    //~ Methods ------------------------------------------------------------------------------------------------------------------
    
    FlatProfileBuilder(RuntimeCPUCCTNode node, CCTFlattener flattener) {
        appNode = node;
        cctFlattener = flattener;
    }
    
    public synchronized FlatProfileContainer createFlatProfile() {
        if (appNode == null) {
            return null;
        }
        
        CompositeCPUCCTWalker walker = new CompositeCPUCCTWalker();
        int index = 0;
          
        walker.add(index++, cctFlattener);
        walker.walk(appNode);
        lastFlatProfile = cctFlattener.getFlatProfile();
        return lastFlatProfile;
    }
}
