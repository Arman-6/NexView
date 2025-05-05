package com.example.nexview.session;

public class Session {
    private String topic;
    private String dailyGoal;
    private String timeframe;
    private long sessionId;

    // Empty constructor required for Firestore
    public Session() {}

    public Session(String topic, String dailyGoal, String timeframe) {
        this.topic = topic;
        this.dailyGoal = dailyGoal;
        this.timeframe = timeframe;
    }

    // Getters
    public String getTopic() { return topic; }
    public String getDailyGoal() { return dailyGoal; }
    public String getTimeframe() { return timeframe; }

    // Setters (Required for modifying objects)
    public void setTopic(String topic) { this.topic = topic; }
    public void setDailyGoal(String dailyGoal) { this.dailyGoal = dailyGoal; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
}
