/*
 *  Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

var awtScriptPath = "nbres:/org/graalvm/visualvm/modules/tracer/swing/resources/AWTTracer.btrace"
var swingScriptPath = "nbres:/org/graalvm/visualvm/modules/tracer/swing/resources/SwingTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

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
        name: "Swing & AWT",
        desc: "Swing & AWT subsystem statistics",
        icon: "org/graalvm/visualvm/modules/tracer/swing/resources/icon.png",
        position: 540,
        probes: [
            {
                name: "EventQueue Calls Count",
                desc: "Invocations count of EventQueue.dispatchEvent(), EventQueue.invokeAndWait(), EventQueue.invokeLater().",
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
                        name: "dispatchEvent",
                        desc: "Invocations count of EventQueue.dispatchEvent().",
                        value: mbeanAttribute("btrace:name=AWTStats", "dispatchEventCount")
                    },
                    {
                        name: "invokeAndWait",
                        desc: "Invocations count EventQueue.invokeAndWait().",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeWaitCount")
                    },
                    {
                        name: "invokeLater",
                        desc: "Invocations count of EventQueue.invokeLater().",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeLaterCount")
                    }
                ]
            },
            {
                name: "EventQueue Calls Time",
                desc: "Relative time spent in EventQueue.dispatchEvent(), EventQueue.invokeAndWait().",
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
                        name: "dispatchEvent",
                        desc: "Relative time spent in EventQueue.dispatchEvent().",
                        value: mbeanAttribute("btrace:name=AWTStats", "dispatchEventTime"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "invokeAndWait",
                        desc: "Relative time spent in EventQueue.invokeAndWait().",
                        value: mbeanAttribute("btrace:name=AWTStats", "invokeWaitTime"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            awt_paints_count = {
                name: "Component Paints Count",
                desc: "Invocations count of Component.paint(), Component.update(), Component.repaint().",
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
                        name: "paint",
                        desc: "Invocations count of Component.paint().",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "paint")

                    },
                    {
                        name: "update",
                        desc: "Invocations count of Component.update().",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "update")
                    },
                    {
                        name: "repaint",
                        desc: "Invocations count of Component.repaint().",
                        value: invocations(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "repaint")
                    }
                ]
            },
            awt_paints_time = {
                name: "Component Paints Time",
                desc: "Relative time spent in Component.paint(), Component.update().",
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
                        name: "paint",
                        desc: "Relative time spent in Component.paint().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "paint"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "update",
                        desc: "Relative time spent in Component.update().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=AWTStats", "awtPaintProfiler"), "update"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            jc_paints_count = {
                name: "JComponent Paints Count",
                desc: "Invocations count of JComponent.paintComponent(), JComponent.paintBorder(), JComponent.paintChildren().",
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
                        name: "paintComponent",
                        desc: "Invocations count of JComponent.paintComponent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintComponent")
                    },
                    {
                        name: "paintBorder",
                        desc: "Invocations count of JComponent.paintBorder().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintBorder")
                    },
                    {
                        name: "paintChildren",
                        desc: "Invocations count of JComponent.paintChildren().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintChildren")
                    }
                ]
            },
            jc_paints_time = {
                name: "JComponent Paints Time",
                desc: "Relative time spent in JComponent.paintComponent(), JComponent.paintBorder(), JComponent.paintChildren().",
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
                        name: "paintComponent",
                        desc: "Relative time spent in JComponent.paintComponent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintComponent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "paintBorder",
                        desc: "Relative time spent in JComponent.paintBorder().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcPaintProfiler"), "paintBorder"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "paintChildren",
                        desc: "Relative time spent in JComponent.paintChildren().",
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
                desc: "Relative time spent in ComponentUI.paint(), ComponentUI.update().",
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
                        name: "paint",
                        desc: "Relative time spent in ComponentUI.paint().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "paint"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "update",
                        desc: "Relative time spent in ComponentUI.update().",
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
                desc: "Relative time spent in ComponentUI.getPreferredSize(), ComponentUI.getMinimumSize(), ComponentUI.getMaximumSize().",
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
                        name: "getPreferredSize",
                        desc: "Relative time spent in ComponentUI.getPreferredSize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getPreferredSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "getMinimumSize",
                        desc: "Relative time spent in ComponentUI.getMinimumSize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getMaximumSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    },
                    {
                        name: "getMaximumSize",
                        desc: "Relative time spent in ComponentUI.getMaximumSize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "cuiProfiler"), "getMinimumSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000
                        }
                    }
                ]
            },
            layout_counts = {
                name: "Layouts Count",
                desc: "Invocations count of LayoutManager.preferredLayoutSize(), LayoutManager.minimumLayoutSize(), LayoutManager.layoutContainer().",
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
                        name: "preferredLayoutSize",
                        desc: "Invocations count of LayoutManager.preferredLayoutSize().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "preferredLayoutSize")
                    },
                    {
                        name: "minimumLayoutSize",
                        desc: "Invocations count of LayoutManager.minimumLayoutSize().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "minimumLayoutSize")
                    },
                    {
                        name: "layoutContainer",
                        desc: "Invocations count of LayoutManager.layoutContainer().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "layoutContainer")
                    }
                ]
            },
            layout_time = {
                name: "Layouts Time",
                desc: "Relative time spent in LayoutManager.preferredLayoutSize(), LayoutManager.minimumLayoutSize(), LayoutManager.layoutContainer().",
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
                        name: "preferredLayoutSize",
                        desc: "Relative time spent in LayoutManager.preferredLayoutSize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "preferredLayoutSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "minimumLayoutSize",
                        desc: "Relative time spent in LayoutManager.minimumLayoutSize().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "minimumLayoutSize"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "layoutContainer",
                        desc: "Relative time spent in LayoutManager.layoutContainer().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "layoutProfiler"), "layoutContainer"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            },
            events_counts = {
                name: "Common Events Count",
                desc: "Invocations count of ActionListener.actionPerformed(), ChangeListener.stateChanged(), PropertyChangeListener.propertyChange().",
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
                        name: "actionPerformed",
                        desc: "Invocations count of ActionListener.actionPerformed().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "actionPerformed")
                    },
                    {
                        name: "stateChanged",
                        desc: "Invocations count of ChangeListener.stateChanged().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "stateChanged")
                    },
                    {
                        name: "propertyChange",
                        desc: "Invocations count of PropertyChangeListener.propertyChange().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "propertyChange")
                    }
                ]
            },
            events_time = {
                name: "Common Events Time",
                desc: "Relative time spent in ActionListener.actionPerformed(), ChangeListener.stateChanged(), PropertyChangeListener.propertyChange().",
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
                        name: "actionPerformed",
                        desc: "Relative time spent in ActionListener.actionPerformed().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "actionPerformed"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "stateChanged",
                        desc: "Relative time spent in ChangeListener.stateChanged().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "stateChanged"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "propertyChange",
                        desc: "Relative time spent in PropertyChangeListener.propertyChange().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "eventsProfiler"), "propertyChange"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            },
            jc_events_count = {
                name: "Component Events Count",
                desc: "Invocations count of Component.processComponentEvent(), Component.processFocusEvent(), Component.processKeyEvent(), Component.processMouseEvent(), Component.processMouseMotionEvent(), Component.processMouseWheelEvent(), Component.processInputMethodEvent(), Component.processHierarchyEvent().",
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
                        name: "processComponentEvent",
                        desc: "Invocations count of Component.processComponentEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processComponentEvent")
                    },
                    {
                        name: "processFocusEvent",
                        desc: "Invocations count of Component.processFocusEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processFocusEvent")
                    },
                    {
                        name: "processKeyEvent",
                        desc: "Invocations count of Component.processKeyEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processKeyEvent")
                    },
                    {
                        name: "processMouseEvent",
                        desc: "Invocations count of Component.processMouseEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseEvent")
                    },
                    {
                        name: "processMouseMotionEvent",
                        desc: "Invocations count of Component.processMouseMotionEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"),"processMouseMotionEvent")
                    },
                    {
                        name: "processMouseWheelEvent",
                        desc: "Invocations count of Component.processMouseWheelEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseWheelEvent")
                    },
                    {
                        name: "processInputMethodEvent",
                        desc: "Invocations count of Component.processInputMethodEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processInputMethodEvent")
                    },
                    {
                        name: "processHierarchyEvent",
                        desc: "Invocations count of Component.processHierarchyEvent().",
                        value: invocations(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processHierarchyEvent")
                    }
                ]
            },
            jc_events_time = {
                name: "Component Events Time",
                desc: "Relative time spent in Component.processComponentEvent(), Component.processFocusEvent(), Component.processKeyEvent(), Component.processMouseEvent(), Component.processMouseMotionEvent(), Component.processMouseWheelEvent(), Component.processInputMethodEvent(), Component.processHierarchyEvent().",
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
                        name: "processComponentEvent",
                        desc: "Relative time spent in Component.processComponentEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processComponentEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processFocusEvent",
                        desc: "Relative time spent in Component.processFocusEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processFocusEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processKeyEvent",
                        desc: "Relative time spent in Component.processKeyEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processKeyEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processMouseEvent",
                        desc: "Relative time spent in Component.processMouseEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processMouseMotionEvent",
                        desc: "Relative time spent in Component.processMouseMotionEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseMotionEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processMouseWheelEvent",
                        desc: "Relative time spent in Component.processMouseWheelEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processMouseWheelEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processInputMethodEvent",
                        desc: "Relative time spent in Component.processInputMethodEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processInputMethodEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    },
                    {
                        name: "processHierarchyEvent",
                        desc: "Relative time spent in Component.processHierarchyEvent().",
                        value: selfTimePercent(mbeanAttribute("btrace:name=SwingStats", "jcEventsProfiler"), "processHierarchyEvent"),
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT
                        }
                    }
                ]
            }
        ]
    }
])
