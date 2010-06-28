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

importPackage(com.sun.tools.visualvm.modules.tracer.dynamic.impl)
importPackage(com.sun.tools.visualvm.modules.tracer)
importPackage(com.sun.tools.visualvm.tools.jmx)
importPackage(javax.management)
importPackage(org.openide.util)

var Color = Packages.java.awt.Color;

var Format = {
    percent: "percent",
    decimal: "decimal",
    bytes: "bytes"
}

var NULL_VALUE = new ValueProvider({
    getValue: function(timestamp) {
        return 0;
    }
})

function L11N(baseName) {
    var bundle = NbBundle.getBundle(baseName + ".Bundle");

    this.message = function (key, attrs) {
        if (bundle != undefined) {
            var msg = bundle.getString(key);
            if (attrs != undefined && msg != undefined && msg != null) {
                msg = Packages.java.text.MessageFormat(msg, attrs);
            }
            return msg;
        }
        return "No resource bundle available for " + baseName;
    }

    L11N.prototype.getMessage = function (key, attrs) {
        return this.message(key, attrs);
    }
}

var MBeanSupport = {
    listMBeanNames: function(objectNamePattern, query) {
        if (query != undefined && (query instanceof QueryExp)) {
            return undefined;
        }
        objectNamePattern = objectNamePattern == undefined ? "*" : objectNamePattern;
        query = query == undefined ? null : query;

        var jmxModel = JmxModelFactory.getJmxModelFor(application);
        if (jmxModel != undefined && jmxModel != null) {
            var connection = jmxModel.getMBeanServerConnection();
            if (connection != undefined && connection != null) {
                var names = connection.queryNames(Packages.javax.management.ObjectName.getInstance(objectNamePattern), query);
                var iter = names.iterator();
                var nameArr = new Array();
                while (iter.hasNext()) {
                    nameArr[nameArr.length] = iter.next().toString();
                }
                return nameArr;
            }
        }
        return undefined;
    }
}


var vars = new Array();

function getContinousItemProvider(formatter) {
    formatter = formatter || ItemValueFormatter.DECIMAL;

    return function(property) {
        var factor = property.presenter.factor != undefined ? property.factor : 1;
        var min = property.presenter.min != undefined ? property.presenter.min : 0;
        var max = property.presenter.max || ProbeItemDescriptor.MAX_VALUE_UNDEFINED;
        var lineWidth = property.presenter.lineWidth || ProbeItemDescriptor.DEFAULT_LINE_WIDTH;
        var lineColor = null;
        var fillColor = null;
        if (property.presenter.lineColor == undefined && property.presenter.fillColor == undefined) {
            lineColor = ProbeItemDescriptor.DEFAULT_COLOR;
        } else {
            if (property.presenter.lineColor != undefined) {
                lineColor = property.presenter.lineColor;
            }
            if (property.presenter.fillColor != undefined) {
                fillColor = property.presenter.fillColor;
            }
        }
        if (fillColor != undefined && lineColor == undefined) {
            lineColor = fillColor;
        }

        return ProbeItemDescriptor.continuousItem(property.name, property.desc || "", formatter, factor, min, max, lineWidth, lineColor, fillColor);
    }
}

function getDiscreteItemProvider(formatter) {
    formatter = formatter || ItemValueFormatter.DECIMAL;

    return function(property) {
        var factor = property.presenter.factor || 1;
        var min = property.presenter.min || 0;
        var max = property.presenter.max || ProbeItemDescriptor.MAX_VALUE_UNDEFINED;
        var lineWidth = property.presenter.lineWidth || ProbeItemDescriptor.DEFAULT_LINE_WIDTH;
        var lineColor = null;
        var fillColor = null;
        if (property.presenter.lineColor == undefined && property.presenter.fillColor == undefined) {
            lineColor = ProbeItemDescriptor.DEFAULT_COLOR;
        } else {
            if (property.presenter.lineColor != undefined) {
                lineColor = property.presenter.lineColor;
            }
            if (property.presenter.fillColor != undefined) {
                fillColor = property.presenter.fillColor;
            }
        }

        if (fillColor != undefined && lineColor == undefined) {
            lineColor = fillColor;
        }

        return ProbeItemDescriptor.discreteItem(property.name, property.desc || "",
            formatter, factor, min, max, lineWidth, lineColor, fillColor,
            property.presenter.width || 0, property.presenter.fixedWidth || false, property.presenter.topLine || false,
            property.presenter.outline || false
        );
    }
}

