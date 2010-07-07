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

var scriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/javase/resources/JavaIOTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;


var Format_KBPS = {
    formatValue: function (value, format) {
        return (value / 1024).toFixed(2);
    },
    getUnits: function (format) {
        return "kB/s"
    }
}

function getGCRunProvider(on) {
    return function(timestamp) {
        if (this.delta == undefined) {
            this.delta = delta(mbeanAttribute(on, "CollectionCount"))
        }
        if (this.lastTs == undefined) {
            this.lastTs = timestamp;
        }
        var timeDelta = timestamp - this.lastTs;
        this.lastTs = timestamp;
        if (timeDelta > 0) {
            return (this.delta.getValue(timestamp) * 1000) / timeDelta;
        }
        return 0;
    }
}

function getGCRuns() {
    var metrics = new Array();
    var gcMbNames = VisualVM.MBeans.listMBeanNames("java.lang:type=GarbageCollector,name=*");
    if (gcMbNames != undefined) {
        for(var i in gcMbNames) {
            var name = gcMbNames[i];
            name.match(/name=(.*)/);
            var dispName = RegExp.$1;
            metrics[metrics.length] = {
                name: dispName,
                desc: "GC runs of " + dispName + " GC",
                value: getGCRunProvider(name),
                presenter: {
                    type: VisualVM.Tracer.Type.discrete,
                    format: {
                        getUnits: function(format) {
                            return "runs/s";
                        }
                    }
                }
            }
        }
    }
    return metrics;
}

function getReclaimedMemoryProvider(on) {
    var keys = mbeanAttribute(on, "LastGcInfo").get("memoryUsageBeforeGc").getKeys();
    // externalize the indexed values
    var before = new Array();
    var after = new Array();
    for(var pool in keys) {
        before[pool] = mbeanAttribute(on, "LastGcInfo").get("memoryUsageBeforeGc").get(keys[pool]).get("used");
        after[pool] = mbeanAttribute(on, "LastGcInfo").get("memoryUsageAfterGc").get(keys[pool]).get("used");
    }

    return function(timestamp) {
        var delta = 0;
        for(var pool in keys) {
            var pre = before[pool].getValue(timestamp);
            var post = after[pool].getValue(timestamp);
            delta += (pre - post);
        }
        return delta;
    }
}

function getReclaimedMemory() {
    var props = new Array();
    var gcMbNames = VisualVM.MBeans.listMBeanNames("java.lang:type=GarbageCollector,name=*");
    if (gcMbNames != undefined) {
        for(var i in gcMbNames) {
            var mbName = gcMbNames[i];
            mbName.match(/name=(.*)/);
            var dispName = RegExp.$1;

            props[props.length] = {
                name: dispName,
                desc: "Memory reclaimed during the last run of " + dispName + " GC",
                value: getReclaimedMemoryProvider(mbName),
                presenter: {
                    format: ItemValueFormatter.DEFAULT_BYTES,
                    type: VisualVM.Tracer.Type.discrete,
                    fillColor: AUTOCOLOR
                }
            }
        }
    }
    return props;
}

function getNIOBufferProperties(attrName, attrPresenter) {
    var props = new Array();
    var bufferNames = VisualVM.MBeans.listMBeanNames("java.nio:type=BufferPool,name=*");
    if (bufferNames != undefined) {
        for(var i in bufferNames) {
            var mbName = bufferNames[i];
            mbName.match(/name=(.*)/);
            var dispName = RegExp.$1;

            props[props.length] = {
                name: dispName,
                value: mbeanAttribute(mbName, attrName),
                presenter: attrPresenter
            }
        }
    }
    return props;
}

function isNIOBuffersSupported() {
    var list = VisualVM.MBeans.listMBeanNames("java.nio:type=BufferPool,name=*");
    return list != undefined && list.length > 0;
}

