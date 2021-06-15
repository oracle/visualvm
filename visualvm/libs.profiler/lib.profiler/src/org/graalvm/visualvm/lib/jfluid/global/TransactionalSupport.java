/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package org.graalvm.visualvm.lib.jfluid.global;


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
