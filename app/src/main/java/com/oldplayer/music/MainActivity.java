package com.oldplayer.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    private static final int ACCENT = 0xFF2DD4BF;
    private static final int NAV_OFF = 0xFF7C8598;
    private static final int ART_SIZE = 600;
    private static final int ART_RADIUS = 38;

    private static final int TAB_HOME = 0, TAB_SEARCH = 1, TAB_LIBRARY = 2;
    private static final int LIB_SONGS = 0, LIB_ALBUMS = 1, LIB_ARTISTS = 2;

    // home tiles
    private static final int K_FAV = 0, K_TOP = 1, K_ADDED = 2, K_RECENT = 3,
            K_ALL = 4, K_ALBUMS = 5, K_ARTISTS = 6, K_SHUFFLE = 7;
    private static final String[] TILE_NAMES = {
            "Favorites", "Top Played", "Recently Added", "Recently Played",
            "All Songs", "Albums", "Artists", "Shuffle All"};
    private static final int[] TILE_ICONS = {
            IconView.HEART, IconView.TREND, IconView.STAR, IconView.CLOCK,
            IconView.NOTE, IconView.DISC, IconView.PERSON, IconView.SHUFFLE};
    private static final int[] TILE_ACCENT = {
            0xFFFF5C7A, 0xFF2DD4BF, 0xFFFBBF24, 0xFF60A5FA,
            0xFFA78BFA, 0xFFFB923C, 0xFF4ADE80, 0xFFF472B6};
    private static final int[] TILE_BG1 = {
            0xFF3D1C2C, 0xFF103B38, 0xFF3D3013, 0xFF1B2B4D,
            0xFF2B2050, 0xFF3D2513, 0xFF123B24, 0xFF3D1A3A};
    private static final int[] TILE_BG2 = {
            0xFF1D1019, 0xFF0D1F22, 0xFF1D1810, 0xFF111A2D,
            0xFF181230, 0xFF20150E, 0xFF0E2016, 0xFF1F0F20};

    /** An album or an artist with its songs. */
    private static class Group {
        String name;
        String sub;
        long albumId;
        final List<Song> songs = new ArrayList<Song>();
    }

    // player page
    private FrameLayout root;
    private ArtView art;
    private TextView title, artist, timeNow, timeTotal;
    private SeekBar seek;
    private IconView btnShuffle, btnMoon, btnMore, btnRepeat, btnPrev, btnPlay, btnNext, btnHeart, btnUp;

    // home shell
    private View libraryPage, pageHome, pageList;
    private TextView greeting, homeSub, listTitle, listCount, emptyView;
    private LinearLayout tiles, chips;
    private final View[] tileViews = new View[8];
    private final TextView[] tileCounts = new TextView[8];
    private IconView btnBack;
    private TextView chipSongs, chipAlbums, chipArtists;
    private EditText search;
    private ListView list;
    private View miniWrap, miniBar, miniProgress;
    private ImageView miniArt;
    private TextView miniTitle, miniArtist;
    private IconView miniPlay, miniNext;
    private final IconView[] navIcons = new IconView[3];
    private final TextView[] navLabels = new TextView[3];

    private final List<Song> all = new ArrayList<Song>();
    private final List<Song> shown = new ArrayList<Song>();
    private final List<Group> groups = new ArrayList<Group>();
    private SongAdapter songAdapter;
    private GroupAdapter groupAdapter;
    private boolean groupMode;
    private boolean showPlays;
    private String emptyMsg = "";

    private int tab = TAB_HOME;
    private int libMode = LIB_SONGS;
    private boolean inDetail;

    private PlayerService svc;
    private final Handler handler = new Handler();
    private boolean dragging;
    private long seekHoldUntil;
    private boolean libraryShown = true;
    private boolean miniVisible;
    private long shownArtSongId = -1;
    private long shownMiniSongId = -1;
    private float density;

    private SharedPreferences prefs;
    private final Set<String> favs = new HashSet<String>();

    // ------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        density = getResources().getDisplayMetrics().density;

        prefs = getSharedPreferences("music", MODE_PRIVATE);
        favs.addAll(prefs.getStringSet("fav", new HashSet<String>()));

        root = (FrameLayout) findViewById(R.id.root);
        art = (ArtView) findViewById(R.id.art);
        title = (TextView) findViewById(R.id.now_title);
        artist = (TextView) findViewById(R.id.now_artist);
        timeNow = (TextView) findViewById(R.id.time_now);
        timeTotal = (TextView) findViewById(R.id.time_total);
        seek = (SeekBar) findViewById(R.id.seek);
        btnShuffle = (IconView) findViewById(R.id.btn_shuffle);
        btnMoon = (IconView) findViewById(R.id.btn_moon);
        btnMore = (IconView) findViewById(R.id.btn_more);
        btnRepeat = (IconView) findViewById(R.id.btn_repeat);
        btnPrev = (IconView) findViewById(R.id.btn_prev);
        btnPlay = (IconView) findViewById(R.id.btn_play);
        btnNext = (IconView) findViewById(R.id.btn_next);
        btnHeart = (IconView) findViewById(R.id.btn_heart);
        btnUp = (IconView) findViewById(R.id.btn_up);

        libraryPage = findViewById(R.id.library_page);
        pageHome = findViewById(R.id.page_home);
        pageList = findViewById(R.id.page_list);
        greeting = (TextView) findViewById(R.id.greeting);
        homeSub = (TextView) findViewById(R.id.home_sub);
        tiles = (LinearLayout) findViewById(R.id.tiles);
        btnBack = (IconView) findViewById(R.id.btn_back);
        listTitle = (TextView) findViewById(R.id.list_title);
        listCount = (TextView) findViewById(R.id.list_count);
        chips = (LinearLayout) findViewById(R.id.chips);
        chipSongs = (TextView) findViewById(R.id.chip_songs);
        chipAlbums = (TextView) findViewById(R.id.chip_albums);
        chipArtists = (TextView) findViewById(R.id.chip_artists);
        search = (EditText) findViewById(R.id.search);
        list = (ListView) findViewById(R.id.list);
        emptyView = (TextView) findViewById(R.id.empty);
        miniWrap = findViewById(R.id.mini_wrap);
        miniBar = findViewById(R.id.mini_bar);
        miniProgress = findViewById(R.id.mini_progress);
        miniArt = (ImageView) findViewById(R.id.mini_art);
        miniTitle = (TextView) findViewById(R.id.mini_title);
        miniArtist = (TextView) findViewById(R.id.mini_artist);
        miniPlay = (IconView) findViewById(R.id.mini_play);
        miniNext = (IconView) findViewById(R.id.mini_next);
        navIcons[TAB_HOME] = (IconView) findViewById(R.id.ic_home);
        navIcons[TAB_SEARCH] = (IconView) findViewById(R.id.ic_search);
        navIcons[TAB_LIBRARY] = (IconView) findViewById(R.id.ic_library);
        navLabels[TAB_HOME] = (TextView) findViewById(R.id.lbl_home);
        navLabels[TAB_SEARCH] = (TextView) findViewById(R.id.lbl_search);
        navLabels[TAB_LIBRARY] = (TextView) findViewById(R.id.lbl_library);

        btnShuffle.setIcon(IconView.SHUFFLE);
        btnShuffle.setCircle(true);
        btnShuffle.setActive(false);
        btnMoon.setIcon(IconView.MOON);
        btnMoon.setCircle(true);
        btnMore.setIcon(IconView.MORE);
        btnMore.setCircle(true);
        btnRepeat.setIcon(IconView.REPEAT);
        btnPrev.setIcon(IconView.PREV);
        btnPlay.setIcon(IconView.PLAY);
        btnNext.setIcon(IconView.NEXT);
        btnHeart.setIcon(IconView.HEART);
        btnUp.setIcon(IconView.UP);

        btnBack.setIcon(IconView.BACK);
        miniPlay.setIcon(IconView.PLAY);
        miniNext.setIcon(IconView.NEXT);
        navIcons[TAB_HOME].setIcon(IconView.HOME);
        navIcons[TAB_SEARCH].setIcon(IconView.SEARCH);
        navIcons[TAB_LIBRARY].setIcon(IconView.LIBRARY);
        for (IconView n : navIcons) n.setClickable(false);

        applyTint(null);
        setupClicks();

        // library data
        all.addAll(Song.loadAll(this));
        songAdapter = new SongAdapter();
        groupAdapter = new GroupAdapter();
        buildTiles();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (groupMode) {
                    if (position < groups.size()) {
                        Group g = groups.get(position);
                        openDetail(g.name, g.songs, false, "Nothing here");
                    }
                    return;
                }
                if (svc == null) {
                    Toast.makeText(MainActivity.this, "Starting player...", Toast.LENGTH_SHORT).show();
                    return;
                }
                svc.setQueue(shown, position);
                showLibrary(false, true);
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        showTab(TAB_HOME);

        // service
        Intent intent = new Intent(this, PlayerService.class);
        startService(intent);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    private int dp(float v) {
        return (int) (v * density + 0.5f);
    }

    /** A fresh, delay-free animator (ViewPropertyAnimator remembers the previous start delay). */
    private ViewPropertyAnimator anim(View v) {
        return v.animate().setStartDelay(0).setListener(null);
    }

    private void setupClicks() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc == null) return;
                if (svc.current() == null) showLibrary(true, true);
                else svc.toggle();
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.next();
            }
        });
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.prev();
            }
        });
        btnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.toggleShuffle();
            }
        });
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.cycleRepeat();
            }
        });
        btnMoon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc == null) return;
                int cur = svc.getSleepMinutes();
                int next = cur == 0 ? 15 : cur == 15 ? 30 : cur == 30 ? 60 : 0;
                svc.setSleepTimer(next);
                Toast.makeText(MainActivity.this,
                        next == 0 ? "Sleep timer off" : "Music will stop in " + next + " min",
                        Toast.LENGTH_SHORT).show();
            }
        });
        btnHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Song s = svc == null ? null : svc.current();
                if (s == null) return;
                String id = String.valueOf(s.id);
                if (!favs.remove(id)) favs.add(id);
                prefs.edit().putStringSet("fav", new HashSet<String>(favs)).apply();
                btnHeart.setFilled(favs.contains(id));
                refreshHome();
                // little "pop" on the heart
                btnHeart.setScaleX(0.7f);
                btnHeart.setScaleY(0.7f);
                anim(btnHeart).scaleX(1f).scaleY(1f).setDuration(220).start();
            }
        });
        View.OnClickListener openLibrary = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLibrary(true, true);
            }
        };
        btnUp.setOnClickListener(openLibrary);
        btnMore.setOnClickListener(openLibrary);

        // mini player
        miniBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLibrary(false, true);
            }
        });
        miniPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.toggle();
            }
        });
        miniNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svc != null) svc.next();
            }
        });

        // bottom navigation
        findViewById(R.id.nav_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(TAB_HOME);
            }
        });
        findViewById(R.id.nav_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(TAB_SEARCH);
            }
        });
        findViewById(R.id.nav_library).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(TAB_LIBRARY);
            }
        });

        // list page
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTab(tab);
            }
        });
        chipSongs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                libMode = LIB_SONGS;
                applyLibMode();
            }
        });
        chipAlbums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                libMode = LIB_ALBUMS;
                applyLibMode();
            }
        });
        chipArtists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                libMode = LIB_ARTISTS;
                applyLibMode();
            }
        });

        // seek bar
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) timeNow.setText(Song.fmt(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                dragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                if (svc != null) svc.seekTo(sb.getProgress());
                // MediaPlayer.seekTo is asynchronous: don't let the ticker snap the bar back
                seekHoldUntil = SystemClock.uptimeMillis() + 800;
                dragging = false;
            }
        });
    }

    // ------------------------------------------------------------------ service

    private final PlayerService.Listener serviceListener = new PlayerService.Listener() {
        @Override
        public void onStateChanged() {
            refreshUi();
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            svc = ((PlayerService.LocalBinder) binder).getService();
            svc.setListener(serviceListener);
            refreshUi();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            svc = null;
        }
    };

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (svc != null && svc.current() != null) {
                int d = svc.duration();
                int p = svc.position();
                if (d != seek.getMax()) {
                    seek.setMax(d);
                    timeTotal.setText(Song.fmt(d));
                }
                if (!dragging && SystemClock.uptimeMillis() > seekHoldUntil) {
                    seek.setProgress(p);
                    timeNow.setText(Song.fmt(p));
                }
                if (miniVisible && d > 0) {
                    int max = miniWrap.getWidth() - dp(48);
                    ViewGroup.LayoutParams lp = miniProgress.getLayoutParams();
                    int w = (int) ((long) max * p / d);
                    if (max > 0 && lp.width != w) {
                        lp.width = w;
                        miniProgress.setLayoutParams(lp);
                    }
                }
            }
            handler.postDelayed(this, 300);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(ticker);
        refreshGreeting();
        refreshUi();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (svc != null) svc.setListener(null);
        unbindService(connection);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!libraryShown) {
            showLibrary(true, true);
        } else if (inDetail) {
            showTab(tab);
        } else if (tab != TAB_HOME) {
            showTab(TAB_HOME);
        } else {
            super.onBackPressed();
        }
    }

    // ------------------------------------------------------------------ player UI

    private void refreshUi() {
        if (svc == null) return;
        Song s = svc.current();
        if (s == null) {
            title.setText("Nothing playing");
            artist.setText("Open the library to pick a song");
            btnPlay.setIcon(IconView.PLAY);
            setMiniVisible(false);
        } else {
            title.setText(s.title);
            title.setSelected(true);
            artist.setText(s.artist);
            if (s.id != shownArtSongId) {
                shownArtSongId = s.id;
                art.setAlpha(0f);
                anim(art).alpha(1f).setDuration(350).start();
                ArtLoader.load(this, s.albumId, art, ART_SIZE, ART_RADIUS, new ArtLoader.Callback() {
                    @Override
                    public void onArt(Bitmap bitmap, boolean real) {
                        applyTint(real ? bitmap : null);
                    }
                });
            }
            boolean playing = svc.isPlaying();
            btnPlay.setIcon(playing ? IconView.PAUSE : IconView.PLAY);
            btnHeart.setFilled(favs.contains(String.valueOf(s.id)));
            int dur = svc.duration();
            if (dur != seek.getMax()) seek.setMax(dur);
            timeTotal.setText(Song.fmt(dur));

            // mini player
            miniTitle.setText(s.title);
            miniArtist.setText(s.artist);
            miniPlay.setIcon(playing ? IconView.PAUSE : IconView.PLAY);
            if (s.id != shownMiniSongId) {
                shownMiniSongId = s.id;
                ArtLoader.load(this, s.albumId, miniArt, dp(44), dp(8), null);
            }
            setMiniVisible(true);
        }
        btnShuffle.setActive(svc.isShuffle());
        btnRepeat.setActive(svc.getRepeat() != PlayerService.REPEAT_OFF);
        btnRepeat.setRepeatOne(svc.getRepeat() == PlayerService.REPEAT_ONE);
        btnMoon.setColorOverride(svc.getSleepMinutes() > 0 ? ACCENT : 0);
        songAdapter.notifyDataSetChanged();
        groupAdapter.notifyDataSetChanged();
        refreshHome();
    }

    private void setMiniVisible(boolean show) {
        if (show == miniVisible) return;
        miniVisible = show;
        if (show) {
            miniWrap.setVisibility(View.VISIBLE);
            miniWrap.setAlpha(0f);
            miniWrap.setTranslationY(dp(30));
            anim(miniWrap).alpha(1f).translationY(0).setDuration(260).start();
        } else {
            miniWrap.setVisibility(View.GONE);
        }
    }

    /** Colours the page background from the album cover (or a default navy). */
    private void applyTint(Bitmap cover) {
        int top = 0xFF18344D;
        if (cover != null) {
            try {
                Bitmap mid = Bitmap.createBitmap(cover, cover.getWidth() / 4, cover.getHeight() / 4,
                        cover.getWidth() / 2, cover.getHeight() / 2);
                Bitmap px = Bitmap.createScaledBitmap(mid, 1, 1, true);
                float[] hsv = new float[3];
                Color.colorToHSV(px.getPixel(0, 0), hsv);
                hsv[2] = Math.max(0.16f, Math.min(0.40f, hsv[2] * 0.55f));
                top = Color.HSVToColor(hsv);
            } catch (Exception ignored) {
            }
        }
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{top, 0xFF080B12});
        root.setBackground(bg);
    }

    private void showLibrary(boolean show, boolean animate) {
        if (show == libraryShown) return;
        libraryShown = show;
        hideKeyboard();
        final int h = root.getHeight();
        if (show) {
            libraryPage.setVisibility(View.VISIBLE);
            if (animate) {
                libraryPage.setTranslationY(h);
                anim(libraryPage).translationY(0).setDuration(260).start();
            } else {
                libraryPage.setTranslationY(0);
            }
        } else {
            if (animate) {
                anim(libraryPage).translationY(h).setDuration(260).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (!libraryShown) libraryPage.setVisibility(View.GONE);
                    }
                }).start();
            } else {
                libraryPage.setVisibility(View.GONE);
            }
        }
    }

    // ------------------------------------------------------------------ home / tabs

    private void showTab(int t) {
        tab = t;
        inDetail = false;
        hideKeyboard();
        updateNav();
        if (t == TAB_HOME) {
            swapPage(pageHome);
            refreshHome();
            animateTiles();
        } else {
            swapPage(pageList);
            btnBack.setVisibility(View.GONE);
            showPlays = false;
            if (t == TAB_SEARCH) {
                listTitle.setText("Search");
                chips.setVisibility(View.GONE);
                search.setVisibility(View.VISIBLE);
                emptyMsg = "No matches";
                setGroupMode(false);
                filter(search.getText().toString());
            } else {
                listTitle.setText("Library");
                chips.setVisibility(View.VISIBLE);
                search.setVisibility(View.GONE);
                applyLibMode();
            }
        }
    }

    private void updateNav() {
        for (int i = 0; i < 3; i++) {
            int c = i == tab ? ACCENT : NAV_OFF;
            navIcons[i].setColorOverride(c);
            navLabels[i].setTextColor(c);
        }
    }

    private void swapPage(View target) {
        View[] pages = {pageHome, pageList};
        for (View p : pages) {
            if (p != target) p.setVisibility(View.GONE);
        }
        target.setVisibility(View.VISIBLE);
        target.setAlpha(0f);
        anim(target).alpha(1f).setDuration(180).start();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            im.hideSoftInputFromWindow(search.getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }

    private void applyLibMode() {
        styleChip(chipSongs, libMode == LIB_SONGS);
        styleChip(chipAlbums, libMode == LIB_ALBUMS);
        styleChip(chipArtists, libMode == LIB_ARTISTS);
        showPlays = false;
        if (libMode == LIB_SONGS) {
            shown.clear();
            shown.addAll(all);
            emptyMsg = "No songs found on this phone";
            setGroupMode(false);
        } else {
            buildGroups(libMode == LIB_ALBUMS);
            emptyMsg = "Nothing here";
            setGroupMode(true);
        }
    }

    private void styleChip(TextView c, boolean on) {
        c.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip);
        c.setTextColor(on ? 0xFF0B1220 : 0xFFC9D1DE);
    }

    private void setGroupMode(boolean on) {
        groupMode = on;
        list.setAdapter(on ? (BaseAdapter) groupAdapter : songAdapter);
        updateCount();
    }

    private void updateCount() {
        int n = groupMode ? groups.size() : shown.size();
        String word = groupMode ? (libMode == LIB_ALBUMS ? "album" : "artist") : "song";
        listCount.setText(n + " " + word + (n == 1 ? "" : "s"));
        emptyView.setText(all.isEmpty() ? "No songs found on this phone" : emptyMsg);
        emptyView.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
    }

    /** Opens a list of songs (a tile, an album or an artist) with a back arrow. */
    private void openDetail(String name, List<Song> songs, boolean plays, String empty) {
        inDetail = true;
        hideKeyboard();
        swapPage(pageList);
        btnBack.setVisibility(View.VISIBLE);
        chips.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        listTitle.setText(name);
        showPlays = plays;
        emptyMsg = empty;
        shown.clear();
        shown.addAll(songs);
        setGroupMode(false);
    }

    private void filter(String q) {
        String needle = q.trim().toLowerCase(Locale.getDefault());
        if (tab != TAB_SEARCH || inDetail) return;
        shown.clear();
        for (Song s : all) {
            if (needle.length() == 0
                    || s.title.toLowerCase(Locale.getDefault()).contains(needle)
                    || s.artist.toLowerCase(Locale.getDefault()).contains(needle)
                    || s.album.toLowerCase(Locale.getDefault()).contains(needle)) {
                shown.add(s);
            }
        }
        songAdapter.notifyDataSetChanged();
        updateCount();
    }

    // ------------------------------------------------------------------ collections

    private void buildGroups(boolean albums) {
        groups.clear();
        Map<String, Group> map = new LinkedHashMap<String, Group>();
        for (Song s : all) {
            String key = albums ? String.valueOf(s.albumId) : s.artist.toLowerCase(Locale.getDefault());
            Group g = map.get(key);
            if (g == null) {
                g = new Group();
                g.name = albums ? s.album : s.artist;
                g.sub = albums ? s.artist : "";
                g.albumId = s.albumId;
                map.put(key, g);
            }
            g.songs.add(s);
        }
        groups.addAll(map.values());
        Collections.sort(groups, new Comparator<Group>() {
            @Override
            public int compare(Group a, Group b) {
                return a.name.compareToIgnoreCase(b.name);
            }
        });
    }

    private List<Song> collection(int kind) {
        final Context c = this;
        List<Song> out = new ArrayList<Song>();
        switch (kind) {
            case K_FAV:
                for (Song s : all) {
                    if (favs.contains(String.valueOf(s.id))) out.add(s);
                }
                break;
            case K_TOP:
                for (Song s : all) {
                    if (Stats.plays(c, s.id) > 0) out.add(s);
                }
                Collections.sort(out, new Comparator<Song>() {
                    @Override
                    public int compare(Song a, Song b) {
                        return Stats.plays(c, b.id) - Stats.plays(c, a.id);
                    }
                });
                break;
            case K_ADDED:
                out.addAll(all);
                Collections.sort(out, new Comparator<Song>() {
                    @Override
                    public int compare(Song a, Song b) {
                        return a.added == b.added ? 0 : (a.added < b.added ? 1 : -1);
                    }
                });
                break;
            case K_RECENT:
                for (Song s : all) {
                    if (Stats.lastPlayed(c, s.id) > 0) out.add(s);
                }
                Collections.sort(out, new Comparator<Song>() {
                    @Override
                    public int compare(Song a, Song b) {
                        long x = Stats.lastPlayed(c, a.id), y = Stats.lastPlayed(c, b.id);
                        return x == y ? 0 : (x < y ? 1 : -1);
                    }
                });
                break;
            default:
                out.addAll(all);
                break;
        }
        int limit = kind == K_ADDED ? 100 : (kind == K_TOP || kind == K_RECENT) ? 50 : Integer.MAX_VALUE;
        if (out.size() > limit) return new ArrayList<Song>(out.subList(0, limit));
        return out;
    }

    private int countDistinct(boolean albums) {
        Set<String> set = new HashSet<String>();
        for (Song s : all) {
            set.add(albums ? String.valueOf(s.albumId) : s.artist.toLowerCase(Locale.getDefault()));
        }
        return set.size();
    }

    private static String plural(int n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    private String tileSub(int kind) {
        switch (kind) {
            case K_ALBUMS:
                return plural(countDistinct(true), "album");
            case K_ARTISTS:
                return plural(countDistinct(false), "artist");
            case K_SHUFFLE:
                return "Play everything";
            default:
                return plural(collection(kind).size(), "song");
        }
    }

    private void refreshGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = hour < 5 ? "Good night" : hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        greeting.setText(g);
    }

    private void refreshHome() {
        homeSub.setText(all.isEmpty() ? "No songs found on this phone"
                : plural(all.size(), "song") + " on your phone");
        for (int i = 0; i < tileCounts.length; i++) {
            if (tileCounts[i] != null) tileCounts[i].setText(tileSub(i));
        }
    }

    // ------------------------------------------------------------------ tiles

    private void buildTiles() {
        tiles.removeAllViews();
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int pad = dp(10), gap = dp(6);
        int tileW = (screenW - 2 * pad - 4 * gap) / 2;
        int tileH = (int) (tileW * 0.88f);
        tiles.setPadding(pad, 0, pad, 0);
        for (int i = 0; i < 8; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < i + 2; j++) {
                View t = makeTile(j);
                tileViews[j] = t;
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tileW, tileH);
                lp.setMargins(gap, gap, gap, gap);
                row.addView(t, lp);
            }
            tiles.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        refreshGreeting();
        refreshHome();
    }

    private View makeTile(final int kind) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{TILE_BG1[kind], TILE_BG2[kind]});
        bg.setCornerRadius(dp(22));
        f.setBackground(bg);

        IconView ic = new IconView(this);
        ic.setIcon(TILE_ICONS[kind]);
        ic.setCircle(true);
        ic.setColorOverride(TILE_ACCENT[kind]);
        ic.setClickable(false);
        FrameLayout.LayoutParams ilp = new FrameLayout.LayoutParams(dp(58), dp(58));
        ilp.gravity = Gravity.TOP | Gravity.LEFT;
        ilp.setMargins(dp(16), dp(16), 0, 0);
        f.addView(ic, ilp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(TILE_NAMES[kind]);
        name.setTextColor(Color.WHITE);
        name.setTextSize(16);
        name.setTypeface(name.getTypeface(), android.graphics.Typeface.BOLD);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        TextView count = new TextView(this);
        count.setTextColor(0xB3FFFFFF);
        count.setTextSize(12);
        count.setSingleLine(true);
        texts.addView(name);
        texts.addView(count);
        FrameLayout.LayoutParams tlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.gravity = Gravity.BOTTOM;
        tlp.setMargins(dp(16), 0, dp(12), dp(14));
        f.addView(texts, tlp);
        tileCounts[kind] = count;

        f.setClickable(true);
        f.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTile(kind);
            }
        });
        f.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int a = e.getAction();
                if (a == MotionEvent.ACTION_DOWN) {
                    anim(v).scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
                } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                    anim(v).scaleX(1f).scaleY(1f).setDuration(140).start();
                }
                return false;
            }
        });
        return f;
    }

    private void animateTiles() {
        for (int i = 0; i < tileViews.length; i++) {
            View v = tileViews[i];
            if (v == null) continue;
            v.setScaleX(1f);
            v.setScaleY(1f);
            v.setAlpha(0f);
            v.setTranslationY(dp(22));
            v.animate().setListener(null).setStartDelay(i * 45L).alpha(1f).translationY(0)
                    .setDuration(280).start();
        }
    }

    private void onTile(int kind) {
        switch (kind) {
            case K_SHUFFLE:
                playShuffled();
                break;
            case K_ALBUMS:
                libMode = LIB_ALBUMS;
                showTab(TAB_LIBRARY);
                break;
            case K_ARTISTS:
                libMode = LIB_ARTISTS;
                showTab(TAB_LIBRARY);
                break;
            case K_FAV:
                openDetail("Favorites", collection(kind), false,
                        "Tap the heart on the player to add favorites");
                break;
            case K_TOP:
                openDetail("Top Played", collection(kind), true, "Play some songs first");
                break;
            case K_RECENT:
                openDetail("Recently Played", collection(kind), false, "Play some songs first");
                break;
            case K_ADDED:
                openDetail("Recently Added", collection(kind), false, "No songs found on this phone");
                break;
            default:
                openDetail("All Songs", collection(kind), false, "No songs found on this phone");
                break;
        }
    }

    private void playShuffled() {
        if (all.isEmpty()) {
            Toast.makeText(this, "No songs found on this phone", Toast.LENGTH_SHORT).show();
            return;
        }
        if (svc == null) {
            Toast.makeText(this, "Starting player...", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Song> q = new ArrayList<Song>(all);
        Collections.shuffle(q);
        if (!svc.isShuffle()) svc.toggleShuffle();
        svc.setQueue(q, 0);
        showLibrary(false, true);
    }

    // ------------------------------------------------------------------ list adapters

    private class SongAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Song getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return shown.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_song, parent, false);
            }
            Song s = shown.get(position);
            ImageView rowArt = (ImageView) v.findViewById(R.id.row_art);
            TextView t = (TextView) v.findViewById(R.id.row_title);
            TextView a = (TextView) v.findViewById(R.id.row_artist);
            TextView d = (TextView) v.findViewById(R.id.row_time);
            t.setText(s.title);
            String sub = s.artist;
            if (showPlays) {
                int n = Stats.plays(MainActivity.this, s.id);
                if (n > 0) sub += " · " + plural(n, "play");
            }
            a.setText(sub);
            d.setText(Song.fmt(s.duration));
            Song cur = svc == null ? null : svc.current();
            t.setTextColor(cur != null && cur.id == s.id ? ACCENT : 0xFFF2F4F8);
            ArtLoader.load(MainActivity.this, s.albumId, rowArt, (int) (100 * density / 2), (int) (10 * density / 2), null);
            return v;
        }
    }

    private class GroupAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return groups.size();
        }

        @Override
        public Group getItem(int position) {
            return groups.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = LayoutInflater.from(MainActivity.this).inflate(R.layout.row_song, parent, false);
            }
            Group g = groups.get(position);
            ImageView rowArt = (ImageView) v.findViewById(R.id.row_art);
            TextView t = (TextView) v.findViewById(R.id.row_title);
            TextView a = (TextView) v.findViewById(R.id.row_artist);
            TextView d = (TextView) v.findViewById(R.id.row_time);
            t.setText(g.name);
            t.setTextColor(0xFFF2F4F8);
            String count = plural(g.songs.size(), "song");
            a.setText(g.sub.length() > 0 ? g.sub + " · " + count : count);
            d.setText("");
            ArtLoader.load(MainActivity.this, g.albumId, rowArt, (int) (100 * density / 2), (int) (10 * density / 2), null);
            return v;
        }
    }
}
