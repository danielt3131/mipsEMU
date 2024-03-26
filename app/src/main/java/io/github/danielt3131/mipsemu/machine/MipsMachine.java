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

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Scanner;

import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.Reference;

/**
 * The mips emulator that will read a file written in MIPS and then execute those commands using virtual registers
 */
public class MipsMachine {

    //Register Variables
    private int hi, lo; //high and low of multiplication and division
    private int pc; //program counter

    private int[] register = new int[32];

    //Machine memory
    private byte[] memory;

    //labels
    private ArrayList<Label> labels;

    private MachineInterface machineInterface;
    private InputStream inputFileStream;
    private int memoryFormat;

    /**
     * Constructor for the mips emulator
     *
     * @param memorySize the amount of memory the machine will have in bytes
     */
    public MipsMachine(int memorySize, MachineInterface machineInterface){

        memory = new byte[memorySize];
        this.machineInterface = machineInterface;

    }

    public void setInputFileStream(InputStream inputFileStream) {
        this.inputFileStream = inputFileStream;
    }

    public void setMemoryFormat(int memoryFormat) {
        this.memoryFormat = memoryFormat;
    }

    /**
     * Reads in a file and puts the instructions into memory
     */
    public void readFile() throws FileNotFoundException {

        int tp = 0; //tp for text pointer : where to place word in text block of memory

        Scanner fileScanner = new Scanner(inputFileStream);

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();
            Scanner lineScanner = new Scanner(line);

            //Get where to start writing code
            lineScanner.useDelimiter(":");
            tp = lineScanner.nextInt(2);

            //System.out.println("Starting writing at memory " + tp);

            lineScanner.reset();

            //Get rest of the line
            String code = lineScanner.nextLine();

            //remove ':'
            code = code.substring(1);
            //System.out.println(code);
            //remove spaces
            while(code.contains(" "))
            {
                code = code.substring(0, code.indexOf(" ")) + code.substring(code.indexOf(" ") + 1);
                //System.out.println(code);
            }
            //look at next 8 characters and convert to byte and then put in memory
            while(code.length() != 0)
            {
                String part = code.substring(0,8);
                Log.d("MipsMachine.readFile Part", part);
                byte b = (byte)Integer.parseInt(part,2); //Byte.parseByte crashes due to signed bit so this is a workaround
                memory[tp] = b;
                machineInterface.updateMemoryDisplay(Arrays.toString(memory));
                code = code.substring(8);
                tp++;
                //System.out.printf("Writing %d at %d%n", b, tp - 1);
            }

        }
    }

    /**
     * has the machine read from the program counter to fetch and execute the next instruction
     */
    public void nextStep() {
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


    public void sendMemory() {
        String memoryStr = "";
        if (memoryFormat == Reference.HEX_MODE) {
            memoryStr = HexFormat.ofDelimiter(" ").formatHex(memory);
        } else if (memoryFormat == Reference.BINARY_MODE) {
            memoryStr = new BigInteger(memory).toString();
        } else if (memoryFormat == Reference.DECIMIAL_MODE) {
            memoryStr = Arrays.toString(memory);
        }
        // Pass the memoryStr to update memory
        machineInterface.updateMemoryDisplay(memoryStr);
    }

}
