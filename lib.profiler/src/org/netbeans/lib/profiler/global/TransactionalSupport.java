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

package org.netbeans.lib.profiler.global;


//import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author Jaroslav Bachorik
 */
public class TransactionalSupport {
    //~ Static fields/initializers -----------------------------------------------------------------------------------------------

    static final boolean DEBUG = System.getProperty(TransactionalSupport.class.getName()) != null;

    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    private final Object transactionLock = new Object();
    private final ThreadLocal interruptedFlag = new ThreadLocal();
    private final ThreadLocal lockRead = new ThreadLocal();
    private final ThreadLocal lockWrite = new ThreadLocal();

    //  final static private Logger LOGGER = Logger.getLogger(TransactionalSupport.class.getName());
    private boolean lockedExclusively = false;

    //  final static private Logger LOGGER = Logger.getLogger(TransactionalSupport.class.getName());
    private boolean lockedShared = false;
    private int sharedLockCount = 0;

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public void beginTrans(boolean mutable) {
        beginTrans(mutable, false);
    }

    public boolean beginTrans(boolean mutable, boolean failEarly) {
        if (DEBUG) {
            System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Starting transaction: mutable = " + mutable
                               + ", failEarly = " + failEarly); // NOI18N
        }

        synchronized (transactionLock) {
            boolean result = false;

            do {
                if (mutable) {
                    result = lockExclusively();
                } else {
                    result = lockShared();
                }

                if (!result && !failEarly) {
                    if (DEBUG) {
                        System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                           + "] Couldn't start transaction. Going to wait for some time"); // NOI18N
                    }

                    boolean interrupted = false;

                    do {
                        interrupted = false;

                        try {
                            transactionLock.wait();
                        } catch (InterruptedException e) {
                            interruptedFlag.set(new Object());
                            interrupted = true;
                            Thread.interrupted();
                        }
                    } while (interrupted);
                }
            } while (!result && !failEarly);

            return result;
        }
    }

    public void endTrans() {
        synchronized (transactionLock) {
            Integer roCounter = (Integer) lockRead.get();
            Integer rwCounter = (Integer) lockWrite.get();

            if (roCounter == null) {
                unlockShared();
            } else {
                if (DEBUG) {
                    System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Releasing ownership for a shared lock"); // NOI18N
                }

                int counter = roCounter.intValue();

                if (counter > 1) {
                    lockRead.set(new Integer(counter - 1));
                } else {
                    lockRead.set(null);
                    unlockShared();
                }
            }

            if (rwCounter == null) {
                if (DEBUG) {
                    System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Unlocking an exclusive lock"); // NOI18N
                }

                lockedExclusively = false;
            } else {
                if (DEBUG) {
                    System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                       + "] Releasing ownership for an exclusive lock"); // NOI18N
                }

                int counter = rwCounter.intValue();

                if (counter > 1) {
                    lockWrite.set(new Integer(counter - 1));
                } else {
                    lockWrite.set(null);

                    if (DEBUG) {
                        System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Unlocking an exclusive lock"); // NOI18N
                    }

                    lockedExclusively = false;
                }
            }

            transactionLock.notifyAll();
        }

        rethrowInterrupt();
    }

    private boolean lockExclusively() {
        if (lockedShared) {
            return promoteToExclusive();
        }

        if (lockedExclusively) {
            return relockExclusively();
        } else {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Grabbing an exclusive lock for transaction"); // NOI18N
            }

            lockedExclusively = true;
            lockWrite.set(new Integer(1));

            return true;
        }
    }

    private boolean lockShared() {
        boolean result;

        if (lockedExclusively) {
            return relockExclusively();
        } else {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Grabbing shared lock for transaction"); // NOI18N
            }

            lockedShared = true;

            Integer counter = (Integer) lockRead.get();

            if (counter == null) {
                lockRead.set(new Integer(1));
                sharedLockCount++; // turn the lock counter only if the current thread doesn't own the sahred lock already
            } else {
                lockRead.set(new Integer(counter.intValue() + 1));
            }

            result = true;

            return result;
        }
    }

    private boolean promoteToExclusive() {
        boolean result;

        if (sharedLockCount > 1) {
            System.err.println("WARNING: [" + Thread.currentThread().getName() + "] Cant promote a shared lock held by "
                               + sharedLockCount + " threads!"); // NOI18N

            return false; // can't promote a shared lock held by more threads
        }

        Integer counter = (Integer) lockRead.get();

        if (counter != null) {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                   + "] Promoting a previously owned shared lock to the exclusive one"); // NOI18N
            }

            lockedShared = false;
            sharedLockCount = 0;
            lockedExclusively = true;
            lockWrite.set(new Integer(counter.intValue() + 1));
            lockRead.set(null);
            result = true;
        } else {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                   + "] Failed to promote a previously owned shared lock"); // NOI18N
            }

            result = false;
        }

        return result;
    }

    private boolean relockExclusively() {
        boolean result;
        Integer counter = (Integer) lockWrite.get();

        if (counter != null) {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                   + "] Relocking a previously owned exclusive lock"); // NOI18N
            }

            lockWrite.set(new Integer(counter.intValue() + 1));
            result = true;
        } else {
            if (DEBUG) {
                System.out.println("DEBUG: [" + Thread.currentThread().getName()
                                   + "] Failed to relock an exclusive lock. Not an owner."); // NOI18N
            }

            result = false;
        }

        return result;
    }

    private void rethrowInterrupt() {
        if (interruptedFlag.get() != null) {
            Thread.currentThread().interrupt();
            interruptedFlag.set(null);
        }
    }

    private void unlockShared() {
        if (DEBUG) {
            System.out.println("DEBUG: [" + Thread.currentThread().getName() + "] Unlocking a shared lock"); // NOI18N
        }

        lockedShared = false;

        if (sharedLockCount > 0) {
            sharedLockCount--;
        }
    }
}
