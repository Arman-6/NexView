package com.example.nexview.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.nexview.PomodoroTimerService;
import com.example.nexview.R;
import com.example.nexview.GreyDecorator;
import com.example.nexview.GreenDecorator;
import com.example.nexview.auth.SigninActivity;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private GreenDecorator greenDecorator;
    private GreyDecorator greyDecorator;
    private FirebaseFirestore db;
    private ImageView profileImage;
    private TextView username, email;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private String userId, sessionName;
    private float watchTime, dailyGoal;
    private PieChart pieChart;
    private TextView ut, tf, dg;
    private static final String TAG = "ProfileFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize calendarView - missing initialization in original code
        calendarView = view.findViewById(R.id.calendarView);
        setupPomodoroToggle(view);

        // Animation
        calendarView.setAlpha(0f);
        calendarView.animate()
                .alpha(1f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        greenDecorator = new GreenDecorator();
        greyDecorator = new GreyDecorator();
        calendarView.addDecorator(greenDecorator);
        calendarView.addDecorator(greyDecorator);

        profileImage = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);
        logoutButton = view.findViewById(R.id.logout);
        email = view.findViewById(R.id.email);
        pieChart = view.findViewById(R.id.pieChart);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        ut = view.findViewById(R.id.userTopic);
        tf = view.findViewById(R.id.timeframe);
        dg = view.findViewById(R.id.dailyGoal);

        if (user != null) {
            String userEmail = user.getEmail();
            if (userEmail != null) {
                userId = userEmail.replace(".", "");

                username.setText(user.getDisplayName() != null ? user.getDisplayName() : "No Name");
                email.setText(userEmail);

                // Load both session details and chart data
                loadUserSessionDetails(userEmail);

                Uri photoUrl = user.getPhotoUrl();
                if (photoUrl != null) {
                    Glide.with(this)
                            .load(photoUrl)
                            .placeholder(R.drawable.profile_pic)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.profile_pic);
                }
            } else {
                Toast.makeText(getContext(), "User email is null", Toast.LENGTH_SHORT).show();
            }
        }

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), SigninActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        // Get the current selected topic from SharedPreferences
        SharedPreferences preferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sessionName = preferences.getString("selectedTopic", "DefaultSession");

        // Initial chart setup with no data
        setupPieChart(0, 60); // Default values until data loads

        // Load the actual session data
        loadSessionData();

        // Moved loadDailyRecords() here to ensure userId and sessionName are initialized
        loadDailyRecords();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when coming back to the fragment
        if (userId != null && sessionName != null) {
            loadSessionData();
            loadDailyRecords(); // Also reload calendar data
        }
    }

    private void loadSessionData() {
        if (userId == null || sessionName == null) {
            return;
        }

        DocumentReference sessionRef = db.collection("users")
                .document(userId)
                .collection("sessions")
                .document(sessionName);

        sessionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get current date
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                // Check daily records
                Map<String, Object> dailyRecords = (Map<String, Object>) documentSnapshot.get("dailyRecords");

                if (dailyRecords != null && dailyRecords.containsKey(currentDate)) {
                    // Get today's watch time
                    Object todayWatchTime = dailyRecords.get(currentDate);

                    if (todayWatchTime != null) {
                        watchTime = todayWatchTime instanceof Long ?
                                ((Long) todayWatchTime).floatValue() :
                                Float.parseFloat(todayWatchTime.toString());
                    } else {
                        watchTime = 0;
                    }
                } else {
                    // If no record for today, set watch time to 0
                    watchTime = 0;
                }

                // Get daily goal
                if (documentSnapshot.contains("dailyGoal")) {
                    Object dailyGoalObj = documentSnapshot.get("dailyGoal");

                    if (dailyGoalObj != null) {
                        String dailyGoalString = dailyGoalObj.toString();
                        try {
                            dailyGoal = Float.parseFloat(dailyGoalString) * 60; // Convert hours to minutes
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid dailyGoal format", Toast.LENGTH_SHORT).show();
                            dailyGoal = 60; // Default to 1 hour
                        }
                    } else {
                        dailyGoal = 60; // Default to 1 hour
                    }
                } else {
                    dailyGoal = 60; // Default to 1 hour if not set
                }

                // Setup pie chart with current day's data
                setupPieChart(watchTime, dailyGoal);

                // Also update the text fields with session details
                updateSessionTextFields(documentSnapshot);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to load session data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateSessionTextFields(DocumentSnapshot documentSnapshot) {
        // Update the topic, timeframe and daily goal text views
        String topic = documentSnapshot.getString("topic");
        String timeFrame = documentSnapshot.getString("timeframe");
        String dailyGoalStr = documentSnapshot.getString("dailyGoal");

        if (topic != null) {
            ut.setText(topic);
        } else {
            ut.setText(sessionName); // Fall back to session name
        }

        if (timeFrame != null) {
            tf.setText(timeFrame + " Months");
        } else {
            tf.setText("Unknown");
        }

        if (dailyGoalStr != null) {
            dg.setText(dailyGoalStr + " Hours");
        } else {
            dg.setText("1 Hour"); // Default
        }
    }

    private void loadUserSessionDetails(String email) {
        if (email == null) return;

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String selectedTopic = sharedPreferences.getString("selectedTopic", "Technology");

        if ("Technology".equals(selectedTopic)) {
            ut.setText("Technology");
            tf.setText("Default");
            dg.setText("1 Hour");
            return;
        }

        // Rest of your session loading logic is now handled in loadSessionData()
    }

    private void setupPieChart(float watchTime, float dailyGoal) {
        if (pieChart == null || getView() == null) return;

        ArrayList<PieEntry> entries = new ArrayList<>();

        // Calculate remaining time, ensure it's not negative
        float remainingTime = Math.max(0, dailyGoal - watchTime);

        // Add watched time entry if there is any
        if (watchTime > 0) {
            entries.add(new PieEntry(watchTime, "Watched"));
        }

        // Add remaining time if there is any goal remaining
        if (remainingTime > 0) {
            entries.add(new PieEntry(remainingTime, "Remaining"));
        }

        // If both values are 0, add a placeholder
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "No data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Set colors based on progress
        if (watchTime >= dailyGoal) {
            // Goal achieved - use green for watched
            dataSet.setColors(Color.rgb(255, 0, 0), Color.LTGRAY);
        } else {
            // Goal not achieved - use blue for watched, orange for remaining
            dataSet.setColors(Color.rgb(33, 150, 243), Color.rgb(255, 152, 0));
        }

        dataSet.setValueTextColors(Arrays.asList(Color.WHITE, Color.WHITE));
        dataSet.setValueTextSize(14f);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);

        // Configure chart appearance
        pieChart.setDrawHoleEnabled(true);
        pieChart.setCenterText(String.format(Locale.getDefault(),
                "%.0f / %.0f\nminutes", watchTime, dailyGoal));
        pieChart.setCenterTextSize(14f);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        // Disable legend and description
        pieChart.getLegend().setEnabled(true);
        pieChart.getDescription().setEnabled(false);

        // Disable rotation and set animation
        pieChart.setRotationEnabled(false);
        pieChart.animateY(1000, Easing.EaseInOutCubic);

        pieChart.invalidate();
    }

    private void loadDailyRecords() {
        if (userId == null || sessionName == null || db == null) {
            return;
        }

        DocumentReference sessionRef = db.collection("users")
                .document(userId)
                .collection("sessions")
                .document(sessionName);

        sessionRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get daily goal in its original unit (hours)
                float dailyGoalHours = 1.0f; // Default 1 hour

                if (documentSnapshot.contains("dailyGoal")) {
                    Object dailyGoalObj = documentSnapshot.get("dailyGoal");
                    if (dailyGoalObj != null) {
                        try {
                            dailyGoalHours = Float.parseFloat(dailyGoalObj.toString());
                        } catch (NumberFormatException e) {
                            dailyGoalHours = 1.0f; // Default if parsing fails
                        }
                    }
                }

                // Get the unit information (whether watchTime is stored in minutes or hours)
                // If this field doesn't exist, assume minutes as the default
                boolean watchTimeInMinutes = true;
                if (documentSnapshot.contains("timeUnit")) {
                    String timeUnit = documentSnapshot.getString("timeUnit");
                    watchTimeInMinutes = !"hours".equalsIgnoreCase(timeUnit);
                }

                // Process daily records
                Map<String, Object> dailyRecords = (Map<String, Object>) documentSnapshot.get("dailyRecords");
                if (dailyRecords != null) {
                    // Reset decorators
                    greenDecorator = new GreenDecorator();
                    greyDecorator = new GreyDecorator();
                    calendarView.removeDecorators();

                    // Log the daily goal for debugging
                    Log.d(TAG, "Daily goal: " + dailyGoalHours + " hours");

                    for (Map.Entry<String, Object> entry : dailyRecords.entrySet()) {
                        String dateStr = entry.getKey();
                        Object watchTimeObj = entry.getValue();

                        float watchTimeValue = 0;
                        if (watchTimeObj instanceof Long) {
                            watchTimeValue = ((Long) watchTimeObj).floatValue();
                        } else if (watchTimeObj instanceof Double) {
                            watchTimeValue = ((Double) watchTimeObj).floatValue();
                        } else if (watchTimeObj instanceof String) {
                            try {
                                watchTimeValue = Float.parseFloat((String) watchTimeObj);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                        }

                        // Convert for comparison (based on the stored unit)
                        float watchTimeForComparison = watchTimeValue;
                        float goalForComparison = dailyGoalHours;

                        // If watch time is in minutes but goal is in hours, convert goal to minutes
                        if (watchTimeInMinutes) {
                            goalForComparison = dailyGoalHours * 60;
                        }
                        // If watch time is in hours but goal is in hours, no conversion needed

                        // Add date to appropriate decorator
                        CalendarDay date = convertToCalendarDay(dateStr);
                        if (date != null) {
                            Log.d(TAG, "Date: " + dateStr +
                                    ", Watch time: " + watchTimeValue +
                                    (watchTimeInMinutes ? " minutes" : " hours") +
                                    ", Goal: " + goalForComparison +
                                    (watchTimeInMinutes ? " minutes" : " hours") +
                                    ", Completed: " + (watchTimeForComparison >= goalForComparison));

                            if (watchTimeForComparison >= goalForComparison) {
                                greenDecorator.addDate(date);
                            } else {
                                greyDecorator.addDate(date);
                            }
                        }
                    }

                    // Apply decorators
                    calendarView.addDecorator(greenDecorator);
                    calendarView.addDecorator(greyDecorator);
                    calendarView.invalidateDecorators();
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error loading daily records", e);
        });
    }

    private CalendarDay convertToCalendarDay(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date date = sdf.parse(dateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            return CalendarDay.from(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupPomodoroToggle(View view) {
        SwitchCompat pomodoroSwitch = view.findViewById(R.id.pomodoroSwitch);

        // Load current setting
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        boolean pomodoroEnabled = prefs.getBoolean("pomodoroEnabled", true);
        pomodoroSwitch.setChecked(pomodoroEnabled);

        // Set up listener
        pomodoroSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save preference
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("pomodoroEnabled", isChecked);
            editor.apply();

            // Update the PomodoroTimerService about the toggle state change
            updatePomodoroService(isChecked);

            // Show confirmation to user
            String message = isChecked ?
                    "Study timer enabled" :
                    "Study timer disabled";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    // Method to notify the PomodoroTimerService about toggle changes
    private void updatePomodoroService(boolean pomodoroEnabled) {
        // Get current user email or ID
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Log.e(TAG, "Cannot update Pomodoro service: User not logged in");
            return;
        }

        String userEmail = user.getEmail();

        // Get current session name
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String currentSessionName = prefs.getString("selectedTopic", "DefaultSession");

        // Check if video is currently playing (you may need to get this from another part of your app)
        // For now, we'll assume we can access this information from preferences or another source
        boolean isVideoCurrentlyPlaying = prefs.getBoolean("isVideoPlaying", false);

        // Create intent to update service
        Intent pomodoroIntent = new Intent(requireActivity(), PomodoroTimerService.class);
        pomodoroIntent.putExtra("mailId", userEmail);
        pomodoroIntent.putExtra("sessionName", currentSessionName);
        pomodoroIntent.putExtra("isVideoPlaying", isVideoCurrentlyPlaying);
        pomodoroIntent.putExtra("pomodoroToggled", true);  // This signals the service to check the preference

        // Start or update the service
        requireActivity().startService(pomodoroIntent);

        Log.d(TAG, "Updated PomodoroTimerService with new toggle state: " + pomodoroEnabled);
    }
}