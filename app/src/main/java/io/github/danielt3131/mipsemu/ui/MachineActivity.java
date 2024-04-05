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

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

import io.github.danielt3131.mipsemu.FileUtils;
import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.R;
import io.github.danielt3131.mipsemu.Reference;
import io.github.danielt3131.mipsemu.machine.MipsMachine;

public class MachineActivity extends AppCompatActivity implements ProgramCounterDialog.ProgramCounterDialogListener{

    Toolbar machineToolbar;
    Button runOneTime, runMicroStep, runContinously, runFromState;
    CheckBox decimalMode, binaryMode, hexMode;
    TextView memoryDisplay, programCounterDisplay, instructionDisplay;
    private final int FILE_OPEN_REQUEST = 4;
    Uri inputFileUri, outputFileUri;
    MipsMachine mipsMachine;
    MachineInterface machineInterface;
    InputStream fileInputStream;
    private boolean gotInputStream = false;

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
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set toolbar
        machineToolbar = findViewById(R.id.materialToolbar);

        // Set textViews
        memoryDisplay = findViewById(R.id.memoryView);
        programCounterDisplay = findViewById(R.id.programCounterDisplay);
        instructionDisplay = findViewById(R.id.instructionDisplay);

        // Set buttons
        runOneTime = findViewById(R.id.runStepButton);
        runMicroStep = findViewById(R.id.runMicroStepButton);
        runContinously = findViewById(R.id.runContinouslyButton);
        runFromState = findViewById(R.id.runStateButton);

        // Set Checkboxes
        decimalMode = findViewById(R.id.decimialDisplayMode);
        binaryMode = findViewById(R.id.binaryDisplayMode);
        hexMode = findViewById(R.id.hexDisplayMode);

        // Inflate toolbar
        setSupportActionBar(machineToolbar);

        // Create Machine interface
        machineInterface = new MachineInterface(memoryDisplay, programCounterDisplay, instructionDisplay);

        // Create the machine
        createMipsMachine();

        // Set buttons and checkboxes to their listeners
        decimalMode.setOnClickListener(decimalModeListener);
        hexMode.setOnClickListener(hexModeListener);
        binaryMode.setOnClickListener(binaryModeListener);
        memoryDisplay.setMovementMethod(new ScrollingMovementMethod());
        runMicroStep.setOnClickListener(runMicroStepListener);
        runOneTime.setOnClickListener(runOneStepListener);
        runContinously.setOnClickListener(runContinuouslyListener);
        runFromState.setOnClickListener(runFromStateListener);
    }

    // Create Mips Machine method

    private void createMipsMachine() {
        mipsMachine = new MipsMachine(2000, machineInterface);
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
            DialogFragment dialogFragment = new ProgramCounterDialog();
            dialogFragment.show(getSupportFragmentManager(), "pc");
            return true;
        }
        if (item.getItemId() == R.id.machineReset) {
            mipsMachine = null; // Deallocate the object
            System.gc();    // Call the garbage collector to clean up mipsMachine
            // Reset the machine by creating new object with the same reference name
            createMipsMachine();
            return true;
        }
        if (item.getItemId() == R.id.saveState) {
            createOutputStream();
        }
        return false;
    }

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
                    mipsMachine.saveState(outputStream);
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
     * Listeners for all checkboxes
     */
    View.OnClickListener hexModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Switch the other 2 checkboxes to be off
            decimalMode.setChecked(false);
            binaryMode.setChecked(false);

            // Tell MipsMachine the memory display option
            mipsMachine.setMemoryFormat(Reference.HEX_MODE);
            mipsMachine.sendMemory();
        }
    };

    View.OnClickListener decimalModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Switch the other 2 checkboxes to be off
            hexMode.setChecked(false);
            binaryMode.setChecked(false);

            // Tell MipsMachine the memory display option
            mipsMachine.setMemoryFormat(Reference.DECIMIAL_MODE);
            mipsMachine.sendMemory();
        }
    };

    View.OnClickListener binaryModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Switch the other 2 checkboxes to be off
            decimalMode.setChecked(false);
            hexMode.setChecked(false);

            // Tell MipsMachine the memory display option
            mipsMachine.setMemoryFormat(Reference.BINARY_MODE);
            mipsMachine.sendMemory();
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
            mipsMachine.setPc(Integer.parseInt(programCounterValue));
        } catch (NumberFormatException e) {
            Log.e("SetPC", e.getMessage());
        }
    }

    /**
     * Methods for the clicking buttons
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


    View.OnClickListener runFromStateListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (FileUtils.getFileName(MachineActivity.this, inputFileUri).contains(".mst")) {
                mipsMachine.readState();
                Log.d("State", "Reading in the state");
            } else {
                Toast.makeText(MachineActivity.this, "Wrong file", Toast.LENGTH_SHORT).show();
            }
        }
    };
}