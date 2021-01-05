/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * 
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 * 
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.visualvm.heapviewer.truffle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graalvm.visualvm.lib.jfluid.heap.FieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.GCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.Heap;
import org.graalvm.visualvm.lib.jfluid.heap.Instance;
import org.graalvm.visualvm.lib.jfluid.heap.JavaClass;
import org.graalvm.visualvm.lib.jfluid.heap.JavaFrameGCRoot;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ObjectFieldValue;
import org.graalvm.visualvm.lib.jfluid.heap.PrimitiveArrayInstance;
import org.graalvm.visualvm.lib.jfluid.heap.ThreadObjectGCRoot;
import org.graalvm.visualvm.lib.profiler.heapwalk.details.spi.DetailsUtils;

/**
 *
 * @author Tomas Hurka
 */
public class TruffleStackTraces {

    private Collection<StackTrace> truffleStackTraces;

    public TruffleStackTraces(Heap heap) {
        DefaultTruffleRuntime defaultImpl = new DefaultTruffleRuntime(heap);

        if (defaultImpl.isDefaultTruffleRuntime()) {
            truffleStackTraces = defaultImpl.getStackTraces();
        }

        HotSpotTruffleRuntime hotSpotImpl = new HotSpotTruffleRuntime(heap);

        if (hotSpotImpl.isHotSpotTruffleRuntime()) {
            truffleStackTraces = hotSpotImpl.getStackTraces();
        }
    }

    public Collection<StackTrace> getStackTraces() {
        return truffleStackTraces;
    }

    public abstract static class StackTrace {

        private final Instance thread;
        final Heap heap;

        private StackTrace(Heap h, Instance t) {
            heap = h;
            thread = t;
        }

        public Instance getThread() {
            return thread;
        }

        public abstract List<Frame> getFrames();
    }

    public static class Frame {

        private final String name;
        private final List<FieldValue> fieldValues;

        private Frame(Heap heap, Instance callTarget, TruffleFrame localFrame) {
            name = getFrameName(callTarget, heap);
            fieldValues = localFrame.getFieldValues();
        }

        private Frame(Heap heap, Instance callTarget, Instance localFrame) {
            name = getFrameName(callTarget, heap);
            fieldValues = new TruffleFrame(localFrame).getFieldValues();
        }

        public String getName() {
            return name;
        }

        public List<FieldValue> getFieldValues() {
            return fieldValues;
        }

        private static String getFrameName(Instance callTarget, Heap heap) {
            Instance rootNode = (Instance) callTarget.getValueOfField("rootNode"); // NOI18N

            if (rootNode != null) {
                String name = DetailsUtils.getInstanceString(rootNode, heap);
                Instance sourceSection = getSourceSection(rootNode);
                if (sourceSection != null) {
                    Instance source = (Instance) sourceSection.getValueOfField("source"); // NOI18N
                    if (source != null) {
                        String fileName = getFileName(source, heap);
                        return name+" ("+fileName+":"+getLineNumber(sourceSection, source)+")"; // NOI18N
                    }
                }
                return name;
            }
            return DetailsUtils.getInstanceString(callTarget, heap);
        }
    }

    private static String getFileName(Instance source, Heap heap) {
        Object key = source.getValueOfField("key"); // NOI18N
        if (key instanceof Instance) {
            source = (Instance) key;
        }
        String fileName = DetailsUtils.getInstanceFieldString(source, "name", heap); // NOI18N
        int slash = fileName.lastIndexOf('/'); // NOI18N

        if (slash != -1) {
            fileName = fileName.substring(slash+1);
        }
        return fileName;
    }

    private static Instance getSourceSection(Instance rootNode) {
        if (rootNode == null) return null;
        for (Object fv : rootNode.getFieldValues()) {
            FieldValue fieldVal  = (FieldValue) fv;

            if ("sourceSection".equals(fieldVal.getField().getName())) { // NOI18N
                Instance sc = ((ObjectFieldValue)fieldVal).getInstance();

                if (sc != null) {
                    return sc;
                }
            }
        }
        return null;
    }

