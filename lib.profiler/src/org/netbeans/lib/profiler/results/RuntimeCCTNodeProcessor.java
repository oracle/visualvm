/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing the
 * software, include this License Header Notice in each file and include the
 * License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by Oracle
 * in the GPL Version 2 section of the License file that accompanied this code.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or only
 * the GPL Version 2, indicate your decision by adding "[Contributor] elects to
 * include this software in this distribution under the [CDDL or GPL Version 2]
 * license." If you do not indicate a single choice of license, a recipient has
 * the option to distribute your version of this file under either the CDDL, the
 * GPL Version 2 or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.lib.profiler.results;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MarkedCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.MethodCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ServletRequestCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.SimpleCPUCCTNode;
import org.netbeans.lib.profiler.results.cpu.cct.nodes.ThreadCPUCCTNode;

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
         * @see Plugin#onNode(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onNode(MethodCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onNode(MarkedCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onNode(ThreadCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onNode(SimpleCPUCCTNode node) {}
        /**
         * @see Plugin#onNode(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onNode(ServletRequestCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onBackout(MethodCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onBackout(MarkedCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onBackout(ThreadCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onBackout(SimpleCPUCCTNode node) {}
        /**
         * @see Plugin#onBackout(org.netbeans.lib.profiler.results.RuntimeCCTNode) 
         */
        protected void onBackout(ServletRequestCPUCCTNode node) {}
    }
    
    private static abstract class Item<T extends RuntimeCCTNode> {
        final protected T instance;
        final protected Plugin[] plugins;
        
        public Item(T instance, Plugin ... plugins) {
            this.instance = instance;
            this.plugins = plugins;
        }
        
        abstract void process(int maxMethodId);
    }
    
    private static class SimpleItem extends Item<RuntimeCCTNode> {
        final private Deque<Item<RuntimeCCTNode>> stack;
        public SimpleItem(Deque<Item<RuntimeCCTNode>> stack, RuntimeCCTNode instance, Plugin ... plugins) {
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
        public BackoutItem(RuntimeCCTNode instance, Plugin ... plugins) {
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
