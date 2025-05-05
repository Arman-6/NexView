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
import com.example.nexview.pages.VideoPlayerActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LikedFragment extends Fragment {
    private RecyclerView likeRecyclerView;
    private List<Map<String, Object>> likeVideosList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String sessionName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liked, container, false);

        likeRecyclerView = view.findViewById(R.id.recyclerView);
        likeRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        likeVideosList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Retrieve session name from SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        sessionName = sharedPreferences.getString("selectedTopic", null);

        if (sessionName == null) {
            Toast.makeText(getContext(), "No session found", Toast.LENGTH_SHORT).show();
            return view;
        }

        loadLikedVideos();
        return view;
    }

    private void loadLikedVideos() {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (userEmail != null) {
            userEmail = userEmail.replace(".", ""); // Firestore doesn't allow dots in document IDs
        } else {
            Toast.makeText(getContext(), "User email is null", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchLikedVideos(userEmail, sessionName);
    }

    private void fetchLikedVideos(String userEmail, String sessionName) {
        db.collection("users").document(userEmail).collection("sessions").document(sessionName)
                .collection("likeVideos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        likeVideosList.clear();
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "No liked videos found", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> videoData = document.getData();
                            videoData.put("docId", document.getId());
                            likeVideosList.add(videoData);
                        }
                            likeRecyclerView.setAdapter(new LikeVideosAdapter(getContext(), likeVideosList));
                    } else {
                        Toast.makeText(getContext(), "Error loading liked videos: " + task.getException(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static class LikeVideosAdapter extends RecyclerView.Adapter<LikeVideosAdapter.ViewHolder> {
        private final List<Map<String, Object>> likeVideos;
        private final Context context; // Added context

        public LikeVideosAdapter(Context context, List<Map<String, Object>> likeVideos) {
            this.context = context;
            this.likeVideos = likeVideos;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_liked_video, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> videoData = likeVideos.get(position);

            String title = videoData.get("title") != null ? videoData.get("title").toString() : "Unknown Title";
            String videoId = videoData.get("videoId") != null ? videoData.get("videoId").toString() : "";

            // Generate YouTube thumbnail
            String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            holder.videoTitle.setText(title);
            Glide.with(holder.thumbnail.getContext())
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.logo)
                    .into(holder.thumbnail);

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra("videoId", videoId);
                intent.putExtra("videoTitle", title); // Pass video title as well
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return likeVideos.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView videoTitle;
            ImageView thumbnail;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                videoTitle = itemView.findViewById(R.id.videoTitle);
                thumbnail = itemView.findViewById(R.id.thumbnail);
            }
        }
    }


    private void updateLikedVideos(String videoId, String title) {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (userEmail != null) {
            userEmail = userEmail.replace(".", "");
        } else {
            Toast.makeText(getContext(), "User email is null", Toast.LENGTH_SHORT).show();
            return;
        }

        String finalUserEmail = userEmail;
        String sessionPath = "users/" + finalUserEmail + "/sessions/" + sessionName + "/likeVideos";
        CollectionReference likedVideosRef = db.collection(sessionPath);

        likedVideosRef.whereEqualTo("videoId", videoId).get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                // Update the existing entry's sequence timestamp
                for (QueryDocumentSnapshot doc : querySnapshot) {
                    likedVideosRef.document(doc.getId()).update("sequence", FieldValue.serverTimestamp());
                }
            } else {
                // Add a new record if the video does not exist
                Map<String, Object> newEntry = Map.of(
                        "videoId", videoId,
                        "title", title,
                        "sequence", FieldValue.serverTimestamp()
                );

                likedVideosRef.add(newEntry).addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Liked videos updated", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update liked videos", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
