package com.musicapp.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.musicapp.R;
import com.musicapp.model.TrackOut;
import com.musicapp.network.ApiClient;
import com.musicapp.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreground service for music playback using ExoPlayer (Media3).
 * Controls: play, pause, next, previous, shuffle, repeat.
 */
public class MusicService extends Service {

    private static final String CHANNEL_ID  = "MusicPlaybackChannel";
    private static final int    NOTIF_ID    = 101;

    // Actions
    public static final String ACTION_PLAY     = "com.musicapp.PLAY";
    public static final String ACTION_PAUSE    = "com.musicapp.PAUSE";
    public static final String ACTION_NEXT     = "com.musicapp.NEXT";
    public static final String ACTION_PREV     = "com.musicapp.PREV";
    public static final String ACTION_STOP     = "com.musicapp.STOP";

    private ExoPlayer player;
    private final IBinder binder = new MusicBinder();

    private List<TrackOut> queue = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShuffle = false;
    private int repeatMode = Player.REPEAT_MODE_OFF;

    private PlaybackCallback callback;

    // ─── Binder ────────────────────────────────────────────────
    public class MusicBinder extends Binder {
        public MusicService getService() { return MusicService.this; }
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    // ─── Lifecycle ─────────────────────────────────────────────
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        player = new ExoPlayer.Builder(this).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    playNext();
                }
                if (callback != null) callback.onStateChanged(player.isPlaying());
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updateNotification();
                if (callback != null) callback.onStateChanged(isPlaying);
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_PLAY:  resume(); break;
                case ACTION_PAUSE: pause();  break;
                case ACTION_NEXT:  playNext(); break;
                case ACTION_PREV:  playPrevious(); break;
                case ACTION_STOP:  stopSelf(); break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    // ─── Public Controls ───────────────────────────────────────

    public void setQueue(List<TrackOut> tracks, int startIndex) {
        queue.clear();
        queue.addAll(tracks);
        currentIndex = startIndex;
        playCurrentTrack();
    }

    public void playTrack(TrackOut track) {
        queue.clear();
        queue.add(track);
        currentIndex = 0;
        playCurrentTrack();
    }

    public void resume() {
        if (player != null) player.play();
    }

    public void pause() {
        if (player != null) player.pause();
    }

    public void playNext() {
        if (queue.isEmpty()) return;
        if (isShuffle) {
            currentIndex = (int) (Math.random() * queue.size());
        } else {
            if (repeatMode == Player.REPEAT_MODE_ONE) {
                playCurrentTrack();
                return;
            }
            currentIndex = (currentIndex + 1) % queue.size();
        }
        playCurrentTrack();
    }

    public void playPrevious() {
        if (queue.isEmpty()) return;
        if (player != null && player.getCurrentPosition() > 3000) {
            player.seekTo(0);
            return;
        }
        currentIndex = (currentIndex - 1 + queue.size()) % queue.size();
        playCurrentTrack();
    }

    public void seekTo(long positionMs) {
        if (player != null) player.seekTo(positionMs);
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
    }

    public void toggleRepeat() {
        switch (repeatMode) {
            case Player.REPEAT_MODE_OFF: repeatMode = Player.REPEAT_MODE_ALL; break;
            case Player.REPEAT_MODE_ALL: repeatMode = Player.REPEAT_MODE_ONE; break;
            default: repeatMode = Player.REPEAT_MODE_OFF; break;
        }
    }

    public boolean isPlaying()     { return player != null && player.isPlaying(); }
    public boolean isShuffle()     { return isShuffle; }
    public int getRepeatMode()     { return repeatMode; }
    public long getCurrentPos()    { return player != null ? player.getCurrentPosition() : 0; }
    public long getDuration()      { return player != null ? player.getDuration() : 0; }
    public TrackOut getCurrentTrack() {
        if (!queue.isEmpty() && currentIndex < queue.size()) return queue.get(currentIndex);
        return null;
    }

    public void setCallback(PlaybackCallback callback) {
        this.callback = callback;
    }

    // ─── Private ───────────────────────────────────────────────

    private void playCurrentTrack() {
        if (queue.isEmpty() || currentIndex >= queue.size()) return;
        TrackOut track = queue.get(currentIndex);

        String url = track.getFullFileUrl(ApiClient.BASE_URL);
        MediaItem mediaItem = MediaItem.fromUri(url);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        startForeground(NOTIF_ID, buildNotification(track));
        if (callback != null) callback.onTrackChanged(track);
    }

    private Notification buildNotification(TrackOut track) {
        Intent playerIntent = new Intent(this, PlayerActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, playerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, MusicService.class);
        pauseIntent.setAction(isPlaying() ? ACTION_PAUSE : ACTION_PLAY);
        PendingIntent pausePi = PendingIntent.getService(this, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPi = PendingIntent.getService(this, 1, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(track.title)
                .setContentText(track.artistName)
                .setContentIntent(pi)
                .addAction(R.drawable.ic_skip_previous, "Prev",
                        PendingIntent.getService(this, 2,
                                new Intent(this, MusicService.class).setAction(ACTION_PREV),
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .addAction(isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play,
                        isPlaying() ? "Pause" : "Play", pausePi)
                .addAction(R.drawable.ic_skip_next, "Next", nextPi)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .setOngoing(isPlaying())
                .build();
    }

    private void updateNotification() {
        TrackOut current = getCurrentTrack();
        if (current != null) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIF_ID, buildNotification(current));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music App playback controls");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    // ─── Callback Interface ────────────────────────────────────
    public interface PlaybackCallback {
        void onTrackChanged(TrackOut track);
        void onStateChanged(boolean isPlaying);
    }

    private class MediaStyle {
        public NotificationCompat.Style setShowActionsInCompactView(int i, int i1, int i2) {
            return null;
        }
    }
}
