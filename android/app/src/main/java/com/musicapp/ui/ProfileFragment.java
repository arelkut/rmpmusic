package com.musicapp.ui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.musicapp.R;
import com.musicapp.databinding.FragmentProfileBinding;
import com.musicapp.model.UserOut;
import com.musicapp.model.UserStatsOut;
import com.musicapp.model.UserUpdateRequest;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.auth.LoginActivity;
import com.musicapp.util.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;
    private UserOut currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
            return;
        }

        apiService = ApiClient.getApiService(requireContext());

        loadUserProfile();
        loadUserStats();

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // Обработчик кнопки выхода
        binding.btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        apiService.getMe().enqueue(new Callback<UserOut>() {
            @Override
            public void onResponse(@NonNull Call<UserOut> call,
                                   @NonNull Response<UserOut> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();

                    binding.tvDisplayName.setText(
                            currentUser.displayName != null ? currentUser.displayName : currentUser.username);
                    binding.tvUsername.setText("@" + currentUser.username);

                    if (currentUser.avatarUrl != null && !currentUser.avatarUrl.isEmpty()) {
                        String avatarUrl = ApiClient.BASE_URL.replaceAll("/$", "") + currentUser.avatarUrl;
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(binding.ivAvatar);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserOut> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.tvDisplayName.setText(sessionManager.getDisplayName());
                binding.tvUsername.setText("@" + sessionManager.getUsername());
            }
        });
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(requireContext(), R.style.Theme_MusicApp_Dialog);
        dialog.setContentView(R.layout.dialog_edit_profile);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextInputEditText etName = dialog.findViewById(R.id.etEditName);
        TextInputEditText etBio  = dialog.findViewById(R.id.etEditBio);
        Button btnCancel = dialog.findViewById(R.id.btnCancelEdit);
        Button btnSave   = dialog.findViewById(R.id.btnSaveEdit);

        // Заполняем текущими данными
        if (currentUser != null) {
            etName.setText(currentUser.displayName != null ? currentUser.displayName : "");
            etBio.setText(currentUser.bio != null ? currentUser.bio : "");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText() != null ? etName.getText().toString().trim() : "";
            String newBio  = etBio.getText()  != null ? etBio.getText().toString().trim()  : "";

            if (newName.isEmpty()) {
                etName.setError("Введите имя");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Сохранение...");

            UserUpdateRequest req = new UserUpdateRequest();
            req.displayName = newName;
            req.bio = newBio;

            apiService.updateMe(req).enqueue(new Callback<UserOut>() {
                @Override
                public void onResponse(@NonNull Call<UserOut> call,
                                       @NonNull Response<UserOut> response) {
                    if (!isAdded() || binding == null) return;
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");
                    if (response.isSuccessful() && response.body() != null) {
                        currentUser = response.body();
                        String name = currentUser.displayName != null
                                ? currentUser.displayName : currentUser.username;
                        binding.tvDisplayName.setText(name);
                        binding.tvUsername.setText("@" + currentUser.username);
                        sessionManager.updateDisplayName(name);
                        Toast.makeText(getContext(), "Профиль обновлён!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<UserOut> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    btnSave.setEnabled(true);
                    btnSave.setText("Сохранить");
                    Toast.makeText(getContext(), "Нет связи", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void loadUserStats() {
        apiService.getMyStats().enqueue(new Callback<UserStatsOut>() {
            @Override
            public void onResponse(@NonNull Call<UserStatsOut> call,
                                   @NonNull Response<UserStatsOut> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserStatsOut stats = response.body();

                    binding.tvListenHours.setText(
                            String.format("%.0fч", stats.totalListenHours));
                    binding.tvTracksPlayed.setText(
                            String.valueOf(stats.totalTracksPlayed));
                    binding.tvFavoriteCount.setText(
                            String.valueOf(stats.favoriteCount));
                    binding.tvTopPercent.setText(
                            String.format("Top %.0f%%", stats.listenerTopPct));
                    binding.tvTrackCount.setText(
                            stats.totalTracksPlayed + " треков");
                    binding.tvPlaylistCount.setText(
                            stats.playlistCount + " плейлистов");

                    if (stats.favoriteGenre != null) {
                        binding.tvFavoriteGenre.setText(stats.favoriteGenre);
                        binding.tvGenrePercent.setText(
                                String.format("%.0f%% прослушиваний", stats.favoriteGenrePct));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserStatsOut> call, @NonNull Throwable t) { }
        });
    }

    /**
     * Выход из аккаунта: очистка сессии и переход на экран входа.
     */
    private void logout() {
        sessionManager.clearSession();  // Используем существующий метод clearSession()
        startActivity(new Intent(requireContext(), LoginActivity.class));
        requireActivity().finish();
        Toast.makeText(requireContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}