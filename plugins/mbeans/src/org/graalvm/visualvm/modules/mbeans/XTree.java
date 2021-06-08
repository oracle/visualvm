/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.modules.mbeans;

import java.util.*;
import javax.management.*;
import javax.swing.*;
import javax.swing.tree.*;
import org.graalvm.visualvm.modules.mbeans.options.GlobalPreferences;
import static org.graalvm.visualvm.modules.mbeans.XNodeInfo.Type;

@SuppressWarnings("serial")
class XTree extends JTree {

    private List<String> orderedKeyPropertyList;
    private MBeansTab mbeansTab;
    private Map<String, DefaultMutableTreeNode> nodes =
            new HashMap<String, DefaultMutableTreeNode>();

    public XTree(MBeansTab mbeansTab) {
        super(new DefaultMutableTreeNode("MBeanTreeRootNode")); // NOI18N
        this.mbeansTab = mbeansTab;
        setRootVisible(false);
        setShowsRootHandles(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        orderedKeyPropertyList = getOrderedKeyPropertyList();
    }

    /**
     * This method removes the node from its parent
     */
    // Call on EDT
    private synchronized void removeChildNode(DefaultMutableTreeNode child) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        model.removeNodeFromParent(child);
    }

    /**
     * This method adds the child to the specified parent node
     * at specific index.
     */
    // Call on EDT
    private synchronized void addChildNode(
            DefaultMutableTreeNode parent,
            DefaultMutableTreeNode child,
            int index) {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        boolean isRootLeaf = (parent == model.getRoot()) && parent.isLeaf();
        model.insertNodeInto(child, parent, index);
        // Make the root node's children visible if the
        // parent node is the root node and is a leaf
        if (isRootLeaf) {
            model.nodeStructureChanged(parent);
        }
    }

    /**
     * This method adds the child to the specified parent node.
     * The index where the child is to be added depends on the
     * child node being Comparable or not. If the child node is
     * not Comparable then it is added at the end, i.e. right
     * after the current parent's children.
     */
    // Call on EDT
    private synchronized void addChildNode(
            DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        int childCount = parent.getChildCount();
        if (childCount == 0) {
            addChildNode(parent, child, 0);
            return;
        }
        if (child instanceof ComparableDefaultMutableTreeNode) {
            ComparableDefaultMutableTreeNode comparableChild =
                    (ComparableDefaultMutableTreeNode) child;
            for (int i = childCount - 1; i >= 0; i--) {
                DefaultMutableTreeNode brother =
                        (DefaultMutableTreeNode) parent.getChildAt(i);
                // "child >= brother", add child after brother
                if (comparableChild.compareTo(brother) >= 0) {
                    addChildNode(parent, child, i + 1);
                    return;
                }
            }
            // "child < all brothers", add at the beginning
            addChildNode(parent, child, 0);
            return;
        }
        // "child not comparable", add at the end
        addChildNode(parent, child, childCount);
    }

    /**
     * This method removes all the displayed nodes from the tree,
     * but does not affect actual MBeanServer contents.
     */
    // Call on EDT
    @Override
    public synchronized void removeAll() {
        DefaultTreeModel model = (DefaultTreeModel) getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        root.removeAllChildren();
        model.nodeStructureChanged(root);
        nodes.clear();
    }

    // Call on EDT
    public synchronized void removeMBeanFromView(ObjectName mbean) {
        // We assume here that MBeans are removed one by one (on MBean
        // unregistered notification). Deletes the tree node associated
        // with the given MBean and recursively all the node parents
        // which are leaves and non XMBean.
        //
        DefaultMutableTreeNode node = null;
        Dn dn = new Dn(mbean, orderedKeyPropertyList);
        if (dn.getTokenCount() > 0) {
            DefaultTreeModel model = (DefaultTreeModel) getModel();
            Token token = dn.getToken(0);
            String hashKey = dn.getHashKey(token);
            node = nodes.get(hashKey);
            if ((node != null) && (!node.isRoot())) {
                if (node.getChildCount() > 0) {
                    String label = token.getValue();
                    XNodeInfo userObject = new XNodeInfo(
                            Type.NONMBEAN, label,
                            label, token.getTokenValue());
                    changeNodeValue(node, userObject);
                } else {
                    DefaultMutableTreeNode parent =
                            (DefaultMutableTreeNode) node.getParent();
                    model.removeNodeFromParent(node);
                    nodes.remove(hashKey);
                    removeParentFromView(dn, 1, parent);
                }
            }
        }
    }

