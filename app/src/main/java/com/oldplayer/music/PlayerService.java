package com.oldplayer.music;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Plays music in the background and keeps playing when the app is closed. */
public class PlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    public interface Listener {
        void onStateChanged();
    }

    public class LocalBinder extends Binder {
        public PlayerService getService() {
            return PlayerService.this;
        }
    }

    public static final int REPEAT_OFF = 0, REPEAT_ALL = 1, REPEAT_ONE = 2;
    private static final int NOTIF_ID = 1;

    private final IBinder binder = new LocalBinder();
    private final Random random = new Random();
    private final Handler handler = new Handler();

    private MediaPlayer player;
    private AudioManager audio;
    private NotificationManager notifications;
    private final List<Song> queue = new ArrayList<Song>();
    private int index = -1;
    private boolean shuffle;
    private int repeat = REPEAT_ALL;
    private boolean prepared;
    private boolean pausedByFocus;
    private boolean noisyRegistered;
    private int sleepMinutes;
    private Listener listener;

    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pause(); // headphones unplugged
        }
    };

    private final Runnable sleepRunnable = new Runnable() {
        @Override
        public void run() {
            sleepMinutes = 0;
            pause();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        notifications = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        player = new MediaPlayer();
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!isPlaying()) stopSelf();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(sleepRunnable);
        unregisterNoisy();
        audio.abandonAudioFocus(this);
        player.release();
        super.onDestroy();
    }

    // ---------- public API used by the UI ----------

    public void setListener(Listener l) {
        listener = l;
    }

    public void setQueue(List<Song> songs, int start) {
        queue.clear();
        queue.addAll(songs);
        if (start >= 0 && start < queue.size()) playIndex(start);
    }

    public Song current() {
        return (index >= 0 && index < queue.size()) ? queue.get(index) : null;
    }

    public boolean isPlaying() {
        try {
            return prepared && player.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public int position() {
        return prepared ? player.getCurrentPosition() : 0;
    }

    public int duration() {
        return prepared ? player.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (prepared) player.seekTo(ms);
    }

    public void toggle() {
        if (current() == null) return;
        if (isPlaying()) pause();
        else start();
    }

    public void next() {
        if (queue.isEmpty()) return;
        int n;
        if (shuffle && queue.size() > 1) {
            do {
                n = random.nextInt(queue.size());
            } while (n == index);
        } else {
            n = (index + 1) % queue.size();
        }
        playIndex(n);
    }

    public void prev() {
        if (queue.isEmpty()) return;
        if (position() > 3000) {
            seekTo(0);
            return;
        }
        playIndex((index - 1 + queue.size()) % queue.size());
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
        fire();
    }

    public int getRepeat() {
        return repeat;
    }

    public void cycleRepeat() {
        repeat = (repeat + 1) % 3;
        fire();
    }

    public int getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepTimer(int minutes) {
        handler.removeCallbacks(sleepRunnable);
        sleepMinutes = minutes;
        if (minutes > 0) handler.postDelayed(sleepRunnable, minutes * 60L * 1000L);
        fire();
    }

    // ---------- playback ----------

    private void playIndex(int i) {
        if (queue.isEmpty()) return;
        index = i;
        Song s = queue.get(index);
        try {
            player.reset();
            player.setDataSource(s.path);
            player.prepare();
            prepared = true;
            Stats.recordPlay(this, s.id);
            start();
        } catch (Exception e) {
            prepared = false;
            Toast.makeText(this, "Can't play: " + s.title, Toast.LENGTH_SHORT).show();
            fire();
        }
    }

    private void start() {
        if (!prepared) return;
        int r = audio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (r != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return;
        player.setVolume(1f, 1f);
        player.start();
        registerNoisy();
        startForeground(NOTIF_ID, buildNotification());
        fire();
    }

    private void pause() {
        if (!prepared) return;
        if (player.isPlaying()) player.pause();
        stopForeground(false);
        notifications.notify(NOTIF_ID, buildNotification());
        fire();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (repeat == REPEAT_ONE) {
            playIndex(index);
            return;
        }
        boolean last = index >= queue.size() - 1;
        if (!shuffle && last && repeat == REPEAT_OFF) {
            player.seekTo(0);
            stopForeground(false);
            notifications.notify(NOTIF_ID, buildNotification());
            fire();
            return;
        }
        next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        prepared = false;
        player.reset();
        Toast.makeText(this, "Playback error", Toast.LENGTH_SHORT).show();
        fire();
        return true;
    }

    @Override
    public void onAudioFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pausedByFocus = false;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (isPlaying()) {
                    pausedByFocus = true;
                    pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (prepared) player.setVolume(0.3f, 0.3f);
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                if (prepared) player.setVolume(1f, 1f);
                if (pausedByFocus) {
                    pausedByFocus = false;
                    start();
                }
                break;
            default:
                break;
        }
    }

    private Notification buildNotification() {
        Song s = current();
        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this)
                .setSmallIcon(isPlaying() ? android.R.drawable.ic_media_play : android.R.drawable.ic_media_pause)
                .setContentTitle(s != null ? s.title : "Music")
                .setContentText(s != null ? s.artist : "")
                .setContentIntent(pi)
                .setOngoing(isPlaying())
                .build();
    }

    private void registerNoisy() {
        if (!noisyRegistered) {
            registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            noisyRegistered = true;
        }
    }

    private void unregisterNoisy() {
        if (noisyRegistered) {
            unregisterReceiver(noisy);
            noisyRegistered = false;
        }
    }

    private void fire() {
        if (listener != null) listener.onStateChanged();
    }
}
