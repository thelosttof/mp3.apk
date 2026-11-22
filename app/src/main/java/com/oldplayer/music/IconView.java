package com.oldplayer.music;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** A button whose icon is drawn in code, so no image assets or vector support are needed (works on 4.4). */
public class IconView extends View {
    public static final int PLAY = 0, PAUSE = 1, NEXT = 2, PREV = 3, SHUFFLE = 4, REPEAT = 5,
            HEART = 6, MOON = 7, MORE = 8, UP = 9, DOWN = 10,
            NOTE = 11, CLOCK = 12, DISC = 13, PERSON = 14, TREND = 15, STAR = 16, SEARCH = 17,
            HOME = 18, LIBRARY = 19, BACK = 20;

    private static final int DARK = 0xFF0B1220;

    private int icon = PLAY;
    private boolean circle;
    private boolean active = true;
    private boolean repeatOne;
    private boolean filled;
    private int colorOverride;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public IconView(Context context) {
        super(context);
        init();
    }

    public IconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        text.setTextAlign(Paint.Align.CENTER);
        text.setFakeBoldText(true);
        setClickable(true);
    }

    public void setIcon(int icon) {
        this.icon = icon;
        invalidate();
    }

    public void setCircle(boolean circle) {
        this.circle = circle;
        invalidate();
    }

    public void setActive(boolean active) {
        this.active = active;
        invalidate();
    }

    public void setRepeatOne(boolean repeatOne) {
        this.repeatOne = repeatOne;
        invalidate();
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
        invalidate();
    }

    /** 0 = default white. */
    public void setColorOverride(int color) {
        this.colorOverride = color;
        invalidate();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h);
        float cx = w / 2f, cy = h / 2f;

        int fg = colorOverride != 0 ? colorOverride : (active ? Color.WHITE : 0x80FFFFFF);
        if (isPressed()) fg = (fg & 0x00FFFFFF) | 0x99000000;

        float sw = s * 0.055f;
        stroke.setStrokeWidth(sw);
        stroke.setColor(fg);
        fill.setColor(fg);

        if (circle) {
            fill.setColor(0x33FFFFFF);
            canvas.drawCircle(cx, cy, s / 2f - 1f, fill);
            fill.setColor(fg);
        }

        float a = s * 0.20f;

        switch (icon) {
            case PLAY:
            case PAUSE: {
                float half = s / 2f - s * 0.03f;
                fill.setColor(isPressed() ? 0xCCFFFFFF : Color.WHITE);
                canvas.drawRoundRect(new RectF(cx - half, cy - half, cx + half, cy + half),
                        half * 0.5f, half * 0.5f, fill);
                fill.setColor(DARK);
                if (icon == PAUSE) {
                    float bw = s * 0.085f, bh = s * 0.32f, gap = s * 0.075f;
                    canvas.drawRoundRect(new RectF(cx - gap / 2 - bw, cy - bh / 2, cx - gap / 2, cy + bh / 2),
                            bw / 3, bw / 3, fill);
                    canvas.drawRoundRect(new RectF(cx + gap / 2, cy - bh / 2, cx + gap / 2 + bw, cy + bh / 2),
                            bw / 3, bw / 3, fill);
                } else {
                    path.reset();
                    path.moveTo(cx - s * 0.09f, cy - s * 0.17f);
                    path.lineTo(cx - s * 0.09f, cy + s * 0.17f);
                    path.lineTo(cx + s * 0.18f, cy);
                    path.close();
                    canvas.drawPath(path, fill);
                }
                break;
            }
            case NEXT:
                drawNext(canvas, cx, cy, s);
                break;
            case PREV:
                canvas.save();
                canvas.scale(-1f, 1f, cx, cy);
                drawNext(canvas, cx, cy, s);
                canvas.restore();
                break;
            case SHUFFLE: {
                float b = a * 0.7f;
                float t = sw * 1.7f;
                canvas.drawLine(cx - a, cy - b, cx + a - t, cy + b, stroke);
                canvas.drawLine(cx - a, cy + b, cx + a - t, cy - b, stroke);
                arrow(canvas, cx + a, cy + b, 1, t);
                arrow(canvas, cx + a, cy - b, 1, t);
                break;
            }
            case REPEAT: {
                float b = a * 0.62f;
                float t = sw * 1.7f;
                float d = a * 0.35f;
                path.reset();
                path.moveTo(cx - a, cy + d);
                path.lineTo(cx - a, cy - b);
                path.lineTo(cx + a - t, cy - b);
                canvas.drawPath(path, stroke);
                arrow(canvas, cx + a, cy - b, 1, t);
                path.reset();
                path.moveTo(cx + a, cy - d);
                path.lineTo(cx + a, cy + b);
                path.lineTo(cx - a + t, cy + b);
                canvas.drawPath(path, stroke);
                arrow(canvas, cx - a, cy + b, -1, t);
                if (repeatOne) {
                    text.setColor(fg);
                    text.setTextSize(s * 0.24f);
                    canvas.drawText("1", cx, cy + s * 0.085f, text);
                }
                break;
            }
            case HEART: {
                float r = s * 0.13f;
                path.reset();
                path.moveTo(cx, cy + r * 1.25f);
                path.cubicTo(cx - r * 2.3f, cy - r * 0.2f, cx - r * 0.9f, cy - r * 1.9f, cx, cy - r * 0.55f);
                path.cubicTo(cx + r * 0.9f, cy - r * 1.9f, cx + r * 2.3f, cy - r * 0.2f, cx, cy + r * 1.25f);
                path.close();
                if (filled) {
                    fill.setColor(0xFFFF5C7A);
                    canvas.drawPath(path, fill);
                } else {
                    canvas.drawPath(path, stroke);
                }
                break;
            }
            case MOON: {
                float r = s * 0.20f;
                Path outer = new Path();
                outer.addCircle(cx, cy, r, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(cx + r * 0.55f, cy - r * 0.35f, r * 0.85f, Path.Direction.CW);
                outer.op(cut, Path.Op.DIFFERENCE);
                canvas.drawPath(outer, fill);
                break;
            }
            case MORE: {
                float r = s * 0.045f;
                float gap = s * 0.13f;
                canvas.drawCircle(cx - gap, cy, r, fill);
                canvas.drawCircle(cx, cy, r, fill);
                canvas.drawCircle(cx + gap, cy, r, fill);
                break;
            }
            case UP:
            case DOWN: {
                canvas.drawCircle(cx, cy, s / 2f - sw, stroke);
                float dir = icon == UP ? 1f : -1f;
                float k = a * 0.55f;
                path.reset();
                path.moveTo(cx - k, cy + dir * k * 0.45f);
                path.lineTo(cx, cy - dir * k * 0.45f);
                path.lineTo(cx + k, cy + dir * k * 0.45f);
                canvas.drawPath(path, stroke);
                break;
            }
            case NOTE: {
                float rx = s * 0.085f, ry = s * 0.06f;
                float x1 = cx - s * 0.15f, y1 = cy + s * 0.17f;
                float x2 = cx + s * 0.15f, y2 = cy + s * 0.11f;
                canvas.drawOval(new RectF(x1 - rx, y1 - ry, x1 + rx, y1 + ry), fill);
                canvas.drawOval(new RectF(x2 - rx, y2 - ry, x2 + rx, y2 + ry), fill);
                stroke.setStrokeWidth(sw * 1.3f);
                float t1 = cy - s * 0.20f, t2 = cy - s * 0.26f;
                float sx1 = x1 + rx * 0.85f, sx2 = x2 + rx * 0.85f;
                canvas.drawLine(sx1, y1, sx1, t1, stroke);
                canvas.drawLine(sx2, y2, sx2, t2, stroke);
                canvas.drawLine(sx1, t1, sx2, t2, stroke);
                break;
            }
            case CLOCK: {
                canvas.drawCircle(cx, cy, s * 0.27f, stroke);
                canvas.drawLine(cx, cy, cx, cy - s * 0.15f, stroke);
                canvas.drawLine(cx, cy, cx + s * 0.11f, cy + s * 0.07f, stroke);
                break;
            }
            case DISC: {
                canvas.drawCircle(cx, cy, s * 0.27f, stroke);
                canvas.drawCircle(cx, cy, s * 0.12f, stroke);
                canvas.drawCircle(cx, cy, s * 0.035f, fill);
                break;
            }
            case PERSON: {
                canvas.drawCircle(cx, cy - s * 0.11f, s * 0.10f, fill);
                canvas.drawArc(new RectF(cx - s * 0.23f, cy + s * 0.03f, cx + s * 0.23f, cy + s * 0.49f),
                        180f, 180f, true, fill);
                break;
            }
            case TREND: {
                stroke.setStrokeWidth(sw * 1.3f);
                path.reset();
                path.moveTo(cx - s * 0.26f, cy + s * 0.14f);
                path.lineTo(cx - s * 0.08f, cy - s * 0.04f);
                path.lineTo(cx + s * 0.04f, cy + s * 0.08f);
                path.lineTo(cx + s * 0.24f, cy - s * 0.16f);
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(cx + s * 0.10f, cy - s * 0.16f);
                path.lineTo(cx + s * 0.24f, cy - s * 0.16f);
                path.lineTo(cx + s * 0.24f, cy - s * 0.02f);
                canvas.drawPath(path, stroke);
                break;
            }
            case STAR: {
                float outer = s * 0.28f, inner = s * 0.12f;
                path.reset();
                for (int i = 0; i < 10; i++) {
                    double ang = -Math.PI / 2 + i * Math.PI / 5;
                    float rad = (i % 2 == 0) ? outer : inner;
                    float px = cx + (float) Math.cos(ang) * rad;
                    float py = cy + s * 0.015f + (float) Math.sin(ang) * rad;
                    if (i == 0) path.moveTo(px, py);
                    else path.lineTo(px, py);
                }
                path.close();
                canvas.drawPath(path, fill);
                break;
            }
            case SEARCH: {
                stroke.setStrokeWidth(sw * 1.2f);
                canvas.drawCircle(cx - s * 0.04f, cy - s * 0.04f, s * 0.17f, stroke);
                canvas.drawLine(cx + s * 0.09f, cy + s * 0.09f, cx + s * 0.24f, cy + s * 0.24f, stroke);
                break;
            }
            case HOME: {
                stroke.setStrokeWidth(sw * 1.2f);
                path.reset();
                path.moveTo(cx - s * 0.26f, cy - s * 0.02f);
                path.lineTo(cx, cy - s * 0.26f);
                path.lineTo(cx + s * 0.26f, cy - s * 0.02f);
                canvas.drawPath(path, stroke);
                path.reset();
                path.moveTo(cx - s * 0.17f, cy - s * 0.07f);
                path.lineTo(cx - s * 0.17f, cy + s * 0.23f);
                path.lineTo(cx + s * 0.17f, cy + s * 0.23f);
                path.lineTo(cx + s * 0.17f, cy - s * 0.07f);
                canvas.drawPath(path, stroke);
                break;
            }
            case LIBRARY: {
                stroke.setStrokeWidth(sw * 1.3f);
                canvas.drawLine(cx - s * 0.22f, cy - s * 0.2f, cx - s * 0.22f, cy + s * 0.2f, stroke);
                canvas.drawLine(cx - s * 0.07f, cy - s * 0.2f, cx - s * 0.07f, cy + s * 0.2f, stroke);
                canvas.drawLine(cx + s * 0.07f, cy - s * 0.18f, cx + s * 0.22f, cy + s * 0.2f, stroke);
                break;
            }
            case BACK: {
                stroke.setStrokeWidth(sw * 1.3f);
                path.reset();
                path.moveTo(cx + s * 0.06f, cy - s * 0.18f);
                path.lineTo(cx - s * 0.10f, cy);
                path.lineTo(cx + s * 0.06f, cy + s * 0.18f);
                canvas.drawPath(path, stroke);
                break;
            }
            default:
                break;
        }
    }

    private void drawNext(Canvas canvas, float cx, float cy, float s) {
        float a = s * 0.17f;
        path.reset();
        path.moveTo(cx - a * 1.0f, cy - a);
        path.lineTo(cx - a * 1.0f, cy + a);
        path.lineTo(cx + a * 0.55f, cy);
        path.close();
        canvas.drawPath(path, fill);
        float bw = s * 0.05f;
        canvas.drawRoundRect(new RectF(cx + a * 0.8f, cy - a, cx + a * 0.8f + bw, cy + a), bw / 2, bw / 2, fill);
    }

    private void arrow(Canvas canvas, float x, float y, int dir, float size) {
        path.reset();
        path.moveTo(x, y);
        path.lineTo(x - dir * size, y - size * 0.95f);
        path.lineTo(x - dir * size, y + size * 0.95f);
        path.close();
        canvas.drawPath(path, fill);
    }
}
