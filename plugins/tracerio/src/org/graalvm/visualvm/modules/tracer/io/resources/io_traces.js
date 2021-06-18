/*
 *  Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

var scriptPath = "nbres:/org/graalvm/visualvm/modules/tracer/io/resources/IOTracer.btrace"
var btraceDeployer = typeof(Packages.org.openjdk.btrace.visualvm.tracer.deployer.BTraceDeployer) == "function" ?
                        Packages.org.openjdk.btrace.visualvm.tracer.deployer.BTraceDeployer.instance() : undefined;

function DivideBytesAcc(mbeanBytes, mbeanCount) {
    this.value = function (ts) {
        var bytes = mbeanBytes.getValue(ts);
        var count = mbeanCount.getValue(ts);
        if (count != 0) {
            return bytes/count;
        }
        return  0;
    }
}

function divideBytes(mbeanBytes, mbeanCount) {
    return new DivideBytesAcc(mbeanBytes, mbeanCount).value;
}

VisualVM.Tracer.addPackages([{
        name: "Java I/O",
        desc: "Java I/O statistics",
        position: 553,
        probes: [
            {
                name: "File I/O count",
                desc: "Invocations count of file read/write operations.",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "Java_IO"
                },
                properties: [
                    {
                        name: "File Read Count",
                        desc: "Invocations count of file read operations.",
                        value: mbeanAttribute("btrace:name=IOStats", "lastReadCount")

                    },
                    {
                        name: "File Write Count",
                        desc: "Invocations count of file write operations.",
                        value: mbeanAttribute("btrace:name=IOStats", "lastWriteCount")

                    }
                ]
            },
            {
                name: "File I/O bytes",
                desc: "Number of bytes read or written to files.",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "Java_IO"
                },
                properties: [
                    {
                        name: "File Read Bytes",
                        desc: "Number of bytes read from files.",
                        value: mbeanAttribute("btrace:name=IOStats", "lastReadBytes")
                    },
                    {
                        name: "File Write Bytes",
                        desc: "Number of bytes written to files.",
                        value: mbeanAttribute("btrace:name=IOStats", "lastWrittenBytes")
                    }
                ]
            },
            {
                name: "File I/O size",
                desc: "Average number of bytes per one read/write operation.",
                reqs: "Requires BTrace Deployer plugin.",
                validator: function() {
                    return btraceDeployer != undefined;
                },
                deployment: {
                    deployer: btraceDeployer,
                    script: scriptPath,
                    fragment: "Java_IO"
                },
                properties: [
                    {
                        name: "File Read Size",
                        desc: "Average number of bytes read per one operation.",
                        value: divideBytes(mbeanAttribute("btrace:name=IOStats", "lastReadBytes"),mbeanAttribute("btrace:name=IOStats", "lastReadCount"))
                    },
                    {
                        name: "File Write Size",
                        desc: "Average number of bytes written per one operation.",
                        value: divideBytes(mbeanAttribute("btrace:name=IOStats", "lastWrittenBytes"),mbeanAttribute("btrace:name=IOStats", "lastWriteCount"))
                    }
                ]
            }
        ]
    }
]);
