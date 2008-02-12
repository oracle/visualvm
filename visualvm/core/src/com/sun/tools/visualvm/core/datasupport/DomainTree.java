/*
 * Copyright 2007-2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.visualvm.core.datasupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Jiri Sedlacek
 */
// N-ary tree based on four-state DomainsComparator
// NOTE: The tree isn't synchronized!
public final class DomainTree<T, D> {
    
    private final Comparator<T> defaultItemsComparator = new ItemsComparator();
    
    private DomainNode<T, D> root;
    private DomainsComparator<T, D> domainsComparator;
    
    
    public DomainTree(DomainsComparator<T, D> domainsComparator) {
        this(null, domainsComparator);
    }
    
    public DomainTree(D rootDomain, DomainsComparator<T, D> domainsComparator) {
        if (domainsComparator == null) throw new IllegalArgumentException("Null domainsComparator not allowed"); // NOI18N
        this.domainsComparator = domainsComparator;
        if (rootDomain != null) root = new DomainNode(rootDomain, getItemsComparator(rootDomain));
    }
    
    
    public void add(T item, D domain) {
        if (item == null) throw new IllegalArgumentException("Null item not allowed"); // NOI18N
        if (domain == null) throw new IllegalArgumentException("Null domain not allowed"); // NOI18N

        if (root == null) root = new DomainNode(item, domain, getItemsComparator(domain));
        else add(item, domain, root);
    }
    
    public void remove(T item, D domain) {
        if (item == null) throw new IllegalArgumentException("Null item not allowed"); // NOI18N
        if (domain == null) throw new IllegalArgumentException("Null domain not allowed"); // NOI18N

        if (root == null) return;
        DomainNode<T, D> node = getNodeFor(domain, root);
        if (node != null) {
            node.removeItem(item);
            if (node.getItems().isEmpty()) {
                if (node == root) {
                    root = null;
                } else {
                    DomainNode<T, D> nodeParent = node.getParent();
                    Iterator<DomainNode<T, D>> childrenIterator = node.getChildren().iterator();
                    while(childrenIterator.hasNext()) nodeParent.addChild(childrenIterator.next());
                    nodeParent.removeChild(node);
                }
            }
        }
    }
    
    public void remove(T item) {
        // TODO: implement !!!
        // NOTE: ideally this should be implemented as follows
        // List<Set<T>> allDomains = getAllDomains();
        //     for(Set<T> domain : allDomains) domain.remove(item);
    }
    
    public List<Set<T>> getPathToClosestDomain(D domain) {
        if (domain == null) throw new IllegalArgumentException("Null domain not allowed"); // NOI18N

        if (root == null) return Collections.EMPTY_LIST;
        DomainNode<T, D> node = getClosestNodeFor(domain, root, null);
        if (node == null) {
            return null;
        } else {
            List<Set<T>> path = new ArrayList();
            while (node != null) {
                path.add(0, new HashSet(node.getItems()));
                node = node.getParent();
            }
            return path;
        }
    }
    
    public List<T> getItemsToClosestDomain(D domain) {
        List<T> itemsToClosestDomain = new ArrayList();
        List<Set<T>> pathToClosestDomain = getPathToClosestDomain(domain);
        for (Set<T> domainItems : pathToClosestDomain) itemsToClosestDomain.addAll(domainItems);
        return itemsToClosestDomain;
    }
    
    
    private void add(T item, D domain, DomainNode<T, D> referenceNode) {
        DomainsComparator.Result result = domainsComparator.compare(domain, referenceNode.getDomain());
        switch (result) {
            case EQUALS:
                // item equals to reference node, add the item there
                referenceNode.addItem(item);
                break;
                
            case LESS:
                // found immediate parent of referenceNode, update the tree and process referenceNode's next sibling
                if (referenceNode == root) {
                    DomainNode<T, D> newRoot = new DomainNode(item, domain, getItemsComparator(domain));
                    newRoot.addChild(root);
                    root = newRoot;
                } else {
                    DomainNode<T, D> referenceNodeParent = referenceNode.getParent();
                    DomainNode<T, D> referenceNodeSibling = referenceNodeParent.getNextChild(referenceNode);
                    if (!referenceNodeParent.getLastChild().getDomain().equals(domain)) referenceNodeParent.addChild(new DomainNode(item, domain, getItemsComparator(domain)));
                    referenceNodeParent.removeChild(referenceNode);
                    referenceNodeParent.getLastChild().addChild(referenceNode);
                    if (referenceNodeSibling != null) add(item, domain, referenceNodeSibling);
                }
                break;
                
            case MORE:
                // item belongs to referenceNode's subtree, process it to it's first child or create a new child
                DomainNode<T, D> referenceNodeFirstChild = referenceNode.getFirstChild();
                if (referenceNodeFirstChild == null) {
                    DomainNode<T, D> newChild = new DomainNode(item, domain, getItemsComparator(domain));
                    referenceNode.addChild(newChild);
                } else {
                    add(item, domain, referenceNodeFirstChild);
                }
                break;
                
            case NOT_COMPARABLE:
                // item is not comparable with referenceNode, process referenceNode's next sibling
                if (referenceNode == root) {
                    DomainNode<T, D> newRoot = new DomainNode(item, domainsComparator.getSuperDomain(domain, root.getDomain()), getItemsComparator(domain));
                    newRoot.addChild(root);
                    root = newRoot;
                } else {
                    DomainNode<T, D> referenceNodeParent = referenceNode.getParent();
                    DomainNode<T, D> referenceNodeSibling = referenceNodeParent.getNextChild(referenceNode);
                    if (referenceNodeSibling != null) {
                        add(item, domain, referenceNodeSibling);
                    } else {
                        if (!referenceNodeParent.getLastChild().getDomain().equals(domain)) referenceNodeParent.addChild(new DomainNode(item, domain, getItemsComparator(domain)));
                    }
                }
                break;
                
            default:
                assert(false); // Shouldn't get here
        }
    }
    
