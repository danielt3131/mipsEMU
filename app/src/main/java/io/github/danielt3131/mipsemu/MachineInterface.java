package io.github.danielt3131.mipsemu;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

/**
 * Class used for communication from {@link io.github.danielt3131.mipsemu.machine.MipsMachine} to {@link io.github.danielt3131.mipsemu.ui.MachineActivity}
 */
public class MachineInterface {
    private TextView memoryDisplay, programCounterDisplay, instructionDisplay;


    public MachineInterface(TextView memoryDisplay, TextView programCounterDisplay, TextView instructionDisplay) {
        this.memoryDisplay = memoryDisplay;
        this.programCounterDisplay = programCounterDisplay;
        this.instructionDisplay = instructionDisplay;
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
        instructionDisplay.setText("Instructions: " + instructions);
    }
}
