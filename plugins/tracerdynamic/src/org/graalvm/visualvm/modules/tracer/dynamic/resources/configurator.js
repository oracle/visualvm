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

var Color = java.awt.Color;
var ProbeItemDescriptor = Packages.org.graalvm.visualvm.modules.tracer.ProbeItemDescriptor;
var ItemValueFormatter = Packages.org.graalvm.visualvm.modules.tracer.ItemValueFormatter;
var TracerProbeDescriptor = Packages.org.graalvm.visualvm.modules.tracer.TracerProbeDescriptor;
var ValueProvider = Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.ValueProvider;
var DynamicPackage = Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.DynamicPackage;
var DynamicProbe = Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.DynamicProbe;
var JmxModelFactory = Packages.org.graalvm.visualvm.tools.jmx.JmxModelFactory;
var NbBundle = Packages.org.openide.util.NbBundle;
var QueryExp = javax.management.QueryExp;
var AUTOCOLOR = ProbeItemDescriptor.DEFAULT_COLOR;

function VisualVM(){}

VisualVM.Tracer = {
    Type: {
        discrete: "discrete",
        continuous: "continuous"
    },
    addPackages: function (packages) {
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
}

VisualVM.MBeans = {
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
                var names = connection.queryNames(javax.management.ObjectName.getInstance(objectNamePattern), query);
                var iter = names.iterator();
                var nameArr = new Array();
                while (iter.hasNext()) {
                    nameArr[nameArr.length] = iter.next().toString();
                }
                return nameArr;
            }
        }
        return undefined;
    },
    attribute: function (objectName, attrName) {
        return new MBeanAttribute(objectName, attrName);
    }
}

var NULL_VALUE = new ValueProvider({
    getValue: function(timestamp) {
        return 0;
    }
})

function L11N(baseName) {
    this.bundle = NbBundle.getBundle(baseName + ".Bundle");

    this.message = function (key, attrs) {
        if (this.bundle != undefined) {
            var msg = this.bundle.getString(key);
            if (attrs != undefined && msg != undefined && msg != null) {
                msg = java.text.MessageFormat(msg, attrs);
            }
            return msg;
        }
        return "No resource bundle available for " + baseName;
    }
}

