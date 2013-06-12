/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

var hatPkg = Packages.org.netbeans.modules.profiler.oql.engine.api.impl;
var snapshot;

/**
 * This is JavaScript interface for heap analysis using HAT
 * (Heap Analysis Tool). HAT classes are refered from
 * this file. In particular, refer to classes in hat.model 
 * package.
 * 
 * HAT model objects are wrapped as convenient script objects so that
 * fields may be accessed in natural syntax. For eg. Java fields can be
 * accessed with obj.field_name syntax and array elements can be accessed
 * with array[index] syntax. 
 */

// returns an enumeration that wraps elements of
// the input enumeration elements.
function wrapperEnumeration(e) {
    return new java.util.Enumeration() {
        hasMoreElements: function() {
            return e.hasMoreElements();
        },
        nextElement: function() {
            return wrapJavaValue(e.nextElement());
        }
    };
}

// returns an enumeration that filters out elements
// of input enumeration using the filter function.
function filterEnumeration(e, func, wrap) {
    var next = undefined;
    var index = 0;

    function findNext() {
        var tmp;
        while (e.hasMoreElements() && !cancelled.get()) {
            tmp = e.nextElement();
            index++;
            if (wrap) {
                tmp = wrapJavaObject(tmp);
            }
            if (func(tmp, index, e)) {
                next = tmp;
                return;
            }
        }
    }

    return new java.util.Iterator() {
        hasNext: function() {
            findNext();
            return next != undefined;
        },

        next: function() {
            if (next == undefined) {
                // user may not have called hasMoreElements?
                findNext();
            }
            if (next == undefined) {
                throw "NoSuchElementException";
            }
            var res = next;
            next = undefined;
            return res;
        },
        
        remove: function() {
            throw "NotSupportedOperation";
        }

    };
}

function filterIterator(e, func, wrap) {
    var next = undefined;
    var index = 0;

    function findNext() {
        var tmp;
        while (e.hasNext() && !cancelled.get()) {
            tmp = e.next();
            index++;
            if (wrap) {
                tmp = wrapJavaObject(tmp);
            }
            if (func(tmp, index, e)) {
                next = tmp;
                return;
            }
        }
    }

    return new java.util.Iterator() {
        hasNext: function() {
            findNext();
            return next != undefined;
        },

        next: function() {
            if (next == undefined) {
                // user may not have called hasMoreElements?
                findNext();
            }
            if (next == undefined) {
                throw "NoSuchElementException";
            }
            var res = next;
            next = undefined;
            return res;
        },

        remove: function() {
            throw "NotSupportedOperation";
        }

    };
}

// enumeration that has no elements ..
var emptyEnumeration = new java.util.Enumeration() {
    hasMoreElements: function() {
        return false;
    },
    nextElement: function() {
        throw "NoSuchElementException";
    }
};

function wrapRoot(root) {
    if (root) {
        return {
            id: root.kind,
            description: "Reference " + root.kind,
            referrer: wrapJavaValue(root.instance),
            type: root.kind,
            wrapped: root
        };
    } else {
        return null;
    }
}

function wrapField(javaField) {
    if (javaField) {
        return {
            name: javaField.name,
            signature: javaField.type.name,
            wrapped: javaField
        };
    } else {
        return null;
    }
}

function JavaClassProto() {    
    function jclass(obj) {
        return obj['wrapped-object'];
    }

    // return whether given class is subclass of this class or not
    this.isSubclassOf = function(other) {
        var tmp = jclass(this);
        var otherid = objectid(other);
        while (tmp != null) {
            if (otherid.equals(tmp.javaClassId)) {
                return true;
            }
            tmp = tmp.superclass;
        }
        return false;
    }

    // return whether given class is superclass of this class or not
    this.isSuperclassOf = function(other) {
        return other.isSubclassOf(this); 
    }

    // includes direct and indirect superclasses
    this.superclasses = function() {
        var res = new Array();
        var tmp = this.superclass;
        while (tmp != null) {
            res[res.length] = tmp;
            tmp = tmp.superclass;
        }
        return res;
    }

    /**
     * Returns an array containing subclasses of this class.
     *
     * @param indirect should include indirect subclasses or not.
     *                 default is true.
     */
    this.subclasses = function(indirect) {
        if (indirect == undefined) indirect = true;
        var classes = wrapIterator(jclass(this).subClasses.iterator(), true);
        var res = new Array();
        while (classes.hasMoreElements()) {
            var subclass = classes.nextElement();
            res[res.length] = subclass;
            if (indirect) {
                res = res.concat(subclass.subclasses());
            }
        }
        return res;
    }

    this.instances = function() {
        return jclass(this).instances.iterator;
    }

    this.toString = function() { 
        return jclass(this).toString();
    }
}

var theJavaClassProto = new JavaClassProto();

// Script wrapper for HAT model objects, values.
// wraps a Java value as appropriate for script object
function wrapJavaValue(thing) {
    if (thing == null || thing == undefined) {
        return null;
    }

    //    printStackTrace();
    //    println(thing);

    if (thing instanceof Packages.org.netbeans.lib.profiler.heap.FieldValue) {
        var type = thing.field.type;

        // map primitive values to closest JavaScript primitives
        if (type.name == "boolean") {
            return thing.value == "true";
        } else if (type.name == "char") {
            return thing.value + '';
        } else if (type.name == "float" ||
            type.name == "double" ||
            type.name == "byte" ||
            type.name == "short" ||
            type.name == "int" ||
            type.name == "long"){
            return java.lang.Double.parseDouble(thing.value);
        } else {
            // wrap Java object as script object
            return wrapJavaObject(thing.instance);
        }
    } else if (thing instanceof Packages.org.netbeans.lib.profiler.heap.GCRoot) {
        return wrapRoot(thing);
    } else if (thing instanceof Packages.org.netbeans.lib.profiler.heap.Field) {
        return wrapField(thing);
    } else {
        return wrapJavaObject(thing);
    }
}

