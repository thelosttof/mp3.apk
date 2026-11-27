package com.oldplayer.music;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Loads album art off the UI thread, crops it square, rounds the corners and caches it. */
public class ArtLoader {

    public interface Callback {
        /** @param real false when no embedded/album art exists and a placeholder was used */
        void onArt(Bitmap bitmap, boolean real);
    }

    private static final Uri ART = Uri.parse("content://media/external/audio/albumart");
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(30);
    private static final Set<Long> MISSING = Collections.synchronizedSet(new HashSet<Long>());

    public static void load(final Context ctx, final long albumId, final ImageView iv,
                            final int size, final int radius, final Callback cb) {
        final String key = albumId + "_" + size + "_" + radius;
        iv.setTag(key);
        Bitmap hit = CACHE.get(key);
        if (hit != null) {
            iv.setImageBitmap(hit);
            if (cb != null) cb.onArt(hit, !MISSING.contains(albumId));
            return;
        }
        iv.setImageDrawable(null);
        new AsyncTask<Void, Void, Bitmap>() {
            private boolean real;

            @Override
            protected Bitmap doInBackground(Void... params) {
                Bitmap raw = MISSING.contains(albumId) ? null : decode(ctx, albumId, size);
                if (raw == null) {
                    MISSING.add(albumId);
                    real = false;
                    raw = placeholder(size);
                } else {
                    real = true;
                }
                return round(raw, size, radius);
            }

            @Override
            protected void onPostExecute(Bitmap b) {
                CACHE.put(key, b);
                if (key.equals(iv.getTag())) {
                    iv.setImageBitmap(b);
                    if (cb != null) cb.onArt(b, real);
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private static Bitmap decode(Context ctx, long albumId, int target) {
        Uri uri = ContentUris.withAppendedId(ART, albumId);
        InputStream in = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            in = ctx.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            int sample = 1;
            while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) {
                sample *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = ctx.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(in, null, o2);
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Center-crops to a square of `size` px with rounded corners. */
    private static Bitmap round(Bitmap src, int size, int radius) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = Math.max((float) size / src.getWidth(), (float) size / src.getHeight());
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate((size - src.getWidth() * scale) / 2f, (size - src.getHeight() * scale) / 2f);
        shader.setLocalMatrix(m);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(shader);
        ColorMatrix gray = new ColorMatrix();
        gray.setSaturation(0f);
        p.setColorFilter(new ColorMatrixColorFilter(gray));
        canvas.drawRoundRect(new RectF(0, 0, size, size), radius, radius, p);
        return out;
    }

    /** Grey gradient with a music note, used when a song has no cover. */
    private static Bitmap placeholder(int s) {
        Bitmap b = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, s, s, 0xFF4A4A4A, 0xFF0D0D0D, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, s, s, p);
        p.setShader(null);
        p.setColor(0xD9FFFFFF);
        p.setStyle(Paint.Style.FILL);
        c.drawOval(new RectF(s * 0.30f, s * 0.60f, s * 0.48f, s * 0.74f), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.03f);
        c.drawLine(s * 0.47f, s * 0.67f, s * 0.47f, s * 0.28f, p);
        Path flag = new Path();
        flag.moveTo(s * 0.47f, s * 0.28f);
        flag.cubicTo(s * 0.52f, s * 0.38f, s * 0.64f, s * 0.38f, s * 0.64f, s * 0.50f);
        c.drawPath(flag, p);
        return b;
    }
}
