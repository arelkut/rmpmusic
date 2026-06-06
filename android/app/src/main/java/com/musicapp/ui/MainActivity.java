package com.musicapp.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.bumptech.glide.Glide;
import com.musicapp.R;
import com.musicapp.databinding.ActivityMainBinding;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.service.MusicService;
import com.musicapp.ui.player.PlayerActivity;

public class MainActivity extends AppCompatActivity
        implements MusicService.PlaybackCallback {

    private ActivityMainBinding binding;
    private NavController navController;

    private MusicService musicService;
    private boolean serviceBound = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(MainActivity.this);
            serviceBound = true;
            updateMiniPlayer();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupMiniPlayer();
        bindMusicService();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        }
    }

    private void setupMiniPlayer() {
        binding.miniPlayer.miniPlayerContainer.setOnClickListener(v -> {
            startActivity(new Intent(this, PlayerActivity.class));
        });

        binding.miniPlayer.miniPlayPause.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                if (musicService.isPlaying()) musicService.pause();
                else musicService.resume();
            }
        });
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);   // сервис живёт независимо от привязки
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void updateMiniPlayer() {
        if (!serviceBound || musicService == null) return;
        TrackOut current = musicService.getCurrentTrack();
        if (current != null) {
            binding.miniPlayer.getRoot().setVisibility(View.VISIBLE);
            binding.miniPlayer.miniTrackTitle.setText(current.title);
            binding.miniPlayer.miniArtistName.setText(current.artistName);
            binding.miniPlayer.miniPlayPause.setImageResource(
                    musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
            if (current.coverUrl != null) {
                Glide.with(this)
                        .load(ApiClient.BASE_URL.replaceAll("/$", "") + current.coverUrl)
                        .placeholder(R.drawable.placeholder_album)
                        .into(binding.miniPlayer.miniCover);
            }
        } else {
            binding.miniPlayer.getRoot().setVisibility(View.GONE);
        }
    }

    // MusicService.PlaybackCallback
    @Override
    public void onTrackChanged(TrackOut track) {
        runOnUiThread(this::updateMiniPlayer);
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            binding.miniPlayer.miniPlayPause.setImageResource(
                    isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
            );
        });
    }

    public MusicService getMusicService() { return musicService; }
    public boolean isServiceBound() { return serviceBound; }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
