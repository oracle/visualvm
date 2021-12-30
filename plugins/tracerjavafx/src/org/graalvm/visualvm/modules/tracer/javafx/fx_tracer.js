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

var loc = new L11N("org.graalvm.visualvm.modules.tracer.javafx")

var scriptPath = "nbres:/org/graalvm/visualvm/modules/tracer/javafx/resources/JavaFXTracer.probe"
var btraceDeployerClass;
try {
  btraceDeployerClass = Java.type("org.openjdk.btrace.visualvm.tracer.deployer.BTraceDeployer");
} catch (e) {
  btraceDeployerClass = null;
}
var btraceDeployer = btraceDeployerClass ? btraceDeployerClass.instance() : undefined;
var JvmFactory = Java.type("org.graalvm.visualvm.application.jvm.JvmFactory");

VisualVM.Tracer.addPackages({
    // JavaFX Metrics package
    name: loc.message("VisualVM/Tracer/packages/jfx"),
    desc: "Monitors runtime behavior of JavaFX applications",
    icon: "org/graalvm/visualvm/modules/tracer/javafx/resources/fx.png",
    position: 800,
    reqs: "Available only for JavaFX applications",
    validator: function() {
        var jvm = JvmFactory.getJVMFor(application);
        return jvm != undefined && jvm.getMainClass() == "com.sun.javafx.runtime.Main";
    },
    probes: [
        {
            // FX Metrics
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/metrics"),
            desc: "Monitors Invalidation Rate and Replacement Rate",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "metrics",
                script: scriptPath
            },
            properties: [
                {
                    // invalidation rate
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/metrics/properties/invalidationRate"),
                    desc: "Monitors number of invalidations per second",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "invalidationRate")
                },
                {
                    // replacement rate
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/metrics/properties/replacementRate"),
                    desc: "Monitors number of replacements per second",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "replacementRate")
                }
            ]
        },
        {
            // FX Objects
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/objects"),
            desc: "Monitors Overall Rate and Hot Class Rate",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "objects",
                script: scriptPath
            },
            properties: [
                {
                    // Overall Rate
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/objects/properties/fxObjectCreationRate"),
                    desc: "Monitors the number of created objects per second",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "fxObjectCreationRate")
                }
            ]
        },
        {
            // Average FPS
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/fps"),
            desc: "Monitors average frame per second rate",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "fps",
                script: scriptPath
            },
            properties: [
                {
                    // Average FPS
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/fps/properties/averageFPS"),
                    desc: "Monitors average frame per second rate",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "averageFPS")
                }
            ]
        },
        {
            // Scenegraph mouse and key statistics
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseCount"),
            desc: "Monitors mouse and keyboard activity",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "pulseCount",
                script: scriptPath
            },
            properties: [
                {
                    // Key events
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseCount/properties/keyPulses"),
                    desc: "Monitors keyboard activity",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "keyPulses")
                },
                {
                    // Mouse events
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseCount/properties/mousePulses"),
                    desc: "Monitors mouse activity",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "mousePulses")
                }
            ]
        },
        {
            // Scenegraph mouse and key event timing
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseTiming"),
            desc: "Monitors mouse and keyboard event timing",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "pulseTiming",
                script: scriptPath
            },
            properties: [
                {
                    // Key events time
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseTiming/properties/keyPulsesCumulativeTime"),
                    desc: "Monitors keyboard processing time",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "keyPulsesCumulativeTime")
                },
                {
                    // Mouse events time
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/pulseTiming/properties/mousePulsesCumulativeTime"),
                    desc: "Monitors mouse processing time",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "mousePulsesCumulativeTime")
                }
            ]
        },
        {
            // Scenegraph timing monitoring
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgTiming"),
            desc: "Monitors scenegraph timing",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "sgTiming",
                script: scriptPath
            },
            properties: [
                {
                    // Dirty regions
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgTiming/properties/dirtyRegionsCumulativeTime"),
                    desc: "Dirty regions processing time",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "dirtyRegionsCumulativeTime")
                },
                {
                    // Paint time
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgTiming/properties/paintCumulativeTime"),
                    desc: "Paint processing time",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "paintCumulativeTime")
                },
                {
                    // Synchronization time
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgTiming/properties/synchronizationTime"),
                    desc: "Synchronization time",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "synchronizationTime")
                }
            ]
        },
        {
            // Scenegraph nodes statistics
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgNode"),
            desc: "Scenegraph nodes statistic",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "sgNode",
                script: scriptPath
            },
            properties: [
                {
                    // Layout required
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgNode/properties/needsLayout"),
                    desc: "How many nodes layout required",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "needsLayout")
                },
                {
                    // Node count
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgNode/properties/nodeCount"),
                    desc: "Amount of nodes in sceengraph",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "nodeCount")
                }
            ]
        },
        {
            // Scenegraph CSS statistics
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgCss"),
            desc: "Scenegraph CSS statistic",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "sgCss",
                script: scriptPath
            },
            properties: [
                {
                    // Style helpers calls
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgCss/properties/getStyleHelperCalls"),
                    desc: "Style helpers calls",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "getStyleHelperCalls")
                },
                {
                    // Style helpers count
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgCss/properties/styleHelperCount"),
                    desc: "Style helpers count",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "styleHelperCount")
                },
                {
                    // Style helpers count
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgCss/properties/transitionToStateCalls"),
                    desc: "Transitions",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "transitionToStateCalls")
                },
                {
                    // Style helpers count
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/sgCss/properties/processCssCount"),
                    desc: "CSS process calls",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "processCssCount")
                }
            ]
        },
        {
            // Synchronization calls
            name: loc.message("VisualVM/Tracer/packages/jfx/probes/synCalls"),
            desc: "Monitors amount of synchronization calls",
            reqs: "Requires BTrace Deployer plugin.",
            validator: function() {
                return btraceDeployer != undefined;
            },
            deployment: {
                deployer: btraceDeployer,
                fragment: "synCalls",
                script: scriptPath
            },
            properties: [
                {
                    // Synchronization calls
                    name: loc.message("VisualVM/Tracer/packages/jfx/probes/synCalls/properties/synchronizationCalls"),
                    desc: "Monitors amount of synchronization calls",
                    value: mbeanAttribute("btrace:name=FxBtraceTracker", "synchronizationCalls")
                }
            ]
        },
    ]
})
