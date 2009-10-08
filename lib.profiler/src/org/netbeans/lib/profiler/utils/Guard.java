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

package org.netbeans.lib.profiler.utils;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


/**
 *
 * @author Jaroslav Bachorik
 */
public class Guard {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    private static final int READ_INDEX = 0;
    private static final int WRITE_INDEX = 1;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Map readLocks = new HashMap();
    private Thread owner = null;
    private boolean starving = false;
    private int xLockCounter = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public synchronized boolean isDemanded() {
        return starving;
    }

    public synchronized Thread getOwner() {
        return owner;
    }

    public synchronized boolean enter(final boolean exclusive) {
        if (exclusive) {
            return enterExclusive(-1);
        } else {
            return enterShared(-1);
        }
    }

    public synchronized boolean enter(final boolean exclusive, final long timeout) {
        if (exclusive) {
            return enterExclusive(timeout);
        } else {
            return enterShared(timeout);
        }
    }

    public synchronized void exit() {
        Long threadId = Long.valueOf(Thread.currentThread().getId());

        try {
            if (owner == Thread.currentThread()) {
                if (--xLockCounter == 0) {
                    owner = null;

                    return;
                }
            }

            if (readLocks.containsKey(threadId)) {
                Long lockCnt = Long.valueOf(((Long) readLocks.get(threadId)).longValue() - 1);

                if (lockCnt.longValue() <= 0L) {
                    readLocks.remove(threadId);
                } else {
                    readLocks.put(threadId, lockCnt);
                }
            }
        } finally {
            this.notifyAll();
        }
    }

    private synchronized void setStarving(final boolean value) {
        starving = value;
    }

    private synchronized boolean enterExclusive(final long timeout) {
        try {
            if (owner != Thread.currentThread()) {
                boolean firstTry = true;
                Long threadId = Long.valueOf(Thread.currentThread().getId());

                while (((xLockCounter > 0) || ((readLocks.size() > 0) && !readLocks.containsKey(threadId))
                           || (readLocks.size() > 1)) && firstTry) {
                    setStarving(true);

                    try {
                        if (timeout > -1) {
                            this.wait(timeout);
                        } else if (timeout > 0) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                    }

                    firstTry = false;
                }

                if ((xLockCounter > 0) && (readLocks.size() > 0)) {
                    return false;
                }

                readLocks.remove(threadId); // promote an already existing shared lock
                owner = Thread.currentThread();
            }

            xLockCounter++;
        } finally {
            setStarving(false);
            this.notifyAll();
        }
        assert ((owner == Thread.currentThread()) && (xLockCounter > 0) && (readLocks.size() == 0)); // postocondition

        return true;
    }

    private synchronized boolean enterShared(final long timeout) {
        try {
            if (owner != null) {
                if (owner == Thread.currentThread()) {
                    return true;
                }

                boolean firstTry = true;

                while ((owner != null) && (owner != Thread.currentThread()) && firstTry) {
                    setStarving(true);

                    try {
                        if (timeout > -1) {
                            this.wait(timeout);
                        } else if (timeout > 0) {
                            this.wait();
                        }
                    } catch (InterruptedException e) {
                    }

                    firstTry = false;
                }

                if ((owner != null) && (owner != Thread.currentThread())) {
                    return false;
                }
            }

            Long threadId = Long.valueOf(Thread.currentThread().getId());

            if (readLocks.containsKey(threadId)) {
                readLocks.put(threadId, Long.valueOf(((Long) readLocks.get(threadId)).longValue() + 1));
            } else {
                readLocks.put(threadId, Long.valueOf(1L));
            }
        } finally {
            setStarving(false);
            this.notifyAll();
        }
        assert ((owner == null) && (readLocks.size() > 0));

        return true;
    }
}
