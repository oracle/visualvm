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

package com.sun.tools.visualvm.core.dataview.threads;

import com.sun.tools.visualvm.core.datasource.Application;
import com.sun.tools.visualvm.core.model.jmx.CachedMBeanServerConnection;
import com.sun.tools.visualvm.core.model.jmx.JMXModelFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.netbeans.lib.profiler.global.CommonConstants;
import org.netbeans.lib.profiler.results.threads.ThreadsDataManager;
import org.netbeans.lib.profiler.wireprotocol.MonitoredNumbersResponse;
import org.netbeans.modules.profiler.ui.NBSwingWorker;

/**
 *
 * @author Tomas Hurka
 */
class ThreadMXBeanDataManager extends ThreadsDataManager {
  private static final long[] dummyLong = new long[0];

  private ThreadMXBean threadBean;
  private CachedMBeanServerConnection serverConnection;
  private Set<Long> threadIdSet = new HashSet();
  private boolean refreshRunning;

  public ThreadMXBeanDataManager(Application app, ThreadMXBean tb) {
    serverConnection = JMXModelFactory.getJmxModelFor(app).getCachedMBeanServerConnection();
    threadBean = tb;
  }
  
  public void refreshThreads() {
    if (refreshRunning)
      return;
    refreshRunning = true;
    NBSwingWorker worker = new NBSwingWorker() {
      protected void doInBackground() {
        try {
          ThreadMonitoredDataResponce resp = new ThreadMonitoredDataResponce();
          if (serverConnection != null) { // flush data
            serverConnection.flush();
          }
          resp.fillInThreadData();
          processData(org.netbeans.lib.profiler.client.MonitoredData.getMonitoredData(resp));
        } catch (Exception ex) {
          // ex.printStackTrace();
        } finally {
          refreshRunning = false;
        }
      }
    };
    worker.execute();
  }
  
  public int getDaemonThreadCount() {
      return threadBean.getDaemonThreadCount();
  }

  public int getThreadCount() {
      return threadBean.getThreadCount();
  }  
  
  class ThreadMonitoredDataResponce extends MonitoredNumbersResponse {
    
    ThreadMonitoredDataResponce() {
      super(dummyLong);
      setGCstartFinishData(dummyLong,dummyLong);
    }

    private void fillInThreadData() {
      long[] currentThreadIds = threadBean.getAllThreadIds();
      ThreadInfo[] threadInfos = threadBean.getThreadInfo(currentThreadIds,1);
      Set<Long> currentIdSet = new HashSet(currentThreadIds.length*4/3);
      int nThreads = 0;
      long timeStamps[] = {System.currentTimeMillis()};
      int maxThreads = currentThreadIds.length+threadIdSet.size();
      int tids[] = new int[maxThreads];
      byte states[] = new byte[maxThreads];
      
      int nNewThreads = 0;
      int newThreadsId[] = new int[currentThreadIds.length];
      String[] newThreadsNames = new String[currentThreadIds.length];
      String[] newThreadsClasses = new String[currentThreadIds.length];
      
      for(int i=0;i<currentThreadIds.length;i++) {
        ThreadInfo tinfo = threadInfos[i];
        long threadId = currentThreadIds[i];
        Long threadIdLong;
        
        if (tinfo == null) {
          continue;
        }
        threadIdLong = Long.valueOf(threadId);
        currentIdSet.add(threadIdLong);
        tids[nThreads]=(int)threadId;
        states[nThreads]=getState(tinfo);
        nThreads++;
        
        if (!threadIdSet.remove(threadIdLong)) { // New Thread
          newThreadsId[nNewThreads] = (int)threadId;
          newThreadsNames[nNewThreads] = tinfo.getThreadName();
          newThreadsClasses[nNewThreads] = "";
          nNewThreads++;
        }
      }
      // set remaining threads as terminated
      for (Iterator it = threadIdSet.iterator(); it.hasNext();) {
        Long elem = (Long) it.next();
        tids[nThreads]=elem.intValue();
        states[nThreads]=CommonConstants.THREAD_STATUS_ZOMBIE;
        nThreads++;
      }
      threadIdSet = currentIdSet;
      setDataOnNewThreads(nNewThreads,newThreadsId,newThreadsNames,newThreadsClasses);
      setDataOnThreads(nThreads,timeStamps.length,tids,timeStamps,states);
    }
    
    byte getState(ThreadInfo threadInfo) {
      Thread.State state = threadInfo.getThreadState();
      switch(state) {
        case BLOCKED:
          return CommonConstants.THREAD_STATUS_MONITOR;
        case RUNNABLE:
          return CommonConstants.THREAD_STATUS_RUNNING;          
        case TIMED_WAITING:
        case WAITING:
          return isSleeping(threadInfo.getStackTrace()[0]) ?
            CommonConstants.THREAD_STATUS_SLEEPING :
            CommonConstants.THREAD_STATUS_WAIT;
        case TERMINATED:
        case NEW:
          return CommonConstants.THREAD_STATUS_ZOMBIE;
      }
      return CommonConstants.THREAD_STATUS_UNKNOWN;
    }
    
    boolean isSleeping(StackTraceElement element) {
      return Thread.class.getName().equals(element.getClassName()) &&
        "sleep".equals(element.getMethodName());
    }
  }
}