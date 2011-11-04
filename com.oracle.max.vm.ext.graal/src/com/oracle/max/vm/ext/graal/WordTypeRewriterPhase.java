/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.max.vm.ext.graal;

import com.oracle.max.graal.compiler.phases.*;
import com.oracle.max.graal.graph.*;
import com.oracle.max.graal.nodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;
import com.sun.max.vm.actor.holder.*;
import com.sun.max.vm.compiler.*;
import com.sun.max.vm.type.*;


public class WordTypeRewriterPhase extends Phase {
    @Override
    protected void run(StructuredGraph graph) {
        for (Node n : graph.getNodes()) {
            if (n instanceof ValueNode) {
                ValueNode valueNode = (ValueNode) n;
                if (isWord(valueNode)) {
                    changeToWord(valueNode);
                }
            }
        }
    }

    public boolean isWord(ValueNode node) {
        if (node.kind() == CiKind.Object) {
            if (node instanceof ConstantNode) {
                ConstantNode c = (ConstantNode) node;
                assert c.value.kind == CiKind.Object || c.value.kind == WordUtil.archKind();
                return c.value.kind == WordUtil.archKind();
            }
            return isWord(node.declaredType());
        }
        return false;
    }

    public boolean isWord(RiType type) {
        if (!(type instanceof ClassActor)) {
            return false;
        }
        ClassActor actor = (ClassActor) type;
        assert actor.kind == Kind.REFERENCE || actor.kind == Kind.WORD;
        return actor.kind == Kind.WORD;
    }

    private void changeToWord(ValueNode valueNode) {
        if (valueNode.kind() != CiKind.Object) {
            assert valueNode.kind() == CiKind.Long;
            return;
        }
        valueNode.setKind(CiKind.Long);

        // Propagate word kind.
        for (Node n : valueNode.usages()) {
            if (n instanceof PhiNode || n instanceof ReturnNode) {
                changeToWord((ValueNode) n);
            }
        }
    }
}