// wrap Java object with appropriate script object
function wrapJavaObject(thing) {
    if (thing == null) return null;
    
    // HAT Java model object wrapper. Handles all cases 
    // (instance, object/primitive array and Class objects)	
    function javaObject(jobject) {		
        //        // FIXME: Do I need this? or can I assume that these would
        //        // have been resolved already?
        //        if (jobject instanceof hatPkg.model.JavaObjectRef) {
        //            jobject = jobject.dereference();
        //            if (jobject instanceof hatPkg.model.HackJavaValue) {
        //                print(jobject);
        //                return null;
        //            }
        //        }

        //        print(jobject.getClass());
        if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.JavaClass) {
            //            println("wrapping as Class");
            return new JavaClassWrapper(jobject);
        } else if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.ObjectArrayInstance) {
            //            println("wrapping as ObjectArray");
            return new JavaObjectArrayWrapper(jobject);
        } else if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.PrimitiveArrayInstance) {
            // println("wrapping as ValueArray");
            return new JavaValueArrayWrapper(jobject);
        } else if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.Instance) {
            //            println("wrapping as Instance");
            return new JavaObjectWrapper(jobject);
        } else {
            //            println("unknown heap object type: " + jobject.getClass());
            return jobject;
        }
    }
    
    // returns wrapper for Java instances
    function JavaObjectWrapper(instance) {
        var things = instance.fieldValues;
        var fldValueCache = new Array();

        // instance fields can be accessed in natural syntax
        return new JSAdapter() {
            __getIds__ : function() {
                var res = new Array(things.size());
                for(var j=0;j<things.size();j++) {
                    res[j] = things.get(j).field.name;
                }
                return res;
            },
            __has__ : function(name) {
                for (var i=0;i<things.size();i++) {
                    if (name == things.get(i).field.name) return true;
                }
                return name == 'clazz' || name == 'toString' ||
                name == 'id' || name == 'wrapped-object' || name == 'statics';
            },
            __get__ : function(name) {
                if (name == 'clazz') {
                    if (fldValueCache[name] == undefined) {
                        fldValueCache[name] = wrapJavaObject(instance.javaClass);
                    }
                    return fldValueCache[name];
                } else if (name == 'statics') {
                    if (fldValueCache[name] == undefined) {
                        var clz = wrapJavaObject(instance.javaClass);
                        if (clz != undefined) {
                            fldValueCache[name] = clz.statics;
                        } else {
                            fldValueCache[name] = null;
                        }
                    }
                    return fldValueCache[name];
                } else if (name == 'id') {
                    if (fldValueCache[name] == undefined) {
                        fldValueCache[name] = instance.instanceId;
                    }
                    return fldValueCache[name];
                } else if (name == 'toString') {
                    return function() {
                        if (instance.javaClass.name == "java.lang.String") {
                            return snapshot.valueString(instance);
                        }
                        return instance.toString();
                    }
                } else if (name == 'wrapped-object') {
                    return instance;
                } else {
                    if (fldValueCache["_$"+name] == undefined) {
                        fldValueCache["_$"+name] = wrapJavaObject(instance.getValueOfField(name));
                    }
                    return fldValueCache["_$"+name];
                }
            }
        }				
    }

    // return wrapper for Java Class objects
    function JavaClassWrapper(jclass) {
        var static_fields = jclass.staticFieldValues;
        var fldValueCache = new Array();

        // to access static fields of given Class cl, use 
        // cl.statics.<static-field-name> syntax
        this.statics = new JSAdapter() {
            __getIds__ : function() {
                var res = new Array(static_fields.size());
                for (var i=0;i<static_fields.size();i++) {
                    res[i] = static_fields.get(i).field.name;
                }

                return res;
            },
            __has__ : function(name) {
                for (var i=0;i<static_fields.size();i++) {
                    if (name == static_fields.get(i).field.name) {
                        return true;
                    }					
                }
                return name == 'id' || theJavaClassProto[name] != undefined;
            },
            __get__ : function(name) {
                if (name == "toString") {
                    result = jclass.toString();
                } else {
                    if (fldValueCache["_$"+name] == undefined) {
                        var result;
                        if (name == 'id') {
                            result = jclass.javaClassId;
                        } else {
                            result = theJavaClassProto[name];
                            if (result == null) {
                                result = wrapJavaObject(jclass.getValueOfStaticField(name));
                            }
                        }
                        fldValueCache["_$"+name] = result;
                    }
                    return fldValueCache["_$"+name];
                }
            }
        }

        if (jclass.superClass != null) {
            this.superclass = wrapJavaValue(jclass.superClass);
        } else {
            this.superclass = null;
        }

        this.loader = wrapJavaObject(jclass.classLoader);
        this.signers = undefined; //TODO wrapJavaValue(jclass.getSigners());
        this.protectionDomain = undefined; //TODO wrapJavaValue(jclass.getProtectionDomain());
        this.fields = wrapIterator(jclass.fields.iterator(), true);
        this.instanceSize = jclass.instanceSize;
        this.name = jclass.name; 
        this['wrapped-object'] = jclass;
        this.__proto__ = this.statics;
    }
    
    // returns wrapper for Java object arrays
    function JavaObjectArrayWrapper(array) {
        var elements = array.values;
        var fldValueCache = new Array();
        // array elements can be accessed in natural syntax
        // also, 'length' property is supported.
        return new JSAdapter() {
            __getIds__ : function() {
                var res = new Array(elements.size());
                for (var i = 0; i < elements.size(); i++) {
                    res[i] = i;
                }
                return res;
            },
            __has__: function(name) {
                return (typeof(name) == 'number' &&
                    name >= 0 && name < elements.size())  ||
                name == 'length' || name == 'clazz' ||
                name == 'toString' || name == 'wrapped-object';
            },
            __get__ : function(name) {
                if (typeof(name) == 'number' &&
                    name >= 0 && name < elements.size()) {
                    return wrapJavaValue(elements.get(name));
                } else if (name == 'id') {
                    if (fldValueCache[name] == undefined) {
                        fldValueCache[name] = array.instanceId;
                    }
                    return fldValueCache[name];
                } else if (name == 'length') {
                    if (fldValueCache["len"] == undefined) {
                        fldValueCache["len"] = elements.size();
                    }
                    return fldValueCache["len"];
                } else if (name == 'clazz') {
                    if (fldValueCache[name] == undefined) {
                        fldValueCache[name] = wrapJavaObject(array.javaClass);
                    }
                    return fldValueCache[name];
                } else if (name == 'toString') {
                    return function() { 
                        return array.toString();
                    }
                } else if (name == 'wrapped-object') {
                    return array;
                } else {
                    return undefined;
                }				
            }
        }			
    }
    
    // returns wrapper for Java primitive arrays
    function JavaValueArrayWrapper(array) {
        var elements = array.values;
        var fldValueCache = new Array();
        // array elements can be accessed in natural syntax
        // also, 'length' property is supported.
        return new JSAdapter() {
            __getIds__ : function() {
                var r = new Array(elements.size());
                for (var i = 0; i < elements.size(); i++) {
                    r[i] = i;
                }
                return r;
            },
            __has__: function(name) {
                return (typeof(name) == 'number' &&
                    name >= 0 && name < elements.size()) ||
                name == 'length' || name == 'clazz' ||
                name == 'toString' || name == 'wrapped-object';
            },
            __get__: function(name) {
                if (typeof(name) == 'number' &&
                    name >= 0 && name < elements.size()) {
                    return elements.get(name);
                }
    
                if (name == 'length') {
                    if (fldValueCache["len"] == undefined) {
                        fldValueCache["len"] = elements.size();
                    }
                    return fldValueCache["len"];
                } else if (name == 'toString') {
                    return function() { 
                        if (array.javaClass.name == 'char[]') {
                            return snapshot.valueString(array);
                        }
                        return array.toString();
                    }
                } else if (name == 'wrapped-object') {
                    return array;
                } else if (name == 'clazz') {
                    if (fldValueCache[name] == undefined) {
                        fldValueCache[name] = wrapJavaObject(array.javaClass);
                    }
                    return fldValueCache[name];
                } else {
                    return undefined;
                }
            }
        }
    }
    return javaObject(thing);
}

