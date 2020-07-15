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
package com.oracle.max.vm.tests.crossisa;

import com.sun.cri.ci.CiRegister;

import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;

/////////////////////////////////////
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/////////////////////////////////////

import static com.oracle.max.vm.tests.crossisa.CrossISATester.BitsFlag.All64Bits;
import static com.oracle.max.vm.tests.crossisa.CrossISATester.BitsFlag.Lower32Bits;

public abstract class CrossISATester {

    private static final   String ENABLE_QEMU    = "test.crossisa.qemu";

    protected static  int    port_counter   =40004;      
    protected static  int    name_counter   =0;    

    protected static  File   og_gdb = new File("gdb_input");
    protected static  File   og_elf = new File("test.elf");
    protected static  String rplc_path,elf_path;

    protected   File   new_elf;
    protected   File   new_gdb;


    protected static  File   qemuOutput;
    protected static  File   qemuErrors;
    private static    File   bindOutput;
    protected static  File   gdbOutput ;
    protected static  String gdbInput  ;
    protected static  File   gdbErrors ;
    protected static  File   gccOutput ;
    protected static  File   gccErrors ;
    protected static  File   asOutput  ;
    protected static  File   asErrors  ;
    protected static  File   linkOutput;
    protected static  File   linkErrors;

    public static  boolean ENABLE_SIMULATOR = true;
    private static boolean RESET            = false;
    private static boolean DEBUG            = false;

    protected int my_port,my_name;

    private static final int MAX_NUMBER_OF_REGISTERS = 32;
    protected Process gcc;
    protected Process assembler;
    protected Process linker;
    protected Process qemu;
    protected Process gdb;
    protected ProcessBuilder gccProcessBuilder;
    protected ProcessBuilder linkerProcessBuilder;
    protected ProcessBuilder qemuProcessBuilder;
    protected ProcessBuilder gdbProcessBuilder;
    private ProcessBuilder removeFiles;

    protected BitsFlag[] bitMasks                 = new BitsFlag[MAX_NUMBER_OF_REGISTERS];
    protected int[]      simulatedIntRegisters    = new int[MAX_NUMBER_OF_REGISTERS];
    protected int[]      expectedIntRegisters     = new int[MAX_NUMBER_OF_REGISTERS];
    protected boolean[]  testIntRegisters         = new boolean[MAX_NUMBER_OF_REGISTERS];
    protected long[]     simulatedLongRegisters   = new long[MAX_NUMBER_OF_REGISTERS];
    protected long[]     expectedLongRegisters    = new long[MAX_NUMBER_OF_REGISTERS];
    protected boolean[]  testLongRegisters        = new boolean[MAX_NUMBER_OF_REGISTERS];
    protected float[]    simulatedFloatRegisters  = new float[MAX_NUMBER_OF_REGISTERS];
    protected float[]    expectedFloatRegisters   = new float[MAX_NUMBER_OF_REGISTERS];
    protected boolean[]  testFloatRegisters       = new boolean[MAX_NUMBER_OF_REGISTERS];
    protected double[]   simulatedDoubleRegisters = new double[MAX_NUMBER_OF_REGISTERS];
    protected double[]   expectedDoubleRegisters  = new double[MAX_NUMBER_OF_REGISTERS];
    protected boolean[]  testDoubleRegisters      = new boolean[MAX_NUMBER_OF_REGISTERS];

