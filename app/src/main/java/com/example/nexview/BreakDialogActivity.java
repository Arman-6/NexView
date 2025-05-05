package com.example.nexview;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class BreakDialogActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_break);

        // Set up the buttons
        Button btnContinue = findViewById(R.id.btn_continue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // User chose to continue
                Intent intent = new Intent(BreakDialogActivity.this, PomodoroTimerService.class);
                intent.setAction("CONTINUE");
                startService(intent);
                finish();
            }
        });
    }
}