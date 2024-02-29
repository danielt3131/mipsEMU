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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

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
    private Word[] memory;

    //labels
    private ArrayList<Label> labels;

    //The file with mips instructions written
    private File file;

    /**
     * Constructor for the mips emulator
     *
     * @param memorySize the amount of memory the machine will have in bytes
     */
    public MipsMachine(int memorySize) throws MemorySizeException {
        if (memorySize % 4 != 0) {
            throw new MemorySizeException("Memory must be multiple of 4 bytes");
        }
        memory = new Word[memorySize / 4];
        pc = 0;
    }

    /**
     * Reads in a file and puts the instructions into memory
     *
     * @param fileDir the mips file location to read from
     */
    public void readFile(String fileDir) throws FileNotFoundException {

        int tp = 0; //tp for text pointer : where to place word in text block of memory

        file = new File(fileDir);
        if (!file.exists() || !file.canRead()) {
            throw new FileNotFoundException();
        }

        Scanner fileScanner = new Scanner(file);
        Scanner lineScanner;

        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();

            //Ignore empty lines
            if (line.isEmpty()) {
                continue;
            }

            //Trim leading and trailing whitespace
            line = line.trim();

            //Ignore comment lines
            if (line.startsWith("#")) {
                continue;
            }

            //Now remove comments from end of line if it contains a comment
            if (line.contains("#")) {
                line = line.substring(0, line.indexOf("#"));
            }

            //See if line is a label
            if (line.endsWith(":")) {
                labels.add(new Label(line.substring(0, line.indexOf(":")), tp));
                continue;
            }

            //Generate word from line
            lineScanner = new Scanner(line);

            Code code = new Code();

            code.opCode = lineScanner.next();

            if (lineScanner.hasNext()) {
                code.val1 = lineScanner.next();
            }

            if (lineScanner.hasNext()) {
                code.val2 = lineScanner.next();
            }

            if (lineScanner.hasNext()) {
                code.val3 = lineScanner.next();
            }

            memory[tp] = code;
            tp++;
            System.out.println(code);
        }
    }

    /**
     * has the machine read from the program counter to fetch and execute the next instruction
     */
    public void nextStep() {
        //todo have the machine read from the program counter to fetch and execute the next instruction
    }


    public static class MemorySizeException extends Exception {
        public MemorySizeException(String message) {
            super(message);
        }
    }

}