    private static String getLineNumber(Instance sourceSection, Instance source) {
        Integer charIndex = (Integer) sourceSection.getValueOfField("charIndex"); // NOI18N
        Instance textmap = (Instance) source.getValueOfField("textMap"); // NOI18N
        if (textmap != null) {
            PrimitiveArrayInstance nlOffsets = (PrimitiveArrayInstance) textmap.getValueOfField("nlOffsets"); // NOI18N
            List vals = nlOffsets.getValues();

            for (int i=0; i<vals.size(); i++) {
                Integer off = Integer.valueOf((String)vals.get(i));

                if (off>=charIndex) {
                    return String.valueOf(i);
                }
            }
            return String.valueOf(vals.size());
        }
        return "0"; // NOI18N
    }

    private static Instance getSingleton(String javaClass, Heap heap) {
        JavaClass jcls = heap.getJavaClassByName(javaClass);

        if (jcls != null) {
            Collection<JavaClass> subClasses = jcls.getSubClasses();
            subClasses.add(jcls);

            for (JavaClass jc : subClasses) {
                List instances = jc.getInstances();

                if (instances.size() == 1) {
                    return (Instance) instances.get(0);
                }
            }
        }
        return null;
    }

    // implementation for DefaultTruffleRuntime
    private static class DefaultTruffleRuntime {

        private static final String TRUFFLE_RUNTIME_FQN = "com.oracle.truffle.api.impl.DefaultTruffleRuntime"; // NOI18N
        private static final String THREAD_FQN = "java.lang.Thread"; // NOI18N

        private Collection<StackTrace> truffleStackTraces;

        private DefaultTruffleRuntime(Heap heap) {
            Instance runtime = getSingleton(TRUFFLE_RUNTIME_FQN, heap);

            if (runtime != null) {
                Instance stackTraces = (Instance) runtime.getValueOfField("stackTraces"); // NOI18N

                truffleStackTraces = getStackTraces(heap, stackTraces);
            }
        }

        private boolean isDefaultTruffleRuntime() {
            return truffleStackTraces != null;
        }

        private Collection<StackTrace> getStackTraces() {
            return truffleStackTraces;
        }

        private Collection<StackTrace> getStackTraces(Heap heap, Instance stackTraces) {
            Collection<StackTrace> traces = new ArrayList();
            JavaClass threadCls = heap.getJavaClassByName(THREAD_FQN);
            Collection<JavaClass> allThreadCls = threadCls.getSubClasses();

            allThreadCls.add(threadCls);

            for (JavaClass threadSubCls : allThreadCls) {
                List<Instance> threads = threadSubCls.getInstances();

                for (Instance thread : threads) {
                    Instance topFrame = getTruffleFrameInstance(thread, stackTraces);

                    if (topFrame != null) {
                        traces.add(new DefaultStackTrace(heap, thread, topFrame));
                    }
                }
            }
            return Collections.unmodifiableCollection(traces);
        }

        private Instance getTruffleFrameInstance(Instance thread, Instance stackTraces) {
            Instance threadLocals = (Instance) thread.getValueOfField("threadLocals"); // NOI18N

            if (threadLocals != null) {
                List<Instance> mapTable = getObjectArray(threadLocals, "table"); // NOI18N

                return searchTable(mapTable, stackTraces);
            }
            return null;
        }

        private Instance searchTable(List<Instance> entries, Instance item) {
            for (Instance entry : entries) {
                for (; entry != null; entry = (Instance) entry.getValueOfField("next")) { // NOI18N
                    Instance key = (Instance) entry.getValueOfField("referent"); // NOI18N

                    if (item.equals(key)) {
                        return (Instance) entry.getValueOfField("value"); // NOI18N
                    }
                }
            }
            return null;
        }

        private List<Instance> getObjectArray(Instance instance, String field) {
            Object localsInst = instance.getValueOfField(field);

            if (localsInst instanceof ObjectArrayInstance) {
                return ((ObjectArrayInstance) localsInst).getValues();
            }
            return null;
        }
    }

    private static class DefaultStackTrace extends StackTrace {

        private final Instance topFrame;
        private List<Frame> frames;

        private DefaultStackTrace(Heap h, Instance t, Instance f) {
            super(h, t);
            topFrame = f;
        }

