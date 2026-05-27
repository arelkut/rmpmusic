package com.musicapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.musicapp.R;
import com.musicapp.databinding.ActivityLoginBinding;
import com.musicapp.model.LoginRequest;
import com.musicapp.model.TokenResponse;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.MainActivity;
import com.musicapp.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Register link
        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Forgot password
        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        // Google (placeholder)
        binding.btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google auth coming soon", Toast.LENGTH_SHORT).show()
        );

        // Facebook (placeholder)
        binding.btnFacebook.setOnClickListener(v ->
                Toast.makeText(this, "Facebook auth coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void attemptLogin() {
        String email    = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

        // Validation
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_fields));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_fields));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        setLoading(true);

        LoginRequest request = new LoginRequest(email, password);
        apiService.login(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse token = response.body();
                    sessionManager.saveSession(
                            token.accessToken,
                            token.refreshToken,
                            token.userId,
                            token.username,
                            token.displayName
                    );
                    navigateToMain();
                } else {
                    String msg = "Неверный email или пароль";
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnLogin.setEnabled(!loading);
        binding.btnLogin.setText(loading ? "Вход..." : getString(R.string.login));
    }
}
