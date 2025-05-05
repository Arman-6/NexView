package com.example.nexview.session;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.nexview.R;
import com.example.nexview.auth.SigninActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class CreateSessionActivity extends AppCompatActivity {

    private EditText topicInput, dailyGoalInput, timeframeInput;
    private Button submitSessionButton;
    private RecyclerView sessionsRecyclerView;
    private SessionAdapter sessionAdapter;
    private List<Session> sessionList;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        showDndDialog();
        initializeFirebase();
        initializeUI();
        setupRecyclerView();
        loadSessions();
        setupButtonListeners();
        SharedPreferences preferences = getSharedPreferences("UserDetails", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("mailId", userId);  // Replace userId with actual mail ID
        editor.apply();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (userId != null) {
            userId = userId.replace(".", ""); // Firestore does not allow dots in document IDs
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeUI() {
        topicInput = findViewById(R.id.topicInput);
        dailyGoalInput = findViewById(R.id.dailyGoalInput);
        timeframeInput = findViewById(R.id.timeframeInput);
        submitSessionButton = findViewById(R.id.submitSessionButton);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
    }

    private void setupRecyclerView() {
        sessionList = new ArrayList<>();
        sessionAdapter = new SessionAdapter(this, sessionList);
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionAdapter);
    }

    private void setupButtonListeners() {
        submitSessionButton.setOnClickListener(v -> addSessionToFirestore());
        setupLogout();
    }

    private void addSessionToFirestore() {
        String topic = topicInput.getText().toString().trim();
        String dailyGoal = dailyGoalInput.getText().toString().trim();
        String timeframe = timeframeInput.getText().toString().trim();

        if (topic.isEmpty() || dailyGoal.isEmpty() || timeframe.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);
        CollectionReference sessionsRef = userRef.collection("sessions");

        sessionsRef.document(topic).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "Session with this topic already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        addSession(sessionsRef, topic, dailyGoal, timeframe);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking topic uniqueness", Toast.LENGTH_SHORT).show());
    }

    private void addSession(CollectionReference sessionsRef, String topic, String dailyGoal, String timeframe) {
        Session session = new Session(topic, dailyGoal, timeframe);
        sessionsRef.document(topic).set(session)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Session added", Toast.LENGTH_SHORT).show();
                    sessionList.add(session);
                    sessionAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add session", Toast.LENGTH_SHORT).show());
    }

    private void loadSessions() {
        db.collection("users").document(userId).collection("sessions").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sessionList.clear();
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Session session = snapshot.toObject(Session.class);
                        if (session != null) {
                            sessionList.add(session);
                        }
                    }
                    sessionAdapter.notifyDataSetChanged();
                    if (sessionList.isEmpty()) {
                        Toast.makeText(this, "No sessions available", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load sessions", Toast.LENGTH_SHORT).show());
    }

    private void setupLogout() {
        TextView logout = findViewById(R.id.logout);
        logout.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(CreateSessionActivity.this, SigninActivity.class));
            Toast.makeText(CreateSessionActivity.this, "Logout Successful", Toast.LENGTH_SHORT).show();
        });
    }

    private void showDndDialog() {
        LinearLayout ll = findViewById(R.id.dndDialog);
        View view = LayoutInflater.from(CreateSessionActivity.this).inflate(R.layout.dnd_dialog, ll);

        MaterialSwitch tb = view.findViewById(R.id.tg);
        ImageButton btnCancel = view.findViewById(R.id.btnCancel);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(CreateSessionActivity.this);
        builder.setView(view);
        final AlertDialog alertDialog = builder.create();

        // Check if DND Permission is granted
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "Grant DND Permission", Toast.LENGTH_SHORT).show();
        }

        // Set switch state based on current DND state
        if (notificationManager.isNotificationPolicyAccessGranted()) {
            int interruptionFilter = notificationManager.getCurrentInterruptionFilter();
            if (interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE) {
                tb.setChecked(true);
            } else {
                tb.setChecked(false);
            }
        }

        tb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (notificationManager.isNotificationPolicyAccessGranted()) {
                    if (tb.isChecked()) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
                        Toast.makeText(CreateSessionActivity.this, "DND Turned ON", Toast.LENGTH_SHORT).show();
                    } else {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        Toast.makeText(CreateSessionActivity.this, "DND Turned OFF", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CreateSessionActivity.this, "Permission Not Granted", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

}