// unwrap a script object to corresponding HAT object
function unwrapJavaObject(jobject) {
    //    println("Unwrapping object");
    //    println(typeof(jobject));
    
    if (!(jobject instanceof Packages.org.netbeans.lib.profiler.heap.Instance)) {
        if (jobject instanceof Array) {
            //            println("Object is array");
            var arr = new java.util.ArrayList(jobject.length);

            for (var index in jobject) {
                arr.add(jobject[index]);
            }
            return arr.toArray();
        }
        
        try {
            //            println(typeof(jobject));
            var orig = jobject;
            jobject = orig["wrapped-object"];
            if (jobject == undefined) {
                jobject = orig.wrapped;
            }
            if (jobject == undefined) {
                jobject = orig;
            }
        } catch (e) {
            println("unwrapJavaObject: " + jobject + ", " + e);
            jobject = undefined;
        }
    } 
    return jobject;
}

function unwrapMap(jobject) {
    var map = new java.util.HashMap();
    for(var prop in jobject) {
        //        println("adding " + prop + " = " + unwrapJavaObject(jobject[prop]));
        map.put(prop, unwrapJavaObject(jobject[prop]));
    }
    return map;
}

function unwrapArray(jsobject) {
    var array = new Object[jsobject.length];

    for(var i=0;i<jsobject.lenght;i++) {
        array[i] = jsobject[i];
    }

    return array;
}

/**
 * The result object supports the following methods:
 * 
 *  forEachClass  -- calls a callback for each Java Class
 *  forEachObject -- calls a callback for each Java object
 *  findClass -- finds Java Class of given name
 *  findObject -- finds object from given object id
 *  objects -- returns all objects of given class as an enumeration
 *  classes -- returns all classes in the heap as an enumeration
 *  reachables -- returns all objects reachable from a given object
 *  livepaths -- returns an array of live paths because of which an
 *               object alive.
 *  describeRef -- returns description for a reference from a 'from' 
 *              object to a 'to' object.
 */
