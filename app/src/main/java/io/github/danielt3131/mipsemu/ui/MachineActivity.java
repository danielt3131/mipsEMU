package io.github.danielt3131.mipsemu.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

import io.github.danielt3131.mipsemu.R;
import io.github.danielt3131.mipsemu.machine.MipsMachine;

public class MachineActivity extends AppCompatActivity {

    Toolbar machineToolbar;
    Button runOneTime, runThreeTimes, runContinously;
    CheckBox decimalMode, binaryMode, hexMode;
    private final int FILE_OPEN_REQUEST = 4;
    final int HEX_MODE = 1;
    final int BINARY_MODE = 0;
    final int DECIMIAL_MODE = 2;
    Uri fileUri;
    MipsMachine mipsMachine;
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

        // Create Mips Machine
        mipsMachine = new MipsMachine(2000);

        // Set buttons and checkboxes to their listeners
        decimalMode.setOnClickListener(decimalModeListener);
        hexMode.setOnClickListener(hexModeListener);
        binaryMode.setOnClickListener(binaryModeListener);

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
                } catch (FileNotFoundException e) {
                    Log.e("Opening file", e.getMessage());
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // Boolean values for the 3 display modes
    boolean isHex;
    boolean isDecimial;
    boolean isBinary;

    /**
     * Listeners for all checkboxes
     */
    View.OnClickListener hexModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isHex = true;
            // Set the other 2 to false
            isDecimial = false;
            isBinary = false;
            // Update the memory display
            updateMemoryDisplay();
        }
    };

    View.OnClickListener decimalModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isDecimial = true;
            // Set the other 2 to false
            isHex = false;
            isBinary = false;
            // Update the memory display
            updateMemoryDisplay();
        }
    };

    View.OnClickListener binaryModeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            isBinary = true;
            // Set the other 2 to false
            isDecimial = false;
            isHex = false;
            // Update the memory display
            updateMemoryDisplay();
        }
    };


    private void updateMemoryDisplay() {
        // TODO Get string of memory from MipsMachine with the correct display mode via a method call with 0 = binary, 1 = hex, and 2 = decimial
        if (isHex) {
            // Call method with HEX_MODE
        } else if (isBinary) {
            // Call method with BINARY_MODE
        } else if (isDecimial) {
            // Call method with DECIMAL_MODE
        }
    }
}