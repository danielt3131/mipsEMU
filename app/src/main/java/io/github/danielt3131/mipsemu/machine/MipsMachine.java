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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Scanner;

import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.Reference;
import io.github.danielt3131.mipsemu.ui.MachineActivity;

/**
 * The mips emulator that will read a file written in MIPS and then execute those commands using virtual registers
 */
public class MipsMachine {

    private final int EOS = -1; //end of step code
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
    private int displayFormat;
    private Scanner fileScanner;

    /**
     * Constructor for the mips emulator
     *
     * @param memorySize the amount of memory the machine will have in bytes
     */
    public MipsMachine(int memorySize, MachineInterface machineInterface){

        memory = new byte[memorySize];
        this.machineInterface = machineInterface;
        mstep = 0;

    }

    public void setInputFileStream(InputStream inputFileStream) {
        this.inputFileStream = inputFileStream;
        fileScanner = new Scanner(inputFileStream);
        if (fileScanner.hasNext("State")) {
            readState();
        } else {
            readFile();
        }
    }

    /**
     * Sets the display format
     * @param displayFormat The format specifier defined in {@link Reference}
     */
    public void setDisplayFormat(int displayFormat) {
        this.displayFormat = displayFormat;
    }

    /**
     * Reads in a file and puts the instructions into memory
     */
    public void readFile() {

        int tp = 0; //tp for text pointer : where to place word in text block of memory


        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();
            Scanner lineScanner = new Scanner(line);

            //Get where to start writing code
            lineScanner.useDelimiter(":"); // so it stops at :
            String code = lineScanner.next();
            //removes 0x from string
            code = code.substring(2);
            //now turn it into an int
            tp = Integer.parseInt(code,16);

            //System.out.println("Starting writing at memory " + tp);

            lineScanner.reset();

            //Get rest of the line
            code = lineScanner.nextLine();

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
     * reads from file a state and loads it into machine
     */
    public void readState()
    {

        //reads register
        for(int i = 0; i < 32; i++)
        {
            register[i] = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        }
        //reads pc, hi, lo

        pc = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        hi = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        lo = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());

        //reads amount of memory
        int memSize = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        memory = new byte[memSize];

        //sets the stack
        int stackSize = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        for(int i = memSize; i > memSize - stackSize; i--)
        {
            memory[i] = fileScanner.nextByte();
        }

        //sets the text
        int textSize = combineBytes(fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte(), fileScanner.nextByte());
        for(int i = 0; i < textSize; i++)
        {
            memory[i] = fileScanner.nextByte();
        }

    }

    private int getCode()
    {
        return combineBytes(memory[pc], memory[pc+1], memory[pc+2], memory[pc+3]);
    }

    private int mstep; //the micro step to run
    private int code; //the instruction word to run

    /**
     * has the machine read from the program counter to fetch and execute the next instruction
     */
    private void nextStep() {
        //combines the 4 bytes into the full word
        code = getCode();
        Log.d("Code", Integer.toBinaryString(code));
        boolean running = true;
        while(running) //keeps executing until it returns EOS when step is done
        {
            running = nextMicroStep() != EOS;
        }
    }

    /**
     * Method to run the next step as requested from the user or MipsMachine
     */
    public void runNextStep() {
        nextStep();
    }

    /**
     * Method to run next micro step as requested from the user or MipsMachine
     */
    public void runNextMicroStep() {
        nextMicroStep();
    }

    public void runContinuously() {
        // Run continuously
    }

