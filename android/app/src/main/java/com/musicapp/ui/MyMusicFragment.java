package com.musicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.musicapp.R;
import com.musicapp.databinding.FragmentMyMusicBinding;
import com.musicapp.model.PlaylistOut;
import com.musicapp.model.TrackOut;
import com.musicapp.model.UserOut;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.auth.LoginActivity;
import com.musicapp.ui.home.PlaylistGridAdapter;
import com.musicapp.ui.home.TrackRecentAdapter;
import com.musicapp.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyMusicFragment extends Fragment {

    private FragmentMyMusicBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyMusicBinding.inflate(inflater, container, false);
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

        loadProfile();
        loadPlaylists();
        loadFavoriteTracks();
    }

    /** Заполняем шапку профиля данными из сессии, затем уточняем с сервера */
    private void loadProfile() {
        // Быстрое заполнение из кэша сессии
        String displayName = sessionManager.getDisplayName();
        String username    = sessionManager.getUsername();
        binding.tvMyMusicDisplayName.setText(
                displayName != null && !displayName.isEmpty() ? displayName : "Моя музыка");
        if (username != null) {
            binding.tvMyMusicUsername.setText("@" + username);
        }

        // Уточняем с сервера (аватар + актуальные данные)
        apiService.getMe().enqueue(new Callback<UserOut>() {
            @Override
            public void onResponse(@NonNull Call<UserOut> call,
                                   @NonNull Response<UserOut> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    UserOut user = response.body();

                    String name = user.displayName != null ? user.displayName : user.username;
                    binding.tvMyMusicDisplayName.setText(name);
                    binding.tvMyMusicUsername.setText("@" + user.username);

                    if (user.avatarUrl != null && !user.avatarUrl.isEmpty()) {
                        String avatarUrl = ApiClient.BASE_URL.replaceAll("/$", "") + user.avatarUrl;
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .placeholder(R.drawable.ic_person)
                                .error(R.drawable.ic_person)
                                .into(binding.ivMyMusicAvatar);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserOut> call, @NonNull Throwable t) {
                // Оставляем данные из сессии
            }
        });
    }

    private void loadPlaylists() {
        apiService.getMyPlaylists().enqueue(new Callback<List<PlaylistOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<PlaylistOut>> call,
                                   @NonNull Response<List<PlaylistOut>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<PlaylistOut> playlists = response.body();
                    PlaylistGridAdapter adapter = new PlaylistGridAdapter(playlists, playlist ->
                            Toast.makeText(getContext(), playlist.name, Toast.LENGTH_SHORT).show()
                    );
                    binding.rvPlaylists.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvPlaylists.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PlaylistOut>> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), "Ошибка загрузки плейлистов", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavoriteTracks() {
        apiService.getFavoriteTracks().enqueue(new Callback<List<TrackOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<TrackOut>> call,
                                   @NonNull Response<List<TrackOut>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<TrackOut> tracks = response.body();
                    binding.tvFavoritesCount.setText(tracks.size() + " треков");

                    TrackRecentAdapter adapter = new TrackRecentAdapter(tracks, (track, position) -> {
                        if (requireActivity() instanceof MainActivity) {
                            MainActivity main = (MainActivity) requireActivity();
                            if (main.isServiceBound()) {
                                main.getMusicService().setQueue(tracks, position);
                            }
                        }
                    });
                    binding.rvRecentlyAdded.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvRecentlyAdded.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TrackOut>> call, @NonNull Throwable t) { }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}