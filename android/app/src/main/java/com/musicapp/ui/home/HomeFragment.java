package com.musicapp.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.musicapp.R;

import com.musicapp.databinding.FragmentHomeBinding;
import com.musicapp.model.AlbumOut;
import com.musicapp.model.PlaylistOut;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.ui.MainActivity;
import com.musicapp.ui.player.PlayerActivity;
import com.musicapp.util.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService     = ApiClient.getApiService(requireContext());
        sessionManager = new SessionManager(requireContext());

        String displayName = sessionManager.getDisplayName();
        binding.tvWelcome.setText(displayName != null && !displayName.isEmpty()
                ? displayName : getString(R.string.welcome));

        setupRecyclerViews();
        loadData();
    }

    private void setupRecyclerViews() {
        // Trending albums — horizontal
        binding.rvTrending.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        // Recent tracks — vertical
        binding.rvRecentTracks.setLayoutManager(
                new LinearLayoutManager(getContext()));

        // Recommended playlists — 2 columns grid
        binding.rvRecommendedPlaylists.setLayoutManager(
                new GridLayoutManager(getContext(), 2));
    }

    private void loadData() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // Load trending albums
        apiService.getTrendingAlbums(6).enqueue(new Callback<List<AlbumOut>>() {
            @Override
            public void onResponse(Call<List<AlbumOut>> call, Response<List<AlbumOut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AlbumAdapter adapter = new AlbumAdapter(response.body(), album -> {
                        // TODO: Open album detail
                    });
                    binding.rvTrending.setAdapter(adapter);
                }
                binding.progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(Call<List<AlbumOut>> call, Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });

        // Load recently played tracks
        apiService.getRecentTracks(10).enqueue(new Callback<List<TrackOut>>() {
            @Override
            public void onResponse(Call<List<TrackOut>> call, Response<List<TrackOut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<TrackOut> tracks = response.body();
                    TrackRecentAdapter adapter = new TrackRecentAdapter(tracks, track -> {
                        playTrack(track, tracks);
                    });
                    binding.rvRecentTracks.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<TrackOut>> call, Throwable t) {}
        });

        // Load recommended playlists
        apiService.getRecommendedPlaylists(4).enqueue(new Callback<List<PlaylistOut>>() {
            @Override
            public void onResponse(Call<List<PlaylistOut>> call, Response<List<PlaylistOut>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PlaylistGridAdapter adapter = new PlaylistGridAdapter(
                            response.body(), playlist -> {
                        // TODO: Open playlist detail
                    });
                    binding.rvRecommendedPlaylists.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<PlaylistOut>> call, Throwable t) {}
        });
    }

    private void playTrack(TrackOut track, List<TrackOut> queue) {
        if (getActivity() instanceof MainActivity) {
            MainActivity main = (MainActivity) getActivity();
            if (main.isServiceBound() && main.getMusicService() != null) {
                main.getMusicService().setQueue(queue, queue.indexOf(track));
            }
        }
        // Open player
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
