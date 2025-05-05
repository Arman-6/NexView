package com.example.nexview.auth;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import com.example.nexview.R;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.nexview.session.CreateSessionActivity;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "GOOGLEAUTH";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private Dialog dialog;
    private ExoPlayer exoPlayer;
    private StyledPlayerView playerView;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, CreateSessionActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeGoogleSignIn();
        initializeUI();
        initializeExoPlayer();
        signUp();
    }

    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        dialog = new Dialog(LoginActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_wait1);
        dialog.setCanceledOnTouchOutside(false);
    }

    private void initializeUI() {
        ImageView signInBtn = findViewById(R.id.oAuth_btn);
        signInBtn.setOnClickListener(v -> signIn());

        Button signIn = findViewById(R.id.signin);
        TextView signUp = findViewById(R.id.signup);
        EditText username = findViewById(R.id.etUsername);
        EditText password = findViewById(R.id.etPassword);

        signIn.setOnClickListener(v -> handleSignIn(username, password));
        signUp.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    private void handleSignIn(EditText username, EditText password) {
        String email = username.getText().toString().trim();
        String pass = password.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(LoginActivity.this, "Email is Empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(LoginActivity.this, "Password is Empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Authentication Successful.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, CreateSessionActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initializeExoPlayer() {
        playerView = findViewById(R.id.playerView);
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(exoPlayer);
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.logovid);
            MediaItem mediaItem = MediaItem.fromUri(videoUri);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();

            exoPlayer.addListener(new com.google.android.exoplayer2.Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == ExoPlayer.STATE_ENDED) {
                        exoPlayer.seekTo(0);
                        exoPlayer.play();
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeExoPlayer(); // Reinitialize the player when returning
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.pause();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            dialog.show();
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
            e.printStackTrace(); // This will log the exact error
            Toast.makeText(this, "Google Sign-In Failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }

    }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Authentication Successful.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, CreateSessionActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                });
    }

    private void signUp() {
        TextView siup = findViewById(R.id.signup);
        siup.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }
}
