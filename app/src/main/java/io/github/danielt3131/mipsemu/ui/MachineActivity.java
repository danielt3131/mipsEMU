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

import java.io.FileNotFoundException;
import java.io.InputStream;

import io.github.danielt3131.mipsemu.MachineInterface;
import io.github.danielt3131.mipsemu.R;
import io.github.danielt3131.mipsemu.Reference;
import io.github.danielt3131.mipsemu.machine.MipsMachine;

public class MachineActivity extends AppCompatActivity {

    Toolbar machineToolbar;
    Button runOneTime, runThreeTimes, runContinously;
    CheckBox decimalMode, binaryMode, hexMode;
    TextView memoryDisplay;
    private final int FILE_OPEN_REQUEST = 4;
    Uri fileUri;
    MipsMachine mipsMachine;
    MachineInterface machineInterface;
    InputStream fileInputStream;

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

        machineToolbar = findViewById(R.id.materialToolbar);

        // Set textViews
        memoryDisplay = findViewById(R.id.memoryView);

        // Set buttons
        runOneTime = findViewById(R.id.runStepButton);
        runThreeTimes = findViewById(R.id.runStep3Button);
        runContinously = findViewById(R.id.runContinouslyButton);

        // Set Checkboxes
        decimalMode = findViewById(R.id.decimialDisplayMode);
        binaryMode = findViewById(R.id.binaryDisplayMode);
        hexMode = findViewById(R.id.hexDisplayMode);

        // Inflate toolbar
        setSupportActionBar(machineToolbar);

        // Create Machine interface

        machineInterface = new MachineInterface(this, this, memoryDisplay);
        createMipsMachine();
        // Set buttons and checkboxes to their listeners
        decimalMode.setOnClickListener(decimalModeListener);
        hexMode.setOnClickListener(hexModeListener);
        binaryMode.setOnClickListener(binaryModeListener);
        memoryDisplay.setMovementMethod(new ScrollingMovementMethod());
        runContinously.setOnClickListener(v -> {
            try {
                mipsMachine.readFile();  // Debug to test
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

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
        // File selection / open option
        if (item.getItemId() == R.id.fileOpen) {
            // Summon the needed intent
            Intent openFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            openFile.setType("text/*"); // Limit the file selection to text files -> from .txt to .java
            startActivityForResult(openFile, FILE_OPEN_REQUEST);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == FILE_OPEN_REQUEST) {
            if (data != null) {
                fileUri = data.getData();
                try {
                    fileInputStream = getContentResolver().openInputStream(fileUri);
                    Log.d("Opening file", "Opened file");
                    mipsMachine.setInputFileStream(fileInputStream);
                } catch (FileNotFoundException e) {
                    Log.e("Opening file", e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
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
            decimalMode.setChecked(false);

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

}