    /**
     * Removes only the parent nodes which are non MBean and leaf.
     * This method assumes the child nodes have been removed before.
     */
    // Call on EDT
    private DefaultMutableTreeNode removeParentFromView(
            Dn dn, int index, DefaultMutableTreeNode node) {
        if ((!node.isRoot()) && node.isLeaf() &&
                (!(((XNodeInfo) node.getUserObject()).getType().equals(Type.MBEAN)))) {
            DefaultMutableTreeNode parent =
                    (DefaultMutableTreeNode) node.getParent();
            removeChildNode(node);
            String hashKey = dn.getHashKey(dn.getToken(index));
            nodes.remove(hashKey);
            removeParentFromView(dn, index + 1, parent);
        }
        return node;
    }

    // Call on EDT
    public synchronized void addMBeansToView(Set<ObjectName> mbeans) {
        Set<Dn> dns = new TreeSet<Dn>();
        for (ObjectName mbean : mbeans) {
            Dn dn = new Dn(mbean, orderedKeyPropertyList);
            dns.add(dn);
        }
        for (Dn dn : dns) {
            ObjectName mbean = dn.getObjectName();
            XMBean xmbean = new XMBean(mbean, mbeansTab);
            addMBeanToView(mbean, xmbean, dn);
        }
    }

    // Call on EDT
    public synchronized void addMBeanToView(ObjectName mbean) {
        // Build XMBean for the given MBean
        //
        XMBean xmbean = new XMBean(mbean, mbeansTab);
        // Build Dn for the given MBean
        //
        Dn dn = new Dn(mbean, orderedKeyPropertyList);
        // Add the new nodes to the MBean tree from leaf to root
        //
        addMBeanToView(mbean, xmbean, dn);
    }

    // Call on EDT
    private synchronized void addMBeanToView(
            ObjectName mbean, XMBean xmbean, Dn dn) {

        DefaultMutableTreeNode childNode = null;
        DefaultMutableTreeNode parentNode = null;

        // Add the node or replace its user object if already added
        //
        Token token = dn.getToken(0);
        String hashKey = dn.getHashKey(token);
        if (nodes.containsKey(hashKey)) {
            // Found existing node previously created when adding another node
            //
            childNode = nodes.get(hashKey);
            // Replace user object to reflect that this node is an MBean
            //
            Object data = createNodeValue(xmbean, token);
            String label = data.toString();
            XNodeInfo userObject =
                    new XNodeInfo(Type.MBEAN, data, label, mbean.toString());
            changeNodeValue(childNode, userObject);
            return;
        }

        // Create new leaf node
        //
        childNode = createDnNode(dn, token, xmbean);
        nodes.put(hashKey, childNode);

        // Add intermediate non MBean nodes
        //
        for (int i = 1; i < dn.getTokenCount(); i++) {
            token = dn.getToken(i);
            hashKey = dn.getHashKey(token);
            if (nodes.containsKey(hashKey)) {
                // Intermediate node already present, add new node as child
                //
                parentNode = nodes.get(hashKey);
                addChildNode(parentNode, childNode);
                return;
            } else {
                // Create new intermediate node
                //
                if ("domain".equals(token.getTokenType())) { // NOI18N
                    parentNode = createDomainNode(dn, token);
                    DefaultMutableTreeNode root =
                            (DefaultMutableTreeNode) getModel().getRoot();
                    addChildNode(root, parentNode);
                } else {
                    parentNode = createSubDnNode(dn, token);
                }
                nodes.put(hashKey, parentNode);
                addChildNode(parentNode, childNode);
            }
            childNode = parentNode;
        }
    }

    // Call on EDT
    private synchronized void changeNodeValue(
            DefaultMutableTreeNode node, XNodeInfo nodeValue) {
        if (node instanceof ComparableDefaultMutableTreeNode) {
            // should it stay at the same place?
            DefaultMutableTreeNode clone =
                    (DefaultMutableTreeNode) node.clone();
            clone.setUserObject(nodeValue);
            if (((ComparableDefaultMutableTreeNode) node).compareTo(clone) == 0) {
                // the order in the tree didn't change
                node.setUserObject(nodeValue);
                DefaultTreeModel model = (DefaultTreeModel) getModel();
                model.nodeChanged(node);
            } else {
                // delete the node and re-order it in case the
                // node value modifies the order in the tree
                DefaultMutableTreeNode parent =
                        (DefaultMutableTreeNode) node.getParent();
                removeChildNode(node);
                node.setUserObject(nodeValue);
                addChildNode(parent, node);
            }
        } else {
            // not comparable stays at the same place
            node.setUserObject(nodeValue);
            DefaultTreeModel model = (DefaultTreeModel) getModel();
            model.nodeChanged(node);
        }
        // Clear the current selection and set it
        // again so valueChanged() gets called
        if (node == getLastSelectedPathComponent()) {
            TreePath selectionPath = getSelectionPath();
            clearSelection();
            setSelectionPath(selectionPath);
        }
    }

