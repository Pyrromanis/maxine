/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.ins.gui;

import java.io.*;

import com.sun.max.ins.*;
import com.sun.max.lang.*;
import com.sun.max.tele.method.*;
import com.sun.max.tele.object.*;
import com.sun.max.unsafe.*;
import com.sun.max.util.*;
import com.sun.max.vm.reference.*;
import com.sun.max.vm.value.*;

/**
 * @author Bernd Mathiske
 * @author Michael Van De Vanter
 *
 * A parent class for inspectors that should be unique:  no more then one created for any particular
 * thing being inspected.
 */
public abstract class UniqueInspector<Inspector_Type extends UniqueInspector> extends Inspector {

    public abstract static class Key<UniqueInspector_Type extends UniqueInspector> implements Comparable<Key<UniqueInspector_Type>> {
        private final Class<UniqueInspector_Type> type;

        public Class<UniqueInspector_Type> type() {
            return type;
        }

        private Key(Class<UniqueInspector_Type> type) {
            this.type = type;
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, Value subject) {
            return new ValueKey<UniqueInspector_Type>(type, subject);
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, Word subject) {
            return new ValueKey<UniqueInspector_Type>(type, new WordValue(subject));
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Inspection inspection, Class<UniqueInspector_Type> type, Reference subject) {
            return new ValueKey<UniqueInspector_Type>(type, inspection.vm().createReferenceValue(subject));
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, long subject) {
            return new ValueKey<UniqueInspector_Type>(type, LongValue.from(subject));
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, File file) {
            return new FileKey<UniqueInspector_Type>(type, file);
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, TeleRoutine tele) {
            return new TeleRoutineKey<UniqueInspector_Type>(type, tele);
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type, TeleTargetMethod tele) {
            return new TeleTargetMethodKey<UniqueInspector_Type>(type, tele);
        }

        public static <UniqueInspector_Type extends UniqueInspector> Key<UniqueInspector_Type> create(Class<UniqueInspector_Type> type) {
            return new ValueKey<UniqueInspector_Type>(type, VoidValue.VOID);
        }

    }

    private static final class ValueKey<UniqueInspector_Type extends UniqueInspector> extends Key<UniqueInspector_Type> {

        private final Value subject;

        public Value subject() {
            return subject;
        }

        private ValueKey(Class<UniqueInspector_Type> type, Value subject) {
            super(type);
            this.subject = subject;
        }

        @Override
        public int hashCode() {
            if (subject.kind().isReference) {
                return type().hashCode() ^ subject.asReference().toOrigin().toInt();
            }
            return type().hashCode() ^ subject.toInt();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ValueKey) {
                final ValueKey key = (ValueKey) other;
                return type() == key.type() && subject.equals(key.subject);
            }
            return false;
        }

