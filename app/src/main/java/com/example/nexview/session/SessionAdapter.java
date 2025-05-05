package com.example.nexview.session;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.nexview.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import com.example.nexview.pages.MainActivity;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private List<Session> sessionList;
    private String userId;
    private Context context;

    public SessionAdapter(Context context, List<Session> sessionList) {
        this.context = context;
        this.sessionList = sessionList;
        this.userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getEmail().replace(".", "") : null;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_item, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        Session session = sessionList.get(position);
        if (session == null) {
            Log.e("SessionAdapter", "Error: session at position " + position + " is null!");
            return;
        }

        holder.topicTextView.setText(session.getTopic());
        holder.dailyGoalTextView.setText(session.getDailyGoal());
        holder.timeframeTextView.setText(session.getTimeframe() + " months");

        holder.itemView.setOnClickListener(v -> {
            saveSelectedTopic(session.getTopic());
            Context activityContext = v.getContext();
            Intent intent = new Intent(activityContext, MainActivity.class);
            activityContext.startActivity(intent);
        });

        // Show confirmation dialog before deleting
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteConfirmationDialog(session, position, holder);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return sessionList.size();
    }

    private void showDeleteConfirmationDialog(Session session, int position, RecyclerView.ViewHolder holder) {
        new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle("Delete Session")
                .setMessage("Do you really want to delete this session?")
                .setIcon(R.drawable.failure)
                .setPositiveButton("Yes", (dialog, which) -> deleteSession(session, position, holder))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteSession(Session session, int position, RecyclerView.ViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String topicName = session.getTopic();

        db.collection("users").document(userId).collection("sessions").document(topicName)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    sessionList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(holder.itemView.getContext(), "Session deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(holder.itemView.getContext(), "Error deleting session: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveSelectedTopic(String topic) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("selectedTopic", topic);
        editor.apply();
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        TextView topicTextView, dailyGoalTextView, timeframeTextView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            topicTextView = itemView.findViewById(R.id.sessionTopic);
            dailyGoalTextView = itemView.findViewById(R.id.sessionDailyGoal);
            timeframeTextView = itemView.findViewById(R.id.sessionTimeframe);
        }
    }
}
