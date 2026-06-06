package com.musicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.musicapp.R;
import com.musicapp.ui.auth.LoginActivity;
import com.musicapp.ui.auth.RegisterActivity;
import com.musicapp.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Задержка 1.5 секунды, потом проверяем авторизацию
        new Handler().postDelayed(() -> {
            SessionManager sessionManager = new SessionManager(this);
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                // Уже залогинен — сразу на главный экран
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                // Не залогинен — на экран регистрации
                intent = new Intent(SplashActivity.this, RegisterActivity.class);
            }
            startActivity(intent);
            finish();
        }, 1500);
    }
}