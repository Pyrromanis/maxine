/*
 * Copyright (c) 2017-2018, APT Group, School of Computer Science,
 * The University of Manchester. All rights reserved.
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
package test.crossisa;

import java.io.*;
import java.math.*;

import com.sun.max.vm.runtime.*;

public abstract class CrossISATester {

    private static final   String ENABLE_QEMU    = "max.arm.qemu";
    protected static final File   qemuOutput     = new File("qemu_output");
    protected static final File   qemuErrors     = new File("qemu_errors");
    private static final   File   bindOutput     = new File("bind_output");
    protected static final File   gdbOutput      = new File("gdb_output");
    protected static final String gdbInput       = "gdb_input";
    protected static final String gdbInputFPREGS = "gdb_input_fpregs";
    protected static final File   gdbErrors      = new File("gdb_errors");
    protected static final File   gccOutput      = new File("gcc_output");
    protected static final File   gccErrors      = new File("gcc_errors");
    protected static final File   asOutput       = new File("as_output");
    protected static final File   asErrors       = new File("as_errors");
    protected static final File   linkOutput     = new File("link_output");
    protected static final File   linkErrors     = new File("link_errors");

    public static  boolean ENABLE_SIMULATOR = true;
    private static boolean RESET            = false;
    private static boolean DEBUG            = false;

    final     int        NUM_REGS;
    protected BitsFlag[] bitMasks;
    protected Process    gcc;
    protected Process    assembler;
    protected Process    linker;
    protected Process    qemu;
    protected Process    gdb;
    protected long[]     expectRegs;
    protected boolean[]  testRegs;

    protected CrossISATester(int numRegs) {
        NUM_REGS = numRegs;
        expectRegs = new long[NUM_REGS];
        testRegs = new boolean[NUM_REGS];
    }

    public CrossISATester(int numRegs, String[] args) {
        NUM_REGS = numRegs;
        initializeQemu();
        for (int i = 0; i < NUM_REGS; i++) {
            testRegs[i] = false;
        }
        for (int i = 0; i < args.length; i += 2) {
            expectRegs[Integer.parseInt(args[i])] = Integer.parseInt(args[i + 1]);
            testRegs[Integer.parseInt(args[i])] = true;
        }
    }

    public static void setBitMask(BitsFlag[] bitmasks, int i, BitsFlag mask) {
        bitmasks[i] = mask;
    }

    public static void setAllBitMasks(BitsFlag[] bitmasks, BitsFlag mask) {
        for (int i = 0; i < bitmasks.length; i++) {
            setBitMask(bitmasks, i, mask);
        }
    }

    public static void enableDebug() {
        DEBUG = true;
    }

    public static void disableDebug() {
        DEBUG = false;
    }

    protected boolean validateRegisters(long[] simRegisters, long[] expectedRegisters, boolean[] testRegisters) {
        boolean valid   = true;
        long    bitmask = 0;
        for (int i = 0; i < NUM_REGS; i++) {
            log(i + " sim: " + simRegisters[i] + " exp: " + expectedRegisters[i] + " test: " + testRegisters[i]);
            if (testRegisters[i]) {
                final long simulatedRegister = simRegisters[i] & bitMasks[i].value();
                final long expectedRegister  = expectedRegisters[i];
                if (simulatedRegister != expectedRegister) {
                    bitmask = bitmask | (1 << i);
                    valid = false;
                }
            }
        }
        if (!valid) {
            for (int i = 0; i < NUM_REGS; i++) {
                System.out.println(i + " sim: " + simRegisters[i] + " exp: " + expectedRegisters[i] + " test: " + testRegisters[i]);
            }
        }
        return valid;
    }

    protected void log(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }

    protected Process runBlocking(ProcessBuilder processBuilder) {
        Process process = null;
        try {
            process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return process;
    }

    protected void terminateProcess(Process process) {
        if (process != null) {
            process.destroy();
        }
    }

    public void cleanProcesses() {
        terminateProcess(gcc);
        terminateProcess(assembler);
        terminateProcess(linker);
        terminateProcess(gdb);
        terminateProcess(qemu);
    }

    protected void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public void cleanFiles() {
        deleteFile(qemuOutput);
        deleteFile(qemuErrors);
        deleteFile(bindOutput);
        deleteFile(gdbOutput);
        deleteFile(gdbErrors);
        deleteFile(gccOutput);
        deleteFile(gccErrors);
        deleteFile(asOutput);
        deleteFile(asErrors);
        deleteFile(linkOutput);
        deleteFile(linkErrors);
    }

    public void reset() {
        if (RESET) {
            cleanFiles();
        }
        cleanProcesses();
    }

    protected void bindToQemu() throws InterruptedException, IOException {
        do {
            ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:1234");
            bindTest.redirectOutput(bindOutput);
            bindTest.start().waitFor();
            FileInputStream inputStream = new FileInputStream(bindOutput);
            if (inputStream.available() != 0) {
                log("CrossISATester: qemu ready");
                inputStream.close();
                break;
            } else {
                log("CrossISATester: gemu not ready");
                Thread.sleep(500);
            }
        } while (true);
    }

    protected long[] parseRegistersToFile(String file, String startPattern, String endPattern, int numberOfRegisters) throws IOException {
        BufferedReader reader       = new BufferedReader(new FileReader(file));
        String         line;
        boolean        enabled      = false;
        int            i            = 0;
        long[]         parsedValues = new long[numberOfRegisters];
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startPattern)) {
                enabled = true;
                line = line.substring(6, line.length());
            }
            if (!enabled) {
                continue;
            }
            String value = line.split("\\s+")[1];

            BigInteger tmp = new BigInteger(value.substring(2, value.length()), 16);
            if (tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                BigInteger result = BigInteger.valueOf(Long.MIN_VALUE);
                result = result.multiply(BigInteger.valueOf(2)).add(tmp);
                if (result.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
                    throw FatalError.unimplemented();
                } else {
                    parsedValues[i] = result.longValue();
                }
            } else {
                parsedValues[i] = tmp.longValue();
            }
            if (++i >= numberOfRegisters) {
                break;
            }
            if (line.contains(endPattern)) {
                enabled = false;
            }
        }
        reader.close();
        return parsedValues;
    }

    protected void initializeQemu() {
        ENABLE_SIMULATOR = Integer.getInteger(ENABLE_QEMU) != null && Integer.getInteger(ENABLE_QEMU) > 0;
    }

    public enum BitsFlag {
        NZCBits(0xe0000000), NZCVBits(0xf0000000), Lower16Bits(0x0000ffff), Upper16Bits(0xffff0000),
        All32Bits(0xffffffff), Lower32Bits(0xffffffff), All64Bits(0xffffffffffffffffL);

        private final long value;

        BitsFlag(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    public void runSimulation(ProcessBuilder gdbProcess, ProcessBuilder qemuProcess) {
        gdbProcess.redirectOutput(gdbOutput);
        gdbProcess.redirectError(gdbErrors);
        qemuProcess.redirectOutput(qemuOutput);
        qemuProcess.redirectError(qemuErrors);
        try {
            qemu = qemuProcess.start();
            while (!qemuOutput.exists()) {
                Thread.sleep(500);
            }
            bindToQemu();
            gdb = runBlocking(gdbProcess);
        } catch (Exception e) {
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
    }

    public void runSimulation() throws Exception {
        long[] simulatedRegisters = runRegisteredSimulation();
        if (!validateRegisters(simulatedRegisters, expectRegs, testRegs)) {
            cleanProcesses();
            assert false : "Error while validating registers";
        }
    }

    protected abstract long[] runRegisteredSimulation() throws Exception;

    public void link() {
        final ProcessBuilder link = getLinkerProcessBuilder();
        link.redirectOutput(linkOutput);
        link.redirectError(linkErrors);
        linker = runBlocking(link);
    }

    protected abstract ProcessBuilder getLinkerProcessBuilder();

    public void compile() {
        final ProcessBuilder removeFiles = new ProcessBuilder("/bin/rm", "-rR", "test.elf");
        final ProcessBuilder compile = getCompilerProcessBuilder();
        compile.redirectOutput(gccOutput);
        compile.redirectError(gccErrors);
        runBlocking(removeFiles);
        gcc = runBlocking(compile);
    }

    protected abstract ProcessBuilder getCompilerProcessBuilder();

    public void assembleStartup() {
        final ProcessBuilder assemble = getAssemblerProcessBuilder();
        assemble.redirectOutput(asOutput);
        assemble.redirectError(asErrors);
        assembler = runBlocking(assemble);
    }

    protected abstract ProcessBuilder getAssemblerProcessBuilder();

    public void run() throws Exception {
        assembleStartup();
        compile();
        link();
        runSimulation();
    }
}