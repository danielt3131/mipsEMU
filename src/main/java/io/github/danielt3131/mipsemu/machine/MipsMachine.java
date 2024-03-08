/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 or later.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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

    //HELPER METHODS

    /**
     * grabs right bits from an int
     * @param data the integer to grab bits from
     * @param n the amount of bits to grab
     * @return the grabbed bits
     */
    public int grabRightBits(int data, int n)
    {
        int mask = (int)Math.pow(2,n) - 1;
        return data & mask;
    }

    /**
     * grabs left bits from an int
     * @param data the integer to grab bits from
     * @param n the amount of bits to grab
     * @return the grabbed bits
     */
    public int grabLeftBits(int data, int n)
    {
        return data >>> 32-n;
    }


    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     * @param b1 byte 1
     * @param b2 byte 2
     * @param b3 byte 3
     * @param b4 byte 4
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2, byte b3, byte b4)
    {
        int result = 0;
        result += b1;
        result = result << 8;
        result += b2;
        result = result << 8;
        result += b3;
        result = result << 8;
        result += b4;

        return result;
    }

    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     * @param b1 byte 1
     * @param b2 byte 2
     * @param b3 byte 3
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2, byte b3)
    {
        int result = 0;
        result += b1;
        result = result << 8;
        result += b2;
        result = result << 8;
        result += b3;

        return result;
    }

    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     * @param b1 byte 1
     * @param b2 byte 2
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2)
    {
        int result = 0;
        result += b1;
        result = result << 8;
        result += b2;

        return result;
    }


}