var itemDescriptorMap = {
    continuous: {
        decimal: getContinousItemProvider(ItemValueFormatter.DEFAULT_DECIMAL),
        percent: getContinousItemProvider(ItemValueFormatter.DEFAULT_PERCENT),
        bytes: getContinousItemProvider(ItemValueFormatter.DEFAULT_BYTES)
    },
    discrete: {
        decimal: getDiscreteItemProvider(ItemValueFormatter.DEFAULT_DECIMAL),
        percent: getDiscreteItemProvider(ItemValueFormatter.DEFAULT_PERCENT),
        bytes: getDiscreteItemProvider(ItemValueFormatter.DEFAULT_BYTES)
    }
}

var configuredPackages = new Packages.java.util.ArrayList();

function configure(packages) {
    if (application != undefined && packages != undefined) {
        if (packages instanceof Array) {
            for(var index in packages) {
                processPackage(packages[index]);
            }
        } else {
            processPackage(packages);
        }
    }
}

function processPackage(pkg) {
    // don't process package definition if it does not contain any probes
    if (pkg.probes != undefined) {
        var enabled = pkg.validator == undefined || pkg.validator(application);

        var desc = pkg.desc != undefined ? pkg.desc : "";
        var icon = pkg.icon != undefined ? pkg.icon : null;
        var position = pkg.position != undefined ? pkg.position : Packages.java.lang.Integer.MAX_VALUE;

        var dPkg = new DynamicPackage(pkg.name, desc, icon, position);

        var probes = typeof(pkg.probes) == "function" ? pkg.probes() : pkg.probes;
        var inferredPosition = 0;

        for(var probeIndex in probes) {
            var probe = probes[probeIndex];

            // a valid probe must have properties
            if (probe.properties != undefined) {
                var itemDescriptors = new Packages.java.util.ArrayList();
                var valProviders = new Packages.java.util.ArrayList();
                var propArray;
                if (typeof(probe.properties) == "function") {
                    propArray = probe.properties();
                } else if (probe.properties instanceof Array) {
                    propArray = probe.properties;
                }
                if (propArray != undefined) {
                    for(var propIndex in propArray) {
                        var prop = probe.properties[propIndex];
                        if (prop.presenter == undefined) {
                            // setting default
                            prop.presenter = {
                                type: "continuous",
                                format: "decimal"
                            }
                        }
                        var itemDescriptor = itemDescriptorMap[prop.presenter.type || "continuous"][prop.presenter.format || "decimal"](prop);
                        if (itemDescriptor != null && itemDescriptor != undefined) {
                            itemDescriptors.add(itemDescriptor);
                        }
                        if (typeof(prop.value) == "function") {
                            var handler = {
                                getValue: prop.value
                            };
                            valProviders.add(new ValueProvider(handler));
                        } else if (prop.value.getValue != undefined) {
                            valProviders.add(new ValueProvider(prop.value));
                        } else if (prop.value.valueProvider != undefined) {
                            valProviders.add(prop.value.valueProvider);
                        }
                    }

                }

                var dProbe = new DynamicProbe(itemDescriptors, valProviders);
                // deployment is optional
                if (probe.deployment != undefined) {
                    if (probe.deployment instanceof Array) {
                        for (var deployment in probe.deployment) {
                            if (deployment.deployer instanceof Packages.com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl) {
                                dProbe.addDeployment(deployment.deployer, getDeploymentAttributes(deployment));
                            }
                        }
                    } else if (probe.deployment.deployer != undefined) {
                        if (probe.deployment.deployer instanceof Packages.com.sun.tools.visualvm.modules.tracer.dynamic.spi.DeployerImpl) {
                            dProbe.addDeployment(probe.deployment.deployer, getDeploymentAttributes(probe.deployment));
                        }
                    }
                }
                dProbe.setProbeDescriptor(new TracerProbeDescriptor(
                    probe.name,
                    probe.desc || "", null,
                    probe.position != undefined ? probe.position : (inferredPosition += 10),
                    enabled
                ));
                dPkg.addProbe(dProbe);
            }
        }
        configuredPackages.add(dPkg);
    }
}

function getDeploymentAttributes(deployment) {
    var map = new Packages.java.util.HashMap();
    for(var attr in deployment) {
        if (attr != "deployer") {
            map.put(attr, deployment[attr]);
        }
    }
    return map;
}

function mbeanAttribute(objectName, attrName) {
    return new MBeanAttribute(objectName, attrName);
}

function mbeanQuery(params) {
    return function() {
        var ret = new Array();
        if (params.exp != undefined && params.properties != undefined) {
            var jmxModel = JmxModelFactory.getJmxModelFor(application);
            if (jmxModel != undefined && jmxModel != null) {
                var connection = jmxModel.getMBeanServerConnection();
                if (connection != undefined && connection != null) {
                    var names = connection.queryNames(Packages.javax.management.ObjectName.getInstance(params.exp), null);
                    var iter = names.iterator();
                    var counter = 0;
                    while (iter.hasNext()) {
                        var on = iter.next();
                        var props = new Array();
                        for(var propIndex in params.properties) {
                            var template = params.properties[propIndex];
                            props[propIndex] = {
                                name: template.name,
                                desc: template.desc,
                                value: isFunction(template.value.clone) ? template.value.clone() : template.value
                            }
                            if (props[propIndex].value.setObjectName != undefined) {
                                props[propIndex].value.setObjectName(on);
                            }
                        }
                        ret[counter++] = {
                            name: on,
                            desc: on,
                            properties: props
                        }
                    }
                }
            }
        }
        return ret;
    }
}

