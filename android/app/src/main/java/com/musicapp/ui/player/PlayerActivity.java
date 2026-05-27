package com.musicapp.ui.player;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.musicapp.R;
import com.musicapp.databinding.ActivityPlayerBinding;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.network.ApiService;
import com.musicapp.model.MessageResponse;
import com.musicapp.service.MusicService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerActivity extends AppCompatActivity
        implements MusicService.PlaybackCallback {

    private ActivityPlayerBinding binding;
    private MusicService musicService;
    private boolean serviceBound = false;
    private ApiService apiService;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setCallback(PlayerActivity.this);
            serviceBound = true;
            updateUI();
            handler.post(updateSeekRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = ApiClient.getApiService(this);

        setupControls();
        bindService(new Intent(this, MusicService.class),
                serviceConnection, BIND_AUTO_CREATE);
    }

    private void setupControls() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnPlayPause.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                if (musicService.isPlaying()) musicService.pause();
                else musicService.resume();
                updatePlayPauseButton();
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (serviceBound && musicService != null) musicService.playNext();
        });

        binding.btnPrevious.setOnClickListener(v -> {
            if (serviceBound && musicService != null) musicService.playPrevious();
        });

        binding.btnShuffle.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.toggleShuffle();
                binding.btnShuffle.setAlpha(musicService.isShuffle() ? 1.0f : 0.5f);
            }
        });

        binding.btnRepeat.setOnClickListener(v -> {
            if (serviceBound && musicService != null) {
                musicService.toggleRepeat();
                updateRepeatButton();
            }
        });

        binding.btnLike.setOnClickListener(v -> toggleLike());

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound && musicService != null) {
                    long duration = musicService.getDuration();
                    if (duration > 0) {
                        musicService.seekTo((long) progress * duration / 100);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void updateUI() {
        if (!serviceBound || musicService == null) return;
        TrackOut track = musicService.getCurrentTrack();
        if (track == null) return;

        binding.tvTrackTitle.setText(track.title);
        binding.tvArtistName.setText(track.artistName);

        String coverUrl = track.coverUrl != null
                ? ApiClient.BASE_URL.replaceAll("/$", "") + track.coverUrl : null;

        Glide.with(this)
                .load(coverUrl)
                .placeholder(R.drawable.placeholder_album)
                .error(R.drawable.placeholder_album)
                .into(binding.ivAlbumCover);

        // Like state
        binding.btnLike.setImageResource(
                track.isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart
        );

        updatePlayPauseButton();
        updateRepeatButton();
    }

    private void updateSeekBar() {
        if (!serviceBound || musicService == null) return;
        long pos = musicService.getCurrentPos();
        long dur = musicService.getDuration();

        if (dur > 0) {
            int progress = (int) (pos * 100 / dur);
            binding.seekBar.setProgress(progress);
        }
        binding.tvCurrentTime.setText(formatTime(pos));
        binding.tvTotalTime.setText(formatTime(dur > 0 ? dur : 0));
    }

    private void updatePlayPauseButton() {
        if (serviceBound && musicService != null) {
            binding.btnPlayPause.setImageResource(
                    musicService.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
        }
    }

    private void updateRepeatButton() {
        if (!serviceBound || musicService == null) return;
        switch (musicService.getRepeatMode()) {
            case 1: // REPEAT_MODE_ALL
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setAlpha(1.0f);
                break;
            case 2: // REPEAT_MODE_ONE
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat_one);
                binding.btnRepeat.setAlpha(1.0f);
                break;
            default:
                binding.btnRepeat.setImageResource(R.drawable.ic_repeat);
                binding.btnRepeat.setAlpha(0.5f);
        }
    }

    private void toggleLike() {
        if (!serviceBound || musicService == null) return;
        TrackOut track = musicService.getCurrentTrack();
        if (track == null) return;

        if (track.isLiked) {
            apiService.unlikeTrack(track.trackId).enqueue(new Callback<MessageResponse>() {
                @Override
                public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                    if (response.isSuccessful()) {
                        track.isLiked = false;
                        runOnUiThread(() -> binding.btnLike.setImageResource(R.drawable.ic_heart));
                    }
                }
                @Override
                public void onFailure(Call<MessageResponse> call, Throwable t) {}
            });
        } else {
            apiService.likeTrack(track.trackId).enqueue(new Callback<MessageResponse>() {
                @Override
                public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                    if (response.isSuccessful()) {
                        track.isLiked = true;
                        runOnUiThread(() -> binding.btnLike.setImageResource(R.drawable.ic_heart_filled));
                    }
                }
                @Override
                public void onFailure(Call<MessageResponse> call, Throwable t) {}
            });
        }
    }

    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    // MusicService.PlaybackCallback
    @Override
    public void onTrackChanged(TrackOut track) {
        runOnUiThread(this::updateUI);
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        runOnUiThread(this::updatePlayPauseButton);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateSeekRunnable);
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
        super.onDestroy();
    }
}
