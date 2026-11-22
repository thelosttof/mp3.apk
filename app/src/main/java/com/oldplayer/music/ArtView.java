package com.oldplayer.music;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/** An ImageView that is always square and as large as the space allows. */
public class ArtView extends ImageView {
    public ArtView(Context context) {
        super(context);
    }

    public ArtView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        if (MeasureSpec.getMode(heightSpec) == MeasureSpec.UNSPECIFIED) h = w;
        int side = Math.min(w, h);
        setMeasuredDimension(side, side);
    }
}
