package io.github.danielt3131.mipsemu.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.github.danielt3131.mipsemu.R;

public class CreditsActivity extends AppCompatActivity {
    TextView creditsDaniel;
    private int numPress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_credits);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        numPress = 0;
        creditsDaniel = findViewById(R.id.credits_Daniel);
        creditsDaniel.setOnClickListener(creditsDanielListener);
    }
    View.OnClickListener creditsDanielListener = (v -> {
        numPress++;
        if (numPress >= 7) {
            Uri wepage = Uri.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&pp=ygUJcmljayByb2xs");
            Intent intent = new Intent(Intent.ACTION_VIEW, wepage);
            Toast.makeText(this, "You got rick rolled", Toast.LENGTH_LONG).show();
            startActivity(intent);
        }
    });
}