function wrapHeapSnapshot(heap) {
    function getClazz(clazz) {
        if (clazz == undefined) clazz = "java.lang.Object";
        var type = typeof(clazz);
        if (type == "string") {
            clazz = heap.findClass(clazz);
        } else if (type == "object") {
            clazz = unwrapJavaObject(clazz);
        } else {
            throw "class expected";;
        }
        return clazz;
    }

    snapshot = heap;

    // return heap as a script object with useful methods.
    return {
        snapshot: heap,

        /**
         * Class iteration: Calls callback function for each
         * Java Class in the heap. Default callback function 
         * is 'print'. If callback returns true, the iteration 
         * is stopped.
         *
         * @param callback function to be called.
         */
        forEachClass: function(callback) {
            if (callback == undefined) callback = print;
            var classes = this.snapshot.classes;
            while (classes.hasNext() && !cancelled.get()) {
                var wrapped = wrapJavaObject(classes.next());

                if (wrapped != null && callback(wrapped))
                    return;
            }
        },

        /**
         * Returns an Enumeration of all roots.
         */
        roots: function() {
            return wrapIterator(this.snapshot.roots, true);
        },

        /**
         * Returns an Enumeration for all Java classes.
         */
        classes: function() {
            return wrapIterator(this.snapshot.classes, true);
        },

        /**
         * Object iteration: Calls callback function for each
         * Java Object in the heap. Default callback function 
         * is 'print'.If callback returns true, the iteration 
         * is stopped.
         *
         * @param callback function to be called. 
         * @param clazz Class whose objects are retrieved.
         *        Optional, default is 'java.lang.Object'
         * @param includeSubtypes flag to tell if objects of subtypes
         *        are included or not. optional, default is true.
         */
        forEachObject: function(callback, clazz, includeSubtypes) {
            if (includeSubtypes == undefined) includeSubtypes = true;
            if (callback == undefined) callback = print;
            clazz = getClazz(clazz);

            if (clazz) {
                //                var instances = clazz.getInstances(includeSubtypes); // TODO
                var instances = snapshot.getInstances(clazz, includeSubtypes);
                while (instances.hasNext() && !cancelled.get()) {
                    if (callback(wrapJavaObject(instances.next())))
                        return;
                }
            }
        },

        /** 
         * Returns an enumeration of Java objects in the heap.
         * 
         * @param clazz Class whose objects are retrieved.
         *        Optional, default is 'java.lang.Object'
         * @param includeSubtypes flag to tell if objects of subtypes
         *        are included or not. optional, default is true.
         * @param where (optional) filter expression or function to
         *        filter the objects. The expression has to return true
         *        to include object passed to it in the result array. 
         *        Built-in variable 'it' refers to the current object in 
         *        filter expression.
         */
        objects: function(clazz, includeSubtypes, where) {
            if (includeSubtypes == undefined) includeSubtypes = true;
            if (where) {
                if (typeof(where) == 'string') {
                    where = new Function("it", "return " + where);
                }
            }
            clazz = getClazz(clazz);
            if (clazz) {
                if (where) {
                    return filterIterator(snapshot.getInstances(clazz, includeSubtypes), where, true);
                } else {
                    return wrapIterator(snapshot.getInstances(clazz, includeSubtypes), true);
                }
            } else {
                return emptyEnumeration;
            }
        },

        /**
         * Find Java Class of given name.
         * 
         * @param name class name
         */
        findClass: function(name) {
            var clazz = this.snapshot.findClass(name + '');
            return wrapJavaObject(clazz);
        },

        /**
         * Find Java Object from given object id
         *
         * @param id object id as string
         */
        findObject: function(id) {
            return wrapJavaValue(this.snapshot.findThing(id));
        },

        /**
         * Returns an enumeration of objects in the finalizer
         * queue waiting to be finalized.
         */
        finalizables: function() {
            var tmp = this.snapshot.getFinalizerObjects();
            return wrapperIterator(tmp);
        },
 
        /**
         * Returns an array that contains objects referred from the
         * given Java object directly or indirectly (i.e., all 
         * transitively referred objects are returned).
         *
         * @param jobject Java object whose reachables are returned.
         */
        reachables: function (jobject) {
            return reachables(jobject, this.snapshot.reachableExcludes);
        },

        /**
         * Returns array of paths of references by which the given 
         * Java object is live. Each path itself is an array of
         * objects in the chain of references. Each path supports
         * toHtml method that returns html description of the path.
         *
         * @param jobject Java object whose live paths are returned
         * @param weak flag to indicate whether to include paths with
         *             weak references or not. default is false.
         */
        livepaths: function (jobject, weak) {
            if (weak == undefined) {
                weak = false;
            }

            function wrapRefChain(refChain) {
                var path = new Array();

                // compute path array from refChain
                var tmp = refChain;
                while (tmp != null) {
                    var obj = tmp.obj;
                    path[path.length] = wrapJavaValue(obj);
                    tmp = tmp.next;
                }

                function computeDescription(html) {
                    var root = refChain.obj.root;
                    var desc = root.description;
                    if (root.referer) {
                        var ref = root.referer;
                        desc += " (from " + 
                        (html? toHtml(ref) : ref.toString()) + ')';
                    }
                    desc += '->';
                    var tmp = refChain;
                    while (tmp != null) {
                        var next = tmp.next;
                        var obj = tmp.obj;
                        desc += html? toHtml(obj) : obj.toString();
                        if (next != null) {
                            desc += " (" + 
                            obj.describeReferenceTo(next.obj, heap)  +
                            ") ->";
                        }
                        tmp = next;
                    }
                    return desc;
                }

                return new JSAdapter() {
                    __getIds__ : function() {
                        var res = new Array(path.length);
                        for (var i = 0; i < path.length; i++) {
                            res[i] = i;
                        }
                        return res;
                    },
                    __has__ : function (name) {
                        return (typeof(name) == 'number' &&
                            name >= 0 && name < path.length) ||
                        name == 'length' || name == 'toHtml' ||
                        name == 'toString' || name == 'wrapped-object';
                    },
                    __get__ : function(name) {
                        if (typeof(name) == 'number' &&
                            name >= 0 && name < path.length) {
                            return path[name];
                        } else if (name == 'length') {
                            return path.length;
                        } else if (name == 'toHtml') {
                            return function() { 
                                return computeDescription(true);
                            }
                        } else if (name == 'toString') {
                            return function() {
                                return computeDescription(false);
                            }
                        } else if (name == 'wrapped-object') {
                            return refChain;
                        } else {
                            return undefined;
                        }
                    }
                };
            }

            jobject = unwrapJavaObject(jobject);
            var refChains = this.snapshot.rootsetReferencesTo(jobject, weak);

            var paths = new java.util.Enumeration() {
                counter: 0,
                hasMoreElements: function() {
                    return this.counter < refChains.length
                },
                nextElement: function() {
                    return wrapRefChain(refChains[this.counter++])
                }
            }
            return paths;
        },

        /**
         * Return description string for reference from 'from' object
         * to 'to' Java object.
         *
         * @param from source Java object
         * @param to destination Java object
         */
        describeRef: function (from, to) {
            from = unwrapJavaObject(from);
            to = unwrapJavaObject(to);
            return from.describeReferenceTo(to, this.snapshot);
        }

    };
}

