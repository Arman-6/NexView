package com.example.nexview;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PomodoroTimerService extends Service {

    private ScheduledExecutorService scheduler;
    private long totalTime = 0;
    private long sessionStartTime = 0;
    private String mailId;
    private String sessionName;
    private boolean isVideoPlaying = false;
    private static final String TAG = "PomodoroTimerService";
    private static final long UPLOAD_INTERVAL = 60; // in seconds

    // Pomodoro-specific variables
    private static final String CHANNEL_ID = "PomodoroTimerChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final long POMODORO_DURATION = 1500; // 25 minutes in seconds
    private long pomodoroTimeElapsed = 0;
    private Handler pomodoroHandler = new Handler();
    private Runnable pomodoroRunnable;
    private boolean isInBreak = false; // Tracks whether the current session is a break

    @Override
    public void onCreate() {
        super.onCreate();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Schedule periodic upload every 60 seconds
        scheduler.scheduleAtFixedRate(this::uploadToFirebase, UPLOAD_INTERVAL, UPLOAD_INTERVAL, TimeUnit.SECONDS);

        // Reset daily watch time at midnight
        long delayUntilMidnight = getMillisUntilMidnight();
        scheduler.scheduleAtFixedRate(() -> {
            uploadToFirebase();
            totalTime = 0;
        }, delayUntilMidnight, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);

        // Create notification channel for Pomodoro notifications
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Intent is null, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        sessionName = intent.getStringExtra("sessionName");
        mailId = intent.getStringExtra("mailId");
        boolean newIsVideoPlaying = intent.getBooleanExtra("isVideoPlaying", false);

        if (sessionName == null || mailId == null || mailId.isEmpty()) {
            Log.e(TAG, "Missing session data, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Handle video state changes properly
        if (isVideoPlaying != newIsVideoPlaying) {
            isVideoPlaying = newIsVideoPlaying;

            if (isVideoPlaying) {
                startCounter();
                if (isPomodoroEnabled()) {
                    startPomodoroTimer();
                }
            } else {
                // Stop the timer when video stops playing
                stopPomodoroTimer();
            }
        }

        // Check if we need to handle Pomodoro toggle state changes
        if (intent.hasExtra("pomodoroToggled")) {
            boolean isPomodoroEnabled = isPomodoroEnabled();
            Log.d(TAG, "Pomodoro toggle state changed, now: " + (isPomodoroEnabled ? "enabled" : "disabled"));

            if (isVideoPlaying) {
                if (isPomodoroEnabled) {
                    startPomodoroTimer();
                } else {
                    stopPomodoroTimer();
                }
            }
        }

        return START_STICKY;
    }

    private void startCounter() {
        Log.d(TAG, "Starting watch time counter");
        sessionStartTime = System.currentTimeMillis();

        scheduler.scheduleAtFixedRate(() -> {
            if (isVideoPlaying) {
                totalTime += 60; // Add 1 minute
                Log.d(TAG, "Total watch time: " + (totalTime / 60) + " minutes");
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    // New method for Pomodoro timer functionality
    private void startPomodoroTimer() {
        if (!isPomodoroEnabled()) {
            Log.d(TAG, "Pomodoro is disabled, not starting timer");
            return;
        }

        Log.d(TAG, "Starting Pomodoro timer");
        pomodoroTimeElapsed = 0;

        // Cancel any existing timer
        if (pomodoroRunnable != null) {
            pomodoroHandler.removeCallbacks(pomodoroRunnable);
        }

        pomodoroRunnable = new Runnable() {
            @Override
            public void run() {
                if (isVideoPlaying && isPomodoroEnabled()) {
                    pomodoroTimeElapsed++;
                    // For debugging - show elapsed time
                    Log.d("PomodoroTimer", "Time elapsed: " + pomodoroTimeElapsed + " seconds");

                    // Check if we've reached the Pomodoro duration (25 minutes)
                    if (pomodoroTimeElapsed >= POMODORO_DURATION) {
                        sendBreakNotification();

                        // Reset timer for next Pomodoro
                        pomodoroTimeElapsed = 0;
                    }

                    // Continue timer
                    pomodoroHandler.postDelayed(this, 1000);
                } else {
                    // If video is not playing or Pomodoro is disabled, don't continue
                    Log.d(TAG, "Video not playing or Pomodoro disabled, pausing timer");
                }
            }
        };
        // Start the timer
        pomodoroHandler.post(pomodoroRunnable);
    }

    // Stop the Pomodoro timer
    private void stopPomodoroTimer() {
        if (pomodoroRunnable != null) {
            pomodoroHandler.removeCallbacks(pomodoroRunnable);
            Log.d(TAG, "Pomodoro timer stopped");
        }
    }

    // Send a notification when it's time to take a break
    private void sendBreakNotification() {
        Intent intent = new Intent(this, BreakDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required to start an Activity from a Service
        startActivity(intent);

        Log.d(TAG, "Break dialog activity launched");
    }

    // Create notification channel for Android 8.0+
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pomodoro Timer";
            String description = "Notifications for Pomodoro study technique";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Log.d(TAG, "Notification channel created");
        }
    }

    private void uploadToFirebase() {
        if (mailId == null || sessionName == null || mailId.isEmpty() || sessionName.isEmpty()) {
            Log.e(TAG, "Cannot upload: missing mailId or sessionName");
            return;
        }

        if (totalTime <= 0) {
            Log.d(TAG, "No time to upload");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(mailId).collection("sessions").document(sessionName);

        long timeToUpload = totalTime / 60;
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                docRef.set(new HashMap<String, Object>() {{
                            put("watchTime", timeToUpload);
                            put("dailyRecords." + currentDate, timeToUpload);
                        }}).addOnSuccessListener(aVoid -> Log.d(TAG, "Session created and watch time uploaded"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to create session", e));
            } else {
                docRef.update("watchTime", FieldValue.increment(timeToUpload),
                                "dailyRecords." + currentDate, FieldValue.increment(timeToUpload))
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Watch Time Updated: +" + timeToUpload + " minutes"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to update time", e));
            }
        });

        totalTime = 0; // Reset after upload
    }

    private long getMillisUntilMidnight() {
        long now = System.currentTimeMillis();
        long midnight = now + (24 * 60 * 60 * 1000) - (now % (24 * 60 * 60 * 1000));
        return midnight - now;
    }

    @Override
    public void onDestroy() {
        if (isVideoPlaying && totalTime > 0) {
            uploadToFirebase();
        }
        stopPomodoroTimer();
        scheduler.shutdown();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBreakTimer() {
        Log.d(TAG, "Starting break timer");
        isInBreak = true;
        pomodoroTimeElapsed = 0;

        // Start the break timer
        pomodoroHandler.post(pomodoroRunnable);
    }

    private void startNextTimer() {
        Log.d(TAG, "Starting next timer");
        isInBreak = false;
        pomodoroTimeElapsed = 0;

        // Start the next work session
        startPomodoroTimer();
    }

    private boolean isPomodoroEnabled() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getBoolean("pomodoroEnabled", true); // Default to true if not set
    }
}