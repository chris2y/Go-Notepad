package com.example.gonotepad;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.view.animation.ScaleAnimation;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screnn);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Get a reference to the ImageView in your layout
        ImageView imageView = findViewById(R.id.logo); // Replace "imageView" with the ID of your ImageView

        // Create a ScaleAnimation to scale in the ImageView
        Animation scaleAnimation = new ScaleAnimation(0.4f, 1f, 0.4f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(1800);

        // Set the animation to start when the activity is created
        imageView.startAnimation(scaleAnimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Create an Intent to start the next activity
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                startActivity(intent);
                // Finish the current activity to prevent users from returning to it with the back button
                finish();
            }
        }, 2000);
    }

    @Override
    public void onBackPressed() {
        // Disable the back button functionality
    }
}
