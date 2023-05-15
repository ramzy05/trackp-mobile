package com.example.trackp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        client = new OkHttpClient();

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();

                // Appel à l'API de connexion
                login(username, password);
            }
        });
    }

    private void login(String username, String password) {
        RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
//https://trackp-server.000webhostapp.com/api/login, http://127.0.0.1:8000/api/login
        Request request = new Request.Builder()
                .url("https://trackp-server.000webhostapp.com/api/login")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                System.out.println(e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "Error logging in", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            // Declare a Handler
            private Handler handler = new Handler();
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            String responseData = null;
                            try {
                                responseData = response.body().string();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            try {
                                JSONObject json = new JSONObject(responseData);
                                String authToken = json.getJSONObject("user").getString("authToken");

                                // Enregistrement du mot de passe dans le stockage local
                                savePassword(authToken);
                                // Lancement de l'activité de la carte (OpenStreetMap)
                                Toast.makeText(LoginActivity.this, "You have been authenticated successfully: ", Toast.LENGTH_SHORT).show();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startActivity(new Intent(LoginActivity.this, MapActivity.class));
                                        finish();
                                    }
                                }, 2000); // Delay of 1.5 seconds (1500 milliseconds)
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String error = "Login failed";
                            try {
                                String responseData = response.body().string();
                                JSONObject json = new JSONObject(responseData);
                                if (json.has("error")) {
                                    error = json.getString("error");
                                }
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        });
    }

    private void savePassword(String authToken) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("authToken", authToken);
        editor.apply();
    }
}

