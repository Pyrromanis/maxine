/*
 * Copyright (c) 2017, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 */
package jtt.loop;

/*
 * @Harness: java
 * @Runs: 50000 = 11;
 */

public class LoopPhi {

    public static int test(int arg) {
        for (int i = 0; i < arg; i++) {
            test(1, 1, 1, 1, 1, 1);
        }
        return test(1, 1, 1, 1, 1, 1);
    }

    public static int test(int i1, int i2, int i3, int i4, int i5, int i6) {
        if (i1 == 0) {
            i1 = 2;
        } else {
            i2 = 2;
        }
        for (int i = 0; i < 10; i++) {
            if (i == 0) {
                i3 = 2;
            } else {
                i4 = 2;
            }

            for (int j = 0; j < 10; j++) {
                if (j == 0) {
                    i5 = 2;
                } else {
                    i6 = 2;
                }
            }
        }

        return i1 + i2 + i3 + i4 + i5 + i6;
    }
}
