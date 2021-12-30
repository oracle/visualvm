/*
 *  Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

var scriptPath = "nbres:/org/graalvm/visualvm/modules/tracer/collections/resources/CollectionsTracer.btrace"
var btraceDeployerClass;
try {
  btraceDeployerClass = Java.type("org.openjdk.btrace.visualvm.tracer.deployer.BTraceDeployer");
} catch (e) {
  btraceDeployerClass = null;
}
var btraceDeployer = btraceDeployerClass ? btraceDeployerClass.instance() : undefined;


function SelfTimePercentAcc(mbean, blockName) {
    var valAcc = mbean.get("data").get(blockName).get("selfTime.percent");
    this.value = function (ts) {
        var val = valAcc.getValue(ts);
        return  val * 10;
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
            {
                name: "General Search Count",
                desc: "Invocations count of Collections.binarySearch(), Arrays.binarySearch().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "general_search"
                },
                properties: [
                    {
                        name: "Collections.binarySearch",
                        desc: "Invocations count of Collections.binarySearch().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "generalSearchProfiler"), "Collections.binarySearch")

                    },
                    {
                        name: "Arrays.binarySearch",
                        desc: "Invocations count of Arrays.binarySearch().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "generalSearchProfiler"), "Arrays.binarySearch")
                    }
                ]
            },
            {
                name: "General Search Time",
                desc: "Relative time spent in Collections.binarySearch(), Arrays.binarySearch().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "general_search"
                },
                properties: [
                    {
                        name: "Collections.binarySearch",
                        desc: "Relative time spent in Collections.binarySearch().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "generalSearchProfiler"), "Collections.binarySearch"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Arrays.binarySearch",
                        desc: "Relative time spent in Arrays.binarySearch().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "generalSearchProfiler"), "Arrays.binarySearch"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            {
                name: "Collections Search Count",
                desc: "Invocations count of Set.contains(), Set.containsAll(), List.contains(), List.containsAll(), List.indexOf(), List.lastIndexOf(), Queue.contains(), Queue.containsAll().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "collections_search"
                },
                properties: [
                    {
                        name: "Set.contains",
                        desc: "Invocations count of Set.contains().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Set.contains")

                    },
                    {
                        name: "Set.containsAll",
                        desc: "Invocations count of Set.containsAll().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Set.containsAll")

                    },
                    {
                        name: "List.contains",
                        desc: "Invocations count of List.contains().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.contains")
                    },
                    {
                        name: "List.containsAll",
                        desc: "Invocations count of List.containsAll().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.containsAll")

                    },
                    {
                        name: "List.indexOf",
                        desc: "Invocations count of List.indexOf().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.indexOf")

                    },
                    {
                        name: "List.lastIndexOf",
                        desc: "Invocations count of List.lastIndexOf().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.lastIndexOf")

                    },
                    {
                        name: "Queue.contains",
                        desc: "Invocations count of Queue.contains().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Queue.contains")
                    },
                    {
                        name: "Queue.containsAll",
                        desc: "Invocations count of Queue.containsAll().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Queue.containsAll")
                    }
                ]
            },
            {
                name: "Collections Search Time",
                desc: "Relative time spent in Set.contains(), Set.containsAll(), List.contains(), List.containsAll(), List.indexOf(), List.lastIndexOf(), Queue.contains(), Queue.containsAll().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "collections_search"
                },
                properties: [
                    {
                        name: "Set.contains",
                        desc: "Relative time spent in Set.contains().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Set.contains"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Set.containsAll",
                        desc: "Relative time spent in Set.containsAll().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Set.containsAll"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "List.contains",
                        desc: "Relative time spent in List.contains().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.contains"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "List.containsAll",
                        desc: "Relative time spent in List.containsAll().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.containsAll"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "List.indexOf",
                        desc: "Relative time spent in List.indexOf().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.indexOf"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "List.lastIndexOf",
                        desc: "Relative time spent in List.lastIndexOf().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "List.lastIndexOf"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Queue.contains",
                        desc: "Relative time spent in Queue.contains().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Queue.contains"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Queue.containsAll",
                        desc: "Relative time spent in Queue.containsAll().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "collectionsSearchProfiler"), "Queue.containsAll"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            {
                name: "Map Search Count",
                desc: "Invocations count of Map.get(), Map.containsKey(), Map.containsValue().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "map_search"
                },
                properties: [
                    {
                        name: "Map.get",
                        desc: "Invocations count of Map.get().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.get")

                    },
                    {
                        name: "Map.containsKey",
                        desc: "Invocations count of Map.containsKey().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.containsKey")

                    },
                    {
                        name: "Map.containsValue",
                        desc: "Invocations count of Map.containsValue().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.containsValue")
                    }
                ]
            },
            {
                name: "Map Search Time",
                desc: "Relative time spent in Map.get(), Map.containsKey(), Map.containsValue().",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "map_search"
                },
                properties: [
                    {
                        name: "Map.get",
                        desc: "Relative time spent in Map.get().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.get"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Map.containsKey",
                        desc: "Relative time spent in Map.containsKey().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.containsKey"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Map.containsValue",
                        desc: "Relative time spent in Map.containsValue().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapSearchProfiler"), "Map.containsValue"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            {
                name: "Map Resize Count",
                desc: "Invocations count of resize() method in Map implementations.",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "map_resize"
                },
                properties: [
                    {
                        name: "HashMap",
                        desc: "Invocations count of resize().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.HashMap")

                    },
                    {
                        name: "WeakHashMap",
                        desc: "Invocations count of resize().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.WeakHashMap")

                    },
                    {
                        name: "IdentityHashMap",
                        desc: "Invocations count of resize().",
                        value: invocations(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.IdentityHashMap")

                    }
                ]
            },
            {
                name: "Map Resize Time",
                desc: "Relative time spent in resize() method in Map implementations.",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "map_resize"
                },
                properties: [
                    {
                        name: "HashMap",
                        desc: "Relative time spent in resize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.HashMap"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "WeakHashMap",
                        desc: "Relative time spent in resize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.WeakHashMap"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "IdentityHashMap",
                        desc: "Relative time spent in resize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=CollectionsStats", "mapResizeProfiler"), "java.util.IdentityHashMap"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            }
        ]
    }
])