        public int compareTo(Key<UniqueInspector_Type> other) {
            if (type() != other.type) {
                return type().getName().compareTo(other.type.getName());
            }
            if (other instanceof ValueKey) {
                final ValueKey key = (ValueKey) other;
                return subject.compareTo(key.subject);
            }
            assert false;
            return 0;
        }
    }

    private static final class FileKey<UniqueInspector_Type extends UniqueInspector> extends Key<UniqueInspector_Type> {

        private final File file;

        public File file() {
            return file;
        }

        private FileKey(Class<UniqueInspector_Type> type, File file) {
            super(type);
            assert file != null;
            this.file = file;
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof FileKey) {
                final FileKey key = (FileKey) other;
                return type() == key.type() && file.equals(key.file);
            }
            return false;
        }

        public int compareTo(Key<UniqueInspector_Type> other) {
            if (other instanceof FileKey) {
                final FileKey key = (FileKey) other;
                return file.compareTo(key.file);
            }
            return type().getName().compareTo(other.type().getName());
        }
    }

    private static final class TeleRoutineKey<UniqueInspector_Type extends UniqueInspector> extends Key<UniqueInspector_Type> {

        private final TeleRoutine teleRoutine;

        public TeleRoutine teleRoutine() {
            return teleRoutine;
        }

        private TeleRoutineKey(Class<UniqueInspector_Type> type, TeleRoutine teleRoutine) {
            super(type);
            assert teleRoutine != null;
            this.teleRoutine = teleRoutine;
        }

        @Override
        public int hashCode() {
            return teleRoutine.hashCode();
        }

        // Tele objects are canonical, so we use object identity.
        @Override
        public boolean equals(Object other) {
            if (other instanceof TeleRoutineKey) {
                final TeleRoutineKey key = (TeleRoutineKey) other;
                return type() == key.type() && teleRoutine == key.teleRoutine;
            }
            return false;
        }

        public int compareTo(Key<UniqueInspector_Type> other) {
            if (other instanceof TeleRoutineKey) {
                final TeleRoutineKey key = (TeleRoutineKey) other;
                return teleRoutine.getUniqueName().compareTo(key.teleRoutine.getUniqueName());
            }
            return type().getName().compareTo(other.type().getName());
        }
    }

    private static final class TeleTargetMethodKey<UniqueInspector_Type extends UniqueInspector> extends Key<UniqueInspector_Type> {

        private final TeleTargetMethod teleTargetMethod;

        public TeleTargetMethod teleTargetMethod() {
            return teleTargetMethod;
        }

        private TeleTargetMethodKey(Class<UniqueInspector_Type> type, TeleTargetMethod teleTargetMethod) {
            super(type);
            assert teleTargetMethod != null;
            this.teleTargetMethod = teleTargetMethod;
        }

        @Override
        public int hashCode() {
            return teleTargetMethod.hashCode();
        }

        // Tele objects are canonical, so we use object identity.
        @Override
        public boolean equals(Object other) {
            if (other instanceof TeleTargetMethodKey) {
                final TeleTargetMethodKey key = (TeleTargetMethodKey) other;
                return type() == key.type() && teleTargetMethod == key.teleTargetMethod;
            }
            return false;
        }

        public int compareTo(Key<UniqueInspector_Type> other) {
            if (other instanceof TeleTargetMethodKey) {
                final TeleTargetMethodKey key = (TeleTargetMethodKey) other;
                return teleTargetMethod.toString().compareTo(key.teleTargetMethod.toString());
            }
            return type().getName().compareTo(other.type().getName());
        }
    }

    private final Key<Inspector_Type> key;

    public Key<Inspector_Type> key() {
        return key;
    }

    protected UniqueInspector(Inspection inspection, Value subject) {
        super(inspection);
        final Class<Class<Inspector_Type>> classType = null;
        final Class<Inspector_Type> frameType = StaticLoophole.cast(classType, getClass());
        key = new ValueKey<Inspector_Type>(frameType, subject);
    }

    protected UniqueInspector(Inspection inspection, Reference subject) {
        this(inspection, inspection.vm().createReferenceValue(subject));
    }

    protected UniqueInspector(Inspection inspection) {
        this(inspection, VoidValue.VOID);
    }

    protected UniqueInspector(Inspection inspection, File file) {
        super(inspection);
        final Class<Class<Inspector_Type>> classType = null;
        final Class<Inspector_Type> frameType = StaticLoophole.cast(classType, getClass());
        key = new FileKey<Inspector_Type>(frameType, file);
    }

    /**
     * Creates an inspector for an entity representing some code in the tele VM. The key for the inspector is derived
     * from {@code teleTargetRoutine} if it is not null, otherwise it derived from {@code teleRoutine}.
     *
     * @param inspection the inspection context
     * @param teleTargetRoutine the target method in the tele VM containing the code being inspected. This value may be
     *            null if the code being inspected is bytecode and has no compiled form.
     * @param teleRoutine the {@linkplain TeleMethodActor method actor} or {@linkplain TeleNativeRoutine native routine}
     *            containing the code. This value must not be null if {@code teleTargetRoutine} is null.
     */
    protected UniqueInspector(Inspection inspection, TeleTargetMethod teleTargetMethod, TeleRoutine teleRoutine) {
        super(inspection);
        if (teleTargetMethod != null) {
            final Class<Class<Inspector_Type>> classType = null;
            final Class<Inspector_Type> frameType = StaticLoophole.cast(classType, getClass());
            key = new TeleTargetMethodKey<Inspector_Type>(frameType, teleTargetMethod);
        } else {
            assert teleRoutine != null;
            final Class<Class<Inspector_Type>> classType = null;
            final Class<Inspector_Type> frameType = StaticLoophole.cast(classType, getClass());
            key = new TeleRoutineKey<Inspector_Type>(frameType, teleRoutine);
        }
    }

    private static <UniqueInspector_Type extends UniqueInspector> UniqueInspector_Type match(Inspector inspector, Key<UniqueInspector_Type> key) {
       // final Inspector inspector = inspectorFrame.inspector();
        if (key != null && key.type().isInstance(inspector)) {
            final UniqueInspector_Type uniqueInspector = key.type().cast(inspector);
            if (uniqueInspector.key().equals(key)) {
                return uniqueInspector;
            }
        }
        return null;
    }

    public static <UniqueInspector_Type extends UniqueInspector> UniqueInspector_Type find(Inspection inspection, final Key<UniqueInspector_Type> key) {
        final Predicate<Inspector> predicate = new Predicate<Inspector>() {
            public boolean evaluate(Inspector inspector) {
                return match(inspector, key) != null;
            }
        };
        final Inspector inspector =  inspection.gui().findInspector(predicate);
        if (inspector != null && inspector instanceof UniqueInspector) {
            final Class<UniqueInspector_Type> type = null;
            final UniqueInspector_Type result = StaticLoophole.cast(type, inspector);
            return result;
        }
        return null;
    }

    public static <UniqueInspector_Type extends UniqueInspector> UniqueInspector_Type find(Inspection inspection, Class<UniqueInspector_Type> type) {
        return find(inspection, Key.create(type));
    }

    /*
     * Support for marking inspectors, e.g., for GC purposes
     */

    private boolean marked;

    public boolean marked() {
        return marked;
    }

    public void setMarked(boolean val) {
        marked = val;
    }
}
