package com.example.trackp;

import android.view.View;
import android.widget.Button;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.trackp.LoginActivity;
import com.example.trackp.MapActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonLogin = findViewById(R.id.buttonLogin);
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if authToken is defined in the local storage
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                String authToken = sharedPreferences.getString("authToken", "");

                // Redirect to the appropriate activity
                if (!authToken.isEmpty()) {
                    startActivity(new Intent(MainActivity.this, MapActivity.class));
                } else {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                }
            }
        });
    }
}