    private int nextMicroStep()
    {
        Log.d("mstep", "MSTEP: " + mstep);

        if(code == 0) return EOS;

        //R-type instruction
        if(grabLeftBits(code,6) == 0)
        {

            //Add
            if(grabRightBits(code,6) == 0b100000)
            {
                int s = grabRightBits(grabLeftBits(code,11),5); //source 1
                int t = grabRightBits(grabLeftBits(code,16),5); //source 2
                int d = grabRightBits(grabLeftBits(code,21),5); //destination

                if(mstep == 0)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 1)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 2)
                {
                    sendToDisplay("Sending \"add\" to ALU");
                    mstep++;
                    return 0;
                }
                else if(mstep == 3)
                {
                    sendToDisplay(String.format(Locale.US,"Retrieved %d from ALU", register[t] + register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 4)
                {
                    sendToDisplay(String.format(Locale.US,"Placing %d in register %s", register[t] + register[s], Reference.registerNames[d]));
                    register[d] = register[s] + register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                }
                else if(mstep == 5)
                {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }


            }

            //Subtract
            else if(grabRightBits(code,6) == 0b100010)
            {
                int s = grabRightBits(grabLeftBits(code,11),5); //source 1
                int t = grabRightBits(grabLeftBits(code,16),5); //source 2
                int d = grabRightBits(grabLeftBits(code,21),5); //destination

                if(mstep == 0)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 1)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 2)
                {
                    sendToDisplay("Sending \"sub\" to ALU");
                    mstep++;
                    return 0;
                }
                else if(mstep == 3)
                {
                    sendToDisplay(String.format(Locale.US,"Retrieved %d from ALU", register[t] - register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 4)
                {
                    sendToDisplay(String.format(Locale.US,"Placing %d in register %s", register[t] - register[s], Reference.registerNames[d]));
                    register[d] = register[s] - register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                }
                else if(mstep == 5)
                {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }


            }
            //XOR
            else if(grabRightBits(code,6) == 0b100110)
            {
                int s = grabRightBits(grabLeftBits(code,11),5); //source 1
                int t = grabRightBits(grabLeftBits(code,16),5); //source 2
                int d = grabRightBits(grabLeftBits(code,21),5); //destination

                if(mstep == 0)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 1)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 2)
                {
                    sendToDisplay("Sending \"XOR\" to ALU");
                    mstep++;
                    return 0;
                }
                else if(mstep == 3)
                {
                    int result = register[s] ^ register[t];
                    sendToDisplay(String.format(Locale.US,"XOR result: %d", result));
                    mstep++;
                    return 0;
                }
                else if(mstep == 4)
                {
                    sendToDisplay(String.format(Locale.US,"Placing %d in register %s", register[t] ^ register[s], Reference.registerNames[d]));
                    register[d] = register[s] ^ register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                }
                else if(mstep == 5)
                {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }


            }
            //not
            else if(grabRightBits(code,6) == 0b100111) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination
                int result = ~register[s]; //Compute bitwise NOT from source register
                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU for NOT operation", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "NOT result: %d", result));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay(String.format(Locale.US, "PLacing %d in register %s", result, Reference.registerNames[d]));
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }
            }



        }

        //I-type instruction
        else
        { // for load//
            if(grabLeftBits(code, 6) == 0b100011)
            {
            int t = grabRightBits(grabLeftBits(code,16),5); //destination
            int b = grabRightBits(grabLeftBits(code, 11), 5);
            int o = grabRightBits(code, 16);
            int address = b + o;
            int value = combineBytes(memory[address], memory[address + 1], memory[address + 2], memory[address + 3]);

            if(mstep == 0)
            {
                sendToDisplay(String.format(Locale.US,"Grabbing %d from memory %d", value, address));
                mstep++;
                return 0;
            }
            else if(mstep == 1)
            {
                sendToDisplay(String.format(Locale.US,"Putting %d to register %d", value, t));
                register[t] = value;
                mstep = 0;
                return EOS;
            }

        }
            //Addi
            if(grabLeftBits(code, 6) == 0b001000)
            {
                int s = grabRightBits(grabLeftBits(code,11),5); //source
                int t = grabRightBits(grabLeftBits(code,16),5); //destination
                int i = grabRightBits(code,16); //immediate

                if(mstep == 0)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 1)
                {
                    sendToDisplay(String.format(Locale.US,"Sending %d to ALU", i));
                    mstep++;
                    return 0;
                }
                else if(mstep == 2)
                {
                    sendToDisplay("Sending \"add\" to ALU");
                    mstep++;
                    return 0;
                }
                else if(mstep == 3)
                {
                    sendToDisplay(String.format(Locale.US,"Retrieved %d from ALU", i + register[s]));
                    mstep++;
                    return 0;
                }
                else if(mstep == 4)
                {
                    sendToDisplay(String.format(Locale.US,"Placing %d in register %s", i + register[s], Reference.registerNames[t]));
                    register[t] = register[s] + i;
                    sendIndividualRegisterToDisplay(t);
                    mstep++;
                    return 0;
                }
                else if(mstep == 5)
                {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }
            }
        }
        return 0;


    }


    //HELPER METHODS

    /**
     * grabs right bits from an int
     * @param data the integer to grab bits from
     * @param n the amount of bits to grab
     * @return the grabbed bits
     */
    private int grabRightBits(int data, int n)
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
    private int grabLeftBits(int data, int n)
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

    /**
     * Getter for the program counter
     * @return The program counter
     */
    public int getPc() {
        return pc;
    }

    /**
     * Setter for the program counter
     * @param pc The program counter
     */
    public void setPc(int pc) {
        this.pc = pc;
        machineInterface.updateProgramCounter(String.valueOf(pc));  // Update the program counter on the screen

    }

    /**
     * Method to send the memory to {@link MachineActivity} via {@link MachineInterface}
     */
    public void sendMemory() {
        String memoryStr = "";
        if (displayFormat == Reference.HEX_MODE) {
            memoryStr = HexFormat.ofDelimiter(" ").formatHex(memory);
        } else if (displayFormat == Reference.BINARY_MODE) {
            memoryStr = binaryString();
        } else if (displayFormat == Reference.DECIMIAL_MODE) {
            memoryStr = Arrays.toString(memory);
        }
        // Pass the memoryStr to update memory
        machineInterface.updateMemoryDisplay(memoryStr);
    }


    /**
     * Method to get a binary formatted string representing the memory
     * @return The memory formatted as binary with a space between every byte
     */
    private String binaryString() {
        byte[] indivByte = new byte[1];
        String memoryString = "";
        for (int i = 0; i < memory.length; i++) {
            indivByte[0] = memory[i];
            BigInteger getBinary = new BigInteger(indivByte);
            // Add leading zeros and space -> 00101010 01110001 versus 1010101110001
            memoryString = memoryString + String.format("%8s", getBinary.toString(2)).replace(" ", "0") + " ";
        }
       return memoryString;
    }

    public void sendToDisplay(String message)
    {
        //todo add message to text area of app
        Log.d("Step", message);
        machineInterface.updateInstructionDisplay(message);
    }

    public void saveState(OutputStream outputStream) throws IOException {
        // Write header
//        PrintWriter printWriter = new PrintWriter(outputStream);
//        printWriter.println("State");
//        printWriter.close();
        // Save the save
        Log.d("saveState", "Starting to save the state");
        StateManager.toFile(outputStream, register, pc, hi, lo, memory);
    }

    /**
     * Method to send a individual register to {@link MachineInterface} with the correct format
     * @param registerIndex The register to select from in the register array
     */
    private void sendIndividualRegisterToDisplay(int registerIndex) {
        String registerString = "";
        if (displayFormat == Reference.HEX_MODE) {
            registerString = String.format("%8s", Integer.toHexString(register[registerIndex])).replace(" ", "0");  // 4 bytes -> 2 hex per byte  = 8
            //registerString = HexFormat.ofDelimiter("").formatHex(new byte[] {Byte.parseByte(String.valueOf(register[registerIndex]))}, registerIndex, registerIndex);
        } else if (displayFormat == Reference.BINARY_MODE) {
            registerString = String.format("%32s", Integer.toBinaryString(register[registerIndex])).replace(" ", "0");  // 4 bytes = 32 bits
        } else {
            registerString = String.valueOf(register[registerIndex]);
        }
        machineInterface.updateIndividualRegister(registerIndex, registerString);
    }

    /**
     * Method to update all the register displays by calling sendIndividualRegisterToDisplay
     */
    public void sendAllRegistersToDisplay() {
        for (int i = 0; i < register.length; i++) {
            sendIndividualRegisterToDisplay(i);
        }
    }

}
