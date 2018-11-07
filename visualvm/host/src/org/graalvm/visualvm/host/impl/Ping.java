/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.host.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Ping {
  
  // The default daytime port
  static int RMIREGISTRY_PORT = 1099;
  
  // The port we'll actually use
  int port = RMIREGISTRY_PORT;
  List<InetAddress> foundHosts;
  
  // Representation of a ping target
  //
  class Target {
    
    InetSocketAddress address;
    SocketChannel channel;
    boolean failure;
    long connectStart;
    long connectFinish = 0;
    Boolean status;
    
    Target(InetAddress host,int port) {
      address = new InetSocketAddress(host,port);
    }
    
    private void done() {
      status = Boolean.valueOf(connectFinish != 0);
      if (status.booleanValue()) {
        foundHosts.add(address.getAddress());
      }
    }
    
    private void status() {
      if (status == null) {
        System.out.println("Status: "+address.getAddress().getHostAddress()+" no status");
      } else {
        System.out.println("Status: "+address.getHostName()+" "+status);
      }
    }
  }
  
  
  // Thread for printing targets as they're heard from
  //
  
  
  // Thread for connecting to all targets in parallel via a single selector
  //
  static class Connector
      extends Thread {
    Selector sel;
    
    // List of pending targets.  We use this list because if we try to
    // register a channel with the selector while the connector thread is
    // blocked in the selector then we will block.
    //
    LinkedList pending = new LinkedList();
    
    Connector() throws IOException {
      sel = Selector.open();
      setName("Connector"); // NOI18N
    }
    
    // Initiate a connection sequence to the given target and add the
    // target to the pending-target list
    //
    void add(Target t) {
      SocketChannel sc = null;
      try {
        
        // Open the channel, set it to non-blocking, initiate connect
        sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(t.address);
        
        // Record the time we started
        t.channel = sc;
        t.connectStart = System.currentTimeMillis();
        
        // Add the new channel to the pending list
        synchronized (pending) {
          pending.add(t);
        }
        
        // Nudge the selector so that it will process the pending list
        sel.wakeup();
        
      } catch (IOException x) {
        if (sc != null) {
          try {
            sc.close();
          } catch (IOException xx) { }
        }
        t.failure = true;
        t.done();
      }
    }
    
    // Process any targets in the pending list
    //
    void processPendingTargets() throws IOException {
      synchronized (pending) {
        while (pending.size() > 0) {
          Target t = (Target)pending.removeFirst();
          try {
            
            // Register the channel with the selector, indicating
            // interest in connection completion and attaching the
            // target object so that we can get the target back
            // after the key is added to the selector's
            // selected-key set
            t.channel.register(sel, SelectionKey.OP_CONNECT, t);
            
          } catch (IOException x) {
            
            // Something went wrong, so close the channel and
            // record the failure
            t.channel.close();
            t.failure = true;
            t.done();
          }
          
        }
      }
    }
    
    // Process keys that have become selected
    //
    void processSelectedKeys() throws IOException {
      for (Iterator<SelectionKey> i = sel.selectedKeys().iterator(); i.hasNext();) {
        
        // Retrieve the next key and remove it from the set
        SelectionKey sk = i.next();
        i.remove();
        
        // Retrieve the target and the channel
        Target t = (Target)sk.attachment();
        SocketChannel sc = (SocketChannel)sk.channel();
        
        // Attempt to complete the connection sequence
        try {
          if (sc.finishConnect()) {
            sk.cancel();
            t.connectFinish = System.currentTimeMillis();
            sc.close();
            t.done();
          }
        } catch (IOException x) {
          sc.close();
          t.failure = true;
          t.done();
        }
      }
    }
    
    volatile boolean shutdown = false;
    
    // Invoked by the main thread when it's time to shut down
    //
    void shutdown() {
      shutdown = true;
      sel.wakeup();
    }
    
    // Connector loop
    //
    public void run() {
      for (;;) {
        try {
          int n = sel.select();
          if (n > 0)
            processSelectedKeys();
          processPendingTargets();
          if (shutdown) {
            sel.close();
            return;
          }
        } catch (IOException x) {
          x.printStackTrace();
        }
      }
    }
    
  }
  
  
  public List<InetAddress> getActiveHosts()
      throws InterruptedException, IOException {
    port = RMIREGISTRY_PORT;
    InetAddress localAdd = InetAddress.getLocalHost();
    //System.out.println("Local Address :"+localAdd.getHostAddress());
    byte[] addr = localAdd.getAddress();
    foundHosts = new ArrayList();
    
    // Create the threads and start them up
    Connector connector = new Connector();
    connector.start();

    // Create the targets and add them to the connector
    for (int i=1;i<255;i++) {
      addr[addr.length-1]=(byte)i;
      Target t = new Target(InetAddress.getByAddress(addr),port);
      connector.add(t);
    }
    
    // Wait for everything to finish
    Thread.sleep(2000);
    connector.shutdown();
    connector.join();
    foundHosts.remove(localAdd);
    return foundHosts;
  }
  
}
