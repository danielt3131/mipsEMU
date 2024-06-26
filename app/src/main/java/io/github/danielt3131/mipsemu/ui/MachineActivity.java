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
package io.github.danielt3131.mipsemu.ui;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.R;
import io.github.danielt3131.mipsemu.Reference;
import io.github.danielt3131.mipsemu.machine.MipsMachine;

public class MachineActivity extends AppCompatActivity implements ProgramCounterDialog.ProgramCounterDialogListener, MemoryEditDialog.MemoryEditDialogListener {

    Toolbar machineToolbar;
    Button runOneTime, runMicroStep, runContinously;
    RadioButton decimalMode, binaryMode, hexMode;
    TextView memoryDisplay, programCounterDisplay, instructionDisplay, cacheHitRateDisplay;
    TextView[] registerDisplays;
    ScrollView memoryScrollView, registerScrollView;
    private final int FILE_OPEN_REQUEST = 4;
    Uri inputFileUri;
    Uri outputFileUri;
    MipsMachine mipsMachine;
    MachineInterface machineInterface;
    InputStream fileInputStream;
    private boolean gotInputStream = false;
    private int memorySize = 1000*100;    // Default limit 100 KB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_machine);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Force portrait mode
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        /*if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            Log.d("Screen", "Tablet");
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Log.d("Screen", "Phone");
        }*/
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        // Set toolbar
        machineToolbar = findViewById(R.id.materialToolbar);

        // Set textViews
        memoryDisplay = findViewById(R.id.memoryView);
        programCounterDisplay = findViewById(R.id.programCounterDisplay);
        instructionDisplay = findViewById(R.id.instructionDisplay);
        cacheHitRateDisplay = findViewById(R.id.cacheHitRate);

        // Set ScrollViews
        memoryScrollView = findViewById(R.id.memoryScrollView);
        registerScrollView = findViewById(R.id.registerScrollView);

        // Inflate the registers
        inflateRegisters();

        // Set buttons
        runOneTime = findViewById(R.id.runStepButton);
        runMicroStep = findViewById(R.id.runMicroStepButton);
        runContinously = findViewById(R.id.runContinouslyButton);

        // Set Checkboxes
        decimalMode = findViewById(R.id.decimalDisplayMode);
        binaryMode = findViewById(R.id.binaryDisplayMode);
        hexMode = findViewById(R.id.hexDisplayMode);

        // Inflate toolbar
        setSupportActionBar(machineToolbar);

        // Create Machine interface
        machineInterface = new MachineInterface(memoryDisplay, programCounterDisplay, instructionDisplay, registerDisplays, cacheHitRateDisplay, this);

        // Get the amount of memory available -> Java heap limit
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        Log.d("Memory", String.valueOf(memoryInfo.availMem));
        Log.d("Memory", String.valueOf(activityManager.getMemoryClass()));

        // For devices with low memory
        if (memoryInfo.lowMemory) {
            memorySize = 1000*50;   // 50 KB
        }
        // Create the machine
        createMipsMachine();
        /*
        if (screenSize >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            memoryDisplay.setTextSize(28);  // Hacky fix for tablets
        } else {
            // Screen width
            float screenWidth = getWindowManager().getCurrentWindowMetrics().getBounds().width();
            // Calculate memory view font size for phones
            FontUtils fontUtils = new FontUtils(memoryDisplay.getTextSize(), screenWidth, memoryDisplay.getTypeface());
            // Convert from px to sp (pixels to scaled pixels)
            memoryDisplay.setTextSize(TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, fontUtils.binaryTextSize(), getResources().getDisplayMetrics()));
        }
         */

        // Init the displays
        //machineInterface.clearAll();    // Clear the display to display proper register names
        hexMode.setChecked(true);   // Set the default memory display mode to be hex
        mipsMachine.setDisplayFormat(Reference.HEX_MODE);
        // From other apps / share menu
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (type != null) {
            Log.d("Intent", type);
        }
        if (Intent.ACTION_SEND.equals(action) && "application/txt".equals(type)) {
            inputFileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
            gotInputStream = true;
            try {
                fileInputStream = getContentResolver().openInputStream(inputFileUri);
                mipsMachine.setInputFileStream(fileInputStream);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            // For creating a new blank machine -> send blank memory and registers
            mipsMachine.sendMemory();   // Show blank memory to display
            mipsMachine.sendAllRegistersToDisplay();
        }

        // Set buttons and radio buttons to their listeners
        decimalMode.setOnClickListener(decimalModeListener);
        hexMode.setOnClickListener(hexModeListener);
        binaryMode.setOnClickListener(binaryModeListener);
        //memoryDisplay.setMovementMethod(new ScrollingMovementMethod());
        instructionDisplay.setMovementMethod(new ScrollingMovementMethod());
        runMicroStep.setOnClickListener(runMicroStepListener);
        runOneTime.setOnClickListener(runOneStepListener);
        runContinously.setOnClickListener(runContinuouslyListener);


    }

    // Create Mips Machine method
    private void createMipsMachine() {
        mipsMachine = new MipsMachine(memorySize, machineInterface, this);
    }

