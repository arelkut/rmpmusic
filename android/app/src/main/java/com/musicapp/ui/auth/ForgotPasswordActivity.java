package com.musicapp.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.musicapp.R;
import com.musicapp.databinding.ActivityForgotPasswordBinding;
import com.musicapp.model.ForgotPasswordRequest;
import com.musicapp.model.MessageResponse;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSendInstructions.setOnClickListener(v -> attemptForgotPassword());

        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.tvContactSupport.setOnClickListener(v ->
                Toast.makeText(this, "support@musicapp.com", Toast.LENGTH_LONG).show()
        );
    }

    private void attemptForgotPassword() {
        String email = binding.etEmail.getText() != null
                ? binding.etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_fields));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }
        binding.tilEmail.setError(null);
        setLoading(true);

        apiService.forgotPassword(new ForgotPasswordRequest(email))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Инструкции отправлены на " + email,
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Ошибка. Проверьте email.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(ForgotPasswordActivity.this,
                                getString(R.string.error_network), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        binding.btnSendInstructions.setEnabled(!loading);
        binding.btnSendInstructions.setText(
                loading ? "Отправка..." : getString(R.string.send_instructions));
    }
}
