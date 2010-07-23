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

var awtScriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/swing/resources/AWTTracer.btrace"
var swingScriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/swing/resources/SwingTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

function selfTimePercent(mbean, blockName) {
    var lastTs = undefined
    var duration = undefined;
    return function(ts) {
        if (lastTs == undefined) {
            lastTs = mbean.get("startTime").getValue(ts);
        }

        var curTs = mbean.get("lastRefresh").getValue(ts);
        if (curTs > lastTs) {
            duration = curTs - lastTs;
        }
        if (duration == undefined || duration == 0) return 0; // shortcut

        var val = mbean.get("data").get(blockName).get("selfTime").getValue(ts);
        lastTs = curTs;
        return  val / (duration * 1000);
    }
}

function invocations(mbean, blockName) {
    return mbean.get("data").get(blockName).get("invocations");
}

VisualVM.Tracer.addPackages([{
        name: "Swing & AWT",
        desc: "Swing & AWT subsystem statistics",
        position: 540,
        probes: [
            {
                name: "EDT Events Count",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: awtScriptPath,
                    fragment: "edt_counts"
                },
                properties: [
                    {
                        name: "dispatchEvent()",
                        value: mbeanAttribute("btrace:name=AWTStats", "dispatchEventCount")
                    },
                    {
                        name: "invokeAndWait()",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeWaitCount")
                    },
                    {
                        name: "invokeLater()",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeLaterCount")
                    }
                ]
            },
            {
                name: "EDT Events Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: awtScriptPath,
                    fragment: "edt_times"
                },
                properties: [
                    {
                        name: "dispatchEvent()",
                        value: mbeanAttribute("btrace:name=AWTStats", "dispatchEventTime"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "invokeAndWait()",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeWaitTime"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            awt_paints_count = {
                name: "AWT Paints Count",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: awtScriptPath,
                    fragment: "awt_paint"
                },
                properties: [
                    {
                        name: "Paint",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "paint")

                    },
                    {
                        name: "Update",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "update")
                    },
                    {
                        name: "Repaint",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "repaint")
                    }
                ]
            },
            awt_paints_time = {
                name: "AWT Paints Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: awtScriptPath,
                    fragment: "awt_paint"
                },
                properties: [
                    {
                        name: "Paint",
                        value: selfTimePercent(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "paint"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Update",
                        value: selfTimePercent(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "update"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Repaint",
                        value: selfTimePercent(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "repaint"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            jc_paints_count = {
                name: "Swing Paints Count",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "swing_paint"
                },
                properties: [
                    {
                        name: "Component",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintComponent")
                    },
                    {
                        name: "Border",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintBorder")
                    },
                    {
                        name: "Children",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintChildren")
                    }
                ]
            },
            jc_paints_time = {
                name: "Swing Paints Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "swing_paint"
                },
                properties: [
                    {
                        name: "Component",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintComponent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Border",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintBorder"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Children",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintChildren"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            cu_paints_time = {
                name: "ComponentUI Paints Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "componentui"
                },
                properties: [
                    {
                        name: "Paint",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "paint"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "Update",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "update"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            cu_layouts_time = {
                name: "ComponentUI Layouts Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "componentui"
                },
                properties: [
                    {
                        name: "GetPreferredSize",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getPreferredSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "GetMaximumSize",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getMaximumSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "GetMinimumSize",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getMinimumSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            jc_events_time = {
                name: "Component Events Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "component_events"
                },
                properties: [
                    {
                        name: "Component Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processComponentEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Focus Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processFocusEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Key Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processKeyEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Mouse Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "MouseMotion Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseMotionEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "MouseWheel Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseWheelEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "InputMethod Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processInputMethodEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Hierarchy Event",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processHierarchyEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            },
            jc_events_count = {
                name: "Component Events Count",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "component_events"
                },
                properties: [
                    {
                        name: "Component Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processComponentEvent")
                    },
                    {
                        name: "Focus Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processFocusEvent")
                    },
                    {
                        name: "Key Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processKeyEvent")
                    },
                    {
                        name: "Mouse Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseEvent")
                    },
                    {
                        name: "MouseMotion Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"),"processMouseMotionEvent")
                    },
                    {
                        name: "MouseWheel Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseWheelEvent")
                    },
                    {
                        name: "InputMethod Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processInputMethodEvent")
                    },
                    {
                        name: "Hierarchy Event",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processHierarchyEvent")
                    }
                ]
            },
            layout_time = {
                name: "Layouts Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "layout"
                },
                properties: [
                    {
                        name: "Preferred Layout Size",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "preferredLayoutSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Minimum Layout Size",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "minimumLayoutSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "Layout Container",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "layoutContainer"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            },
            layout_counts = {
                name: "Layouts Counts",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "layout"
                },
                properties: [
                    {
                        name: "Preferred Layout Size",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "preferredLayoutSize")
                    },
                    {
                        name: "Minimum Layout Size",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "minimumLayoutSize")
                    },
                    {
                        name: "Layout Container",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "layoutContainer")
                    }
                ]
            },
            events_time = {
                name: "Basic Events Time",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "basic_events"
                },
                properties: [
                    {
                        name: "ActionListener.actionPerformed",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "actionPerformed"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "ChangeListener.stateChanged",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "stateChanged"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "PropertyChangeListener.propertyChanged",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "propertyChanged"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            },
            events_counts = {
                name: "Basic Events Counts",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: swingScriptPath,
                    fragment: "basic_events"
                },
                properties: [
                    {
                        name: "ActionListener.actionPerformed",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "actionPerformed")
                    },
                    {
                        name: "ChangeListener.stateChanged",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "stateChanged")
                    },
                    {
                        name: "PropertyChangeListener.propertyChanged",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "propertyChanged")
                    }
                ]
            }
        ]
    }
])
