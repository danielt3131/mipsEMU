package io.github.danielt3131.mipsemu;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

/**
 * Class used for communication from {@link io.github.danielt3131.mipsemu.machine.MipsMachine} to {@link io.github.danielt3131.mipsemu.ui.MachineActivity}
 */
public class MachineInterface {
    private TextView memoryDisplay, programCounterDisplay, instructionDisplay, cacheHitRateDisplay;
    private TextView[] registers;

    /**
     *
     * @param memoryDisplay The memory display
     * @param programCounterDisplay The program counter display
     * @param instructionDisplay The instruction display
     * @param registers The array of registers
     * @param cacheHitRateDisplay The cache hit rate display
     */
    public MachineInterface(TextView memoryDisplay, TextView programCounterDisplay, TextView instructionDisplay, TextView[] registers, TextView cacheHitRateDisplay) {
        this.memoryDisplay = memoryDisplay;
        this.programCounterDisplay = programCounterDisplay;
        this.instructionDisplay = instructionDisplay;
        this.registers = registers;
        this.cacheHitRateDisplay = cacheHitRateDisplay;
    }

    /**
     * Method to update the memory display on the screen
     * @param memory The memory formatted
     */
    public void updateMemoryDisplay(String memory) {
        memoryDisplay.setText("Memory\n" + memory);
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
