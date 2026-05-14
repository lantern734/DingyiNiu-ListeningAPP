package com.wavplayer.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class MediaPlaybackService extends Service {

    static final String CHANNEL_ID = "wav_playback";
    static final int NID = 1001;
    static final String EXTRA_TITLE = "t";
    static final String EXTRA_ALBUM = "a";

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "音频播放", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String title = intent.getStringExtra(EXTRA_TITLE);
        String album = intent.getStringExtra(EXTRA_ALBUM);

        Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
        PendingIntent pi = launch == null ? null :
            PendingIntent.getActivity(this, 0, launch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title != null && !title.isEmpty() ? title : "正在播放")
            .setContentText(album != null && !album.isEmpty() ? album : "WAV Player")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true).setSilent(true)
            .setContentIntent(pi)
            .build();

        startForeground(NID, n);
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
