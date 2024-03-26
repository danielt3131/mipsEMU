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
    private TextView memoryDisplay;

    public MachineInterface(Activity activity, Context context, TextView memoryDisplay) {
        this.activity = activity;
        this.context = context;
        this.memoryDisplay = memoryDisplay;
    }

    public void updateMemoryDisplay(String memory) {
        memoryDisplay.setText(memory);
    }
}
