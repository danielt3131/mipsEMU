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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.sql.Ref;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.R;
import io.github.danielt3131.mipsemu.Reference;
import io.github.danielt3131.mipsemu.ui.MachineActivity;
import kotlin.text.Regex;

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

    CacheBlock[] l1 = new CacheBlock[8];
    CacheBlock[] l2 = new CacheBlock[16];
    CacheBlock[] l3 = new CacheBlock[32];

    int hits = 0;
    int attempts = 0;

    private MachineInterface machineInterface;
    private InputStream inputFileStream;
    private int displayFormat;
    private Scanner fileScanner;
    private PrintWriter instructionLogWriter;
    private boolean readFile;
    private Context machineContext;
    private String instructionLogFilename = "instructions.txt";

    /**
     * Constructor for the mips emulator
     *
     * @param memorySize the amount of memory the machine will have in bytes
     */
    public MipsMachine(int memorySize, MachineInterface machineInterface, Context machineContext) {

        memory = new byte[memorySize];
        this.machineInterface = machineInterface;
        mstep = 0;
        code = 1;
        readFile = false;
        Log.d("memory siz", "" + (memory.length));
        register[29] = memory.length - 4;
        this.machineContext = machineContext;
        try {
            // Create a Print Writer object to save the instructions to be shared at the end using a buffer writer. -> Stored in internal storage
            instructionLogWriter = new PrintWriter(new BufferedWriter(new FileWriter(new File(machineContext.getFilesDir(), instructionLogFilename))));
        } catch (IOException e) {
            Log.e("Instruction Log", e.getMessage());
        }

    }

    /**
     * Close all file streams for {@link MachineActivity} onDestroy() to prevent resource leak
     */
    public void onDestroy() {
        try {
            fileScanner.close();
            instructionLogWriter.close();
        } catch (RuntimeException e) {
            Log.e("MipsMachine", e.getMessage());
        }
    }

    public void setInputFileStream(InputStream inputFileStream) {
        this.inputFileStream = inputFileStream;
        Thread thread = new Thread(() -> {
            fileScanner = new Scanner(inputFileStream);
            Looper.prepare();
            if (fileScanner.hasNext(Pattern.compile("State.*"))) {
                Log.d("inputFileStream Set", "State Header Exists, readState()");
                readState();
                Toast.makeText(machineContext, "Read in state", Toast.LENGTH_SHORT).show();
                readFile = true;
            } else {
                Log.d("inputFileStream Set", "State Header Does Not Exist, readFile()");
                readFile();
                readFile = true;
                Toast.makeText(machineContext, "Read in file", Toast.LENGTH_SHORT).show();
            }
            sendMemory();
            sendAllRegistersToDisplay();
            sendProgramCounter();
            fileScanner.close();
        });
        thread.start();
    }

    /**
     * Sets the display format
     *
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
            tp = Integer.parseInt(code, 16);

            //System.out.println("Starting writing at memory " + tp);

            lineScanner.reset();

            //Get rest of the line
            code = lineScanner.nextLine();

            //remove ':'
            code = code.substring(1);
            //System.out.println(code);
            //remove spaces
            while (code.contains(" ")) {
                code = code.substring(0, code.indexOf(" ")) + code.substring(code.indexOf(" ") + 1);
                //System.out.println(code);
            }
            //look at next 8 characters and convert to byte and then put in memory
            while (code.length() != 0) {
                String part = code.substring(0, 8);
                Log.d("MipsMachine.readFile Part", part);
                byte b = (byte) Integer.parseInt(part, 2); //Byte.parseByte crashes due to signed bit so this is a workaround
                memory[tp] = b;
                //sendMemory();
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
        //Remove header
        fileScanner.nextLine();

        byte b1;
        byte b2;
        byte b3;
        byte b4;

        int com;

        //register array
        for(int i = 0; i < 32; i++)
        {
            Log.d("Reading File", "Register: " + i);
            b1 = fileScanner.nextByte();
            b2 = fileScanner.nextByte();
            b3 = fileScanner.nextByte();
            b4 = fileScanner.nextByte();

            com = combineBytes(b1, b2, b3, b4);
            register[0] = com;
        }

        //pc
        Log.d("Reading File", "reading PC");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        pc = com;

        //hi
        Log.d("Reading File", "reading HI");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        hi = com;

        //lo
        Log.d("Reading File","reading LO");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        lo = com;

        //Memory
        Log.d("Reading File", "reading size of memory");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        memory = new byte[com];

        //Text
        int sizeOfText;
        Log.d("Reading File", "reading size of text");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        sizeOfText = com;

        for(int i = 0; i < sizeOfText; i++)
        {
            memory[i] = fileScanner.nextByte();
        }

        //Stack
        int sizeOfStack;
        Log.d("Reading File", "reading size of stack");
        b1 = fileScanner.nextByte();
        b2 = fileScanner.nextByte();
        b3 = fileScanner.nextByte();
        b4 = fileScanner.nextByte();
        com = combineBytes(b1, b2, b3, b4);
        sizeOfStack = com;

        for(int i = 0; i < sizeOfStack; i++)
        {
            memory[memory.length - 1 - i] = fileScanner.nextByte();
        }
    }

    private int getCode()
    {
        return combineBytes(memory[pc], memory[pc+1], memory[pc+2], memory[pc+3]);
        //return combineBytes(getFromMemory(pc), getFromMemory(pc+1), getFromMemory(pc+2), getFromMemory(pc+3));
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
        while (running) //keeps executing until it returns EOS when step is done
        {
            running = nextMicroStep() != EOS;
        }
        microStepInstructions = "";
    }

    /**
     * Method to run the next step as requested from the user or MipsMachine
     */
    public void runNextStep() {
        if (readFile && code != 0) {
            nextStep();
            sendAllRegistersToDisplay();
            machineInterface.updateCacheHitDisplay(String.valueOf(hitRate()));
        } else if (code == 0) {
            showCompletedToast();
        } else {
            Toast.makeText(machineContext, "Still reading in the file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method to run next micro step as requested from the user or MipsMachine
     */
    public void runNextMicroStep() {
        if (readFile && code != 0) {
            nextMicroStep();
            sendAllRegistersToDisplay();
            machineInterface.updateCacheHitDisplay(String.valueOf(hitRate()));
        } else if (code == 0) {
            showCompletedToast();
        } else {
            Toast.makeText(machineContext, "Still reading in the file", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method to run all remaining steps as requested from the user
     */
    public void runContinuously() {
        // Run continuously
        // Don't update the memory display
        if (readFile && code != 0) {
            Thread thread = new Thread(() -> {
                Looper.prepare();
                while (code != 0) {
                    nextStep();
                }
                showCompletedToast();
                sendAllRegistersToDisplay();
                sendMemory();
                machineInterface.updateCacheHitDisplay(String.valueOf(hitRate()));
            });
            thread.start();
        }
    }

    /**
     * Shows a Toast message that there is no more instructions to execute
     * <p>
     * Calls shareInstructionLog() to share the instruction log to the user
     */
    private void showCompletedToast() {
        Toast.makeText(machineContext, "No more instructions to execute", Toast.LENGTH_LONG).show();
        shareInstructionLog();
    }

    /**
     * Shares the instruction log to the user via the system share sheet via Intent
     * <p>
     * See <a href="https://developer.android.com/training/sharing/send">...</a>
     */
    private void shareInstructionLog() {
        instructionLogWriter.close();
        // Share the instruction log -> pull up share menu
        Intent instructionShareIntent = new Intent(Intent.ACTION_SEND);
        // Get a Uri from File
        Uri instructionUri = FileProvider.getUriForFile(machineContext, "io.github.danielt3131.mipsemu.provider", new File(machineContext.getFilesDir(), instructionLogFilename));
        instructionShareIntent.setType("text/plain");   // Set the type to a plain text file
        instructionShareIntent.putExtra(Intent.EXTRA_STREAM, instructionUri);   // The file Uri
        instructionShareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        // Get the share sheet instead of intent resolver see https://developer.android.com/training/sharing/send
        instructionShareIntent = Intent.createChooser(instructionShareIntent, null);
        machineContext.startActivity(instructionShareIntent);
    }

    private int nextMicroStep() {
        Log.d("mstep", "MSTEP: " + mstep);

        code = getCode();

        Log.d("OPCODE",Integer.toBinaryString(code));

        if (code == 0) return EOS;

        //R-type instruction
        if (grabLeftBits(code, 6) == 0) {

            //Add
            if (grabRightBits(code, 6) == 0b100000) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"add\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[t] + register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[t] + register[s], Reference.registerNames[d]));
                    register[d] = register[s] + register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }


            }

            //Subtract
            else if (grabRightBits(code, 6) == 0b100010) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"sub\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[t] - register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[t] - register[s], Reference.registerNames[d]));
                    register[d] = register[s] - register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }


            }

            // Multiply
            else if (grabRightBits(code, 6) == 0b011000) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"mul\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[t] * register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    int val1 = register[s];
                    int val2 = register[t];
                    long results = (long) val1 * val2;
                    hi = (int) (results / Math.pow(2, 32));
                    sendToDisplay(String.format(Locale.US, "Placing %d in register hi", hi));
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    int val1 = register[s];
                    int val2 = register[t];
                    long results = (long) val1 * val2;
                    lo = (int) (results % Math.pow(2, 32));
                    sendToDisplay(String.format(Locale.US, "Placing %d in register lo", lo));
                    mstep++;
                    return 0;
                } else if (mstep == 6) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }
            }

            // Boolean AND
            else if (grabRightBits(code, 6) == 0b100100) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"and\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[t] & register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[t] & register[s], Reference.registerNames[d]));
                    register[d] = register[s] & register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }
            }

            // Boolean OR
            else if (grabRightBits(code, 6) == 0b100101) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"or\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[t] | register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[t] | register[s], Reference.registerNames[d]));
                    register[d] = register[s] | register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }
            }

            //XOR
            else if (grabRightBits(code, 6) == 0b100110) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); //source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); //destination

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"XOR\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    int result = register[s] ^ register[t];
                    sendToDisplay(String.format(Locale.US, "XOR result: %d", result));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[t] ^ register[s], Reference.registerNames[d]));
                    register[d] = register[s] ^ register[t];
                    sendIndividualRegisterToDisplay(d);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }


            }
            //not
            else if (grabRightBits(code, 6) == 0b100111) {
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
                    increaseProgramCounter(4);
                    return EOS;
                }
            }

            ///set less than

            else if (grabRightBits(code, 6) == 0b101010) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); // source 1
                int t = grabRightBits(grabLeftBits(code, 16), 5); // source 2
                int d = grabRightBits(grabLeftBits(code, 21), 5); // destination

                if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay("Sending \"<\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    int val = 0;
                    if (register[s] < register[t]) {
                        val = 1;
                    }
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", val));
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    int val = 0;
                    if (register[s] < register[t]) {
                        val = 1;
                    }
                    register[d] = val;
                    sendToDisplay(String.format(Locale.US, "PLacing %d in register %s", val, Reference.registerNames[d]));
                    mstep++;
                    return 0;
                } else if (mstep == 6) {
                    sendToDisplay("Increasing PC by 4");
                    increaseProgramCounter(4);
                    mstep = 0;
                    return EOS;
                }

            }
        }

        //I-type instruction
        else {
                //load
            if (grabLeftBits(code, 6) == 0b100011) {

                int t = grabRightBits(grabLeftBits(code, 16), 5); //destination
                int b = grabRightBits(grabLeftBits(code, 11), 5); //base
                int o = grabRightBits(code, 16); //offset register


                if(o >= 0b1000000000000000)
                {
                    Log.d("Two Bits Compliment", "making negative");
                    int mask = 0b11111111111111110000000000000000;
                    o += mask;
                }

                Log.d("OFFSET LOAD","" + o);

                int address = register[29] + o;
                Log.d("ADDRESS LOAD", "" + address);
                int value = combineBytes(getFromMemory(address), getFromMemory(address + 1), getFromMemory(address + 2), getFromMemory(address + 3));


                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Grabbing %d from memory %s", value, Integer.toHexString(address)));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Putting %d to register %s", value, Reference.registerNames[t]));
                    register[t] = value;
                    mstep ++;
                    return 0;
                } else if (mstep == 2){
                sendToDisplay("Increasing PC by 4");
                increaseProgramCounter(4);
                mstep=0;
                return EOS;
            }
                // Store /////////////////////////////////////
            } else if (grabLeftBits(code, 6) == 0b101011) {
                int s = grabRightBits(grabLeftBits(code, 16), 5); //source
                int b = grabRightBits(grabLeftBits(code, 11), 5); //base
                int o = grabRightBits(code, 16); //offset register

                //check offset for two bits compliment
                if(o >= 0b1000000000000000)
                {
                    int mask = 0b11111111111111110000000000000000;
                    o += mask;
                }

                Log.d("OFFSET STORE","" + o);

                int address = register[29] + o;
                Log.d("ADDRESS STORE", "" + address);

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Grabbing %d from register %s", register[s], Reference.registerNames[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Putting %d into memory %s", register[s], Integer.toHexString(address)));
                    byte p1, p2, p3, p4;
                    p1 = (byte) grabLeftBits(register[s], 8);
                    p2 = (byte) grabRightBits(grabLeftBits(register[s], 16), 8);
                    p3 = (byte) grabRightBits(grabLeftBits(register[s], 24), 8);
                    p4 = (byte) grabRightBits(register[s], 8);

                    sendToMemory(address, p1);
                    sendToMemory(address + 1, p2);
                    sendToMemory(address + 2, p3);
                    sendToMemory(address + 3, p4);


                    sendMemory();   // Update the memory display -> will take time

                    mstep ++;
                    return 0;
                } else if (mstep == 2){
                    sendToDisplay("Increasing PC by 4");
                    increaseProgramCounter(4);
                    mstep=0;
                    return EOS;
                }
            }
            //Jump
            else if (grabLeftBits(code, 6) == 0b000010) {
                int t = grabRightBits(code, 26);

                t <<= 2;

                int p = grabLeftBits(pc, 4);
                p *= (int) Math.pow(2, 28);
                t += p;

                sendToDisplay(String.format(Locale.US, "Setting pc to %d", t));
                setProgramCounter(t);
                return EOS;
            }
            //Jump and Link
            else if (grabLeftBits(code, 6) == 0b000011) {
                int t = grabRightBits(code, 26);

                t <<= 2;

                int p = grabLeftBits(pc, 4);
                p *= (int) Math.pow(2, 28);
                t += p;

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", pc + 4, Reference.registerNames[31]));
                    register[31] = pc + 4;
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Setting pc to %d", t));
                    setProgramCounter(t);
                    return EOS;
                }
            }

            //set less than immediate
            else if (grabLeftBits(code, 6) == 0b01010) {
                Log.d("worked", "worked");
                int s = grabRightBits(grabLeftBits(code, 11), 5); // source 1
                int d = grabRightBits(grabLeftBits(code, 16), 5); // destination
                int i = grabRightBits(code, 16); // immediate

                if (i >> 15 == 1) {
                    int mask = 0b11111111111111110000000000000000;
                    i += mask;
                }

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", i));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"<\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    int val = 0;
                    if (register[s] < i) {
                        val = 1;
                    }
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", val));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    int val = 0;
                    if (register[s] < i) {
                        val = 1;
                    }
                    register[d] = val;
                    sendToDisplay(String.format(Locale.US, "PLacing %d in register %s", val, Reference.registerNames[d]));
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    increaseProgramCounter(4);
                    mstep = 0;
                    return EOS;
                }
            }

            //Branch on equal
            else if (grabLeftBits(code, 6) == 0b000100) {
                Log.d("TEST", "WORKED");
                int offset = grabRightBits(code, 16) << 2;

                int s = grabRightBits(grabLeftBits(code, 11), 5);
                int t = grabRightBits(grabLeftBits(code, 16), 5);

                //Checking two bits compliment
                if (offset >> 15 == 1) {
                    //make negative
                    int mask = 0b11111111111111000000000000000000;
                    offset += mask;
                }
                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"=\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    if (register[s] == register[t]) {
                        sendToDisplay(String.format(Locale.US, "register %s and %s match, will branch", Reference.registerNames[s], Reference.registerNames[t]));
                    } else {
                        sendToDisplay(String.format(Locale.US, "register %s and %s do NOT match, will NOT branch", Reference.registerNames[s], Reference.registerNames[t]));
                    }
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    if (register[s] == register[t]) {
                        sendToDisplay("Setting PC to " + (pc + offset));
                        setProgramCounter(pc + offset);
                    } else {
                        sendToDisplay("Increasing PC by 4");
                        increaseProgramCounter(4);
                    }
                    mstep = 0;
                    return EOS;
                }


            }
            //Branch on NOT equal
            else if (grabLeftBits(code, 6) == 0b000101) {
                int offset = grabRightBits(code, 16) << 2;

                int s = grabRightBits(grabLeftBits(code, 11), 5);
                int t = grabRightBits(grabLeftBits(code, 16), 5);

                //Checking two bits compliment
                if (offset >> 15 == 1) {
                    //make negative
                    int mask = 0b11111111111111000000000000000000;
                    offset += mask;
                }
                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[t]));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"!=\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    if (register[s] != register[t]) {
                        sendToDisplay(String.format(Locale.US, "register %s and %s do NOT match, will branch", Reference.registerNames[s], Reference.registerNames[t]));
                    } else {
                        sendToDisplay(String.format(Locale.US, "register %s and %s do match, will NOT branch", Reference.registerNames[s], Reference.registerNames[t]));
                    }
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    if (register[s] != register[t]) {
                        sendToDisplay("Setting PC to " + (pc + offset));
                        setProgramCounter(pc + offset);
                    } else {
                        sendToDisplay("Increasing PC by 4");
                        increaseProgramCounter(4);
                    }
                    mstep = 0;
                    return EOS;
                }


            }
            //Branch <= 0
            else if (grabLeftBits(code, 6) == 0b000110) {
                int offset = grabRightBits(code, 16) << 2;

                int s = grabRightBits(grabLeftBits(code, 11), 5);

                //Checking two bits compliment
                if (offset >> 15 == 1) {
                    //make negative
                    int mask = 0b11111111111111000000000000000000;
                    offset += mask;
                }
                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", 0));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"<=\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    if (register[s] <= 0) {
                        sendToDisplay(String.format(Locale.US, "register %s is less than or equal to %s, will branch", Reference.registerNames[s], Reference.registerNames[0]));
                    } else {
                        sendToDisplay(String.format(Locale.US, "register %s is NOT less than or equal to %s, will NOT branch", Reference.registerNames[s], Reference.registerNames[0]));
                    }
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    if (register[s] <= 0) {
                        sendToDisplay("Setting PC to " + (pc + offset));
                        setProgramCounter(pc + offset);
                    } else {
                        sendToDisplay("Increasing PC by 4");
                        increaseProgramCounter(4);
                    }
                    mstep = 0;
                    return EOS;
                }

            }
            //Branch > 0
            else if (grabLeftBits(code, 6) == 0b000001) {
                int offset = grabRightBits(code, 16) << 2;

                int s = grabRightBits(grabLeftBits(code, 11), 5);

                //Checking two bits compliment
                if (offset >> 17 == 1) {
                    //make negative
                    int mask = 0b11111111111111000000000000000000;
                    offset += mask;
                }
                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", 0));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \">\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    if (register[s] > 0) {
                        sendToDisplay(String.format(Locale.US, "register %s is greater than %s, will branch", Reference.registerNames[s], Reference.registerNames[0]));
                    } else {
                        sendToDisplay(String.format(Locale.US, "register %s is NOT greater than %s, will NOT branch", Reference.registerNames[s], Reference.registerNames[0]));
                    }
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    if (register[s] > 0) {
                        sendToDisplay("Setting PC to " + (pc + offset));
                        setProgramCounter(pc + offset);
                    } else {
                        sendToDisplay("Increasing PC by 4");
                        increaseProgramCounter(4);
                    }
                    mstep = 0;
                    return EOS;
                }

            }
            //Addi
            else if (grabLeftBits(code, 6) == 0b001000) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); //source
                int t = grabRightBits(grabLeftBits(code, 16), 5); //destination
                int i = grabRightBits(code, 16); //immediate

                if (i >= 0b1000000000000000) {
                    Log.d("two bits before", Integer.toBinaryString(i));
                    Log.d("two bits", "negative");
                    int mask = 0b11111111111111110000000000000000;
                    i += mask;
                    Log.d("two bits after", Integer.toBinaryString(i));
                }

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", i));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"add\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", i + register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", i + register[s], Reference.registerNames[t]));
                    register[t] = register[s] + i;
                    sendIndividualRegisterToDisplay(t);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    increaseProgramCounter(4);
                    return EOS;
                }
            }

            // Boolean ANDi
            else if (grabLeftBits(code, 6) == 0b001100) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); // source
                int t = grabRightBits(grabLeftBits(code, 16), 5); // destination
                int i = grabRightBits(code, 16); // immediate

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", i));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"and\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[s] & i));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[s] & i, Reference.registerNames[t]));
                    register[t] = register[s] & i;
                    sendIndividualRegisterToDisplay(t);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }
            }

            // Boolean ORi
            else if (grabLeftBits(code, 6) == 0b001101) {
                int s = grabRightBits(grabLeftBits(code, 11), 5); // source
                int t = grabRightBits(grabLeftBits(code, 16), 5); // destination
                int i = grabRightBits(code, 16); // immediate

                if (mstep == 0) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", register[s]));
                    mstep++;
                    return 0;
                } else if (mstep == 1) {
                    sendToDisplay(String.format(Locale.US, "Sending %d to ALU", i));
                    mstep++;
                    return 0;
                } else if (mstep == 2) {
                    sendToDisplay("Sending \"or\" to ALU");
                    mstep++;
                    return 0;
                } else if (mstep == 3) {
                    sendToDisplay(String.format(Locale.US, "Retrieved %d from ALU", register[s] | i));
                    mstep++;
                    return 0;
                } else if (mstep == 4) {
                    sendToDisplay(String.format(Locale.US, "Placing %d in register %s", register[s] | i, Reference.registerNames[t]));
                    register[t] = register[s] | i;
                    sendIndividualRegisterToDisplay(t);
                    mstep++;
                    return 0;
                } else if (mstep == 5) {
                    sendToDisplay("Increasing PC by 4");
                    mstep = 0;
                    pc += 4;
                    return EOS;
                }
            }
        }

        Log.e("UNKOWN OP CODE", Integer.toBinaryString(grabLeftBits(getCode(), 6)));
        return EOS;


    }



    public double hitRate()
    {
        if(attempts == 0)
        {
            return 0;
        }
        return (double) hits / attempts;
    }

    public double missRate()
    {
        return 1 - hitRate();
    }

    byte getFromMemory(int address)
    {

        return memory[address];

        //fix later
//        attempts++;
//        hits++;
//
//        int index = address % 8;
//
//        int tag = address/8;
//
//        if (l1[index].isValid() && l1[index].tag == tag) {
//            return l1[index].data;
//        }
//
//        index = address % 16;
//
//        tag = address/16;
//
//        if (l2[index].isValid() && l2[index].tag == tag) {
//            return l2[index].data;
//        }
//
//        index = address % 32;
//
//        tag = address/32;
//
//        if (l3[index].isValid() && l3[index].tag == tag) {
//            return l3[index].data;
//        }
//
//        hits--;
//
//        return memory[address];
    }

    void sendToMemory(int address, byte data)
    {
        memory[address] = data;


//        int index = address % 8;
//
//        l1[index].tag = address/8;
//        l1[index].data = data;
//
//        l1[index].setValid();
//
//        index = address % 16;
//
//        l2[index].tag = address/16;
//        l2[index].data = data;
//
//        l2[index].setValid();
//
//        index = address % 32;
//
//        l3[index].tag = address/32;
//        l3[index].data = data;
//
//        l3[index].setValid();
//
//        memory[address] = data;
    }

    //HELPER METHODS

    /**
     * grabs right bits from an int
     *
     * @param data the integer to grab bits from
     * @param n    the amount of bits to grab
     * @return the grabbed bits
     */
    private int grabRightBits(int data, int n) {
        return (int) (data % Math.pow(2,n));
    }

    /**
     * grabs left bits from an int
     *
     * @param data the integer to grab bits from
     * @param n    the amount of bits to grab
     * @return the grabbed bits
     */
    private int grabLeftBits(int data, int n) {
        return data >>> 32 - n;
    }


    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     *
     * @param b1 byte 1
     * @param b2 byte 2
     * @param b3 byte 3
     * @param b4 byte 4
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2, byte b3, byte b4) {
        return ((0xFF & b1) << 24) | ((0xFF & b2) << 16) | ((0xFF & b3) << 8) | (0xFF & b4);
    }

    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     *
     * @param b1 byte 1
     * @param b2 byte 2
     * @param b3 byte 3
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2, byte b3) {
        return combineBytes((byte) 0,b1,b2,b3);
    }

    /**
     * combines the bytes into a larger format
     * useful if bits from one byte and bits from another are used
     * will be useful for i-type instruction
     *
     * @param b1 byte 1
     * @param b2 byte 2
     * @return the combination
     */
    private int combineBytes(byte b1, byte b2) {

        return combineBytes((byte) 0,(byte)0,b1,b2);
    }

    /**
     * Getter for the program counter
     *
     * @return The program counter
     */
    public int getProgramCounter() {
        return pc;
    }

    /**
     * Setter for the program counter
     *
     * @param pc The program counter
     */
    public void setProgramCounter(int pc) {
        this.pc = pc;
        sendProgramCounter();   // Send the program counter to the display
    }

    /**
     * Method to send the memory to {@link MachineActivity} via {@link MachineInterface}
     */
    public void sendMemory() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String memoryStr = "";
                if (displayFormat == Reference.HEX_MODE) {
                    memoryStr = HexFormat.ofDelimiter(" ").formatHex(memory);
                } else if (displayFormat == Reference.BINARY_MODE) {
                    memoryStr = binaryString();
                } else if (displayFormat == Reference.DECIMIAL_MODE) {
                    memoryStr = Arrays.toString(memory).replace("[", "").replace("]", "").replace(",", "");
                }
                // Pass the memoryStr to update memory
                Log.d("Memory", memoryStr.substring(0, 30));
                machineInterface.updateMemoryDisplay(memoryStr);
            }
        });
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();

    }


    /**
     * Method to get a binary formatted string representing the memory
     *
     * @return The memory formatted as binary with a space between every byte
     */
    private String binaryString() {
        byte[] indivByte = new byte[1];
        String memoryString = "";
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < memory.length; i++) {
            indivByte[0] = memory[i];
            BigInteger getBinary = new BigInteger(1,indivByte);
            // Add leading zeros and space -> 00101010 01110001 versus 1010101110001
            stringBuilder.append(String.format("%8s", getBinary.toString(2)).replace(" ", "0")).append(" ");
        }
        return stringBuilder.toString();
    }

    //    private String binaryString() {
