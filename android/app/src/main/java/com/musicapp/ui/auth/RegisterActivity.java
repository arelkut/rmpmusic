package com.musicapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.musicapp.R;
import com.musicapp.databinding.ActivityRegisterBinding;
import com.musicapp.model.RegisterRequest;
import com.musicapp.model.TokenResponse;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.MainActivity;
import com.musicapp.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService     = ApiClient.getApiService(this);
        sessionManager = new SessionManager(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> attemptRegister());

        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Google auth coming soon", Toast.LENGTH_SHORT).show());

        binding.btnFacebook.setOnClickListener(v ->
                Toast.makeText(this, "Facebook auth coming soon", Toast.LENGTH_SHORT).show());
    }

    private void attemptRegister() {
        String name     = getText(binding.etName);
        String email    = getText(binding.etEmail);
        String password = getText(binding.etPassword);
        String confirm  = getText(binding.etConfirmPassword);

        // Validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirm)) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        if (password.length() < 8) {
            binding.tilPassword.setError(getString(R.string.error_password_short));
            return;
        }
        if (!password.equals(confirm)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_passwords_not_match));
            return;
        }
        if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show();
            return;
        }

        clearErrors();
        setLoading(true);

        // Generate username from email
        String username = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "_");

        RegisterRequest request = new RegisterRequest(username, email, password, name);
        apiService.register(request).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    TokenResponse token = response.body();
                    sessionManager.saveSession(
                            token.accessToken, token.refreshToken,
                            token.userId, token.username, token.displayName
                    );
                    navigateToMain();
                } else {
                    String errorMsg = "Ошибка регистрации";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this,
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

    private String getText(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
        binding.tilName.setError(null);
    }

    private void setLoading(boolean loading) {
        binding.btnRegister.setEnabled(!loading);
        binding.btnRegister.setText(loading ? "Регистрация..." : getString(R.string.register));
    }
}
