package com.hk1089.mettax.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class SquareRelativeLayout extends RelativeLayout {
    public SquareRelativeLayout(Context ctx) { super(ctx); }
    public SquareRelativeLayout(Context ctx, AttributeSet attrs) { super(ctx, attrs); }
    public SquareRelativeLayout(Context ctx, AttributeSet attrs, int defStyleAttr) { super(ctx, attrs, defStyleAttr); }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Force height = width
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