    protected CrossISATester() {
        sync();
        
        qemuOutput     = new File("qemu_output"+my_name);
        qemuErrors     = new File("qemu_errors"+my_name);
        bindOutput     = new File("bind_output"+my_name);
        gdbOutput      = new File("gdb_output"+my_name);
        gdbInput       = "gdb_input"+my_name;

	new_gdb        = new File(gdbInput);	
        new_elf        = new File("test"+my_name+".elf");
        elf_path       ="test" +my_name+ ".elf";
	
	cpf(og_elf,new_elf);                                    //copy gdb_input and test.elf to new files
        cpf(og_gdb,new_gdb);

        rplc(gdbInput,"test.elf",elf_path);                     //replace port and test.elf with the new ones
        rplc(gdbInput,"1234",""+my_port);

        gdbErrors      = new File("gdb_errors"+my_name);
        gccOutput      = new File("gcc_output"+my_name);
        gccErrors      = new File("gcc_errors"+my_name);
        asOutput       = new File("as_output"+my_name);
        asErrors       = new File("as_errors"+my_name);
        linkOutput     = new File("link_output"+my_name);
        linkErrors     = new File("link_errors"+my_name);
        //////////////////////////////////////////
        System.out.println("CrossISATester: Hi im CrossISATester"+my_name+" with port: "+my_port);
        gccProcessBuilder = getCompilerProcessBuilder();
        gccProcessBuilder.redirectOutput(gccOutput);
        gccProcessBuilder.redirectError(gccErrors);
        linkerProcessBuilder = getLinkerProcessBuilder();
        linkerProcessBuilder.redirectOutput(linkOutput);
        linkerProcessBuilder.redirectError(linkErrors);
        qemuProcessBuilder = getQEMUProcessBuilder();
        qemuProcessBuilder.redirectOutput(qemuOutput);
        qemuProcessBuilder.redirectError(qemuErrors);
        gdbProcessBuilder = getGDBProcessBuilder();
        gdbProcessBuilder.redirectOutput(gdbOutput);
        gdbProcessBuilder.redirectError(gdbErrors);
        removeFiles = new ProcessBuilder("/bin/rm", "-rfR", elf_path);
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

    public int[] getSimulatedIntRegisters() {
        return simulatedIntRegisters;
    }

    public long[] getSimulatedLongRegisters() {
        return simulatedLongRegisters;
    }

    public float[] getSimulatedFloatRegisters() {
        return simulatedFloatRegisters;
    }

    public double[] getSimulatedDoubleRegisters() {
        return simulatedDoubleRegisters;
    }

    public void resetTestValues() {
        for (int i = 0; i < testIntRegisters.length; i++) {
            testIntRegisters[i] = false;
        }
        for (int i = 0; i < testLongRegisters.length; i++) {
            testLongRegisters[i] = false;
        }
        for (int i = 0; i < testFloatRegisters.length; i++) {
            testFloatRegisters[i] = false;
        }
        for (int i = 0; i < testDoubleRegisters.length; i++) {
            testDoubleRegisters[i] = false;
        }
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, long expectedValue) {
        final int index = cpuRegister.getEncoding();
        expectedLongRegisters[index] = expectedValue;
        testLongRegisters[index] = true;
        bitMasks[index] = All64Bits;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, int expectedValue) {
        final int index = cpuRegister.getEncoding();
        expectedLongRegisters[index] = expectedValue;
        testLongRegisters[index] = true;
        bitMasks[index] = Lower32Bits;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, short expectedValue) {
        setExpectedValue(cpuRegister, (int) expectedValue);
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, boolean expectedValue) {
        setExpectedValue(cpuRegister, expectedValue ? 1 : 0);
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, byte expectedValue) {
        setExpectedValue(cpuRegister, (int) expectedValue);
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param cpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister cpuRegister, char expectedValue) {
        setExpectedValue(cpuRegister, (int) expectedValue & 0xFFFF);
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param fpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister fpuRegister, float expectedValue) {
        final int index = fpuRegister.getEncoding();
        expectedFloatRegisters[index] = expectedValue;
        testFloatRegisters[index] = true;
    }

    /**
     * Sets the expected value of a register and enables it for inspection.
     *
     * @param fpuRegister the number of the cpuRegister
     * @param expectedValue the expected value
     */
    public void setExpectedValue(CiRegister fpuRegister, double expectedValue) {
        final int index = fpuRegister.getEncoding();
        expectedDoubleRegisters[index] = expectedValue;
        testDoubleRegisters[index] = true;
    }

    public boolean validateIntRegisters() {
        boolean valid   = true;

        assert expectedIntRegisters != null;
        assert simulatedIntRegisters != null;
        assert testIntRegisters != null;
        for (int i = 0; i < simulatedIntRegisters.length; i++) {
            log(i + " sim: " + simulatedIntRegisters[i] + " exp: " + expectedIntRegisters[i] + " test: " + testIntRegisters[i]);
            if (testIntRegisters[i]) {
                final int simulatedRegister = simulatedIntRegisters[i] & (int) bitMasks[i].value();
                final int expectedRegister  = expectedIntRegisters[i] & (int) bitMasks[i].value();
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            System.out.println("validateIntRegisters FAILED");
            for (int i = 0; i < simulatedIntRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedIntRegisters[i] + " exp: " + expectedIntRegisters[i]
                                   + " test: " + testIntRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateLongRegisters() {
        boolean valid   = true;

        assert expectedLongRegisters != null;
        assert simulatedLongRegisters != null;
        assert testLongRegisters != null;
        for (int i = 0; i < simulatedLongRegisters.length; i++) {
            log(i + " sim: " + simulatedLongRegisters[i] + " exp: " + expectedLongRegisters[i] + " test: " + testLongRegisters[i]);
            if (testLongRegisters[i]) {
                final long simulatedRegister = simulatedLongRegisters[i] & bitMasks[i].value();
                final long expectedRegister  = expectedLongRegisters[i] & bitMasks[i].value();
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            System.out.println("validateLongRegisters FAILED");
            for (int i = 0; i < simulatedLongRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedLongRegisters[i] + " exp: " + expectedLongRegisters[i]
                                   + " test: " + testLongRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateFloatRegisters() {
        boolean valid   = true;

        assert expectedFloatRegisters != null;
        assert simulatedFloatRegisters != null;
        assert testFloatRegisters != null;
        for (int i = 0; i < simulatedFloatRegisters.length; i++) {
            log(i + " sim: " + simulatedFloatRegisters[i] + " exp: " + expectedFloatRegisters[i] + " test: " + testFloatRegisters[i]);
            if (testFloatRegisters[i]) {
                final float simulatedRegister = simulatedFloatRegisters[i];
                final float expectedRegister  = expectedFloatRegisters[i];
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            System.out.println("validateFloatRegisters FAILED");
            for (int i = 0; i < simulatedFloatRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedFloatRegisters[i] + " exp: " + expectedFloatRegisters[i]
                                   + " test: " + testFloatRegisters[i]);
            }
        }

        return valid;
    }

    public boolean validateDoubleRegisters() {
        boolean valid   = true;

        assert expectedDoubleRegisters != null;
        assert simulatedDoubleRegisters != null;
        assert testDoubleRegisters != null;
        for (int i = 0; i < simulatedDoubleRegisters.length; i++) {
            log(i + " sim: " + simulatedDoubleRegisters[i] + " exp: " + expectedDoubleRegisters[i] + " test: " + testDoubleRegisters[i]);
            if (testDoubleRegisters[i]) {
                final double simulatedRegister = simulatedDoubleRegisters[i];
                final double expectedRegister  = expectedDoubleRegisters[i];
                if (simulatedRegister != expectedRegister) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            System.out.println("validateDoubleRegisters FAILED");
            for (int i = 0; i < simulatedDoubleRegisters.length; i++) {
                System.out.println(i + " sim: " + simulatedDoubleRegisters[i] + " exp: " + expectedDoubleRegisters[i]
                                   + " test: " + testDoubleRegisters[i]);
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
            final int exitCode = process.waitFor();
            assert exitCode == 0 : processBuilder.command() + " returned exitCode = " + exitCode;
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
        System.out.println("CrossISATester: cleanProcesses");
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
        System.out.println("CrossISATester: cleanFiles");
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
        //deleteFile(new_gdb);
        deleteFile(new_elf);
    }

    public void reset() {
        System.out.println("CrossISATester: reset");
        System.out.println("#########################################");
        if (RESET) {
            cleanFiles();
        }
        cleanProcesses();
    }

    protected void bindToQemu() throws InterruptedException, IOException {
       System.out.println("-----------------------------------------"); 
       System.out.println("CrossISATester: bindToQemu");
        do {
	    System.out.println("bindToQemu at port: "+my_port);
            ProcessBuilder bindTest = new ProcessBuilder("lsof", "-i", "TCP:"+my_port);

            bindTest.redirectOutput(bindOutput);
            System.out.println("bindToQemu bindOutput: "+bindOutput);
            bindTest.start().waitFor();
            System.out.println("CRISA: after waitFor");
            FileInputStream inputStream = new FileInputStream(bindOutput);
            if (inputStream.available() != 0) {
                log("CrossISATester: qemu ready");
                inputStream.close();
                break;
            } else {
                System.out.println("CRSISA: Qemu not ready");
                log("CrossISATester: qemu not ready");
                Thread.sleep(500);
            }
        } while (true);
	System.out.println("BINDED");
    }

    /**
     * Parses the integer registers (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *   r0             0x43d78  277880
     *   r1             0x40000000        1073741824
     *   r2             0x21     33
     *   r3             0x43d08  277768
     *   r4             0x0      0
     * </pre>
     *
     * @param startRegister The first integer register to parse
     * @param endRegister The last integer register to parse
     * @return An array with the parsed values of the integer registers
     * @throws IOException
     */
    protected void parseIntRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader     reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Integer> parsedValues = new ArrayList<>(32);
        String             line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseIntRegister(line));
        } while ((line = reader.readLine()) != null);
        for (int i = 0; i < parsedValues.size(); i++) {
            simulatedIntRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses an integer register (32-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *   r0             0x43d78  277880
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed integer value of the register
     */
    protected int parseIntRegister(String line) {
        String value = line.split("\\s+")[1];
        assert value.startsWith("0x");
        assert value.length() - 2 <= 8;
        BigInteger tmp = new BigInteger(value.substring(2, value.length()), 16);
        if (tmp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
            BigInteger result = BigInteger.valueOf(Integer.MIN_VALUE);
            tmp = result.multiply(BigInteger.valueOf(2)).add(tmp);
            assert tmp.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 : "Parsed non int value";
        }
        return tmp.intValue();
    }

    /**
     * Parses the long registers (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     r0             0x43d78  277880
     *     r1             0x40000000        1073741824
     *     r2             0x21     33
     *     r3             0x43d08  277768
     *     r4             0x0      0
     * </pre>
     *
     * @param startRegister The first integer register to parse
     * @param endRegister The last integer register to parse
     * @return An array with the parsed values of the integer registers
     * @throws IOException
     */
    protected void parseLongRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader  reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Long> parsedValues = new ArrayList<>(32);
        String          line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseLongRegister(line));
        } while ((line = reader.readLine()) != null);
        for (int i = 0; i < parsedValues.size(); i++) {
            simulatedLongRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a long register (64-bit) from the output of the gdb command {@code info all-registers}.  The output is
     * expected to be in the form:
     *
     * <pre>
     *     r0             0x43d78  277880
     * </pre>
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed integer value of the register
     */
    protected long parseLongRegister(String line) {
        String value = line.split("\\s+")[1];
        assert value.startsWith("0x");
        assert value.length() - 2 <= 16;
        BigInteger tmp = new BigInteger(value.substring(2, value.length()), 16);
        if (tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) {
            BigInteger result = BigInteger.valueOf(Long.MIN_VALUE);
            tmp = result.multiply(BigInteger.valueOf(2)).add(tmp);
            assert tmp.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 : "Parsed non long value";
        }
        return tmp.longValue();
    }

    /**
     * Parses the float registers (32-bit) from the output of the gdb command {@code info all-registers}.
     *
     * @param startRegister The first float register to parse
     * @param endRegister The last float register to parse
     * @return An array with the parsed values of the float registers
     * @throws IOException
     */
    protected void parseFloatRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader   reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Float> parsedValues = new ArrayList<>(32);
        String           line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseFloatRegister(line));
        } while ((line = reader.readLine()) != null);
        for (int i = 0; i < parsedValues.size(); i++) {
            simulatedFloatRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a float register (32-bit) from the output of the gdb command {@code info all-registers}.
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed float value of the register
     */
    protected abstract float parseFloatRegister(String line);

    /**
     * Parses the double registers (64-bit) from the output of the gdb command {@code info all-registers}.
     *
     * @param startRegister The first double register to parse
     * @param endRegister The last double register to parse
     * @return An array with the parsed values of the double registers
     * @throws IOException
     */
    protected void parseDoubleRegisters(String startRegister, String endRegister) throws IOException {
        BufferedReader    reader       = new BufferedReader(new FileReader(gdbOutput));
        ArrayList<Double> parsedValues = new ArrayList<>(32);
        String            line;
        // Look for the startRegister
        while ((line = reader.readLine()) != null) {
            if (line.startsWith(startRegister)) {
                break;
            }
        }
        assert line != null : "Reached EOF before matching " + startRegister;
        // Parse the registers
        do {
            if (line.contains(endRegister)) {
                break;
            }
            parsedValues.add(parseDoubleRegister(line));
        } while ((line = reader.readLine()) != null);
        for (int i = 0; i < parsedValues.size(); i++) {
            simulatedDoubleRegisters[i] = parsedValues.get(i);
        }
        reader.close();
    }

    /**
     * Parses a double register (64-bit) from the output of the gdb command {@code info all-registers}.
     *
     * @param line The line from the gdb output to be parsed
     * @return The parsed double value of the register
     */
    protected abstract double parseDoubleRegister(String line);

    /**
     * Gets an int with the encoding of a hex string representing a float.
     *
     * @param hex The hex string to parse
     * @return The encoding of the float parsed from {@code hex}
     */
    protected static int hexToIntBits(String hex) {
        assert hex.length() <= 8;
        if (hex.length() == 8) { // Split hex string to allow parsing
            int lsbs = hexToIntBits(hex.substring(4));
            int msbs = hexToIntBits(hex.substring(0, 4));
            return msbs << 16 | lsbs;
        }
        return Integer.parseInt(hex, 16);
    }

    /**
     * Gets a long with the encoding of a hex string representing a double.
     *
     * @param hex The hex string to parse
     * @return The encoding of the double parsed from {@code hex}
     */
    protected static long hexToLongBits(String hex) {
        assert hex.length() <= 16;
        if (hex.length() == 16) { // Split hex string to allow parsing
            long lsbs = hexToLongBits(hex.substring(8));
            long msbs = hexToLongBits(hex.substring(0, 8));
            return msbs << 32 | lsbs;
        }
        return Long.parseLong(hex, 16);
    }

    protected void initializeQemu() {
        System.out.println("CrossISATester: initializeQemu");
        final Integer enableQemuProperty = Integer.getInteger(ENABLE_QEMU);
        if (enableQemuProperty != null && enableQemuProperty <= 0) {
            System.out.println("false");
            ENABLE_SIMULATOR = false;
        }
    }

    public enum BitsFlag {
        NZCBits(0xe0000000L), NZCVBits(0xf0000000L), Lower8Bits(0x0000ffL), Lower16Bits(0x0000ffffL), Upper16Bits(0xffff0000L),
        All32Bits(0xffffffffL), Lower32Bits(0xffffffffL), All64Bits(0xffffffffffffffffL);

        private final long value;

        BitsFlag(long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    public void runSimulation() throws Exception {
        try {
            System.out.println("CrossISATester: runSimulation");
            qemu = qemuProcessBuilder.start();
            while (!qemuOutput.exists()) {
                Thread.sleep(500);
            }
            bindToQemu();
            gdb = runBlocking(gdbProcessBuilder);
        } catch (Exception e) {
            e.printStackTrace();
            cleanProcesses();
            System.exit(-1);
        }
    }

    public void link() {
        System.out.println("CrossISATester: link");
        linker = runBlocking(linkerProcessBuilder);
    }

    protected abstract ProcessBuilder getLinkerProcessBuilder();

    public void compile() {
        System.out.println("CrossISATester: compile");
        runBlocking(removeFiles);
        gcc = runBlocking(gccProcessBuilder);
    }

    protected abstract ProcessBuilder getCompilerProcessBuilder();

    protected abstract ProcessBuilder getQEMUProcessBuilder();

    protected ProcessBuilder getGDBProcessBuilder() {
        if (gdbProcessBuilder != null) {
            return gdbProcessBuilder;
        }
        return new ProcessBuilder("gdb-multiarch", "-q", "-x", gdbInput);
    }

    public void run() throws Exception {
        System.out.println("CrossISATester: Run");
        compile();
        link();
        runSimulation();
    }

    protected synchronized void sync(){
	name_counter++;
        port_counter++;

	my_name=name_counter;
	my_port=port_counter;
    }

    protected synchronized void sync_files(){


    }

    protected void cpf(File file1,File file2){
        FileChannel src,dest ;
        try {
            file2.createNewFile();
            src = new FileInputStream(file1).getChannel();
            dest = new FileOutputStream(file2).getChannel();
            dest.transferFrom(src, 0, src.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void rplc(String path1,String torep,String rep){
        try {
            Path path = Paths.get(path1);
            Stream<String> lines = Files.lines(path);
            List<String> replaced = lines.map(line -> line.replaceAll(torep, rep)).collect(Collectors.toList());
            Files.write(path, replaced);
            lines.close();
            //System.out.println("CrossISA: rplc done");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
    