//        String memoryString = "";
//        Log.d("Memory", String.valueOf(System.currentTimeMillis()));
//        for (int i = 0; i < memory.length; i++) {
//            memoryString = memoryString + String.format("%8s", Integer.toBinaryString(memory[i] )).replace(" ", "0") + " ";
//        }
//        Log.d("Memory", String.valueOf(System.currentTimeMillis()));
//        return memoryString;
//    }
    String microStepInstructions = "";

    public void sendToDisplay(String message) {
        //todo add message to text area of app
        Log.d("Step", message);
        microStepInstructions = microStepInstructions + "\n" + message;
        machineInterface.updateInstructionDisplay(microStepInstructions);
        instructionLogWriter.println(microStepInstructions);
    }

    public void saveState(OutputStream outputStream, Uri outputFileUri, Activity activity) throws IOException {
        // Write header
//        PrintWriter printWriter = new PrintWriter(outputStream);
//        printWriter.println("State");
//        printWriter.close();
        // Save the save
        Log.d("saveState", "Starting to save the state");
        StateManager.toFile(outputStream, register, pc, hi, lo, memory, outputFileUri, activity);
    }

    /**
     * Increases the program counter
     *
     * @param pcValue The value to increase the program counter by
     */
    private void increaseProgramCounter(int pcValue) {
        pc += pcValue;  // Increment the program counter by pcValue
        sendProgramCounter();   // Send the correct format
    }

    /**
     * Sends the program counter to the display with the correct format
     * <p>
     * Used by increaseProgramCounter and setProgramCounter
     */
    public void sendProgramCounter() {
        if (displayFormat == Reference.HEX_MODE) {
            machineInterface.updateProgramCounter(String.format("%8s", Integer.toHexString(pc)).replace(" ", "0"));
        } else if (displayFormat == Reference.BINARY_MODE) {
            machineInterface.updateProgramCounter(String.format("%32s", Integer.toBinaryString(pc)).replace(" ", "0"));
        } else {
            machineInterface.updateProgramCounter(String.valueOf(pc));
        }
    }

    /**
     * Method to send a individual register to {@link MachineInterface} with the correct format
     *
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
