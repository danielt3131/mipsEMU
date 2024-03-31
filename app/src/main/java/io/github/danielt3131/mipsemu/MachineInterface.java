package io.github.danielt3131.mipsemu;

import android.app.Activity;
import android.content.Context;
import android.widget.TextView;

/**
 * Class used for communication from {@link io.github.danielt3131.mipsemu.machine.MipsMachine} to {@link io.github.danielt3131.mipsemu.ui.MachineActivity}
 */
public class MachineInterface {
    private Activity activity;
    private Context context;
    private TextView memoryDisplay, programCounterDisplay;

    public MachineInterface(Activity activity, Context context, TextView memoryDisplay) {
        this.activity = activity;
        this.context = context;
        this.memoryDisplay = memoryDisplay;
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
}
