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
package io.github.danielt3131.mipsemu;

import android.app.Activity;
import android.text.PrecomputedText;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;

/**
 * Class used for communication from {@link io.github.danielt3131.mipsemu.machine.MipsMachine} to {@link io.github.danielt3131.mipsemu.ui.MachineActivity}
 */
public class MachineInterface {
    private TextView memoryDisplay, programCounterDisplay, instructionDisplay, cacheHitRateDisplay;
    private TextView[] registers;
    private Activity activity;
    private PrecomputedText preComputedMemoryDisplay;

    /**
     *
     * @param memoryDisplay The memory display
     * @param programCounterDisplay The program counter display
     * @param instructionDisplay The instruction display
     * @param registers The array of registers
     * @param cacheHitRateDisplay The cache hit rate display
     */
    public MachineInterface(TextView memoryDisplay, TextView programCounterDisplay, TextView instructionDisplay, TextView[] registers, TextView cacheHitRateDisplay, Activity activity) {
        this.memoryDisplay = memoryDisplay;
        this.programCounterDisplay = programCounterDisplay;
        this.instructionDisplay = instructionDisplay;
        this.registers = registers;
        this.cacheHitRateDisplay = cacheHitRateDisplay;
        this.activity = activity;
    }

    /**
     * Method to update the memory display on the screen
     * @param memory The memory formatted
     */
    public void updateMemoryDisplay(String memory) {
        final String[] finalMemoryString = {""};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] memoryArray = memory.split(" ");
                int memoryAddress = 0;
                boolean isBinary = false;
                if (memoryArray[0].length() == 8) {
                    isBinary = true;
                }
                Log.d("Memory", "Now adding addresses");
                int i = 0;
                //memoryDisplay.append("Memory\n");
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Memory\n");
                while (i < memoryArray.length / 4) {
                    String memoryAddressString =  String.format("0x%6s", Integer.toHexString(memoryAddress)).replace(" ", "0");
                    //memoryDisplay.append(String.format("%s: %s %s %s %s\n", memoryAddressString, memoryArray[i++], memoryArray[i++], memoryArray[i++], memoryArray[i++]));
                    stringBuilder.append(String.format("%s: %s %s %s %s\n", memoryAddressString, memoryArray[i++], memoryArray[i++], memoryArray[i++], memoryArray[i++]));
                    memoryAddress += 4;
                }
                finalMemoryString[0] = stringBuilder.toString();
                Log.d("Memory", "Computing display");
                preComputedMemoryDisplay = PrecomputedText.create(finalMemoryString[0], memoryDisplay.getTextMetricsParams());
                Log.d("Memory", "Sent to UI");
                memoryDisplay.post(() -> {
                    memoryDisplay.setText(preComputedMemoryDisplay);
                });
                //activity.runOnUiThread(() -> memoryDisplay.setText(preComputedMemoryDisplay));
                if (isBinary) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Now showing binary\nTold you it will take time!", Toast.LENGTH_SHORT).show());
                }

            }
        });

        thread.start();

        //memoryDisplay.setText("Memory\n" + memory);
    }

    /**
     * Method to update the program counter on the screen
     * @param programCounter The program counter as a string
     */
    public void updateProgramCounter(String programCounter) {
        programCounterDisplay.setText("Program Counter: " + programCounter);
    }


    /**
     * Method to update the instructionDisplay TextView
     * @param instructions The instructions to display
     */
    public void updateInstructionDisplay(String instructions) {
        instructionDisplay.setText("Instructions:" + instructions);
    }

    /**
     * Updates an individual register display
     * @param register The register to update
     * @param registerValue The register value
     */
    public void updateIndividualRegister(int register, String registerValue) {
        registers[register].setText(Reference.registerNames[register] + ": " + registerValue);
        Log.d("Updated Register: " + Reference.registerNames[register], registerValue);
    }

    /**
     * Updates all the register displays
     * @param registerValues The string array containing the value of every register
     */
    public void updateAllRegisters(String[] registerValues) {
        for (int i = 0; i < registers.length; i++) {
            try {
                registers[i].setText(Reference.registerNames[i] + ": " + registerValues[i]);
                Log.d("Updated Register: " + Reference.registerNames[i], registerValues[i]);
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.e("Update Register", e.getMessage());
            }
        }
    }

    /**
     * Updates the cache hit display
     * @param cacheHitRate The cache hit rate value
     */
    public void updateCacheHitDisplay(String cacheHitRate) {
        cacheHitRateDisplay.setText("Cache Hits: " + cacheHitRate);
    }

    /**
     * Method to clear all the displays when the machine is reset
     */
    public void clearAll() {
        String blank = "";
        String[] blankRegisters = new String[registers.length];
        for (int i = 0; i < blankRegisters.length; i++) {
            blankRegisters[i] = blank;
        }
        updateAllRegisters(blankRegisters);
        updateProgramCounter(blank);
        updateCacheHitDisplay(blank);
        updateMemoryDisplay(blank);
        updateInstructionDisplay(blank);
    }
}
