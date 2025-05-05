package com.example.nexview.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nexview.R;
import com.example.nexview.pages.HomeFragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


public class SignupActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        TextView signin = findViewById(R.id.signin);
        Button signup = findViewById(R.id.signup);

        EditText user = findViewById(R.id.etUsername);
        EditText pass = findViewById(R.id.etPassword);

        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username, password;
                username = user.getText().toString();
                password = pass.getText().toString();


                if(username.isEmpty()){
                    Toast.makeText(SignupActivity.this, "Email is Empty", Toast.LENGTH_SHORT).show();
                }

                if(password.isEmpty()){
                    Toast.makeText(SignupActivity.this, "Password is Empty", Toast.LENGTH_SHORT).show();
                }

                mAuth.createUserWithEmailAndPassword(username, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "Account Creation Successful.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SignupActivity.this, HomeFragment.class);
                                    startActivity(intent);
                                } else {
                                    if (task.getException() != null && task.getException().getMessage().contains("email address is already in use")) {
                                        Toast.makeText(SignupActivity.this, "Account already exists. Please sign in.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(SignupActivity.this, "Account Creation failed.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
            }
        });

        signin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, SigninActivity.class);
                startActivity(intent);
            }
        });
    }
}