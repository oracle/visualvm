/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

var scriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/javase/resources/JavaIOTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

//var img = Packages.org.openide.util.ImageUtilities.loadImageIcon("com/sun/tools/visualvm/modules/tracer/platformjmx/resources/",false)

var Format_KBPS = new Packages.com.sun.tools.visualvm.modules.tracer.dynamic.impl.ItemValueFormatterInterface({
    formatValue: function (value, format) {
        return (value / 1024).toFixed(2);
    },
    getUnits: function (format) {
        return "kB/s"
    }
})

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
                    fillColor: ProbeItemDescriptor.DEFAULT_COLOR
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
    icon: Packages.org.openide.util.ImageUtilities.loadImageIcon("com/sun/tools/visualvm/modules/tracer/javase/resources/hotspot.gif", false),
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
                name: "NIO Buffers",
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
    }
])
