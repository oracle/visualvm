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

package org.graalvm.visualvm.lib.jfluid.results.memory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


/**
 * A terminal node used in Object Liveness Profiling Calling Context Tree (CCT).
 * Contains the information gathered during object liveness profiling, which can be calculated  for intermediate nodes
 * if known for terminal nodes.
 * <p/>
 * Normally used as a leaf, except in case there are multiple same paths in the tree with differfent length.
 * <p/>
 * The information in TermCCTNode represents all objects of the same type allocated using same call path.
 *
 * @author Misha Dmitriev
 * @author Ian Formanek
 */
public class RuntimeObjLivenessTermCCTNode extends RuntimeObjAllocTermCCTNode {
    //~ Instance fields ----------------------------------------------------------------------------------------------------------

    // info on surviving generation
    // [0] - epoch number
    // [1] - # allocated in given epoch
    // e.g.
    // +---------+
    // | 1  | 4  |
    // | 5  | 3  |
    // | 6  | 34 |
    // | 14 | 56 |
    // +---------+
    private int[][] epochAndNLiveObjects; // null in static snapshots

    //~ Constructors -------------------------------------------------------------------------------------------------------------

    public RuntimeObjLivenessTermCCTNode(int methodId) {
        super(methodId);
    }

    protected RuntimeObjLivenessTermCCTNode() {
    } // only for I/O

    //~ Methods ------------------------------------------------------------------------------------------------------------------

    public int getType() {
        return TYPE_RuntimeObjLivenessTermCCTNode;
    }

    /**
     * Only called (from ObjLivenessCallGraphBuilder) when processing results during profiled app runtime
     */
    public void addLiveObjectForEpoch(int epoch) {
        if (epochAndNLiveObjects == null) {
            epochAndNLiveObjects = new int[2][2];
            epochAndNLiveObjects[0][0] = epochAndNLiveObjects[0][1] = -1;
        }

        int len = epochAndNLiveObjects[0].length;
        int lenMinusOne = len - 1;
        int row = lenMinusOne;

        while ((epochAndNLiveObjects[0][row] == -1) && (row > 0)) {
            row--;
        }

        if (epochAndNLiveObjects[0][row] > -1) { // Some epochs already exist

            if (epochAndNLiveObjects[0][row] < epoch) { // This epoch is newer than the newest recorded one; need to open a new epoch

                if (row == lenMinusOne) {
                    int[][] newEpochAndNLiveObjects = new int[2][len + 2];
                    System.arraycopy(epochAndNLiveObjects[0], 0, newEpochAndNLiveObjects[0], 0, len);
                    System.arraycopy(epochAndNLiveObjects[1], 0, newEpochAndNLiveObjects[1], 0, len);
                    epochAndNLiveObjects = newEpochAndNLiveObjects;
                    epochAndNLiveObjects[0][len] = epochAndNLiveObjects[0][len + 1] = -1;
                }

                row++;
            } else {
                // Search if this epoch exists at all. Such a thing (an object allocation record for an old epoch arriving when
                // a newer epoch(s) have already been opened) may happen in heavily multithreaded programs, where rescheduling
                // happens when a thread is inside our ProfilerRuntimeObjLiveness.traceObjAlloc() method.
                while ((row > 0) && (epochAndNLiveObjects[0][row] != epoch)) {
                    row--;
                }

                if (epochAndNLiveObjects[0][row] != epoch) { // Pathological case. We don't track them - they shouldn't be frequent anyway.

                    return;
                }
            }
        }

        epochAndNLiveObjects[0][row] = epoch;
        epochAndNLiveObjects[1][row]++;
    }

    public static float calculateAvgObjectAgeForAllPaths(RuntimeMemoryCCTNode rootNode, int currentEpoch) {
        int[] nObjAndAge = new int[2];
        calculateNObjAndAge(rootNode, currentEpoch, nObjAndAge);

        if (nObjAndAge[0] == 0) {
            return 0.0f; // Zero live objects - zero age
        }

        return (float) ((double) nObjAndAge[1] / (double) nObjAndAge[0]);
    }

    /**
     * Works with epoch
     */
    public int calculateTotalNLiveObjects() {
        if (epochAndNLiveObjects == null) {
            return 0;
        }

        int res = 0;
        int row = 0;
        int len = epochAndNLiveObjects[0].length;

        while ((row < len) && (epochAndNLiveObjects[0][row] != -1)) {
            res += epochAndNLiveObjects[1][row];
            row++;
        }

        return res;
    }

    public static int calculateTotalNumberOfSurvGensForAllPaths(RuntimeMemoryCCTNode rootNode) {
        SurvGenSet sgSet = new SurvGenSet();
        calculateTotalNumberOfSurvGens(rootNode, sgSet);

        return sgSet.getTotalNoOfAges();
    }

    public Object clone() {
        RuntimeObjLivenessTermCCTNode ret = (RuntimeObjLivenessTermCCTNode) super.clone();

        if (epochAndNLiveObjects == null) {
            ret.epochAndNLiveObjects = null;
        } else {
            int len = epochAndNLiveObjects[0].length;
            ret.epochAndNLiveObjects = new int[2][len];
            System.arraycopy(epochAndNLiveObjects[0], 0, ret.epochAndNLiveObjects[0], 0, len);
            System.arraycopy(epochAndNLiveObjects[1], 0, ret.epochAndNLiveObjects[1], 0, len);
        }

        return ret;
    }