    /**
     * Creates the domain node.
     */
    private DefaultMutableTreeNode createDomainNode(Dn dn, Token token) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        String label = dn.getDomain();
        XNodeInfo userObject =
                new XNodeInfo(Type.NONMBEAN, label, label, label);
        node.setUserObject(userObject);
        return node;
    }

    /**
     * Creates the node corresponding to the whole Dn, i.e. an MBean.
     */
    private DefaultMutableTreeNode createDnNode(
            Dn dn, Token token, XMBean xmbean) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        Object data = createNodeValue(xmbean, token);
        String label = data.toString();
        XNodeInfo userObject = new XNodeInfo(Type.MBEAN, data, label,
                xmbean.getObjectName().toString());
        node.setUserObject(userObject);
        return node;
    }

    /**
     * Creates the node corresponding to a subDn, i.e. a non-MBean
     * intermediate node.
     */
    private DefaultMutableTreeNode createSubDnNode(Dn dn, Token token) {
        DefaultMutableTreeNode node = new ComparableDefaultMutableTreeNode();
        String label = isKeyValueView() ? token.getTokenValue() : token.getValue();
        XNodeInfo userObject =
                new XNodeInfo(Type.NONMBEAN, label, label, token.getTokenValue());
        node.setUserObject(userObject);
        return node;
    }

    private Object createNodeValue(XMBean xmbean, Token token) {
        String label = isKeyValueView() ? token.getTokenValue() : token.getValue();
        xmbean.setText(label);
        return xmbean;
    }

    private List<String> getOrderedKeyPropertyList() {
        if (orderedKeyPropertyList == null) {
            orderedKeyPropertyList = new ArrayList<String>();
            String keyPropertyList = GlobalPreferences.sharedInstance().getOrderedKeyPropertyList();
            if (keyPropertyList.isEmpty()) {
                orderedKeyPropertyList.add("type"); // NOI18N
                orderedKeyPropertyList.add("j2eeType"); // NOI18N
            } else {
                String[] tokens = keyPropertyList.split(","); // NOI18N
                for (String token : tokens) {
                    orderedKeyPropertyList.add(token);
                }
            }
        }
        return orderedKeyPropertyList;
    }

    /**
     * Parses the MBean ObjectName comma-separated properties string and puts
     * the individual key/value pairs into the map. Key order in the properties
     * string is preserved by the map.
     */
    private static Map<String, String> extractKeyValuePairs(
            String props, ObjectName mbean) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        int eq = props.indexOf("="); // NOI18N
        while (eq != -1) {
            String key = props.substring(0, eq);
            String value = mbean.getKeyProperty(key);
            map.put(key, value);
            props = props.substring(key.length() + 1 + value.length());
            if (props.startsWith(",")) { // NOI18N
                props = props.substring(1);
            }
            eq = props.indexOf("="); // NOI18N
        }
        return map;
    }

    /**
     * Returns the ordered key property list that will be used to build the
     * MBean tree. If the "com.sun.tools.jconsole.mbeans.keyPropertyList" system
     * property is not specified, then the ordered key property list used
     * to build the MBean tree will be the one returned by the method
     * ObjectName.getKeyPropertyListString() with "type" as first key,
     * and "j2eeType" as second key, if present. If any of the keys specified
     * in the comma-separated key property list does not apply to the given
     * MBean then it will be discarded.
     */
    private static String getKeyPropertyListString(
            ObjectName mbean, List<String> orderedKeyPropertyList) {
        String props = mbean.getKeyPropertyListString();
        Map<String, String> map = extractKeyValuePairs(props, mbean);
        StringBuilder sb = new StringBuilder();
        // Add the key/value pairs to the buffer following the
        // key order defined by the "orderedKeyPropertyList"
        for (String key : orderedKeyPropertyList) {
            if (map.containsKey(key)) {
                sb.append(key + "=" + map.get(key) + ","); // NOI18N
                map.remove(key);
            }
        }
        // Add the remaining key/value pairs to the buffer
        for (Map.Entry<String, String> entry : map.entrySet()) {
            sb.append(entry.getKey() + "=" + entry.getValue() + ","); // NOI18N
        }
        String orderedKeyPropertyListString = sb.toString();
        orderedKeyPropertyListString = orderedKeyPropertyListString.substring(
                0, orderedKeyPropertyListString.length() - 1);
        return orderedKeyPropertyListString;
    }

    //
    // Tree preferences
    //
    private static boolean treeView;
    private static boolean treeViewInit = false;

    private static boolean isTreeView() {
        if (!treeViewInit) {
            treeView = getTreeViewValue();
            treeViewInit = true;
        }
        return treeView;
    }

    private static boolean getTreeViewValue() {
        String tv = System.getProperty("treeView"); // NOI18N
        return ((tv == null) ? true : !(tv.equals("false"))); // NOI18N
    }

    //
    // MBean key-value preferences
    //
    private boolean keyValueView = Boolean.getBoolean("keyValueView"); // NOI18N

    private boolean isKeyValueView() {
        return keyValueView;
    }

    //
    // Utility classes
    //
    private static class ComparableDefaultMutableTreeNode
            extends DefaultMutableTreeNode
            implements Comparable<DefaultMutableTreeNode> {

        public int compareTo(DefaultMutableTreeNode node) {
            return (this.toString().compareTo(node.toString()));
        }
    }

    private static class Dn implements Comparable<Dn> {

        private ObjectName mbean;
        private String domain;
        private String keyPropertyList;
        private String hashDn;
        private List<Token> tokens = new ArrayList<Token>();

        public Dn(ObjectName mbean, List<String> orderedKeyPropertyList) {
            this.mbean = mbean;
            this.domain = mbean.getDomain();
            this.keyPropertyList = getKeyPropertyListString(mbean, orderedKeyPropertyList);

            if (isTreeView()) {
                // Tree view
                Map<String, String> map =
                        extractKeyValuePairs(keyPropertyList, mbean);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    tokens.add(new Token("key", entry.getKey() + "=" + entry.getValue())); // NOI18N
                }
            } else {
                // Flat view
                tokens.add(new Token("key", "properties=" + keyPropertyList)); // NOI18N
            }

            // Add the domain as the first token in the Dn
            tokens.add(0, new Token("domain", "domain=" + domain)); // NOI18N

            // Reverse the Dn (from leaf to root)
            Collections.reverse(tokens);

            // Compute hash for Dn
            computeHashDn();
        }

        public ObjectName getObjectName() {
            return mbean;
        }

        public String getDomain() {
            return domain;
        }

        public String getKeyPropertyList() {
            return keyPropertyList;
        }

        public Token getToken(int index) {
            return tokens.get(index);
        }

        public int getTokenCount() {
            return tokens.size();
        }

        public String getHashDn() {
            return hashDn;
        }

        public String getHashKey(Token token) {
            final int begin = hashDn.indexOf(token.getTokenValue());
            return hashDn.substring(begin, hashDn.length());
        }

        private void computeHashDn() {
            if (tokens.isEmpty()) {
                return;
            }
            final StringBuilder hdn = new StringBuilder();
            for (int i = 0; i < tokens.size(); i++) {
                hdn.append(tokens.get(i).getTokenValue());
                hdn.append(","); // NOI18N
            }
            hashDn = hdn.substring(0, hdn.length() - 1);
        }

        @Override
        public String toString() {
            return domain + ":" + keyPropertyList; // NOI18N
        }

        public int compareTo(Dn dn) {
            return this.toString().compareTo(dn.toString());
        }
    }

    private static class Token {

        private String tokenType;
        private String tokenValue;
        private String key;
        private String value;

        public Token(String tokenType, String tokenValue) {
            this.tokenType = tokenType;
            this.tokenValue = tokenValue;
            buildKeyValue();
        }

        public String getTokenType() {
            return tokenType;
        }

        public String getTokenValue() {
            return tokenValue;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        private void buildKeyValue() {
            int index = tokenValue.indexOf("="); // NOI18N
            if (index < 0) {
                key = tokenValue;
                value = tokenValue;
            } else {
                key = tokenValue.substring(0, index);
                value = tokenValue.substring(index + 1, tokenValue.length());
            }
        }
    }
}