function delta(valProvider) {
    return new Delta(valProvider);
}

function get(map, keys) {
    var ret;
    var keyArray = isArray(keys);
    var key = keyArray ? keys[0] : keys;
    if (map == undefined || map == null) return 0;
    if (map.length != undefined && map.length > 0 && map[0].getCompositeType != undefined) {
        for(var counter=0; counter < map.length; counter++) {
            if (map[counter].get("key") == key) {
                if (!keyArray || keys.length == 1) {
                    ret = map[counter].get("value");
                    return ret;
                } else {
                    keys.shift();
                    ret = get(map[counter].get("value"), keys);
                    return ret;
                }
            }
        }
    } else if (map.getTabularType != undefined) {
        // javax.management.openmbean.TabularDataSupport -> effectively a Map instance
        if (!keyArray || keys.length == 1) {
            ret = map.get([key]).get(["value"]);
            return ret;
        } else {
            keys.shift();
            ret = get(map.get([key]).get(["value"]), keys);
            return ret;
        }
    } else if (map.getCompositeType != undefined) {
        if (!keyArray || keys.length == 1) {
            ret = map.get(key);
            return ret;
        } else {
            keys.shift();
            ret = get(map.get(key), keys);
            return ret;
        }
    }
    return 0;
}

function isArray(obj) {
   if (obj.constructor.toString().indexOf("Array") == -1)
      return false;
   else
      return true;
}

function printStackTrace(exp) {
    if (exp == undefined) {
        try {
            exp.toString();
        } catch (e) {
            exp = e;
        }
    }
    // note that user could have caught some other
    // "exception"- may be even a string or number -
    // and passed the same as argument. Also, check for
    // rhinoException property before using it
    if (exp instanceof Error &&
        exp.rhinoException != undefined) {
        exp.rhinoException.printStackTrace();
    }
}

function isFunction(obj) {
    return obj != undefined && typeof(obj) == "function";
}

// ======================== Prototypes ===============================
function MBeanAttribute(objectName, attributeName) {
    var provider;

    MBeanAttribute.prototype.sharedId++;

    this.on = objectName != undefined ? objectName : "";
    this.an = attributeName != undefined ? attributeName : "";
    this.id = MBeanAttribute.prototype.sharedId;

    this.get = function (keys) {
        return function(timestamp) {
            var mykeys = keys.concat();
            var val = getProvider().value(timestamp);
            var ret = get(val, mykeys);
            return ret;
        };
        return ret;
    }
    this.value = function (timestamp) {
        return this.getProvider().value(timestamp);
//        return 0;
    }

    this.getProvider = function() {
        if (provider == undefined) {
            provider = new Packages.com.sun.tools.visualvm.modules.tracer.dynamic.jmx.JMXValueProvider(this.on, this.an, application);
        }
        return provider;
    }
}

MBeanAttribute.prototype.sharedId = 0;
MBeanAttribute.prototype.getValue = function(timestamp) {
    return this.value(timestamp);
}
MBeanAttribute.prototype.clone = function() {
    return new MBeanAttribute(this.on, this.an);
}
MBeanAttribute.prototype.setObjectName = function(objectName) {
    this.on = objectName;
}
MBeanAttribute.prototype.setAttributeName = function(attribName) {
    this.an = attribName;
}

function Delta(valProvider) {
    var oldValue;
    var oldTimeStamp;
    this.provider = valProvider;

    this.getDelta = function(timeStamp) {
        if (timeStamp == oldTimeStamp) return 0;
        
        var newValue = valProvider.getValue(timeStamp);
        var delta = oldValue != undefined ? (newValue - oldValue) : 0;
        oldValue = newValue;
        return delta;
    }
}

Delta.prototype.setObjectName = function(objectName) {
    if (this.provider.setObjectName != undefined) {
        this.provider.setObjectName(objectName);
    }
}
Delta.prototype.setAttribute = function(attrName) {
    if (this.provider.setAttribute != undefined) {
        this.provider.setAttributeName(attrName);
    }
}
Delta.prototype.getValue = function(timestamp) {
    return this.getDelta(timestamp);
}
Delta.prototype.clone = function() {
    return new Delta(isFunction(this.provider.clone) ? this.provider.clone() : this.provider);
}