    public void readFromStream(DataInputStream in) throws IOException {
        super.readFromStream(in);

        int len = in.readInt();

        epochAndNLiveObjects = new int[2][len];

        for (int i = 0; i < len; i++) {
            epochAndNLiveObjects[0][i] = in.readInt();
            epochAndNLiveObjects[1][i] = in.readInt();
        }
    }

    /**
     * Only called (from ObjLivenessCallGraphBuilder) when processing results during profiled app runtime
     */
    public void removeLiveObjectForEpoch(int epoch) {
        assert (epochAndNLiveObjects != null);

        int len = epochAndNLiveObjects[0].length;
        int lenMinusOne = len - 1;
        int row = lenMinusOne;

        while ((epochAndNLiveObjects[0][row] != epoch) && (row > 0)) {
            row--;
        }

        if (epochAndNLiveObjects[0][row] != epoch) { // Pathological case. We don't track them - they shouldn't be frequent anyway.

            return;
        }

        epochAndNLiveObjects[1][row]--;

        if (epochAndNLiveObjects[1][row] == 0) { // Perform array compaction

            if (row < lenMinusOne) {
                if (!((row < lenMinusOne) && (epochAndNLiveObjects[0][row + 1] == -1))) { // There are non-empty rows behind this one
                    System.arraycopy(epochAndNLiveObjects[0], row + 1, epochAndNLiveObjects[0], row, lenMinusOne - row);
                    System.arraycopy(epochAndNLiveObjects[1], row + 1, epochAndNLiveObjects[1], row, lenMinusOne - row);
                } else {
                    epochAndNLiveObjects[0][row] = -1;
                }
            }

            epochAndNLiveObjects[0][lenMinusOne] = -1;
            epochAndNLiveObjects[1][lenMinusOne] = 0;
        }
    }

    public void writeToStream(DataOutputStream out) throws IOException {
        super.writeToStream(out);

        int len = (epochAndNLiveObjects == null) ? 0 : epochAndNLiveObjects[0].length;
        out.writeInt(len);

        for (int i = 0; i < len; i++) {
            out.writeInt(epochAndNLiveObjects[0][i]);
            out.writeInt(epochAndNLiveObjects[1][i]);
        }
    }

    /**
     * Works with epoch
     */
    protected static void calculateNObjAndAge(RuntimeMemoryCCTNode node, int currentEpoch, int[] nObjAndAge) {
        if (node instanceof RuntimeObjLivenessTermCCTNode) {
            RuntimeObjLivenessTermCCTNode thisNode = (RuntimeObjLivenessTermCCTNode) node;
            int[][] epochAndNLiveObjects = thisNode.epochAndNLiveObjects;

            if (epochAndNLiveObjects != null) {
                int row = 0;
                int len = epochAndNLiveObjects[0].length;

                while ((row < len) && (epochAndNLiveObjects[0][row] != -1)) {
                    nObjAndAge[0] += epochAndNLiveObjects[1][row]; // Add the number of objects for this epoch
                    nObjAndAge[1] += (epochAndNLiveObjects[1][row] * (currentEpoch - epochAndNLiveObjects[0][row])); // Add their total age
                    row++;
                }
            }
        }

        if (node.children != null) {
            if (node.children instanceof RuntimeMemoryCCTNode) {
                calculateNObjAndAge((RuntimeMemoryCCTNode) node.children, currentEpoch, nObjAndAge);
            } else {
                RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) node.children;

                for (int i = 0; i < ar.length; i++) {
                    calculateNObjAndAge(ar[i], currentEpoch, nObjAndAge);
                }
            }
        }
    }

    /**
     * Works with epoch
     */
    protected static void calculateTotalNumberOfSurvGens(RuntimeMemoryCCTNode node, SurvGenSet sgSet) {
        if (node instanceof RuntimeObjLivenessTermCCTNode) {
            RuntimeObjLivenessTermCCTNode thisNode = (RuntimeObjLivenessTermCCTNode) node;
            int[][] epochAndNLiveObjects = thisNode.epochAndNLiveObjects;

            if (epochAndNLiveObjects != null) {
                int[] epochs = epochAndNLiveObjects[0];
                int len = epochs.length;

                for (int i = 0; i < len; i++) {
                    if (epochs[i] != -1) {
                        sgSet.addAge(epochs[i]);
                    } else {
                        break;
                    }
                }
            }
        }

        if (node.children != null) {
            if (node.children instanceof RuntimeMemoryCCTNode) {
                calculateTotalNumberOfSurvGens((RuntimeMemoryCCTNode) node.children, sgSet);
            } else {
                RuntimeMemoryCCTNode[] ar = (RuntimeMemoryCCTNode[]) node.children;

                for (int i = 0; i < ar.length; i++) {
                    calculateTotalNumberOfSurvGens(ar[i], sgSet);
                }
            }
        }
    }

    /**
     * Works with epoch
     */
    protected void dumpEpochs() {
        if (epochAndNLiveObjects != null) {
            int len = epochAndNLiveObjects[0].length;

            for (int i = 0; i < len; i++) {
                System.err.println("epoch = " + epochAndNLiveObjects[0][i] + ", objno = " + epochAndNLiveObjects[1][i]); // NOI18N
            }
        } else {
            System.err.println("epoch = null"); // NOI18N
        }
    }
}
