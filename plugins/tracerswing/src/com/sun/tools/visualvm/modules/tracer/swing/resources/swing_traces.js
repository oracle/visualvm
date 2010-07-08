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

var scriptPath = "nbres:/com/sun/tools/visualvm/modules/tracer/swing/resources/AWTTracer.btrace"
var btraceDeployer = typeof(Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.net.java.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

VisualVM.Tracer.addPackages([{
        name: "Swing & AWT",
        desc: "Swing & AWT subsystem statistics",
        position: 540,
        probes: [
            {
                name: "EDT Utilization",
                desc: "Measures utilization of the Event Dispatch Thread.",
                reqs: "Requires Tracer-BTrace Support plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
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
