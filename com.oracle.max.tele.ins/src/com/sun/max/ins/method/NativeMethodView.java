/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.ins.method;

import com.sun.max.ins.*;
import com.sun.max.tele.*;
import com.sun.max.tele.object.*;

/**
 * Visual view and debugger for code discovered in the VM that is not compiled Java.
 * It is runtime assembled code such as a {@linkplain RuntimeStub stub} or
 * is other native code about which little is known.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class NativeMethodView extends MethodView<NativeMethodView> {

    private final MaxExternalCode externalCode;
    private MachineCodeViewer machineCodeViewer = null;
    private final String shortName;
    private final String longName;

    public NativeMethodView(Inspection inspection, MethodViewContainer container, MaxExternalCode externalCode) {
        super(inspection, container);
        this.externalCode = externalCode;
        shortName = inspection().nameDisplay().shortName(externalCode);
        longName = inspection().nameDisplay().longName(externalCode);
        createTabFrame(container);
    }

    @Override
    public MaxExternalCode machineCode() {
        return externalCode;
    }

    @Override
    public TeleClassMethodActor teleClassMethodActor() {
        return null;
    }

    @Override
    public String getTextForTitle() {
        return shortName;
    }

    @Override
    public String getToolTip() {
        return longName;
    }

    @Override
    public void createViewContent() {
        machineCodeViewer =  new JTableMachineCodeViewer(inspection(), this, externalCode);
        getContentPane().add(machineCodeViewer);
        pack();
    }

    @Override
    protected void refreshState(boolean force) {
        if (getJComponent().isShowing() || force) {
            machineCodeViewer.refresh(force);
        }
    }

    @Override
    public void viewConfigurationChanged() {
        machineCodeViewer.redisplay();
    }

    @Override
    public void print() {
        machineCodeViewer.print(getTextForTitle());
    }

    /**
     * Receive request from codeViewer to close; there's only one, so close the whole Method view.
     */
    @Override
    public void closeCodeViewer(CodeViewer codeViewer) {
        assert codeViewer == machineCodeViewer;
        close();
    }

    /**
     * Global code selection has changed; update viewer.
     */
    @Override
    public void codeLocationFocusSet(MaxCodeLocation codeLocation, boolean interactiveForNative) {
        if (machineCodeViewer.updateCodeFocus(codeLocation) && !isSelected()) {
            highlight();
        }
    }
}