// per-object functions

/**
 * Returns allocation site trace (if available) of a Java object
 *
 * @param jobject object whose allocation site trace is returned
 */
function allocTrace(jobject) {
    try {
        jobject = unwrapJavaObject(jobject);			
        var trace = jobject.allocatedFrom;
        return (trace != null) ? trace.frames : null;
    } catch (e) {
        print("allocTrace: " + jobject + ", " + e);
        return null;
    }
}

/**
 * Returns Class object for given Java object
 *
 * @param jobject object whose Class object is returned
 */
function classof(jobject) {
    jobject = unwrapJavaObject(jobject);
    return wrapJavaValue(jobject.javaClass);
}

/**
 * Find referers (a.k.a in-coming references). Calls callback
 * for each referrer of the given Java object. If the callback 
 * returns true, the iteration is stopped.
 *
 * @param callback function to call for each referer
 * @param jobject object whose referers are retrieved
 */
function forEachReferrer(callback, jobject) {
    //    jobject = unwrapJavaObject(jobject);
    var refs = referrers(jobject);
    while (refs.hasMoreElements() && !cancelled.get()) {
        var referrer = refs.nextElement();
        if (callback(wrapJavaValue(referrer))) {
            return;
        }
    }
}

function forEachReferee(callback, jobject) {
    var refs = referees(jobject);
    while (refs.hasMoreElements() && !cancelled.get()) {
        var referrer = refs.nextElement();
        if (callback(wrapJavaValue(referrer))) {
            return;
        }
    }
}

/**
 * Compares two Java objects for object identity.
 *
 * @param o1, o2 objects to compare for identity
 */
function identical(o1, o2) {
    return objectid(o1) == objectid(o2);
}

/**
 * Returns Java object id as string
 *
 * @param jobject object whose id is returned
 */
function objectid(jobject) {
    try {
        jobject = unwrapJavaObject(jobject);
        if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.Instance) {
            return String(jobject.instanceId);
        } else if (jobject instanceof Packages.org.netbeans.lib.profiler.heap.JavaClass) {
            return String(jobject.javaClassId);
        }
    } catch (e) {
        print("objectid: " + jobject + ", " + e);
        return null;
    }
}

/**
 * Prints allocation site trace of given object
 *
 * @param jobject object whose allocation site trace is returned
 */
function printAllocTrace(jobject) {
    var frames = this.allocTrace(jobject);
    if (frames == null || frames.length == 0) {
        print("allocation site trace unavailable for " + 
            objectid(jobject));
        return;
    }    
    print(objectid(jobject) + " was allocated at ..");
    for (var i in frames) {
        var frame = frames[i];
        var src = frame.sourceFileName;
        if (src == null) src = '<unknown source>';
        print('\t' + frame.className + "." +
            frame.methodName + '(' + frame.methodSignature + ') [' +
            src + ':' + frame.lineNumber + ']');
    }
}

/**
 * Returns an enumeration of referrers of the given Java object.
 *
 * @param jobject Java object whose referrers are returned.
 * @param weak Boolean flag indicating whether to include weak references
 */
function referrers(jobject, weak) {
    try {
        if (weak == undefined) {
            weak = false
        }
        jobject = unwrapJavaObject(jobject);
        return wrapIterator(this.snapshot.getReferrers(jobject, weak));
    } catch (e) {
        println("referrers: " + jobject + ", " + e);
        return emptyEnumeration;
    }
}

/**
 * Returns an array that contains objects referred from the
 * given Java object.
 *
 * @param jobject Java object whose referees are returned.
 * @param weak Boolean flag indicating whether to include weak references
 */
function referees(jobject, weak) {
    try {
        if (weak == undefined) {
            weak = false;
        }
        jobject = unwrapJavaObject(jobject);
        return wrapIterator(this.snapshot.getReferees(jobject, weak));
    } catch (e) {
        println("referees: " + jobject + ", " + e);
        return emptyEnumeration;
    }
}

/**
 * Returns an array that contains objects referred from the
 * given Java object directly or indirectly (i.e., all 
 * transitively referred objects are returned).
 *
 * @param jobject Java object whose reachables are returned.
 * @param excludes optional comma separated list of fields to be 
 *                 removed in reachables computation. Fields are
 *                 written as class_name.field_name form.
 */