        public synchronized List<Frame> getFrames() {
            if (frames == null) {
                Instance frame = topFrame;
                frames = new ArrayList();

                do {
                    frames.add(new DefaultFrame(heap, frame));
                    frame = (Instance) frame.getValueOfField("callerFrame"); // NOI18N
                } while (frame != null);
            }
            return frames;
        }
    }

    private static class DefaultFrame extends Frame {

        private DefaultFrame(Heap heap, Instance frame) {
            super(heap,
                    (Instance) frame.getValueOfField("target"), // NOI18N
                    (Instance) frame.getValueOfField("frame") // NOI18N
            );
        }
    }

    // implementation for org.graalvm.compiler.truffle.hotspot.HotSpotTruffleRuntime
    private static class HotSpotTruffleRuntime {

        private static final String HOTSPOT_TRUFFLE_RUNTIME_FQN = "org.graalvm.compiler.truffle.hotspot.HotSpotTruffleRuntime"; // NOI18N
        private static final String HOTSPOT_TRUFFLE_RUNTIME1_FQN = "org.graalvm.compiler.truffle.runtime.hotspot.HotSpotTruffleRuntime"; // NOI18N
        private static final String GRAAL_TRUFFLE_RUNTIME_FQN = "org.graalvm.compiler.truffle.runtime.GraalTruffleRuntime"; // NOI18N

        private static final String DEFAULT_CALL_TARGET_FQN = "com.oracle.truffle.api.impl.DefaultCallTarget";   // NOI18N
        private static final String OPTIMIZED_CALL_TARGET_FQN = "org.graalvm.compiler.truffle.OptimizedCallTarget"; //NOI18N
        private static final String ENT_OPTIMIZED_CALL_TARGET_FQN = "com.oracle.graal.truffle.OptimizedCallTarget"; // NOI18N

        private static final String OPTIMIZED_CALL_TARGET1_FQN = "org.graalvm.compiler.truffle.runtime.OptimizedCallTarget"; // NOI18N
        private static final String OPTIMIZED_CALL_TARGET2_FQN = "org.graalvm.compiler.truffle.runtime.hotspot.HotSpotOptimizedCallTarget";    // NOI18N

        private Collection<StackTrace> truffleStackTraces;
        private Instance hotSpotRuntime;
        private Heap heap;

        private HotSpotTruffleRuntime(Heap h) {
            heap = h;
            hotSpotRuntime = getSingleton(HOTSPOT_TRUFFLE_RUNTIME_FQN, heap);
            if (hotSpotRuntime == null) {
                hotSpotRuntime = getSingleton(HOTSPOT_TRUFFLE_RUNTIME1_FQN, heap);
            }
            if (hotSpotRuntime == null) {
                hotSpotRuntime = getSingleton(GRAAL_TRUFFLE_RUNTIME_FQN, heap);
            }
        }

        private boolean isHotSpotTruffleRuntime() {
            return hotSpotRuntime != null && FrameVisitor.getFrameClass(heap) != null;
        }

        private void computeStackTrace(Heap heap, FrameVisitor visitor) {
            Collection<GCRoot> roots = heap.getGCRoots();
            Map<ThreadObjectGCRoot, Map<Integer, List<JavaFrameGCRoot>>> javaFrameMap = computeJavaFrameMap(roots);
            for (GCRoot root : roots) {
                if (root.getKind().equals(GCRoot.THREAD_OBJECT)) {
                    ThreadObjectGCRoot threadRoot = (ThreadObjectGCRoot) root;
                    Instance threadInstance = threadRoot.getInstance();

                    if (threadInstance != null) {
                        StackTraceElement stack[] = threadRoot.getStackTrace();

                        if (stack != null) {
                            Map<Integer, List<JavaFrameGCRoot>> localsMap = javaFrameMap.get(threadRoot);

                            if (localsMap != null) {
                                HotSpotStackTrace hsStackTrace = new HotSpotStackTrace(heap, threadInstance);
                                for (int i = 0; i < stack.length; i++) {
                                    StackTraceElement stackElement = stack[i];
                                    List<JavaFrameGCRoot> locals = localsMap.get(Integer.valueOf(i));
                                    Frame frame = visitor.visitFrame(stackElement, locals);

                                    if (frame != null) {
                                        hsStackTrace.addFrame(frame);
                                    }
                                }
                                if (!hsStackTrace.getFrames().isEmpty()) {
                                    truffleStackTraces.add(hsStackTrace);
                                }
                            }
                        }
                    }
                }
            }
        }

