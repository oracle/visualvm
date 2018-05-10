/*
 * A sample definition file for dynamic tracer
 */

VisualVM.Tracer.addPackages([
    {
        name: "Package 1",
        desc: "Sample package 1",
        icon: "org/graalvm/visualvm/modules/somemodule/resources/myicon.png",
        validator: function() {
            return true; // always enabled
        },
        probes: [
            {
                name: "Probe 1",
                desc: "Sample probe 1",
                properties: [
                    {
                        name: "Property 1",
                        desc: "Sample property 1",
                        value: mbeanAttribute("java.lang:type=Compilation", "CompilationTime"),
                        presenter: {
                            type: "discrete",
                            lineColor: AUTOCOLOR,
                            format: { // custom format
                                getUnits: function(format) {
                                    return "years";
                                }
                            }
                        }
                    },
                    {
                        name: "Property 2",
                        value: function(timestamp) {
                            if (this.counter == undefined) {
                                this.counter = 0;
                            }
                            return this.counter++;
                        }
                    }
                ]
            },
            {
                name: "Probe 2",
                validator: function() { // overriding the availability inherited from the package
                    return false; // never enabled
                },
                properties: [
                    {
                        name: "Property 3",
                        value: NULL_VALUE, // always return 0
                        presenter: {
                            format: ItemValueFormatter.DEFAULT_PERCENT,
                            max: 1000 // required by the DEFAULT_PERCENT formatter
                        }
                    }
                ]
            }
        ]
    }
])