function reachables(jobject, excludes) {
    if (excludes == undefined) {
        excludes = null;
    } else if (typeof(excludes) == 'string') {
        var st = new java.util.StringTokenizer(excludes, ",");
        var excludedFields = new Array();
        while (st.hasMoreTokens() && !cancelled.get()) {
            excludedFields[excludedFields.length] = st.nextToken().trim();
        }
        if (excludedFields.length > 0) { 
            excludes = new hatPkg.ReachableExcludes() {
                isExcluded: function (field) {
                    for (var index in excludedFields) {
                        if (field.equals(excludedFields[index])) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        } else {
            // nothing to filter...
            excludes = null;
        }
    } else if (! (excludes instanceof hatPkg.ReachableExcludes)) {
        excludes = null;
    }

    jobject = unwrapJavaObject(jobject);
    var ro = new hatPkg.ReachableObjects(jobject, excludes);  
    return wrapIterator(ro.reachables, true);
}


/**
 * Returns whether 'from' object refers to 'to' object or not.
 *
 * @param from Java object that is source of the reference.
 * @param to Java object that is destination of the reference.
 */
function refers(from, to) {
    try {
        var tmp = unwrapJavaObject(from);
        if (tmp instanceof Packages.org.netbeans.lib.profiler.heap.JavaClass) {
            from = from.statics;
        } else if (tmp instanceof Packages.org.netbeans.lib.profiler.heap.PrimitiveArrayInstance) {
            return false;
        }
        for (var i in from) {
            if (identical(from[i], to)) {
                return true;
            }
        }
    } catch (e) {
        println("refers: " + from + ", " + e);
    }
    return false;
}

/**
 * If rootset includes given jobject, return Root
 * object explanining the reason why it is a root.
 *
 * @param jobject object whose Root is returned
 */
function root(jobject) {
    try {
        jobject = unwrapJavaObject(jobject);
        var root = wrapRoot(snapshot.findRoot(jobject));
        return root;
    } catch (e) {
        return null;
    }
}

/**
 * Returns size of the given Java object
 *
 * @param jobject object whose size is returned
 */
function sizeof(jobject) {
    try {
        jobject = unwrapJavaObject(jobject);
        return jobject.size;
    } catch (e) {
        print("sizeof: " + jobject + ", " + e);
        return null;
    }
}

function rsizeof(jobject) {
    try {
        jobject = unwrapJavaObject(jobject);
        return jobject.retainedSize;
    } catch (e) {
        print("rsizeof: " + jobject + ", " + e);
        return null;
    }
}

/**
 * Returns HTML string for the given object.
 *
 * @param obj object for which HTML string is returned.
 */
function toHtml(obj) {
    if (obj == null) {
        return "null";
    } 

    if (obj == undefined) {
        return "undefined";
    } 

    var tmp = unwrapJavaObject(obj);
    if (tmp != undefined) {
        if (tmp instanceof Packages.org.netbeans.lib.profiler.heap.JavaClass) {
            var id = tmp.javaClassId;
            var name = tmp.name;
            return "<a href='file://class/" + name + "'>class " + name + "</a>";
        }else if (tmp instanceof Packages.org.netbeans.lib.profiler.heap.Instance) {
            var id = tmp.instanceId;
            var number = tmp.instanceNumber;
            var name = tmp.javaClass.name;
            return "<a href='file://instance/" + name +"@" + id + "'>" +
            name + "#" + number + "</a>";
        }
    }
    if ((typeof(obj) == 'object') || (obj instanceof JSAdapter)) {
        if (obj instanceof java.lang.Object) {
            // script wrapped Java object
            obj = wrapIterator(obj);
            // special case for enumeration
            if (obj instanceof java.util.Enumeration) {
                var res = "[ ";
                while (obj.hasMoreElements() && !cancelled.get()) {
                    res += toHtml(obj.nextElement()) + ", ";
                }
                res += "]";
                return res; 
            } else {
                return obj;
            }
        } else if (obj instanceof Array) {
            // script array
            var res = "[ ";
            for (var i in obj) {
                res += toHtml(obj[i]);
                if (i != obj.length - 1) {
                    res += ", ";
                }
                if (cancelled.get()) break;
            } 
            res += " ]";
            return res;
        } else {
            // if the object has a toHtml function property
            // just use that...
            if (typeof(obj.toHtml) == 'function') {
                return obj.toHtml();
            } else {
                // script object
                var res = "{ ";
                for (var i in obj) {
                    res +=  i + ":" + toHtml(obj[i]) + ", ";
                }
                res += "}";
                return res;
            }
        }
    } else {
        // JavaScript primitive value
        return obj.toString().replace("<", "&lt;").replace(">", "&gt;");
    }
}

/*
 * Generic array/iterator/enumeration [or even object!] manipulation 
 * functions. These functions accept an array/iteration/enumeration
 * and expression String or function. These functions iterate each 
 * element of array and apply the expression/function on each element.
 */

// private function to wrap an Iterator as an Enumeration
function wrapIterator(itr, wrap) {
    if (isJsArray(itr)) {
        return itr;
    } else if (itr instanceof java.util.Iterator) {
        return new java.util.Enumeration() {
            hasMoreElements: function() {
                return itr.hasNext() && !cancelled.get();
            },
            nextElement: function() {
                return wrap? wrapJavaValue(itr.next()) : itr.next();
            },
            wrapped: itr
        };
    } else if (itr instanceof java.util.Enumeration) {
        return itr; // already wrapped
    } else if (itr instanceof org.netbeans.lib.profiler.heap.ArrayDump) {
        return wrapJavaObject(itr);
    } else if (itr.constructor == JavaClassProto && !(itr instanceof JSAdapter)) {
        var arr = new Array();
        arr[0] = itr;
        return arr;
    } else {
        return itr;
    }
}

/**
 * Converts an enumeration/iterator/object into an array
 *
 * @param obj enumeration/iterator/object
 * @return array that contains values of enumeration/iterator/object
 */
function toArray(obj) {	
    obj = wrapIterator(obj);
    if (obj instanceof java.util.Enumeration) {
        var res = new Array();
        while (obj.hasMoreElements() && !cancelled.get()) {
            res[res.length] = obj.nextElement();
        }
        return res;
    } else if (obj instanceof Array) {
        return obj;
    } else {
        var res = new Array();
        for (var index in obj) {
            res[res.length] = obj[index];
            if (cancelled.get()) break;
        }
        return res;
    }
}

function top(array, code, num) {
    if (array == undefined) {
        return array;
    }
    var func;
    if (code == undefined) {
        func = function(lhs, rhs) {
            return 1; // first-come order
        }
    } else if (typeof(code) == 'string') {
        func = new Function("lhs", "rhs", "return " + code);
    } else {
        func = code;
    }

    if (num == undefined) {
        num = 10;
    }
    array = wrapIterator(array, true);

    if (array instanceof java.util.Enumeration) {
        var sorted = new Array();

        while(array.hasMoreElements() && !cancelled.get()) {
            var element = array.nextElement();
            if (sorted.length > 0) {
                if (sorted.length >= num && func(element, sorted[num -1]) >=0 ) continue;
            }

            var index = search(sorted, element, true, func);
            for(var counter=Math.min(sorted.length, num - 1);counter > index;counter--) {
                sorted[counter] = sorted[counter - 1];
            }
            sorted[index] = element;
        }
        sorted.length = Math.min(sorted.length, num);
        return sorted;
    } else if (array instanceof Array) {
        var result = array.sort(func);
        result.length = Math.min(result.length, num);
        return result;
    }
    return array;
}

/**
 * Returns whether the given array/iterator/enumeration contains 
 * an element that satisfies the given boolean expression specified 
 * in code. 
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code  expression string or function 
 * @return boolean result
 *
 * The code evaluated can refer to the following built-in variables. 
 *
 * 'it' -> currently visited element
 * 'index' -> index of the current element
 * 'array' -> array that is being iterated
 */
function contains(array, code) {
    array = wrapIterator(array);
    var func = code;
    if (typeof(func) != 'function') {
        func = new Function("it", "index", "array",  "return " + code);
    }

    if (array instanceof java.util.Enumeration) {
        var index = 0;
        while (array.hasMoreElements()) {
            var it = array.nextElement();
            if (func(it, index, array)) {
                return true;
            }
            index++;
        }
    } else {
        for (var index in array) {
            var it = array[index];
            if (func(it, index, array)) {
                return true;
            }
        }
    }
    return false;
}

/**
 * concatenates two arrays/iterators/enumerators.
 *
 * @param array1 array/iterator/enumeration
 * @param array2 array/iterator/enumeration
 *
 * @return concatenated array or composite enumeration
 */
function concat(array1, array2) {
    array1 = wrapIterator(array1);
    array2 = wrapIterator(array2);
    if (array1 instanceof Array && array2 instanceof Array) {
        return array1.concat(array2);
    } else if (array1 instanceof java.util.Enumeration &&
        array2 instanceof java.util.Enumeration) {
        return new Packages.com.sun.tools.hat.internal.util.CompositeEnumeration(array1, array2);
    } else {
        return undefined;
    }
}

/**
 * Returns the number of array/iterator/enumeration elements 
 * that satisfy the given boolean expression specified in code. 
 * The code evaluated can refer to the following built-in variables. 
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code  expression string or function 
 * @return number of elements
 *
 * 'it' -> currently visited element
 * 'index' -> index of the current element
 * 'array' -> array that is being iterated
 */
function count(array, code) {
    if (code == undefined) {
        return length(array);
    }
    array = wrapIterator(array);
    var func = code;
    if (typeof(func) != 'function') {
        func = new Function("it", "index", "array",  "return " + code);
    }

    var result = 0;
    if (array instanceof java.util.Enumeration) {
        var index = 0;
        while (array.hasMoreElements()) {
            var it = array.nextElement();
            if (func(it, index, array)) {
                result++;
            }
            index++;
        }
    } else {
        for (var index in array) {
            var it = array[index];
            if (func(it, index, array)) {
                result++;
            }
        }
    }
    return result;
}

/**
 * filter function returns an array/enumeration that contains 
 * elements of the input array/iterator/enumeration that satisfy 
 * the given boolean expression. The boolean expression code can 
 * refer to the following built-in variables. 
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code  expression string or function 
 * @return array/enumeration that contains the filtered elements
 *
 * 'it' -> currently visited element
 * 'index' -> index of the current element
 * 'array' -> array that is being iterated
 * 'result' -> result array
 */
function filter(array, code) {
    array = wrapIterator(array);
    var func = code;
    if (typeof(code) != 'function') {
        func = new Function("it", "index", "array", "result", "return " + code);
    }
    if (array instanceof java.util.Enumeration) {
        return filterEnumeration(array, func, true);
    } else if (array instanceof java.util.Iterator) {
        return filterIterator(array, func, true);
    } else {
        var result = new Array();
        for (var index in array) {
            var it = array[index];
            if (func(wrapJavaObject(it), index, array, result)) {
                result[result.length] = it;
            }
            if (cancelled.get()) break;
        }
        return result;
    }
}

/**
 * Returns the number of elements of array/iterator/enumeration.
 *
 * @param array input array/iterator/enumeration that is iterated
 */
function length(array) {
    array = wrapIterator(array);
    var length = array.length;

    if (length != undefined) return length;
    
    if (array instanceof java.util.Enumeration) {
        var cnt = 0;
        while (array.hasMoreElements()) {
            array.nextElement(); 
            cnt++;
        }
        return cnt;
    } else {
        var cnt = 0;
        for (var index in array) {
            cnt++;
        }
        return cnt;
    }
}

/**
 * Transforms the given object or array by evaluating given code
 * on each element of the object or array. The code evaluated
 * can refer to the following built-in variables. 
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code  expression string or function 
 * @return array/enumeration that contains mapped values
 *
 * 'it' -> currently visited element
 * 'index' -> index of the current element
 * 'array' -> array that is being iterated
 * 'result' -> result array
 *
 * map function returns an array/enumeration of values created 
 * by repeatedly calling code on each element of the input
 * array/iterator/enumeration.
 */
function map(array, code) {
    array = wrapIterator(array);
    var func = code;
    if(typeof(code) != 'function') {
        func = new Function("it", "index", "array", "result", "return " + code);
    }

    if (array instanceof java.util.Enumeration) {
        var index = 0;
        var result = new java.util.Enumeration() {
            hasMoreElements: function() {
                return array.hasMoreElements();
            },
            nextElement: function() {
                return func(wrapJavaObject(array.nextElement()), index++, array, result);
            }
        };
        return result;
    } else {
        var result = new Array();
        for (var index in array) {
            var it = array[index];
            if (it instanceof java.util.Enumeration) {
                var counter = 0;
                while(it.hasMoreElements() && !cancelled.get()) {
                    result[result.length] = func(wrapJavaObject(it.nextElement()), counter++, it, result);
                }
            } else {
                result[result.length] = func(wrapJavaObject(it), index, array, result);
            }
            if (cancelled.get()) break;
        }
        return result;
    }
}

// private function used by min, max functions
function minmax(array, code) {
    if (typeof(code) == 'string') {
        code = new Function("lhs", "rhs", "return " + code);
    }
    array = wrapIterator(array);
    if (array instanceof java.util.Enumeration) {
        if (! array.hasMoreElements()) {
            return undefined;
        }
        var res = array.nextElement();
        while (array.hasMoreElements() && !cancelled.get()) {
            var next = array.nextElement();
            if (code(next, res)) {
                res = next;
            }
        }
        return res;
    } else {
        if (array.length == 0) {
            return undefined;
        }
        var res = array[0];
        for (var index = 1; index < array.length; index++) {
            if (code(array[index], res)) {
                res = array[index];
            }
            if (cancelled.get()) break;
        } 
        return res;
    }
}

/**
 * Returns the maximum element of the array/iterator/enumeration
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code (optional) comparision expression or function
 *        by default numerical maximum is computed.
 */
function max(array, code) {
    if (code == undefined) {
        code = function (lhs, rhs) { 
            return lhs > rhs;
        }
    }
    return minmax(array, code);
}

/**
 * Returns the minimum element of the array/iterator/enumeration
 *
 * @param array input array/iterator/enumeration that is iterated
 * @param code (optional) comparision expression or function
 *        by default numerical minimum is computed.
 */
function min(array, code) {
    if (code == undefined) {
        code = function (lhs, rhs) { 
            return lhs < rhs;
        }
    } 
    return minmax(array, code);
}

/**
 * sort function sorts the input array. optionally accepts
 * code to compare the elements. If code is not supplied,
 * numerical sort is done.
 *
 * @param array input array/iterator/enumeration that is sorted
 * @param code  expression string or function 
 * @return sorted array 
 *
 * The comparison expression can refer to the following
 * built-in variables:
 *
 * 'lhs' -> 'left side' element
 * 'rhs' -> 'right side' element
 */
function sort(array, code) {
    // we need an array to sort, so convert non-arrays
    array = toArray(array);
    
    // by default use numerical comparison
    var func = code;
    if (code == undefined) {
        func = function(lhs, rhs) { 
            return lhs - rhs;
        };
    } else if (typeof(code) == 'string') {
        func = new Function("lhs", "rhs", "return " + code);
    }
    return array.sort(func);
}

/**
 * Returns the sum of the elements of the array
 *
 * @param array input array that is summed.
 * @param code optional expression used to map
 *        input elements before sum.
 */
function sum(array, code) {
    array = wrapIterator(array);
    if (code != undefined) {
        array = map(array, code);
    }
    var result = 0;
    if (array instanceof java.util.Enumeration) {
        while (array.hasMoreElements() && !cancelled.get()) {
            result += Number(array.nextElement());
        }
    } else {
        for (var index in array) {
            result += Number(array[index]);
            if (cancelled.get()) break;
        }
    }
    return result;
}

/**
 * Returns array of unique elements from the given input 
 * array/iterator/enumeration.
 *
 * @param array from which unique elements are returned.
 * @param code optional expression (or function) giving unique
 *             attribute/property for each element.
 *             by default, objectid is used for uniqueness.
 */
function unique(array, code) {
    array = wrapIterator(array);
    if (code == undefined) {
        code = new Function("it", "var id = objectid(it);return id != undefined ? id : it;");
    } else if (typeof(code) == 'string') {
        code = new Function("it", "return " + code);
    }
    var tmp = new Object();
    if (array instanceof java.util.Enumeration) {
        while (array.hasMoreElements() && !cancelled.get()) {
            var it = array.nextElement();
            tmp[code(it)] = it;
        }
    } else {
        for (var index in array) {
            var it = array[index];
            tmp[code(it)] = it;
            if (cancelled.get()) break;
        }
    }
    var res = new Array();
    for (var index in tmp) {
        res[res.length] = tmp[index];
        if (cancelled.get()) break;
    }
    return res;
}

function printStackTrace() {
    try {
        var c = undefined;
        c.toString();
    } catch (e) {
        e.rhinoException.printStackTrace();
    }
}

function isJsArray(obj) {
    if (obj.constructor == undefined) {
        return false;
    }
    return obj.constructor == Array;
}

function search(a, v, i, func){
    var h = a.length, l = -1, m;
    while(h - l > 1) {
        if(func(a[m = h + l >> 1], v) < 0) l = m;
        else h = m;
        if (cancelled.get()) return -1;
    }
    return a[h] != v ? i ? h : -1 : h;
}