        private Map<ThreadObjectGCRoot, Map<Integer, List<JavaFrameGCRoot>>> computeJavaFrameMap(Collection<GCRoot> roots) {
            Map<ThreadObjectGCRoot, Map<Integer, List<JavaFrameGCRoot>>> javaFrameMap = new HashMap();

            for (GCRoot root : roots) {
                if (GCRoot.JAVA_FRAME.equals(root.getKind())) {
                    JavaFrameGCRoot frameGCroot = (JavaFrameGCRoot) root;
                    ThreadObjectGCRoot threadObj = frameGCroot.getThreadGCRoot();
                    Integer frameNo = Integer.valueOf(frameGCroot.getFrameNumber());
                    Map<Integer, List<JavaFrameGCRoot>> stackMap = javaFrameMap.get(threadObj);
                    List<JavaFrameGCRoot> locals;

                    if (stackMap == null) {
                        stackMap = new HashMap();
                        javaFrameMap.put(threadObj, stackMap);
                    }
                    locals = stackMap.get(frameNo);
                    if (locals == null) {
                        locals = new ArrayList(2);
                        stackMap.put(frameNo, locals);
                    }
                    locals.add(frameGCroot);
                }
            }
            return javaFrameMap;
        }

        private Instance findLocalInstance(List<JavaFrameGCRoot> locals, String... classes) {
            if (locals != null) {
                for (JavaFrameGCRoot local : locals) {
                    Instance i = local.getInstance();
                    String className = i.getJavaClass().getName();

                    for (String cls : classes) {
                        if (cls.equals(className)) {
                            return i;
                        }
                    }
                }
            }
            return null;
        }

        private TruffleFrame findLocalFrame(List<JavaFrameGCRoot> locals) {
            if (locals != null) {
                for (JavaFrameGCRoot local : locals) {
                    Instance i = local.getInstance();
                    TruffleFrame localFrame = new TruffleFrame(i);

                    if (localFrame.isTruffleFrame()) {
                        return localFrame;
                    }
                }
            }
            return null;
        }

        private Frame visitFrame(List<JavaFrameGCRoot> callTargetFrame, List<JavaFrameGCRoot> callNodeFrame) {
            Instance callTarget = findLocalInstance(callTargetFrame,
                                    DEFAULT_CALL_TARGET_FQN, OPTIMIZED_CALL_TARGET_FQN, ENT_OPTIMIZED_CALL_TARGET_FQN,
                                    OPTIMIZED_CALL_TARGET1_FQN, OPTIMIZED_CALL_TARGET2_FQN);
            TruffleFrame localFrame = findLocalFrame(callTargetFrame);
            if (localFrame == null)
                localFrame = findLocalFrame(callNodeFrame);

            if (callTarget != null && localFrame != null) {
                return new Frame(heap, callTarget, localFrame);
            }
            callTarget = findLocalInstance(callNodeFrame,
                                    DEFAULT_CALL_TARGET_FQN, OPTIMIZED_CALL_TARGET_FQN, ENT_OPTIMIZED_CALL_TARGET_FQN,
                                    OPTIMIZED_CALL_TARGET1_FQN, OPTIMIZED_CALL_TARGET2_FQN);
            localFrame = findLocalFrame(callTargetFrame);
            if (callTarget != null && localFrame != null) {
                return new Frame(heap, callTarget, localFrame);
            }
            return null;
        }

        private synchronized Collection<StackTrace> getStackTraces() {
            if (isHotSpotTruffleRuntime() && truffleStackTraces == null) {
                FrameVisitor visitor = new FrameVisitor(this, heap, 0);

                truffleStackTraces = new ArrayList();

                computeStackTrace(heap, visitor);
            }
            return truffleStackTraces;
        }

    }

    private static class HotSpotStackTrace extends StackTrace {

        List<Frame> frames;

        public HotSpotStackTrace(Heap h, Instance t) {
            super(h, t);
            frames = new ArrayList();
        }

        @Override
        public List<Frame> getFrames() {
            return frames;
        }

        private void addFrame(Frame frame) {
            frames.add(frame);
        }
    }

