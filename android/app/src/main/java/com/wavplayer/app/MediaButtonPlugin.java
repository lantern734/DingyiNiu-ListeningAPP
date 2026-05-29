package com.wavplayer.app;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "MediaButtonPlugin")
public class MediaButtonPlugin extends Plugin {

    private static final String TAG = "MediaButtonPlugin";
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean hasAudioFocus = false;
    private boolean isFgRunning = false;

    private void startFg(String title, String album) {
        try {
            Intent i = new Intent(getContext(), MediaPlaybackService.class);
            i.putExtra(MediaPlaybackService.EXTRA_TITLE, title);
            i.putExtra(MediaPlaybackService.EXTRA_ALBUM, album);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getContext().startForegroundService(i);
            else getContext().startService(i);
            isFgRunning = true;
        } catch (Exception ignored) {}
    }

    private void stopFg() {
        if (!isFgRunning) return;
        try { getContext().stopService(new Intent(getContext(), MediaPlaybackService.class)); } catch (Exception ignored) {}
        isFgRunning = false;
    }

    @Override
    public void load() {
        audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        setupMediaSession();
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(getContext(), "WavPlayerSession");

        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                Log.d(TAG, "onPlay");
                sendEvent("play");
            }

            @Override
            public void onPause() {
                Log.d(TAG, "onPause");
                sendEvent("pause");
            }

            @Override
            public void onSkipToNext() {
                Log.d(TAG, "onSkipToNext");
                sendEvent("nexttrack");
            }

            @Override
            public void onSkipToPrevious() {
                Log.d(TAG, "onSkipToPrevious");
                sendEvent("previoustrack");
            }

            @Override
            public void onStop() {
                Log.d(TAG, "onStop");
                sendEvent("stop");
            }

            @Override
            public void onSeekTo(long pos) {
                Log.d(TAG, "onSeekTo: " + pos);
                JSObject data = new JSObject();
                data.put("action", "seekto");
                data.put("position", pos / 1000.0); // ms -> seconds
                notifyListeners("mediaButton", data);
            }

            @Override
            public boolean onMediaButtonEvent(android.content.Intent mediaButtonEvent) {
                // Let the default handler process the key event
                // This handles single/double/triple click logic for headphones
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });
    }

    private void sendEvent(String action) {
        JSObject data = new JSObject();
        data.put("action", action);
        notifyListeners("mediaButton", data);
    }

    @PluginMethod
    public void activate(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
        requestAudioFocus();
        mediaSession.setActive(true);
        updatePlaybackState(true);
        startFg(call.getString("title", ""), call.getString("album", ""));
        Log.d(TAG, "MediaSession activated");
        call.resolve();
    }

    @PluginMethod
    public void deactivate(PluginCall call) {
        updatePlaybackState(false);
        Log.d(TAG, "MediaSession set to paused");
        call.resolve();
    }

    @PluginMethod
    public void updateState(PluginCall call) {
        boolean isPlaying = call.getBoolean("isPlaying", false);
        String title = call.getString("title", "");
        String album = call.getString("album", "");
        long durationMs = (long) (call.getDouble("duration", 0.0) * 1000);
        long positionMs = (long) (call.getDouble("position", 0.0) * 1000);

        // Update metadata
        MediaMetadataCompat.Builder metaBuilder = new MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "WAV Player");
        if (durationMs > 0) {
            metaBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs);
        }
        mediaSession.setMetadata(metaBuilder.build());

        // Update playback state with position
        long actions = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;

        int state = isPlaying
            ? PlaybackStateCompat.STATE_PLAYING
            : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, positionMs, isPlaying ? 1.0f : 0.0f)
            .build();
        mediaSession.setPlaybackState(playbackState);

        if (isPlaying && !hasAudioFocus) {
            requestAudioFocus();
        }
        startFg(title, album);

        call.resolve();
    }

    @PluginMethod
    public void destroy(PluginCall call) {
        stopFg();
        abandonAudioFocus();
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        call.resolve();
    }

    private void updatePlaybackState(boolean isPlaying) {
        if (mediaSession == null) return;

        long actions = PlaybackStateCompat.ACTION_PLAY
            | PlaybackStateCompat.ACTION_PAUSE
            | PlaybackStateCompat.ACTION_PLAY_PAUSE
            | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            | PlaybackStateCompat.ACTION_STOP
            | PlaybackStateCompat.ACTION_SEEK_TO;

        int state = isPlaying
            ? PlaybackStateCompat.STATE_PLAYING
            : PlaybackStateCompat.STATE_PAUSED;

        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, isPlaying ? 1.0f : 0.0f)
            .build();

        mediaSession.setPlaybackState(playbackState);
    }

    private void requestAudioFocus() {
        if (hasAudioFocus) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusChange -> {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            sendEvent("pause");
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Could auto-resume but let the user decide
                            break;
                    }
                })
                .build();
            int result = audioManager.requestAudioFocus(focusRequest);
            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        } else {
            @SuppressWarnings("deprecation")
            int result = audioManager.requestAudioFocus(
                focusChange -> {
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        sendEvent("pause");
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            );
            hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        }
        Log.d(TAG, "Audio focus requested, granted: " + hasAudioFocus);
    }

    private void abandonAudioFocus() {
        if (!hasAudioFocus) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.abandonAudioFocusRequest(focusRequest);
        }
        hasAudioFocus = false;
    }
}