VisualVM.Tracer.addPackages([{
    name: "HotSpot",
    desc: "Displays JVM HotSpot Metrics",
    icon: "com/sun/tools/visualvm/modules/tracer/javase/resources/hotspot.gif",
    position: 500,
    probes: [
        {
            name: "CPU Usage",
            desc: "CPU usage reported by HotSpot components",
            properties: [
                {
                    name: "JIT Compiler",
                    value: function(timestamp) {
                        if (this.valDelta == undefined) {
                            this.valDelta = delta(mbeanAttribute("java.lang:type=Compilation", "TotalCompilationTime"));
                        }
                        if (this.timeDelta == undefined) {
                            this.timeDelta = delta(new ValueProvider({
                                getValue: function(timestamp) {
                                    return timestamp;
                                }
                            }));
                        }

                        var valDiff = this.valDelta.getValue(timestamp);
                        var timeDiff = this.timeDelta.getValue(timestamp);

                        return timeDiff > 0 ? (valDiff * 1000 / timeDiff) : 0;
                    },
                    presenter: {
                        format: ItemValueFormatter.DEFAULT_PERCENT,
                        min: 0,
                        max: 1000 // 1000 = 100.0%
                    }
                }
            ]
        },
    ]
    },
    {
        name: "Garbage Collectors",
        desc: "GC Statistics",
        position: 510,
        probes: [
            {
                name: "GC Runs",
                desc: "Shows the average number of runs per second for a particular GC during the last sample period",
                properties: getGCRuns()
            },
            {
                name: "Reclaimed Memory",
                desc: "The amout of memory reclaimed during the last GC run",
                properties: getReclaimedMemory()
            }
        ]
    },
    {
        name: "NIO Buffers",
        desc: "NIO Buffers overview",
        position: 520,
        probes: [
            {
                name: "Count",
                validator: isNIOBuffersSupported,
                properties: getNIOBufferProperties("Count")
            },
            {
                name: "Memory Used",
                validator: isNIOBuffersSupported,
                properties: getNIOBufferProperties("MemoryUsed", {
                    format: ItemValueFormatter.DEFAULT_BYTES
                })
            },
            {
                name: "Total Capacity",
                validator: isNIOBuffersSupported,
                properties: getNIOBufferProperties("TotalCapacity", {
                    format: ItemValueFormatter.DEFAULT_BYTES
                })
            }
        ]

    },
    {
        name: "Java I/O",
        desc: "IO subsystem stats",
        position: 530,
        probes: [
            {
                name: "File Descriptors",
                validator: function() {
                    return mbeanAttribute("java.lang:type=OperatingSystem", "OpenFileDescriptorCount").getInfo() != null;
                },
                properties: [
                    {
                        name: "Available",
                        value: mbeanAttribute("java.lang:type=OperatingSystem", "MaxFileDescriptorCount"),
                        presenter: {
                            lineColor: Color.RED
                        }
                    },
                    {
                        name: "Used",
                        value: mbeanAttribute("java.lang:type=OperatingSystem", "OpenFileDescriptorCount"),
                        presenter: {
                            fillColor: new Color(((80 & 0xFF) << 16) |
                                                 ((180 & 0xFF) << 8)  |
                                                 ((250 & 0xFF) << 0))
                        }
                    },
                ]
            },
            {
                name: "Java Files",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "files"
                },
                properties: [
                    {
                        name: "Reading Rate",
                        value: mbeanAttribute("btrace:name=JavaIOStats", "fileReadRate"),
                        presenter: {
                            lineColor: Color.GREEN,
                            format: Format_KBPS
                        }
                    },
                    {
                        name: "Writing Rate",
                        value: mbeanAttribute("btrace:name=JavaIOStats", "fileWriteRate"),
                        presenter: {
                            lineColor: Color.RED,
                            format: Format_KBPS
                        }
                    }
                ]
            },
            {
                name: "NIO Utilization",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "nio"
                },
                properties: [
                    {
                        name: "Reading Rate",
                        value: mbeanAttribute("btrace:name=JavaIOStats", "nioReadRate"),
                        presenter: {
                            lineColor: Color.GREEN,
                            format: Format_KBPS
                        }
                    },
                    {
                        name: "Writing Rate",
                        value: mbeanAttribute("btrace:name=JavaIOStats", "nioWriteRate"),
                        presenter: {
                            lineColor: Color.RED,
                            format: Format_KBPS
                        }
                    }
                ]
            }
        ]
    },
    {
        name: "AWT",
        position: 540,
        probes: [
            {
                name: "EDT Utilization",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: "nbres:/com/sun/tools/visualvm/modules/tracer/javase/resources/AWTTracer.btrace",
                    fragment: "utilization"
                },
                properties: [
                    {
                        name: "Dispatch",
                        desc: "Displays the approximate percentage of procesing time spent in dispatching event requests",
                        value: mbeanAttribute("btrace:name=AWTStats", "dispatch"),
                        presenter: {
                            type: VisualVM.Tracer.Type.discrete,
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            fillColor: AUTOCOLOR,
                            max: 1000
                        }
                    },
                    {
                        name: "Paint",
                        desc: "Displays the approximate percentage of procesing time spent in painting AWT components",
                        value: mbeanAttribute("btrace:name=AWTStats", "paint"),
                        presenter: {
                            type: VisualVM.Tracer.Type.discrete,
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            fillColor: AUTOCOLOR,
                            max: 1000
                        }
                    },
                    {
                        name: "Layout",
                        value: mbeanAttribute("btrace:name=AWTStats", "layout"),
                        desc: "Displays the approximate percentage of procesing time spent in laying out AWT components",
                        presenter: {
                            type: VisualVM.Tracer.Type.discrete,
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            fillColor: AUTOCOLOR,
                            max: 1000
                        }
                    }
                ]
            }
        ]
    }
])