function getContinousItemDescriptorProvider(formatter) {
    formatter = formatter || ItemValueFormatter.DECIMAL;

    return function(property) {
        var factor = property.presenter.factor != undefined ? property.factor : 1;
        var min = property.presenter.min != undefined ? property.presenter.min : 0;
        var max = property.presenter.max || ProbeItemDescriptor.MAX_VALUE_UNDEFINED;
        var lineWidth = property.presenter.lineWidth || ProbeItemDescriptor.DEFAULT_LINE_WIDTH;
        var lineColor = null;
        var fillColor = null;
        if (property.presenter.lineColor == undefined && property.presenter.fillColor == undefined) {
            lineColor = AUTOCOLOR;
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

function getDiscreteItemDescriptorProvider(formatter) {
    formatter = formatter || ItemValueFormatter.DECIMAL;

    return function(property) {
        var factor = property.presenter.factor == undefined ? 1 : property.presenter.factor;
        var min = property.presenter.min == undefined ? 0 : property.presenter.min;
        var max = property.presenter.max == undefined ? ProbeItemDescriptor.MAX_VALUE_UNDEFINED : property.presenter.max;
        var lineWidth = property.presenter.lineWidth == undefined ? ProbeItemDescriptor.DEFAULT_LINE_WIDTH : property.presenter.lineWidth;
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
        
        if (property.presenter.topLine == undefined) {
            property.presenter.topLine = false;
        }
        if (property.presenter.outline == undefined) {
            property.presenter.outline = false;
        }
        if (property.presenter.fixedWidth == undefined) {
            property.presenter.fixedWidth = false;
        }
        
        if (property.presenter.topLine && !property.presenter.outline) {
            return ProbeItemDescriptor.discreteToplineItem(property.name, property.desc || "",
                formatter, factor, min, max, lineWidth, lineColor, fillColor,
                property.presenter.width == undefined ? 0 : property.presenter.width,
                property.presenter.fixedWidth
            )
        } else if (!property.presenter.fixedWidth) {
            return ProbeItemDescriptor.discreteOutlineItem(property.name, property.desc || "",
                formatter, factor, min, max, lineWidth, lineColor, fillColor)
        } else {
            return ProbeItemDescriptor.discreteBarItem(property.name, property.desc || "",
                formatter, factor, min, max, lineWidth, lineColor, fillColor,
                property.presenter.width == undefined ? 0 : property.presenter.width,
                property.presenter.fixedWidth
            )
        }
    }
}

function getItemDescriptor(property) {
    if (property.presenter.type == undefined) {
        property.presenter.type = VisualVM.Tracer.Type.continuous
    }
    if (property.presenter.format == undefined) {
        property.presenter.format = ItemValueFormatter.DEFAULT_DECIMAL
    } else if (property.presenter.format instanceof Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.ItemValueFormatterInterface) {
        property.presenter.format = new Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.ItemValueFormatterProxy(property.presenter.format);
    } else if (property.presenter != undefined &&
        property.presenter.format != undefined &&
        !(property.presenter.format instanceof ItemValueFormatter)) {
        var presFormat = property.presenter.format;
        var forward = {
            formatValue: function(value, format) {
                if (presFormat.formatValue != undefined) {
                    return presFormat.formatValue(value, format);
                }
                return value;
            },
            getUnits: function(format) {
                if (presFormat.getUnits != undefined) {
                    return presFormat.getUnits(format);
                }
                return "";
            }
        }
        property.presenter.format = new Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.ItemValueFormatterProxy(
                                        new Packages.org.graalvm.visualvm.modules.tracer.dynamic.impl.ItemValueFormatterInterface(forward)
                                    );
    }

    if (property.presenter.type == VisualVM.Tracer.Type.continuous) {
        return getContinousItemDescriptorProvider(property.presenter.format)(property);
    } else if (property.presenter.type == VisualVM.Tracer.Type.discrete) {
        return getDiscreteItemDescriptorProvider(property.presenter.format)(property);
    }
    return undefined;
}

var configuredPackages = new java.util.ArrayList();

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
        if (!enabled) {
            desc = desc.concat(getReqDesc(pkg));
        }
        var icon = pkg.icon != undefined ? pkg.icon : null;
        var position = pkg.position != undefined ? pkg.position : java.lang.Integer.MAX_VALUE;

        if (typeof(icon) == "string") {
            try {
                icon = Packages.org.openide.util.ImageUtilities.loadImageIcon(icon, true);
            } catch (e) {
                icon = null;
            }
        }

        var dPkg = new DynamicPackage(pkg.name, desc, icon, position);

        var probes = typeof(pkg.probes) == "function" ? pkg.probes() : pkg.probes;
        var inferredPosition = 0;

        for(var probeIndex in probes) {
            var probe = probes[probeIndex];

            // a valid probe must have properties
            if (probe.properties != undefined) {
                var itemDescriptors = new java.util.ArrayList();
                var valProviders = new java.util.ArrayList();
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
                                format: ItemValueFormatter.DEFAULT_DECIMAL
                            }
                        }
                        var itemDescriptor = getItemDescriptor(prop);
                        
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
                if (itemDescriptors.size() > 0) {
                    var dProbe = new DynamicProbe(itemDescriptors, valProviders);
                    // deployment is optional
                    if (probe.deployment != undefined) {
                        if (probe.deployment instanceof Array) {
                            for (var deployment in probe.deployment) {
                                if (deployment.deployer instanceof Packages.org.graalvm.visualvm.modules.tracer.dynamic.spi.DeployerImpl) {
                                    dProbe.addDeployment(deployment.deployer, getDeploymentAttributes(deployment));
                                }
                            }
                        } else if (probe.deployment.deployer != undefined) {
                            if (probe.deployment.deployer instanceof Packages.org.graalvm.visualvm.modules.tracer.dynamic.spi.DeployerImpl) {
                                dProbe.addDeployment(probe.deployment.deployer, getDeploymentAttributes(probe.deployment));
                            }
                        }
                    }
                    var pEnabled = enabled;
                    desc = probe.desc != undefined ? probe.desc : "";
                    if (pEnabled) {
                        if (probe.validator != undefined) {
                            pEnabled = probe.validator();
                        }
                        if (probe.deployment != undefined && probe.deployment.deployer != undefined) {
                            pEnabled = probe.deployment.deployer.isApplicable(application);
                        }
                    }
                    if (!pEnabled && enabled) {
                        desc = desc.concat(getReqDesc(probe));
                    }
                    dProbe.setProbeDescriptor(new TracerProbeDescriptor(
                        probe.name,
                        desc || "", icon,
                        probe.position != undefined ? probe.position : (inferredPosition += 10),
                        pEnabled
                    ));
                    dPkg.addProbe(dProbe);
                }
            }
        }
        if (dPkg.getProbeDescriptors().length > 0) { // add only packages with at least one probe
            configuredPackages.add(dPkg);
        }
    }
}

