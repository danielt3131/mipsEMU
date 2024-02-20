package io.github.danielt3131.mipsemu.machine;

import java.io.File;

/**
 * The mips emulator that will read a file written in MIPS and then execute those commands using virtual registers
 */
public class MipsMachine {

    //Register Variables
    private final int zero = 0; //zero register $0
    private int hi, lo; //high and low of multiplication and division
    private int pc; //program counter
    private int t0, t1, t2, t3, t4, t5, t6, t7, t8, t9; //temporary registers $8-$15, $24,$25
    private int a0, a1, a2, a3; //argument registers $4-$7
    private int v0, v1; //result registers $2, $3
    private int s0, s1, s2, s3, s4, s5, s6, s7; //saved registers $16-$23
    private int at, k0, k1, gp, fp, sp, ra; //specific-purpose registers $1, $26-$31

    //Machine memory
    private byte[] memory;

    //The file with mips instructions written
    private File file;

    /**
     * Constructor for the mips emulator
     * @param memorySize the amount of memory the machine will have in bytes
     */
    public MipsMachine(int memorySize)
    {
        memory = new byte[memorySize];
        pc = 0;
    }

    /**
     * Reads in a file and puts the instructions into memory
     * @param file the mips file to read from
     */
    public void readFile(File file)
    {
        //todo read in a file and put it into the memory
    }

    /**
     * has the machine read from the program counter to fetch and execute the next instruction
     */
    public void nextStep()
    {
        //todo have the machine read from the program counter to fetch and execute the next instruction
    }


}
