/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.graalvm.visualvm.lib.jfluid.results;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.ServletRequestCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.SimpleCPUCCTNode;
import org.graalvm.visualvm.lib.jfluid.results.cpu.cct.nodes.ThreadCPUCCTNode;

/**
 * Provides a pluggable implementation of {@linkplain RuntimeCCTNode} hierarchy traversal<br/>
 *
 * @author Jaroslav Bachorik
 */
final public class RuntimeCCTNodeProcessor {
    final private static Logger LOGGER = Logger.getLogger(RuntimeCCTNodeProcessor.class.getName());

    /**
     * A processor plugin definition. <br/>
     * Plugin implementations should be based rather on {@linkplain PluginAdapter}
     */
    public static interface Plugin {
        /**
         * {@linkplain RuntimeCCTNode} hierarchy traversal starts
         */
        void onStart();
        /**
         * {@linkplain RuntimeCCTNode} hierarchy traversal stops
         */
        void onStop();
        /**
         * A node is being processed
         * @param node The node being processed
         */
        void onNode(RuntimeCCTNode node);
        /**
         * A node and all its children have been processed
         * @param node The node having been processed
         */
        void onBackout(RuntimeCCTNode node);
    }

    /**
     * An adapter for {@linkplain Plugin}.<br/>
     * Provides default empty implementations and implements simple dispatching
     * mechanism for typed <b>onNode</b> calls.
     */
    public static abstract class PluginAdapter implements Plugin {
        @Override
        final public void onBackout(RuntimeCCTNode node) {
            if (node instanceof MethodCPUCCTNode) {
                onBackout((MethodCPUCCTNode)node);
            } else if (node instanceof MarkedCPUCCTNode) {
                onBackout((MarkedCPUCCTNode)node);
            } else if (node instanceof ThreadCPUCCTNode) {
                onBackout((ThreadCPUCCTNode)node);
            } else if (node instanceof SimpleCPUCCTNode) {
                onBackout((SimpleCPUCCTNode)node);
            } else if (node instanceof ServletRequestCPUCCTNode) {
                onBackout((ServletRequestCPUCCTNode)node);
            } else {
                LOGGER.log(Level.WARNING, "Can not process uncrecoginzed node class {0}", node.getClass());
            }
        }

        @Override
        final public void onNode(RuntimeCCTNode node) {
            if (node instanceof MethodCPUCCTNode) {
                onNode((MethodCPUCCTNode)node);
            } else if (node instanceof MarkedCPUCCTNode) {
                onNode((MarkedCPUCCTNode)node);
            } else if (node instanceof ThreadCPUCCTNode) {
                onNode((ThreadCPUCCTNode)node);
            } else if (node instanceof SimpleCPUCCTNode) {
                onNode((SimpleCPUCCTNode)node);
            } else if (node instanceof ServletRequestCPUCCTNode) {
                onNode((ServletRequestCPUCCTNode)node);
            } else {
                LOGGER.log(Level.WARNING, "Can not process uncrecoginzed node class {0}", node.getClass());
            }
        }

        @Override
        public void onStart() {
        }

        @Override
        public void onStop() {
        }
        /**
         * @see Plugin#onNode(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onNode(MethodCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onNode(MarkedCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onNode(ThreadCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onNode(SimpleCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onNode(ServletRequestCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onBackout(MethodCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onBackout(MarkedCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onBackout(ThreadCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onBackout(SimpleCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.graalvm.visualvm.lib.jfluid.results.RuntimeCCTNode) 
         */
        protected void onBackout(ServletRequestCPUCCTNode node) {}
    }
    
    private static abstract class Item<T extends RuntimeCCTNode> {
        final protected T instance;
        final protected Plugin[] plugins;
        
        Item(T instance, Plugin ... plugins) {
            this.instance = instance;
            this.plugins = plugins;
        }
        
        abstract void process(int maxMethodId);
    }
    
    private static class SimpleItem extends Item<RuntimeCCTNode> {
        final private Deque<Item<RuntimeCCTNode>> stack;
        SimpleItem(Deque<Item<RuntimeCCTNode>> stack, RuntimeCCTNode instance, Plugin ... plugins) {
            super(instance, plugins);
            this.stack = stack;
        }

        @Override
        void process(int maxMethodId) {
            stack.add(new BackoutItem(instance, plugins));
            for(RuntimeCCTNode n : instance.getChildren()) {
                if (n instanceof MethodCPUCCTNode) {
                    if (((MethodCPUCCTNode)n).getMethodId() >= maxMethodId) continue;
                }
                stack.add(new SimpleItem(stack, n, plugins));
            }
            for(Plugin p : plugins) {
                if (p != null) {
                    p.onNode(instance);
                }
            }
        }
    }
    
    private static class BackoutItem extends Item<RuntimeCCTNode> {
        BackoutItem(RuntimeCCTNode instance, Plugin ... plugins) {
            super(instance, plugins);
        }

        @Override
        void process(int maxMethodId) {
            for(Plugin p : plugins) {
                if (p != null) {
                    p.onBackout(instance);
                }
            }
        }
    }
    
    private RuntimeCCTNodeProcessor() {}
    
    public static void process(RuntimeCCTNode root, Plugin ... plugins) {
        Deque<Item<RuntimeCCTNode>> nodeStack = new ArrayDeque<Item<RuntimeCCTNode>>();
        
        for(Plugin p : plugins) {
            if (p != null) {
                p.onStart();
            }
        }
        nodeStack.push(new SimpleItem(nodeStack, root, plugins));
        int maxMethodId = (root instanceof SimpleCPUCCTNode) ? ((SimpleCPUCCTNode)root).getMaxMethodId() : Integer.MAX_VALUE;
        processStack(maxMethodId, nodeStack, plugins);
        for(Plugin p : plugins) {
            if (p != null) {
                p.onStop();
            }
        }
    }
    
    private static void processStack(int maxMethodId, Deque<Item<RuntimeCCTNode>> stack, Plugin ... plugins) {
        while (!stack.isEmpty()) {
            Item<RuntimeCCTNode> item = stack.pollLast();
            if (item != null) {
                item.process(maxMethodId);
            }
        }
    }
}