    /**
     * Destroys the activity
     */
    @Override
    protected void onDestroy() {
        mipsMachine.onDestroy();    // Ensure that the file streams are closed
        super.onDestroy();
    }

    /**
     * Creates register text view array and set each element to its text view
     */
    private void inflateRegisters() {
        registerDisplays = new TextView[32];    // The number of registers
        registerDisplays[Reference.REGISTER_ZERO] = findViewById(R.id.register_ZERO);

        registerDisplays[Reference.REGISTER_V0] = findViewById(R.id.register_V0);
        registerDisplays[Reference.REGISTER_V1] = findViewById(R.id.register_V1);

        registerDisplays[Reference.REGISTER_T0] = findViewById(R.id.register_T0);
        registerDisplays[Reference.REGISTER_T1] = findViewById(R.id.register_T1);
        registerDisplays[Reference.REGISTER_T2] = findViewById(R.id.register_T2);
        registerDisplays[Reference.REGISTER_T3] = findViewById(R.id.register_T3);
        registerDisplays[Reference.REGISTER_T4] = findViewById(R.id.register_T4);
        registerDisplays[Reference.REGISTER_T5] = findViewById(R.id.register_T5);
        registerDisplays[Reference.REGISTER_T6] = findViewById(R.id.register_T6);
        registerDisplays[Reference.REGISTER_T7] = findViewById(R.id.register_T7);
        registerDisplays[Reference.REGISTER_T8] = findViewById(R.id.register_T8);
        registerDisplays[Reference.REGISTER_T9] = findViewById(R.id.register_T9);

        registerDisplays[Reference.REGISTER_A0] = findViewById(R.id.register_A0);
        registerDisplays[Reference.REGISTER_A1] = findViewById(R.id.register_A1);
        registerDisplays[Reference.REGISTER_A2] = findViewById(R.id.register_A2);
        registerDisplays[Reference.REGISTER_A3] = findViewById(R.id.register_A3);

        registerDisplays[Reference.REGISTER_K0] = findViewById(R.id.register_K0);
        registerDisplays[Reference.REGISTER_K1] = findViewById(R.id.register_K1);

        registerDisplays[Reference.REGISTER_S0] = findViewById(R.id.register_S0);
        registerDisplays[Reference.REGISTER_S1] = findViewById(R.id.register_S1);
        registerDisplays[Reference.REGISTER_S2] = findViewById(R.id.register_S2);
        registerDisplays[Reference.REGISTER_S3] = findViewById(R.id.register_S3);
        registerDisplays[Reference.REGISTER_S4] = findViewById(R.id.register_S4);
        registerDisplays[Reference.REGISTER_S5] = findViewById(R.id.register_S5);
        registerDisplays[Reference.REGISTER_S6] = findViewById(R.id.register_S6);
        registerDisplays[Reference.REGISTER_S7] = findViewById(R.id.register_S7);

        registerDisplays[Reference.REGISTER_FP] = findViewById(R.id.register_FP);
        registerDisplays[Reference.REGISTER_SP] = findViewById(R.id.register_SP);
        registerDisplays[Reference.REGISTER_GP] = findViewById(R.id.register_GP);
        registerDisplays[Reference.REGISTER_RA] = findViewById(R.id.register_RA);
        registerDisplays[Reference.REGISTER_AT] = findViewById(R.id.register_AT);


    }

