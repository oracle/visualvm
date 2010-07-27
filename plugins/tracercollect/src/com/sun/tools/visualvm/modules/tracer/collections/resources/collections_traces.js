/*
 *  Copyright 2007-2010 Sun Microsystems, Inc.  All Rights Reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Sun designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Sun in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 *  CA 95054 USA or visit www.sun.com if you need additional information or
 *  have any questions.
 */

var scriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/collections/resources/CollectionsTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

function SelfTimePercentAcc(mbean, blockName) {
    this.value = function (ts) {
        if (this.lastTs == undefined) {
            this.lastTs = mbean.get("startTime").getValue(ts);
        }

        var curTs = mbean.get("lastRefresh").getValue(ts);
        if (curTs > this.lastTs) {
            this.duration = curTs - this.lastTs;
        }
        if (this.duration == undefined || this.duration == 0) return 0; // shortcut

        var val = mbean.get("data").get(blockName).get("selfTime").getValue(ts);
        this.lastTs = curTs;
        return  val / (this.duration * 1000);
    }
}

function selfTimePercent(mbean, blockName) {
    return new SelfTimePercentAcc(mbean, blockName).value;
}

function invocations(mbean, blockName) {
    return mbean.get("data").get(blockName).get("invocations");
}

VisualVM.Tracer.addPackages([{
        name: "Java Collections",
        desc: "Java Collections framework statistics",
        position: 522,
        probes: [
            {
                name: "Sorting Count",
                desc: "Invocations count of Collections.sort(), Arrays.sort().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "sorting"
                },
                properties: [
                    {
                        name: "Collections.sort",
                        desc: "Invocations count of Collections.sort().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "sortingProfiler"), "Collections.sort")

                    },
                    {
                        name: "Arrays.sort",
                        desc: "Invocations count of Arrays.sort().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "sortingProfiler"), "Arrays.sort")
                    }
                ]
            },
            {
                name: "Sorting Time",
                desc: "Relative time spent in Collections.sort(), Arrays.sort().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "sorting"
                },
                properties: [
                    {
                        name: "Collections.sort",
                        desc: "Relative time spent in Collections.sort().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "sortingProfiler"), "Collections.sort"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Arrays.sort",
                        desc: "Relative time spent in Arrays.sort().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "sortingProfiler"), "Arrays.sort"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            {
                name: "Conversions Count",
                desc: "Invocations count of Set.toArray(), List.toArray(), Queue.toArray(), Arrays.asList().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "conversions"
                },
                properties: [
                    {
                        name: "Set.toArray",
                        desc: "Invocations count of Set.toArray.",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Set.toArray")

                    },
                    {
                        name: "List.toArray",
                        desc: "Invocations count of List.toArray().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "List.toArray")
                    },
                    {
                        name: "Queue.toArray",
                        desc: "Invocations count of Queue.toArray().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Queue.toArray")
                    },
                    {
                        name: "Arrays.asList",
                        desc: "Invocations count of Arrays.asList().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Arrays.asList")
                    }
                ]
            },
            {
                name: "Conversions Time",
                desc: "Relative time spent in Set.toArray(), List.toArray(), Queue.toArray(), Arrays.asList().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "conversions"
                },
                properties: [
                    {
                        name: "Set.toArray",
                        desc: "Relative time spent in Set.toArray.",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Set.toArray"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "List.toArray",
                        desc: "Relative time spent in List.toArray().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "List.toArray"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Queue.toArray",
                        desc: "Relative time spent in Queue.toArray().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Queue.toArray"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Arrays.asList",
                        desc: "Relative time spent in Arrays.asList().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "conversionsProfiler"), "Arrays.asList"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
//            General Search (Collections.binarySearch, Arrays.binarySearch),
//            Collection Search (Set.contains, Set.containsAll, List.contains, List.containsAll, Queue.contains, Queue.containsAll),
//            Map Search (Map.containsKey, Map.containsValue, Map.get)
        ]
    }
])
