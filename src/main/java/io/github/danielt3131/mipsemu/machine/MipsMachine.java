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
    private int hi, lo; //high and low of multiplication and division
    private int pc; //program counter

    private int[] register = new int[32];

    //Machine memory
    private Integer[] memory;

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
        memory = new Integer[memorySize / 4];
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

            Integer word = Integer.parseInt(line);

            memory[tp] = word;
            tp++;
            System.out.println(word);
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
