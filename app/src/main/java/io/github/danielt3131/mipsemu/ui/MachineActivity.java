package io.github.danielt3131.mipsemu.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;

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
                fileInputStream = getContentResolver().openInputStream(fileUri);
                }
            }
        super.onActivityResult(requestCode, resultCode, data);
    }


}