    // Create the menu options in the toolbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    // Menu selection
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Menu options
        if (item.getItemId() == R.id.fileOpen) {
            // Summon the needed intent
            Intent openFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            openFile.setType("text/*"); // Limit the file selection to text files -> from .txt to .java
            startActivityForResult(openFile, FILE_OPEN_REQUEST);
            return true;
        }
        if (item.getItemId() == R.id.editPC) {
            // Pull up a dialog box for the user to edit the PC (Program Counter) variable
            DialogFragment dialogFragment = new ProgramCounterDialog(String.valueOf(mipsMachine.getProgramCounter()));
            dialogFragment.show(getSupportFragmentManager(), "pc");
            return true;
        }
        if (item.getItemId() == R.id.machineReset) {
            resetMachine(true);
            return true;
        }
        if (item.getItemId() == R.id.saveState) {
            createOutputStream();
        }
        if (item.getItemId() == R.id.creditsOption) {
            Intent startCredits = new Intent(MachineActivity.this, CreditsActivity.class);
            startActivity(startCredits);
        }
        if (item.getItemId() == R.id.scrollTop) {
            memoryScrollView.fullScroll(View.FOCUS_UP);
            registerScrollView.fullScroll(View.FOCUS_UP);
        }
        if (item.getItemId() == R.id.scrollBottom) {
            memoryScrollView.fullScroll(View.FOCUS_DOWN);
            registerScrollView.fullScroll(View.FOCUS_DOWN);
        }
        if (item.getItemId() == R.id.setMemorySize) {
            DialogFragment dialogFragment = new MemoryEditDialog(String.valueOf(memorySize / 1000));
            dialogFragment.show(getSupportFragmentManager(), "memory");
        }
        return false;
    }

    /**
     * Method to reset the machine
     * @param resetMemoryDisplay If true then the memory and cache displays will be updated otherwise the memory and register displays will not be updated
     */
    private void resetMachine(boolean resetMemoryDisplay) {
        if (gotInputStream) {
            mipsMachine.onDestroy();    // Ensure that the file streams are closed
        }
        mipsMachine = null; // Deallocate the object
        System.gc();// Call the garbage collector to clean up mipsMachine
        gotInputStream = false; // Require a new file selection
        // Reset the machine by creating new object with the same reference name
        createMipsMachine();

        mipsMachine.setDisplayFormat(getDisplayMode());  // Provide the display mode to the machine

        // Clear the instruction and cache hit displays
        machineInterface.clearAll();
        if (resetMemoryDisplay) {
            // Send the blank memory to be displayed
            mipsMachine.sendMemory();
            mipsMachine.sendAllRegistersToDisplay();
        }
        mipsMachine.sendProgramCounter();
    }

    /**
     * Method to get the current display mode hex, binary, or decimal
     * @return The display mode
     */
    private int getDisplayMode() {
        if (binaryMode.isChecked()) {
            return Reference.BINARY_MODE;
        } else if (hexMode.isChecked()) {
            return Reference.HEX_MODE;
        } else {
            return Reference.DECIMIAL_MODE;
        }
    }

    /**
     * Creates an output stream to save a file in shared storage by using the Storage Access Frameworks
     * <p>
     * See <a href="https://developer.android.com/training/data-storage/shared/documents-files">...</a>
     */
    private void createOutputStream() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, Reference.CREATE_OUTPUTSTREAM);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == FILE_OPEN_REQUEST) {
            if (data != null) {
                inputFileUri = data.getData();
                try {
                    fileInputStream = getContentResolver().openInputStream(inputFileUri);
                    Log.d("Opening file", "Opened file");
                    if (gotInputStream) {
                        resetMachine(false);     // Don't update the memory and register displays to not cause a race condition
                    }
                    mipsMachine.setInputFileStream(fileInputStream);
                    gotInputStream = true;
                } catch (FileNotFoundException e) {
                    Log.e("Opening file", e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (resultCode == RESULT_OK && requestCode == Reference.CREATE_OUTPUTSTREAM) {
            if (data != null) {
                outputFileUri = data.getData();
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(outputFileUri);
                    mipsMachine.saveState(outputStream, outputFileUri, this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
    /**
     * Listeners for all radio buttons
     */
    View.OnClickListener hexModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Tell MipsMachine the memory display option
            mipsMachine.setDisplayFormat(Reference.HEX_MODE);
            mipsMachine.sendMemory();
            mipsMachine.sendAllRegistersToDisplay();
            mipsMachine.sendProgramCounter();
        }
    };

    View.OnClickListener decimalModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Tell MipsMachine the memory display option
            mipsMachine.setDisplayFormat(Reference.DECIMIAL_MODE);
            mipsMachine.sendMemory();
            mipsMachine.sendAllRegistersToDisplay();
            mipsMachine.sendProgramCounter();
        }
    };

    View.OnClickListener binaryModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Tell MipsMachine the memory display option
            mipsMachine.setDisplayFormat(Reference.BINARY_MODE);
            mipsMachine.sendMemory();
            mipsMachine.sendAllRegistersToDisplay();
            mipsMachine.sendProgramCounter();
        }
    };

    /**
     * Method interface to get the program counter value from a dialog
     * @param dialog The dialog
     * @param programCounterValue The program counter value as a string
     */
    @Override
    public void onPositiveClick(DialogFragment dialog, String programCounterValue) {
        try {
            mipsMachine.setProgramCounter(Integer.parseInt(programCounterValue));
        } catch (NumberFormatException e) {
            Log.e("SetPC", e.getMessage());
        }
    }

    /**
     * Method interface to get the program counter value from a dialog
     * @param dialog The dialog
     * @param memorySize The memory size in KB
     */
    @Override
    public void onPositiveClick(DialogFragment dialog, int memorySize) {
        this.memorySize = memorySize * 1000;    // Kilobytes to bytes
        resetMachine(true);
        Log.d("Memory", "Set memory size to " + this.memorySize);
        Toast.makeText(this, "The memory size is now " + memorySize + "KB", Toast.LENGTH_SHORT).show();
    }


    /**
     * Click listeners for the run buttons
     */
    View.OnClickListener runMicroStepListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (gotInputStream) {
                // Run microstep
                mipsMachine.runNextMicroStep();
            } else {
                Toast.makeText(MachineActivity.this, "Need file", Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnClickListener runOneStepListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (gotInputStream) {
                // Run one step
                mipsMachine.runNextStep();
            } else {
                Toast.makeText(MachineActivity.this, "Need file", Toast.LENGTH_SHORT).show();
            }
        }
    };

    View.OnClickListener runContinuouslyListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (gotInputStream) {
                mipsMachine.runContinuously();
            } else {
                Toast.makeText(MachineActivity.this, "Need file", Toast.LENGTH_SHORT).show();
            }
        }
    };
}