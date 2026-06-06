package com.musicapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.musicapp.databinding.FragmentHomeBinding;
import com.musicapp.model.AlbumOut;
import com.musicapp.model.PlaylistOut;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.auth.LoginActivity;
import com.musicapp.ui.home.AlbumAdapter;
import com.musicapp.ui.home.PlaylistGridAdapter;
import com.musicapp.ui.home.TrackRecentAdapter;
import com.musicapp.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        if (!sessionManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        // Сбрасываем кэш Retrofit при каждом открытии фрагмента
        ApiClient.reset();
        apiService = ApiClient.getApiService(requireContext());

        String name = sessionManager.getDisplayName();
        if (name != null && !name.isEmpty()) {
            binding.tvWelcome.setText("Привет, " + name + " 👋");
        }

        loadData();
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        loadTrendingAlbums();
        loadRecentTracks();
        loadRecommendedPlaylists();
    }

    private void loadTrendingAlbums() {
        apiService.getTrendingAlbums(10).enqueue(new Callback<List<AlbumOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<AlbumOut>> call,
                                   @NonNull Response<List<AlbumOut>> response) {
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<AlbumOut> albums = response.body();
                    Log.d(TAG, "Trending albums: " + albums.size());
                    AlbumAdapter adapter = new AlbumAdapter(albums, album ->
                            Toast.makeText(getContext(), album.title, Toast.LENGTH_SHORT).show()
                    );
                    binding.rvTrending.setLayoutManager(
                            new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                    binding.rvTrending.setAdapter(adapter);
                } else {
                    Log.e(TAG, "Trending error: " + response.code());
                    if (response.code() == 401) goToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<AlbumOut>> call, @NonNull Throwable t) {
                if (!isAdded() || binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Trending network error: " + t.getMessage());
                Toast.makeText(getContext(), "Нет связи с сервером", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRecentTracks() {
        apiService.getRecentTracks(10).enqueue(new Callback<List<TrackOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<TrackOut>> call,
                                   @NonNull Response<List<TrackOut>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<TrackOut> tracks = response.body();
                    Log.d(TAG, "Recent tracks: " + tracks.size());
                    TrackRecentAdapter adapter = new TrackRecentAdapter(tracks, (track, position) -> {
                        if (requireActivity() instanceof MainActivity) {
                            MainActivity main = (MainActivity) requireActivity();
                            if (main.isServiceBound()) {
                                // Передаём весь список — чтобы работали Next/Previous
                                main.getMusicService().setQueue(tracks, position);
                            }
                        }
                    });
                    binding.rvRecentTracks.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvRecentTracks.setAdapter(adapter);
                } else {
                    Log.e(TAG, "Recent error: " + response.code());
                    if (response.code() == 401) goToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<TrackOut>> call, @NonNull Throwable t) {
                Log.e(TAG, "Recent network error: " + t.getMessage());
            }
        });
    }

    private void loadRecommendedPlaylists() {
        apiService.getRecommendedPlaylists(6).enqueue(new Callback<List<PlaylistOut>>() {
            @Override
            public void onResponse(@NonNull Call<List<PlaylistOut>> call,
                                   @NonNull Response<List<PlaylistOut>> response) {
                if (!isAdded() || binding == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<PlaylistOut> playlists = response.body();
                    Log.d(TAG, "Playlists: " + playlists.size());
                    PlaylistGridAdapter adapter = new PlaylistGridAdapter(playlists, playlist ->
                            Toast.makeText(getContext(), playlist.name, Toast.LENGTH_SHORT).show()
                    );
                    binding.rvRecommendedPlaylists.setLayoutManager(
                            new GridLayoutManager(getContext(), 2));
                    binding.rvRecommendedPlaylists.setAdapter(adapter);
                } else {
                    Log.e(TAG, "Playlists error: " + response.code());
                    if (response.code() == 401) goToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PlaylistOut>> call, @NonNull Throwable t) {
                Log.e(TAG, "Playlists network error: " + t.getMessage());
            }
        });
    }

    private void goToLogin() {
        if (!isAdded()) return;
        sessionManager.clearSession();
        Intent i = new Intent(requireContext(), LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}