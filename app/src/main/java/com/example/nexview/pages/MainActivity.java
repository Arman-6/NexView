package com.example.nexview.pages;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.nexview.R;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SearchFragment searchFragment = new SearchFragment();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.frameLay, searchFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNav);
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String selectedTopic = sharedPreferences.getString("selectedTopic", "No Topic Found");

        String navigateTo = getIntent().getStringExtra("navigateTo");
        if (navigateTo != null) {
            Fragment fragment = null;
            switch (navigateTo) {
                case "ProfileFragment":
                    fragment = new ProfileFragment();
                    bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                    break;

                case "LikedFragment":
                    fragment = new LikedFragment();
                    bottomNavigationView.setSelectedItemId(R.id.nav_like);
                    break;

                case "HistoryFragment":
                    fragment = new HistoryFragment();
                    bottomNavigationView.setSelectedItemId(R.id.nav_history);
                    break;

                default:
                    fragment = new HomeFragment();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }

            if (fragment != null) {
                loadFragment(fragment, false); // 🔥 This will dynamically load the correct fragment
            }
        }


        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    fragment = new HomeFragment();
                    animateNavigationItem(bottomNavigationView, 0);  // Slide & Bounce animation
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                    animateNavigationItem(bottomNavigationView, 1);
                } else if (itemId == R.id.nav_history) {
                    fragment = new HistoryFragment();
                    animateNavigationItem(bottomNavigationView, 2);
                } else if (itemId == R.id.nav_like) {
                    fragment = new LikedFragment();
                    animateNavigationItem(bottomNavigationView, 3);
                }

                if (fragment != null) {
                    loadFragment(fragment, false);
                }
                return true;
            }
        });
        loadFragment(new HomeFragment(), true);
    }


    private void loadFragment(Fragment fragment, boolean isAppInitialized) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(
                R.anim.slide_in_right,  // Enter animation
                R.anim.slide_out_left,  // Exit animation
                R.anim.slide_in_left,   // Pop enter animation (when back pressed)
                R.anim.slide_out_right  // Pop exit animation (when back pressed)
        );

        if (isAppInitialized) {
            fragmentTransaction.add(R.id.frameLay, fragment);
        } else {
            fragmentTransaction.replace(R.id.frameLay, fragment);
        }

        fragmentTransaction.commit();
    }

    private void animateNavigationItem(View view, int position) {
        view.animate()
                .translationY(-20f) // Move up slightly
                .setDuration(150)   // Quick bounce up
                .withEndAction(() ->
                        view.animate()
                                .translationY(0f) // Bounce back to original position
                                .setDuration(150)
                                .start()
                )
                .start();
    }

}