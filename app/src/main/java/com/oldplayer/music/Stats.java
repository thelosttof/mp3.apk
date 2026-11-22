package com.oldplayer.music;

import android.content.Context;
import android.content.SharedPreferences;

/** Remembers how often and when each song was played (for "Top Played" / "Recently Played"). */
public class Stats {
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("stats", Context.MODE_PRIVATE);
    }

    public static void recordPlay(Context c, long id) {
        SharedPreferences sp = prefs(c);
        sp.edit()
                .putInt("pc_" + id, sp.getInt("pc_" + id, 0) + 1)
                .putLong("lp_" + id, System.currentTimeMillis())
                .apply();
    }

    public static int plays(Context c, long id) {
        return prefs(c).getInt("pc_" + id, 0);
    }

    public static long lastPlayed(Context c, long id) {
        return prefs(c).getLong("lp_" + id, 0L);
    }
}