    private static class JavaMethod {

        private final String className;
        private final String methodName;
        private final String signature;

        private JavaMethod(Heap heap, JavaClass frameClass, String field) {
            this(heap, (Instance) frameClass.getValueOfStaticField(field));
        }

        private JavaMethod(Heap heap, Instance method) {
            if (method != null) {
                Instance javaClass = (Instance) method.getValueOfField("clazz");   // NOI18N
                className = heap.getJavaClassByID(javaClass.getInstanceId()).getName();
                methodName = DetailsUtils.getInstanceFieldString(method, "name", heap); // NOI18N
                signature = DetailsUtils.getInstanceFieldString(method, "signature", heap); // NOI18N
            } else {
                className = null;
                methodName = null;
                signature = null;
            }
        }

        private boolean isMethod(StackTraceElement frame) {
            return frame.getClassName().equals(className) && frame.getMethodName().equals(methodName);
        }
    }

    private static final class FrameVisitor {

        private static final String GRAAL_FRAME_INSTANCE_FQN = "org.graalvm.compiler.truffle.GraalFrameInstance"; // NOI18N
        private static final String GRAAL_FRAME_INSTANCE1_FQN = "org.graalvm.compiler.truffle.runtime.GraalFrameInstance";  // NOI18N

        private final HotSpotTruffleRuntime visitor;
        private final JavaMethod callOSRMethod;
        private final JavaMethod callTargetMethod;

        private final JavaMethod callNodeMethod;

        private final JavaMethod callDirectMethod;
        private final JavaMethod callIndirectMethod;
        private final JavaMethod callInlinedMethod;
        private final JavaMethod callInlinedAgnosticMethod;
        private final JavaMethod callInliningForcedMethod;
        private int skipFrames;
        private List<JavaFrameGCRoot> callNodeFrame;

        FrameVisitor(HotSpotTruffleRuntime visitor, Heap heap, int skip) {
            this.visitor = visitor;
            JavaClass frameClass = getFrameClass(heap);
            callOSRMethod = new JavaMethod(heap, frameClass, "CALL_OSR_METHOD");  // NOI18N
            callTargetMethod = new JavaMethod(heap, frameClass, "CALL_TARGET_METHOD");  // NOI18N

            callNodeMethod = new JavaMethod(heap, frameClass, "CALL_NODE_METHOD");  // NOI18N

            callDirectMethod = new JavaMethod(heap, frameClass, "CALL_DIRECT");  // NOI18N
            callIndirectMethod = new JavaMethod(heap, frameClass, "CALL_INDIRECT");  // NOI18N
            callInlinedMethod = new JavaMethod(heap, frameClass, "CALL_INLINED");  // NOI18N
            callInlinedAgnosticMethod = new JavaMethod(heap, frameClass, "CALL_INLINED_AGNOSTIC");  // NOI18N
            callInliningForcedMethod = new JavaMethod(heap, frameClass, "CALL_INLINED_FORCED");  // NOI18N
            skipFrames = skip;
        }

        private static JavaClass getFrameClass(Heap heap) {
            JavaClass frameClass = heap.getJavaClassByName(GRAAL_FRAME_INSTANCE_FQN);

            if (frameClass == null) {
                frameClass = heap.getJavaClassByName(GRAAL_FRAME_INSTANCE1_FQN);
            }
            return frameClass;
        }

        private Frame visitFrame(StackTraceElement frame, List<JavaFrameGCRoot> locals) {
            if (callOSRMethod.isMethod(frame)) {
                // we ignore OSR frames.
                skipFrames++;
            } else if (callTargetMethod.isMethod(frame)) {
                try {
                    if (skipFrames == 0) {
                        return visitor.visitFrame(locals, callNodeFrame);
                    } else {
                        skipFrames--;
                    }
                } finally {
                    callNodeFrame = null;
                }
            } else if (callNodeMethod.isMethod(frame) || callDirectMethod.isMethod(frame) ||
                    callIndirectMethod.isMethod(frame) || callInlinedMethod.isMethod(frame) ||
                    callInlinedAgnosticMethod.isMethod(frame) || callInliningForcedMethod.isMethod(frame)) {
                callNodeFrame = locals;
            }
            return null;
        }
    }
}
