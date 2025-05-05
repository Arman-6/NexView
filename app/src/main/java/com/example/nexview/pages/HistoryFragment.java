package com.example.nexview.pages;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.nexview.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {
    private RecyclerView historyRecyclerView;
    private List<Map<String, Object>> historyVideosList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String userTopic;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userTopic = sharedPreferences.getString("selectedTopic", "Technology");

        historyRecyclerView = view.findViewById(R.id.recyclerView);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        historyVideosList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadHistoryVideos();

        return view;
    }

    private void loadHistoryVideos() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (userEmail != null) {
            userEmail = userEmail.replace(".", ""); // Firestore doesn't allow dots in document IDs
        } else {
            Toast.makeText(getContext(), "User email is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalUserEmail = userEmail; // Make email effectively final
        db.collection("users").document(finalUserEmail).collection("sessions")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        List<String> sessionNumbers = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            sessionNumbers.add(document.getId()); // Get session document IDs
                        }


                        if (userTopic != null) {
                            fetchHistoryVideos(finalUserEmail, userTopic);
                        } else {
                            Toast.makeText(getContext(), "No valid session found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No sessions found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fetch watch history from the latest session
    private void fetchHistoryVideos(String userEmail, String sessionNumber) {
        db.collection("users").document(userEmail).collection("sessions").document(sessionNumber)
                .collection("watchHistory")
                .orderBy("sequence", Query.Direction.DESCENDING) // Sort by timestamp in ascending order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        historyVideosList.clear();
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "Nothing to show here", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> videoData = document.getData();
                            videoData.put("docId", document.getId()); // Store document ID if needed
                            historyVideosList.add(videoData);
                        }
                        historyRecyclerView.setAdapter(new HistoryVideosAdapter(historyVideosList));
                    } else {
                        Toast.makeText(getContext(), "Failed to load history videos", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private class HistoryVideosAdapter extends RecyclerView.Adapter<HistoryVideosAdapter.ViewHolder> {
        private final List<Map<String, Object>> historyVideos;

        public HistoryVideosAdapter(List<Map<String, Object>> historyVideos) {
            this.historyVideos = historyVideos;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_liked_video, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> videoData = historyVideos.get(position);

            String title = videoData.get("title") != null ? videoData.get("title").toString() : "Unknown Title";
            String videoId = videoData.get("videoId") != null ? videoData.get("videoId").toString() : "";

            // Generate YouTube thumbnail
            String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            holder.videoTitle.setText(title);
            Glide.with(holder.thumbnail.getContext())
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.logo) // Use a default image if needed
                    .into(holder.thumbnail);

            // Make the video item clickable
            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                updateWatchHistory(videoId, title);
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("videoId", videoId);
                intent.putExtra("videoTitle", title);
                context.startActivity(intent);
            });
        }


        @Override
        public int getItemCount() {
            return historyVideos.size();
        }


        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView videoTitle;
            ImageView thumbnail;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                videoTitle = itemView.findViewById(R.id.videoTitle);
                thumbnail = itemView.findViewById(R.id.thumbnail);
            }
        }
    }
    private void updateWatchHistory(String videoId, String title) {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (userEmail != null) {
            userEmail = userEmail.replace(".", "");
        } else {
            Toast.makeText(getContext(), "User email is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalUserEmail = userEmail;
        String sessionPath = "users/" + finalUserEmail + "/sessions/" + userTopic + "/watchHistory";

        CollectionReference watchHistoryRef = db.collection(sessionPath);

        watchHistoryRef.whereEqualTo("videoId", videoId).get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                // If the video exists, update its timestamp instead of deleting it
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    watchHistoryRef.document(doc.getId()).update("sequence", FieldValue.serverTimestamp());
                }
            } else {
                // If the video does not exist, add it as a new entry
                Map<String, Object> newEntry = Map.of(
                        "videoId", videoId,
                        "title", title,
                        "sequence", FieldValue.serverTimestamp()
                );

                watchHistoryRef.add(newEntry).addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "History updated", Toast.LENGTH_SHORT).show()
                ).addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to update history", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
}
