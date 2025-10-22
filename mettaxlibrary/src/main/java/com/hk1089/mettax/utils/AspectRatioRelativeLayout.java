package com.hk1089.mettax.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

// Put this class in the same file or its own file.
public class AspectRatioRelativeLayout extends RelativeLayout {
    private float aspect = 16f / 9f; // default until real size arrives

    public AspectRatioRelativeLayout(Context ctx) { super(ctx); }
    public AspectRatioRelativeLayout(Context ctx, AttributeSet attrs) { super(ctx, attrs); }
    public AspectRatioRelativeLayout(Context ctx, AttributeSet attrs, int defStyleAttr) { super(ctx, attrs, defStyleAttr); }

    /** Set the desired aspect ratio as width / height. */
    public void setAspect(float width, float height) {
        if (width > 0 && height > 0) {
            this.aspect = width / height;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > 0 && aspect > 0f) {
            int targetHeight = (int) (width / aspect);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(targetHeight, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
