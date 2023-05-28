package com.example.trackp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
    private static final String API_URL = "http://192.168.43.195/trackpapi/public/api/";
    private OkHttpClient client;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        client = new OkHttpClient();
        handler = new Handler();

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();

                // Exécution de la tâche asynchrone pour effectuer l'appel réseau
                new LoginTask().execute(username, password);
            }
        });
    }

    private class LoginTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String username = params[0];
            String password = params[1];

            RequestBody formBody = new FormBody.Builder()
                    .add("username", username)
                    .add("password", password)
                    .build();
            Request request = new Request.Builder()
                    .url(API_URL + "login")
                    .post(formBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    String errorJson = response.body().string(); // Obtenir le corps de la réponse d'erreur
                    return errorJson ;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String responseData) {
            if (responseData != null && !responseData.matches("(?i).*error.*")) {
                try {
                    JSONObject json = new JSONObject(responseData);
                    String authToken = json.getJSONObject("user").getString("authToken");

                    // Enregistrement du mot de passe dans le stockage local
                    savePassword(authToken);
                    // Lancement de l'activité de la carte (OpenStreetMap)
                    Toast.makeText(LoginActivity.this, "You have been authenticated successfully.", Toast.LENGTH_SHORT).show();
                    // Utiliser handler.postDelayed() pour démarrer une activité après un délai
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(LoginActivity.this, MapActivity.class));
                            finish();
                        }
                    }, 1500); // Delay of 1.5 seconds (1500 milliseconds)
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Error parsing response", Toast.LENGTH_SHORT).show();
                }
            } else {
                String error = "Error logging in";
                if (responseData != null) {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if (json.has("error")) {
                            error = json.getString("error");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Toast.makeText(LoginActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void savePassword(String authToken) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("authToken", authToken);
        editor.apply();
    }
}