    private Comparator<T> getItemsComparator(D domain) {
        Comparator<T> itemsComparator = domainsComparator.getItemsComparator(domain);
        if (itemsComparator == null) itemsComparator = defaultItemsComparator;
        return itemsComparator;
    }
    
    private DomainNode<T, D> getNodeFor(D domain, DomainNode<T, D> referenceNode) {
        DomainNode<T, D> closestNode = getClosestNodeFor(domain, referenceNode, null);
        return closestNode.getDomain().equals(domain) ? closestNode : null;
    }
    
    private DomainNode<T, D> getClosestNodeFor(D domain, DomainNode<T, D> referenceNode, DomainNode<T, D> lastReferenceNode) {
        DomainsComparator.Result result = domainsComparator.compare(domain, referenceNode.getDomain());
        switch (result) {
            case EQUALS:
                // found appropriate node, check if it contains the item and eventually return it
                return referenceNode;
                
            case LESS:
                // appropriate node doesn't exist, return lastReferenceNode
                return lastReferenceNode;
                
            case MORE:
                // appropriate node could be in referenceNode's subtree, process it
                DomainNode<T, D> referenceNodeFirstChild = referenceNode.getFirstChild();
                if (referenceNodeFirstChild == null) return referenceNode;
                else return getClosestNodeFor(domain, referenceNode.getFirstChild(), referenceNode);
                
            case NOT_COMPARABLE:
                // appropriate node could be referenceNode's sibling or its subtree, process it
                DomainNode<T, D> refNodeParent = referenceNode.getParent();
                DomainNode<T, D> referenceNodeSibling = refNodeParent.getNextChild(referenceNode);
                if (referenceNodeSibling == null) return refNodeParent;
                else return getClosestNodeFor(domain, referenceNodeSibling, referenceNode);
                
            default:
                assert(false); // Shouldn't get here
                return null;
        }
    }
    
    
    public static interface DomainsComparator<T, D> {
        
        public static enum Result { EQUALS, LESS, MORE, NOT_COMPARABLE };
        
        // cannot return null
        public Result compare(D d1, D d2);
        
        // returns any super-domain (not neccessarily the closest one) for both domains
        // can return null only if the root domain is always comparable with all other domains
        public D getSuperDomain(D d1, D d2);
        
        // can return null
        public Comparator<T> getItemsComparator(D d);
        
    }
    
    private static final class ItemsComparator<T> implements Comparator<T> {

        public int compare(T o1, T o2) {
            return new Integer(o1.hashCode()).compareTo(o2.hashCode());
        }
        
    }
    
    private static final class DomainNode<T, D> {
        
        private D domain;
        private Set<T> items;
        
        private DomainNode<T, D> parent;
        private List<DomainNode<T, D>> children;
        
        
        public DomainNode(D domain, Comparator<T> itemsComparator) {
            this(null, domain, itemsComparator);
        }
        
        public DomainNode(T item, D domain, Comparator<T> itemsComparator) {
            items = new TreeSet(itemsComparator);
            children = new ArrayList();
            
            this.domain = domain;
            if (item != null) items.add(item);
        }
        
        
        public void addItem(T item) {
            items.add(item);
        }
        
        public void removeItem(T item) {
            items.remove(item);
        }
        
        public D getDomain() {
            return domain;
        }
        
        public Set<T> getItems() {
            return items;
        }
        
        public boolean containsItem(T item) {
            return items.contains(item);
        }
        
        private void setParent(DomainNode<T, D> parent) {
            this.parent = parent;
        }
        
        public DomainNode<T, D> getParent() {
            return parent;
        }
        
        public void addChild(DomainNode<T, D> child) {
            children.add(child);
            child.setParent(this);
        }
        
        public void removeChild(DomainNode<T, D> child) {
            children.remove(child);
            child.setParent(null);
        }
        
        public DomainNode<T, D> getFirstChild() {
            if (children.isEmpty()) return null;
            else return children.get(0);
        }
        
        public DomainNode<T, D> getLastChild() {
            if (children.isEmpty()) return null;
            else return children.get(children.size() - 1);
        }
        
        public DomainNode<T, D> getNextChild(DomainNode<T, D> referenceChild) {
            int referenceChildIndex = children.indexOf(referenceChild);
            if (referenceChildIndex == -1 || referenceChildIndex == children.size() - 1) return null;
            else return children.get(referenceChildIndex + 1);
        }
        
        public List<DomainNode<T, D>> getChildren() {
            return children;
        }
        
    }

}
