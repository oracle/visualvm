/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.profiler.utilities.trees;

import junit.framework.TestCase;
import org.netbeans.modules.profiler.utilities.trees.NodeFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;


/**
 *
 * @author Jaroslav Bachorik
 */
public class TreeDecimatorTest extends TestCase {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private DefaultMutableTreeNode treeRoot;
    private TreeDecimator<DefaultMutableTreeNode> decimator = new TreeDecimator<DefaultMutableTreeNode>() {
        @Override
        protected List<DefaultMutableTreeNode> getChildren(DefaultMutableTreeNode aNode) {
            List<DefaultMutableTreeNode> children = new ArrayList<DefaultMutableTreeNode>();
            Enumeration<MutableTreeNode> childrenEnmu = aNode.children();

            while (childrenEnmu.hasMoreElements()) {
                children.add((DefaultMutableTreeNode) childrenEnmu.nextElement());
            }

            return children;
        }

        @Override
        protected void attachChildren(DefaultMutableTreeNode aNode, List<DefaultMutableTreeNode> children) {
            for (DefaultMutableTreeNode child : children) {
                aNode.setParent(aNode);
            }
        }

        @Override
        protected void detachChild(DefaultMutableTreeNode aNode, DefaultMutableTreeNode child) {
            aNode.remove(child);
        }

        @Override
        protected void detachChildren(DefaultMutableTreeNode aNode) {
            aNode.removeAllChildren();
        }
    };


    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public TreeDecimatorTest(String testName) {
        super(testName);
    }

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    /**
     * Test of decimate method, of class TreeDecimator.
     */
    public void testDecimateRoot() {
        System.out.println("decimate Root");

        NodeFilter<DefaultMutableTreeNode> filter = new NodeFilter() {
            public boolean match(Object node) {
                return node.toString().equals("A");
            }

            public boolean maymatch(Object node) {
                return "A".startsWith(node.toString());
            }
        };

        DefaultMutableTreeNode result = decimator.decimate(treeRoot, filter);
        assertEquals("A", result.toString());
        assertEquals(2, result.getChildCount());
    }
    
    public void testDecimateAB() {
        System.out.println("decimate AB");

        NodeFilter<DefaultMutableTreeNode> filter = new NodeFilter() {
            public boolean match(Object node) {
                return node.toString().equals("AB");
            }

            public boolean maymatch(Object node) {
                return "AB".startsWith(node.toString());
            }
        };

        DefaultMutableTreeNode result = decimator.decimate(treeRoot, filter);
        assertEquals("AB", result.toString());
        assertEquals(0, result.getChildCount());
    }
    
    public void testDecimateAC() {
        System.out.println("decimate AC");

        NodeFilter<DefaultMutableTreeNode> filter = new NodeFilter() {
            public boolean match(Object node) {
                return node.toString().equals("AC");
            }

            public boolean maymatch(Object node) {
                return "AC".startsWith(node.toString());
            }
        };

        DefaultMutableTreeNode result = decimator.decimate(treeRoot, filter);
        assertEquals("AC", result.toString());
        assertEquals(2, result.getChildCount());
    }
    
    public void testDecimateACA() {
        System.out.println("decimate ACA");

        NodeFilter<DefaultMutableTreeNode> filter = new NodeFilter() {
            public boolean match(Object node) {
                return node.toString().equals("ACA");
            }

            public boolean maymatch(Object node) {
                return "ACA".startsWith(node.toString());
            }
        };

        DefaultMutableTreeNode result = decimator.decimate(treeRoot, filter);
        assertEquals("ACA", result.toString());
        assertEquals(0, result.getChildCount());
    }
    
    public void testDecimateACAB() {
        System.out.println("decimate ACAB");

        NodeFilter<DefaultMutableTreeNode> filter = new NodeFilter() {
            public boolean match(Object node) {
                return node.toString().equals("ACAB");
            }

            public boolean maymatch(Object node) {
                return "ACAB".startsWith(node.toString());
            }
        };

        DefaultMutableTreeNode result = decimator.decimate(treeRoot, filter);
        assertEquals("ACAB", result.toString());
        assertEquals(0, result.getChildCount());
    }

    @Override
    protected void setUp() throws Exception {
        treeRoot = new DefaultMutableTreeNode("A");

        DefaultMutableTreeNode nodeB = new DefaultMutableTreeNode("AB");
        DefaultMutableTreeNode nodeC = new DefaultMutableTreeNode("AC");
        DefaultMutableTreeNode nodeCA = new DefaultMutableTreeNode("ACA");
        DefaultMutableTreeNode nodeCB = new DefaultMutableTreeNode("ACAB");

        treeRoot.add(nodeB);
        treeRoot.add(nodeC);
        nodeC.add(nodeCA);
        nodeC.add(nodeCB);
    }

    @Override
    protected void tearDown() throws Exception {
        treeRoot = null;
    }
}