function getDeploymentAttributes(deployment) {
    var map = new java.util.HashMap();
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

function delta(valProvider) {
    return new Delta(valProvider);
}

function getKeys(map) {
    var ret = new Array();
    if (map == undefined || map == null) return 0;
    if (map instanceof javax.management.openmbean.TabularData) {
        var iter2 = map.keySet().iterator();
        while (iter2.hasNext()) {
            ret[ret.length] = iter2.next().get(0);
        }        
    } else if (map.length != undefined && map.length > 0 && map[0].getCompositeType != undefined) {
        for(var counter=0; counter < map.length; counter++) {
            ret[ret.length] = map[counter].get("key");
        }
    } else if (map instanceof javax.management.openmbean.CompositeData) {
        if (map.getCompositeType != undefined) {
            var type = map.getCompositeType();
        
            var iter1 = type.keySet().iterator();
            while (iter1.hasNext()) {
                var val = iter1.next();
                ret[ret.length] = val.get != undefined ? val.get(0) : val;
            }
        } else if (map.getTabularType != undefined) {
            var iter2 = map.keySet().iterator();
            while (iter2.hasNext()) {
                ret[ret.length] = iter2.next().get(0);
            }
        }
    }
    return ret;
}

function get(map, keys) {
    var ret;
    var keyArray = isArray(keys);
    var key = keyArray ? keys[0] : keys;
    if (map == undefined || map == null) return 0;
    if (map instanceof javax.management.openmbean.TabularData) {
        // javax.management.openmbean.TabularDataSupport -> effectively a Map instance
        if (!keyArray || keys.length == 1) {
            ret = map.get([key]).get(["value"]);
            return ret;
        } else {
            ret = get(map.get([key]).get(["value"]), keys.slice(1));
            return ret;
        }
    } else if (map.length != undefined && map.length > 0 && map[0].getCompositeType != undefined) {
        for(var counter=0; counter < map.length; counter++) {
            if (map[counter].get("key") == key) {
                if (!keyArray || keys.length == 1) {
                    ret = map[counter].get("value");
                    return ret;
                } else {
                    ret = get(map[counter].get("value"), keys.slice(1));
                    return ret;
                }
            }
        }
    } else if (map instanceof javax.management.openmbean.CompositeData) {
        if (map.getTabularType != undefined) {
            // javax.management.openmbean.TabularDataSupport -> effectively a Map instance
            if (!keyArray || keys.length == 1) {
                ret = map.get([key]).get(["value"]);
                return ret;
            } else {
                ret = get(map.get([key]).get(["value"]), keys.slice(1));
                return ret;
            }
        } else if (map.getCompositeType != undefined) {
            if (!keyArray || keys.length == 1) {
                ret = map.get(key);
                return ret;
            } else {
                ret = get(map.get(key), keys.slice(1));
                return ret;
            }
        }
    }
    return 0;
}

function getReqDesc(reqHolder) {
    var desc = "";
    if (reqHolder.reqs != undefined) {
        desc = desc.concat(" (");
        if (reqHolder.reqs.constructor == Array) {
            for(var pi in reqHolder.reqs) {
                desc = desc.concat((pi > 0) ? ", " : "", reqHolder.reqs[pi]);
            }
        } else {
            desc = desc.concat(reqHolder.reqs);
        }
        desc = desc.concat(")");
    }
    return desc;
}

function isArray(obj) {
   return obj != undefined && obj.constructor == Array
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

    var mbean = this;
    this.get = function (key) {
        return new WrappedValueProvider(this, key);
    }

    this.keys = function() {
        return getKeys(mbean.getProvider().value(0));
    }
    
    this.getInfo = function () {
        return mbean.getProvider().getInfo();
    }

    this.value = function (timestamp) {
        return mbean.getProvider().value(timestamp);
//        return 0;
    }

    this.getValue =  this.value;

    this.getProvider = function() {
        if (provider == undefined) {
            provider = new Packages.org.graalvm.visualvm.modules.tracer.dynamic.jmx.JMXValueProvider(this.on, this.an, application);
        }
        return provider;
    }
}

MBeanAttribute.prototype.sharedId = 0;

MBeanAttribute.prototype.clone = function() {
    return new MBeanAttribute(this.on, this.an);
}
MBeanAttribute.prototype.setObjectName = function(objectName) {
    this.on = objectName;
}
MBeanAttribute.prototype.setAttributeName = function(attribName) {
    this.an = attribName;
}

function WrappedValueProvider(mbeanAttr, keys) {
    this.attribute = mbeanAttr;
    this.keys = new Array();
    if (isArray(keys)) {
        this.keys = this.keys.concat(keys);
    } else {
        this.keys.push(keys);
    }

    this.getNewKeys = function (key) {
        var newKeys = new Array();
        if (isArray(this.keys)) {
            newKeys = newKeys.concat(this.keys);
        } else {
            newKeys.push(this.keys);
        }
            
        newKeys.push(key);

        return newKeys;
    }

    WrappedValueProvider.prototype.get = function (key) {
        var myKeys = this.getNewKeys(key);
        var ret = new WrappedValueProvider(this.attribute, myKeys);
        return ret;
    }

    WrappedValueProvider.prototype.getValue = function (timestamp) {
        var val = this.attribute.value(timestamp);
        return get(val, this.keys);
    }

    WrappedValueProvider.prototype.getKeys = function() {
        return getKeys(this.getValue(0));
    }

    WrappedValueProvider.prototype.dump = function () {
        println(this.keys)
    }
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