package com.oldplayer.music;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class Song {
    public final long id;
    public final long albumId;
    public final long duration;
    public final String title;
    public final String artist;
    public final String album;
    public final String path;
    public final long added;

    public Song(long id, long albumId, long duration, String title, String artist, String album,
                String path, long added) {
        this.id = id;
        this.albumId = albumId;
        this.duration = duration;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.path = path;
        this.added = added;
    }

    /** Reads every music file known to the system media scanner. */
    public static List<Song> loadAll(Context ctx) {
        List<Song> out = new ArrayList<Song>();
        String[] proj = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DATE_ADDED
        };
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                    MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC");
            if (c == null) return out;
            while (c.moveToNext()) {
                String artist = c.getString(2);
                if (artist == null || artist.equals("<unknown>")) artist = "Unknown artist";
                String title = c.getString(1);
                if (title == null) title = "Unknown";
                String album = c.getString(6);
                if (album == null || album.equals("<unknown>")) album = "Unknown album";
                out.add(new Song(c.getLong(0), c.getLong(3), c.getLong(4), title, artist, album,
                        c.getString(5), c.getLong(7)));
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public static String fmt(long ms) {
        long total = ms / 1000;
        long m = total / 60;
        long s = total % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }
}
